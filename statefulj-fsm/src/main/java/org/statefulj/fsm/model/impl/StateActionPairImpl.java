package org.statefulj.fsm.model.impl;

import org.statefulj.fsm.model.Action;
import org.statefulj.fsm.model.State;
import org.statefulj.fsm.model.StateActionPair;

public class StateActionPairImpl<T> implements StateActionPair<T> {

    State<T> state;
    Action<T> action;

    public StateActionPairImpl(final State<T> state, final Action<T> action) {
        this.state = state;
        this.action = action;
    }

    @Override
    public State<T> getState() {
        return state;
    }

    public void setState(final State<T> state) {
        this.state = state;
    }

    @Override
    public Action<T> getAction() {
        return action;
    }

    public void setAction(final Action<T> action) {
        this.action = action;
    }
}
