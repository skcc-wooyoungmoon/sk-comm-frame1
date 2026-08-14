package com.sk.sample.product.service;

import com.sk.framework.common.exception.EntityNotFoundException;
import com.sk.framework.transaction.annotation.DistributedLock;
import com.sk.framework.transaction.annotation.RetryOnConflict;
import com.sk.sample.product.dto.ProductDto;
import com.sk.sample.product.dto.ProductRequest;
import com.sk.sample.product.entity.Product;
import com.sk.sample.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @className    : ProductService
 * @description  : 상품 서비스. 트랜잭션 정책(readOnly 기본 + 쓰기 override)과 도메인 메소드 기반 수정으로
 *                 낙관적 락을 유지한다. 재고 차감처럼 정합성이 중요한 처리는 분산 락 + 재시도를 적용한다.
 * @modification : 2026.08.14(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.14
 * @version      : 1.0
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;

    public List<ProductDto> list(String keyword) {
        List<Product> products = (keyword == null || keyword.isBlank())
                ? repository.findAll()
                : repository.findByNameContaining(keyword);
        return products.stream().map(ProductDto::from).toList();
    }

    public ProductDto get(Long id) {
        return ProductDto.from(find(id));
    }

    @Transactional
    public ProductDto create(ProductRequest req) {
        Product product = Product.builder()
                .name(req.getName())
                .price(req.getPrice())
                .stock(req.getStock())
                .build();
        return ProductDto.from(repository.save(product));
    }

    @RetryOnConflict(maxAttempts = 3)
    @Transactional
    public ProductDto update(Long id, ProductRequest req) {
        Product product = find(id);
        product.update(req.getName(), req.getPrice());   // dirty checking → 커밋 시 반영
        return ProductDto.from(product);
    }

    /**
     * 재고 차감(구매). 동일 상품 동시 차감을 분산 락으로 직렬화하고, 낙관적 락 충돌 시 재시도한다.
     */
    @DistributedLock(key = "'product-stock:' + #id", waitTime = 5, leaseTime = 10, timeUnit = TimeUnit.SECONDS)
    @RetryOnConflict(maxAttempts = 3)
    @Transactional
    public ProductDto purchase(Long id, int qty) {
        Product product = find(id);
        product.decreaseStock(qty);
        return ProductDto.from(product);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Product", id);
        }
        repository.deleteById(id);
    }

    private Product find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product", id));
    }
}
