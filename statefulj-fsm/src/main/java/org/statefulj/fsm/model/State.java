package org.statefulj.fsm.model;

public interface State<T> {

    String getName();

    Transition<T> getTransition(String event);

    boolean isEndState();

    public boolean isBlocking();

    public void setBlocking(boolean isBlocking);

    public void removeTransition(String event);

    public void addTransition(String event, Transition<T> transition);

    public void addTransition(String event, State<T> next, Action<T> action);

    public void addTransition(String event, State<T> next);
}
