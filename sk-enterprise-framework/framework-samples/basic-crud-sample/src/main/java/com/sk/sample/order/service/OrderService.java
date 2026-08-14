package com.sk.sample.order.service;

import com.sk.framework.transaction.annotation.DistributedLock;
import com.sk.framework.transaction.annotation.Idempotent;
import com.sk.framework.transaction.outbox.OutboxRecorder;
import com.sk.sample.order.dto.OrderDto;
import com.sk.sample.order.dto.PlaceOrderRequest;
import com.sk.sample.order.entity.Order;
import com.sk.sample.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @className    : OrderService
 * @description  : 주문 서비스. 거래 패턴을 한 메소드에서 종합 시연한다.
 *                 - {@code @DistributedLock} : 동일 상품 동시 주문 직렬화
 *                 - {@code @Idempotent}      : 동일 주문번호(orderNo) 중복 요청 방지(정확히 한 번 처리)
 *                 - {@code @Transactional}   : 주문 저장 + 아웃박스 이벤트 기록을 원자적으로 커밋
 *                 - {@code OutboxRecorder}   : 커밋과 동일 트랜잭션으로 이벤트 저장(이중 쓰기 방지)
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxRecorder outboxRecorder;

    /**
     * 주문을 생성한다. 동일 orderNo의 중복 요청은 최초 결과를 그대로 반환한다.
     */
    @DistributedLock(key = "'order-product:' + #request.productId")
    @Idempotent(key = "'order:' + #request.orderNo")
    @Transactional
    public OrderDto placeOrder(PlaceOrderRequest request) {
        // 멱등 어드바이스가 1차 방어선이지만, 유니크 제약과 명시적 확인으로 2차 방어선을 둔다.
        if (orderRepository.existsByOrderNo(request.getOrderNo())) {
            log.info("이미 존재하는 주문번호 - 기존 주문 반환. orderNo={}", request.getOrderNo());
            return OrderDto.from(orderRepository.findByOrderNo(request.getOrderNo()).orElseThrow());
        }

        Order order = Order.builder()
                .orderNo(request.getOrderNo())
                .customerId(request.getCustomerId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .amount(request.getAmount())
                .status(Order.OrderStatus.CREATED)
                .build();
        Order saved = orderRepository.save(order);

        // 비즈니스 데이터 변경과 "같은 트랜잭션"으로 이벤트 기록 → 커밋 시 함께 원자적 반영
        String payload = String.format(
                "{\"orderNo\":\"%s\",\"customerId\":\"%s\",\"amount\":%s}",
                saved.getOrderNo(), saved.getCustomerId(), saved.getAmount());
        outboxRecorder.record("Order", saved.getOrderNo(), "OrderPlaced", payload);

        log.info("주문 생성 완료. orderNo={}", saved.getOrderNo());
        return OrderDto.from(saved);
    }

    public OrderDto getOrder(String orderNo) {
        return OrderDto.from(orderRepository.findByOrderNo(orderNo).orElseThrow());
    }
}
