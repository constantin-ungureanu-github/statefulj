package org.statefulj.framework.persistence.jpa;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.statefulj.framework.core.model.Factory;
import org.statefulj.framework.core.model.Finder;
import org.statefulj.framework.core.model.StatefulFSM;
import org.statefulj.framework.core.model.impl.FSMHarnessImpl;
import org.statefulj.fsm.TooBusyException;

public class JPAFSMHarnessImpl<T, CT> extends FSMHarnessImpl<T, CT> {
    private final PlatformTransactionManager transactionManager;

    public JPAFSMHarnessImpl(final StatefulFSM<T> fsm, final Class<T> clazz, final Factory<T, CT> factory, final Finder<T, CT> finder, final PlatformTransactionManager transactionManager) {
        super(fsm, clazz, factory, finder);
        this.transactionManager = transactionManager;
    }

    @Override
    public Object onEvent(final String event, final Object id, final Object[] parms) throws TooBusyException {
        final TransactionTemplate tt = new TransactionTemplate(transactionManager);
        return tt.execute(status -> {
            try {
                return JPAFSMHarnessImpl.super.onEvent(event, id, parms);
            } catch (final TooBusyException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
