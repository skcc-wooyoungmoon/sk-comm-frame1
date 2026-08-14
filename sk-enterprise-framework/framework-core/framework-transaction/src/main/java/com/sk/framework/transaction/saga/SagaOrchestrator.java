package com.sk.framework.transaction.saga;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @className    : SagaOrchestrator
 * @description  : 오케스트레이션 기반 SAGA 실행기.
 *                 등록된 단계를 순서대로 실행하고, 중간에 실패하면 이미 성공한 단계를
 *                 역순으로 보상(compensate)하여 최종 일관성을 맞춥니다.
 *
 * <pre>{@code
 * SagaContext ctx = new SagaContext("order-1001");
 * new SagaOrchestrator()
 *     .step(new ReserveStockStep())
 *     .step(new DebitPaymentStep())
 *     .step(new CreateShipmentStep())
 *     .execute(ctx);
 * }</pre>
 *
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
@Slf4j
public class SagaOrchestrator {

    private final List<SagaStep> steps = new ArrayList<>();

    /**
     * 사가 단계를 추가합니다. (등록 순서대로 실행)
     *
     * @param step 사가 단계
     * @return this (플루언트 체이닝)
     */
    public SagaOrchestrator step(SagaStep step) {
        steps.add(step);
        return this;
    }

    /**
     * 사가를 실행합니다.
     *
     * @param context 사가 컨텍스트
     * @throws SagaExecutionException 실패하여 보상까지 수행된 경우
     */
    public void execute(SagaContext context) {
        List<SagaStep> completed = new ArrayList<>();
        for (SagaStep step : steps) {
            try {
                log.debug("[SAGA:{}] 실행 - {}", context.getSagaId(), step.name());
                step.execute(context);
                completed.add(step);
            } catch (Exception ex) {
                log.error("[SAGA:{}] 단계 '{}' 실패 - 보상을 시작합니다.", context.getSagaId(), step.name(), ex);
                compensate(context, completed);
                throw new SagaExecutionException(
                        "사가 실행 실패 (단계=" + step.name() + ", sagaId=" + context.getSagaId() + ")", ex);
            }
        }
        log.debug("[SAGA:{}] 전체 단계 완료", context.getSagaId());
    }

    /**
     * sagaId 없이 실행합니다. (자동 UUID 부여)
     */
    public void execute() {
        execute(new SagaContext(UUID.randomUUID().toString()));
    }

    /**
     * 완료된 단계를 역순으로 보상합니다. 보상 자체의 실패는 로그로만 남기고 계속 진행합니다.
     */
    private void compensate(SagaContext context, List<SagaStep> completed) {
        for (int i = completed.size() - 1; i >= 0; i--) {
            SagaStep step = completed.get(i);
            try {
                log.debug("[SAGA:{}] 보상 - {}", context.getSagaId(), step.name());
                step.compensate(context);
            } catch (Exception ce) {
                // 보상 실패는 운영 개입이 필요한 심각 상황이므로 반드시 알림/모니터링 대상으로 처리.
                log.error("[SAGA:{}] 보상 실패 - 수동 개입 필요. 단계={}", context.getSagaId(), step.name(), ce);
            }
        }
    }
}
