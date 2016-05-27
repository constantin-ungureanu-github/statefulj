package org.statefulj.fsm.model.impl;

import java.util.List;

import org.statefulj.fsm.RetryException;
import org.statefulj.fsm.model.Action;

public class CompositeActionImpl<T> implements Action<T> {
    List<Action<T>> actions;

    public CompositeActionImpl(final List<Action<T>> actions) {
        this.actions = actions;
    }

    public void execute(final T stateful, final String event, final Object... args) throws RetryException {
        for (final Action<T> action : actions) {
            action.execute(stateful, event, args);
        }
    }
}
