package org.statefulj.persistence.jpa.utils;

import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

public class UnitTestUtils {

    static ThreadLocal<TransactionStatus> tl = new ThreadLocal<>();

    public static void startTransaction(final JpaTransactionManager transactionManager) {
        final DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setName("SomeTxName");
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        final TransactionStatus status = transactionManager.getTransaction(def);
        tl.set(status);
    }

    public static void commitTransaction(final JpaTransactionManager transactionManager) {
        transactionManager.commit(tl.get());
    }

    public static void rollbackTransaction(final JpaTransactionManager transactionManager) {
        transactionManager.rollback(tl.get());
    }
}
