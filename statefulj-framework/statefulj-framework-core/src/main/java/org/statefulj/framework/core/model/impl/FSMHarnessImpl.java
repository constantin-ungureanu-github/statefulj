package org.statefulj.framework.core.model.impl;

import java.util.ArrayList;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.statefulj.framework.core.fsm.ContextWrapper;
import org.statefulj.framework.core.model.FSMHarness;
import org.statefulj.framework.core.model.Factory;
import org.statefulj.framework.core.model.Finder;
import org.statefulj.framework.core.model.StatefulFSM;
import org.statefulj.fsm.TooBusyException;

public class FSMHarnessImpl<T, CT> implements FSMHarness {
    private static final Logger logger = LoggerFactory.getLogger(FSMHarnessImpl.class);

    private final Factory<T, CT> factory;
    private final Finder<T, CT> finder;
    private final StatefulFSM<T> fsm;

    private final Class<T> clazz;

    public FSMHarnessImpl(final StatefulFSM<T> fsm, final Class<T> clazz, final Factory<T, CT> factory, final Finder<T, CT> finder) {
        this.fsm = fsm;
        this.clazz = clazz;
        this.factory = factory;
        this.finder = finder;
    }

    @SuppressWarnings("unchecked")
    public Object onEvent(final String event, final Object id, final Object[] parms) throws TooBusyException {
        final ArrayList<Object> parmList = new ArrayList<Object>(Arrays.asList(parms));
        final CT context = (CT) ((parmList.size() > 0) ? parmList.remove(0) : null);
        final ContextWrapper<CT> retryParms = new ContextWrapper<CT>(context);
        parmList.add(0, retryParms);

        T stateful = null;

        if (id == null) {
            stateful = findStateful(event, context);
        } else {
            stateful = findStateful(event, id, context);
        }

        if (stateful == null) {
            if (id != null) {
                FSMHarnessImpl.logger.error("Unable to locate object of type {}, id={}, event={}", clazz.getName(), id, event);
                throw new RuntimeException("Unable to locate object of type " + clazz.getName() + ", id=" + ((id == null) ? "null" : id) + ", event=" + event);
            } else {
                stateful = this.factory.create(this.clazz, event, context);
                if (stateful == null) {
                    FSMHarnessImpl.logger.error("Unable to create object of type {}, event={}", clazz.getName(), event);
                    throw new RuntimeException("Unable to create object of type " + clazz.getName() + ", event=" + event);
                }
            }
        }

        return fsm.onEvent(stateful, event, parmList.toArray());
    }

    public Object onEvent(final String event, final Object[] parms) throws TooBusyException {
        final ArrayList<Object> parmList = new ArrayList<Object>(Arrays.asList(parms));
        final Object id = parmList.remove(0);
        return onEvent(event, id, parmList.toArray());
    }

    private T findStateful(final String event, final Object id, final CT context) {
        return this.finder.find(clazz, id, event, context);
    }

    private T findStateful(final String event, final CT context) {
        return this.finder.find(clazz, event, context);
    }
}
