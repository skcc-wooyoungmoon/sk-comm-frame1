package com.sk.sample.order.web;

import com.sk.framework.common.dto.ApiResponse;
import com.sk.sample.order.dto.OrderDto;
import com.sk.sample.order.dto.PlaceOrderRequest;
import com.sk.sample.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @className    : OrderController
 * @description  : 주문 API. 멱등성(정확히 한 번)·아웃박스 패턴 시연.
 *                 클라이언트는 요청 본문의 orderNo(또는 Idempotency-Key 헤더)로 중복 요청을 안전하게 재시도할 수 있다.
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
@Tag(name = "주문 관리", description = "멱등성·아웃박스 거래 패턴 시연 API")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "주문 생성", description = "동일 orderNo 재요청은 최초 결과를 그대로 반환합니다.(멱등)")
    @PostMapping
    public ResponseEntity<ApiResponse<OrderDto>> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        OrderDto order = orderService.placeOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("주문이 처리되었습니다.", order));
    }

    @Operation(summary = "주문 조회", description = "주문번호로 주문을 조회합니다.")
    @GetMapping("/{orderNo}")
    public ResponseEntity<ApiResponse<OrderDto>> getOrder(@PathVariable String orderNo) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrder(orderNo)));
    }
}
