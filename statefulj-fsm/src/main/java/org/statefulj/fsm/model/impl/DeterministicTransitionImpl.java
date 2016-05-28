package org.statefulj.fsm.model.impl;

import org.statefulj.fsm.model.Action;
import org.statefulj.fsm.model.State;
import org.statefulj.fsm.model.StateActionPair;
import org.statefulj.fsm.model.Transition;

public class DeterministicTransitionImpl<T> implements Transition<T> {

    private StateActionPair<T> stateActionPair;

    public DeterministicTransitionImpl(final State<T> from, final State<T> to, final String event) {
        stateActionPair = new StateActionPairImpl<>(to, null);
        from.addTransition(event, this);
    }

    public DeterministicTransitionImpl(final State<T> from, final State<T> to, final String event, final Action<T> action) {
        stateActionPair = new StateActionPairImpl<>(to, action);
        from.addTransition(event, this);
    }

    public DeterministicTransitionImpl(final State<T> to, final Action<T> action) {
        stateActionPair = new StateActionPairImpl<>(to, action);
    }

    public DeterministicTransitionImpl(final State<T> to) {
        stateActionPair = new StateActionPairImpl<>(to, null);
    }

    @Override
    public StateActionPair<T> getStateActionPair(final T stateful) {
        return stateActionPair;
    }

    public void setStateActionPair(final StateActionPair<T> stateActionPair) {
        this.stateActionPair = stateActionPair;
    }

    @Override
    public String toString() {
        return "DeterministicTransition[state=" + this.stateActionPair.getState().getName() + ", action=" + this.stateActionPair.getAction() + "]";
    }
}
