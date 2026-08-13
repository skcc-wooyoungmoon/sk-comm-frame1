package com.sk.sample.order.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * @className    : PlaceOrderRequest
 * @description  : 주문 생성 요청 DTO. orderNo가 멱등키 역할을 한다.
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceOrderRequest {

    @NotBlank(message = "주문번호는 필수입니다.")
    @Size(max = 50)
    private String orderNo;

    @NotBlank(message = "고객 ID는 필수입니다.")
    private String customerId;

    @NotBlank(message = "상품 ID는 필수입니다.")
    private String productId;

    @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
    private int quantity;

    @NotNull(message = "금액은 필수입니다.")
    @DecimalMin(value = "0.0", inclusive = false, message = "금액은 0보다 커야 합니다.")
    private BigDecimal amount;
}
