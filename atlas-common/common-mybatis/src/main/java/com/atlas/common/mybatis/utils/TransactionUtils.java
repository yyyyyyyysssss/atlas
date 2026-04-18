package com.atlas.common.mybatis.utils;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class TransactionUtils {

    /**
     * 在事务提交后执行操作
     * 如果当前没有事务，则立即执行
     *
     * @param runnable 待执行的任务
     */
    public static void executeAfterCommit(Runnable runnable) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runnable.run();
                }
            });
        } else {
            // 如果当前没有事务，直接执行
            runnable.run();
        }
    }

}
