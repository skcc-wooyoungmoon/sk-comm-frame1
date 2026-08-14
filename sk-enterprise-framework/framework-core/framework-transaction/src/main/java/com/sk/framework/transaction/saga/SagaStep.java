package com.sk.framework.transaction.saga;

/**
 * @className    : SagaStep
 * @description  : SAGA의 단일 단계. 실행(action)과 보상(compensation)을 한 쌍으로 정의합니다.
 *                 분산 트랜잭션에서 2PC 대신 "로컬 트랜잭션 + 실패 시 역순 보상"으로
 *                 최종 일관성(eventual consistency)을 확보합니다.
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
public interface SagaStep {

    /**
     * 단계 이름(로그/추적용).
     *
     * @return 단계 이름
     */
    String name();

    /**
     * 정방향 실행. 자체 로컬 트랜잭션으로 수행되어야 합니다.
     *
     * @param context 사가 컨텍스트(단계 간 데이터 공유)
     */
    void execute(SagaContext context);

    /**
     * 보상 트랜잭션. 이 단계 또는 이후 단계가 실패했을 때 역순으로 호출됩니다.
     * 보상은 멱등하게 구현해야 하며, 예외를 던지지 않도록 방어적으로 작성합니다.
     *
     * @param context 사가 컨텍스트
     */
    void compensate(SagaContext context);
}
