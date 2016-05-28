package org.statefulj.fsm;

import org.statefulj.fsm.model.State;

public interface Persister<T> {
    State<T> getCurrent(T stateful);

    void setCurrent(T stateful, State<T> current, State<T> next) throws StaleStateException;
}
