package com.sk.sample.order.entity;

import com.sk.framework.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * @className    : Order
 * @description  : 주문 엔티티. 멱등성/아웃박스/분산락 등 거래 패턴 시연용 도메인.
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
@Entity
@Table(name = "tb_order",
        uniqueConstraints = @UniqueConstraint(name = "uk_order_no", columnNames = "order_no"))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    /** 클라이언트가 생성하는 멱등한 주문번호 */
    @Column(name = "order_no", nullable = false, length = 50)
    private String orderNo;

    @Column(name = "customer_id", nullable = false, length = 50)
    private String customerId;

    @Column(name = "product_id", nullable = false, length = 50)
    private String productId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.CREATED;

    public enum OrderStatus {
        CREATED, PAID, CANCELLED
    }

    public void markPaid() {
        this.status = OrderStatus.PAID;
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
    }
}
