package com.sk.sample.product.repository;

import com.sk.sample.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @className    : ProductRepository
 * @description  : 상품 저장소 - 도메인별 Repository 구현.
 * @modification : 2026.08.14(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.14
 * @version      : 1.0
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /** 상품명 부분 검색 */
    List<Product> findByNameContaining(String name);
}
