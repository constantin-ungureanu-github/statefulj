package org.statefulj.fsm.model.impl;

import org.statefulj.fsm.RetryException;
import org.statefulj.fsm.WaitAndRetryException;
import org.statefulj.fsm.model.Action;

public class WaitAndRetryActionImpl<T> implements Action<T> {
    private int wait = 0;

    public WaitAndRetryActionImpl(final int wait) {
        this.wait = wait;
    }

    public void execute(final T obj, final String event, final Object... args) throws RetryException {
        throw new WaitAndRetryException(this.wait);
    }
}
