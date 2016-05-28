package org.statefulj.fsm.model.impl;

import java.util.HashMap;
import java.util.Map;

import org.statefulj.fsm.model.Action;
import org.statefulj.fsm.model.State;
import org.statefulj.fsm.model.Transition;

public class StateImpl<T> implements State<T> {
    private String name;
    private Map<String, Transition<T>> transitions = new HashMap<>();
    boolean isEndState = false;
    boolean isBlocking = false;

    public StateImpl() {
    }

    public StateImpl(final String name) {
        if ((name == null) || name.trim().equals("")) {
            throw new RuntimeException("Name must be a non-empty value");
        }
        this.name = name;
    }

    public StateImpl(final String name, final boolean isEndState) {
        this(name);
        this.isEndState = isEndState;
    }

    public StateImpl(final String name, final boolean isEndState, final boolean isBlocking) {
        this(name, isEndState);
        this.isBlocking = isBlocking;
    }

    public StateImpl(final String name, final Map<String, Transition<T>> transitions, final boolean isEndState) {
        this(name, isEndState);
        this.transitions = transitions;
    }

    public StateImpl(final String name, final Map<String, Transition<T>> transitions, final boolean isEndState, final boolean isBlocking) {
        this(name, transitions, isEndState);
        this.isBlocking = isBlocking;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public Transition<T> getTransition(final String event) {
        return transitions.get(event);
    }

    public Map<String, Transition<T>> getTransitions() {
        return transitions;
    }

    public void setTransitions(final Map<String, Transition<T>> transitions) {
        this.transitions = transitions;
    }

    @Override
    public boolean isEndState() {
        return isEndState;
    }

    public void setEndState(final boolean isEndState) {
        this.isEndState = isEndState;
    }

    @Override
    public void addTransition(final String event, final State<T> next) {
        this.transitions.put(event, new DeterministicTransitionImpl<>(next, null));
    }

    @Override
    public void addTransition(final String event, final State<T> next, final Action<T> action) {
        this.transitions.put(event, new DeterministicTransitionImpl<>(next, action));
    }

    @Override
    public void addTransition(final String event, final Transition<T> transition) {
        this.transitions.put(event, transition);
    }

    @Override
    public void removeTransition(final String event) {
        this.transitions.remove(event);
    }

    @Override
    public void setBlocking(final boolean isBlocking) {
        this.isBlocking = isBlocking;
    }

    @Override
    public boolean isBlocking() {
        return this.isBlocking;
    }

    @Override
    public String toString() {
        return "State[name=" + this.name + ", isEndState=" + this.isEndState + ", isBlocking=" + this.isBlocking + "]";
    }
}
