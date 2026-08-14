package com.sk.sample.product.entity;

import com.sk.framework.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

/**
 * @className    : Product
 * @description  : 상품 엔티티. BaseEntity를 상속하여 감사필드/낙관적 락(@Version)을 자동 적용한다.
 *                 수정은 setter가 아닌 도메인 메소드로만 수행하여 정합성을 유지한다.
 * @modification : 2026.08.14(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.14
 * @version      : 1.0
 */
@Entity
@Table(name = "tb_product")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @NotBlank(message = "상품명은 필수입니다.")
    @Size(max = 100, message = "상품명은 100자를 초과할 수 없습니다.")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "price", nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Column(name = "stock", nullable = false)
    @Builder.Default
    private int stock = 0;

    /** 상품 정보 수정. null 인자는 변경하지 않는다. */
    public void update(String name, BigDecimal price) {
        if (name != null) {
            this.name = name;
        }
        if (price != null) {
            this.price = price;
        }
    }

    /** 재고 차감. 부족하면 비즈니스 예외. */
    public void decreaseStock(int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("차감 수량은 1 이상이어야 합니다.");
        }
        if (qty > this.stock) {
            throw new IllegalArgumentException("재고가 부족합니다. (현재고=" + this.stock + ", 요청=" + qty + ")");
        }
        this.stock -= qty;
    }

    /** 재고 증가(입고). */
    public void increaseStock(int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("입고 수량은 1 이상이어야 합니다.");
        }
        this.stock += qty;
    }
}
