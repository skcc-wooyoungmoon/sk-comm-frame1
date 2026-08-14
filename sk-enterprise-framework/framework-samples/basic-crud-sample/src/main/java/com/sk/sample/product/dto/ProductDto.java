package com.sk.sample.product.dto;

import com.sk.sample.product.entity.Product;
import lombok.*;

import java.math.BigDecimal;

/**
 * @className    : ProductDto
 * @description  : 상품 응답 DTO - 응답 전용 DTO.
 * @modification : 2026.08.14(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.14
 * @version      : 1.0
 */
@Getter
@AllArgsConstructor
@Builder
public class ProductDto {

    private Long id;
    private String name;
    private BigDecimal price;
    private int stock;
    private Long version;
    private String createdAt;

    public static ProductDto from(Product p) {
        return ProductDto.builder()
                .id(p.getId())
                .name(p.getName())
                .price(p.getPrice())
                .stock(p.getStock())
                .version(p.getVersion())
                .createdAt(p.getCreatedAt() != null ? p.getCreatedAt().toString() : null)
                .build();
    }
}
