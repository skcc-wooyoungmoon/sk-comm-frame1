package com.sk.framework.transaction.support;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * @className    : TransactionSupport
 * @description  : 프로그래밍 방식 트랜잭션 및 커밋 후 콜백을 위한 헬퍼.
 *                 - {@code runInTransaction} : 코드 블록을 새 트랜잭션 경계로 실행
 *                 - {@code runAfterCommit}   : 현재 트랜잭션이 성공적으로 커밋된 뒤에만 실행
 *                   (예: 커밋 후 이벤트 발행/알림 발송 - 롤백 시 부작용 방지)
 * @modification : 2026.08.13(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.13
 * @version      : 1.0
 */
@RequiredArgsConstructor
public class TransactionSupport {

    private final TransactionTemplate transactionTemplate;

    /**
     * 결과를 반환하는 트랜잭션 블록을 실행합니다.
     */
    public <T> T runInTransaction(Supplier<T> action) {
        return transactionTemplate.execute(status -> action.get());
    }

    /**
     * 반환값이 없는 트랜잭션 블록을 실행합니다.
     */
    public void runInTransaction(Runnable action) {
        transactionTemplate.executeWithoutResult(status -> action.run());
    }

    /**
     * 현재 트랜잭션이 커밋된 뒤 실행할 작업을 등록합니다.
     * 트랜잭션이 없으면 즉시 실행합니다.
     *
     * @param action 커밋 후 실행할 작업
     */
    public void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
