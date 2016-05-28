package org.statefulj.fsm.model;

import org.statefulj.fsm.RetryException;

public interface Transition<T> {
    StateActionPair<T> getStateActionPair(T stateful) throws RetryException;
}
