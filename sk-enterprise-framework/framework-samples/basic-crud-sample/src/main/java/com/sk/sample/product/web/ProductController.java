package com.sk.sample.product.web;

import com.sk.framework.common.dto.ApiResponse;
import com.sk.sample.product.dto.ProductDto;
import com.sk.sample.product.dto.ProductRequest;
import com.sk.sample.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @className    : ProductController
 * @description  : 상품 관리 REST 컨트롤러. ResponseEntity + ApiResponse 표준 응답,
 *                 Bean Validation, GlobalExceptionHandler 정책을 따른다.(try-catch 미사용)
 * @modification : 2026.08.14(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.14
 * @version      : 1.0
 */
@Tag(name = "상품 관리", description = "상품 CRUD 및 재고 처리 API")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    @Operation(summary = "상품 목록 조회", description = "keyword로 상품명 부분 검색이 가능합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductDto>>> list(
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.success(service.list(keyword)));
    }

    @Operation(summary = "상품 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.get(id)));
    }

    @Operation(summary = "상품 생성")
    @PostMapping
    public ResponseEntity<ApiResponse<ProductDto>> create(@Valid @RequestBody ProductRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("상품이 생성되었습니다.", service.create(req)));
    }

    @Operation(summary = "상품 수정", description = "낙관적 락 + 충돌 시 자동 재시도")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> update(@PathVariable Long id,
                                                          @Valid @RequestBody ProductRequest req) {
        return ResponseEntity.ok(ApiResponse.success("상품이 수정되었습니다.", service.update(id, req)));
    }

    @Operation(summary = "상품 구매(재고 차감)", description = "분산 락으로 동시 차감을 직렬화합니다.")
    @PostMapping("/{id}/purchase")
    public ResponseEntity<ApiResponse<ProductDto>> purchase(@PathVariable Long id,
                                                            @RequestBody Map<String, Integer> body) {
        int qty = body.getOrDefault("quantity", 1);
        return ResponseEntity.ok(ApiResponse.success("구매가 처리되었습니다.", service.purchase(id, qty)));
    }

    @Operation(summary = "상품 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("상품이 삭제되었습니다.", null));
    }
}
