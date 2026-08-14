package com.sk.sample.order.dto;

import com.sk.sample.order.entity.Order;
import lombok.*;

import java.math.BigDecimal;

/**
 * @className    : OrderDto
 * @description  : 주문 응답 DTO.
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDto {

    private Long id;
    private String orderNo;
    private String customerId;
    private String productId;
    private int quantity;
    private BigDecimal amount;
    private String status;
    private String createdAt;

    public static OrderDto from(Order order) {
        return OrderDto.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .customerId(order.getCustomerId())
                .productId(order.getProductId())
                .quantity(order.getQuantity())
                .amount(order.getAmount())
                .status(order.getStatus().name())
                .createdAt(order.getCreatedAt() != null ? order.getCreatedAt().toString() : null)
                .build();
    }
}
