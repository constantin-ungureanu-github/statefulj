package org.statefulj.persistence.memory;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.statefulj.common.utils.ReflectionUtils;
import org.statefulj.fsm.Persister;
import org.statefulj.fsm.StaleStateException;
import org.statefulj.fsm.model.State;

public class MemoryPersisterImpl<T> implements Persister<T> {
    private final Map<String, State<T>> states = new HashMap<>();
    private State<T> start;
    private String stateFieldName;
    private volatile Field stateField;

    public MemoryPersisterImpl(final Collection<State<T>> states, final State<T> start) {
        setStart(start);
        setStates(states);
    }

    public MemoryPersisterImpl(final List<State<T>> states, final State<T> start, final String stateFieldName) {
        this(states, start);
        this.stateFieldName = stateFieldName;
    }

    public MemoryPersisterImpl(final T stateful, final List<State<T>> states, final State<T> start) {
        this(states, start);
        this.setCurrent(stateful, start);
    }

    public MemoryPersisterImpl(final T stateful, final List<State<T>> states, final State<T> start, final String stateFieldName) {
        this(states, start, stateFieldName);
        this.setCurrent(stateful, start);
    }

    public synchronized Collection<State<T>> getStates() {
        return states.values();
    }

    public synchronized State<T> addState(final State<T> state) {
        return states.put(state.getName(), state);
    }

    public State<T> removeState(final State<T> state) {
        return removeState(state.getName());
    }

    public synchronized State<T> removeState(final String name) {
        return states.remove(name);
    }

    public synchronized void setStates(final Collection<State<T>> states) {
        this.states.clear();

        for (final State<T> state : states) {
            this.states.put(state.getName(), state);
        }
    }

    public State<T> getStart() {
        return start;
    }

    public void setStart(final State<T> start) {
        this.start = start;
    }

    public String getStateFieldName() {
        return stateFieldName;
    }

    public void setStateFieldName(final String stateFieldName) {
        this.stateFieldName = stateFieldName;
    }

    @Override
    public State<T> getCurrent(final T stateful) {
        try {
            final String key = (String) getStateField(stateful).get(stateful);
            final State<T> state = (key != null) ? states.get(key) : null;
            return (state != null) ? state : this.start;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setCurrent(final T stateful, final State<T> current) {
        synchronized (stateful) {
            try {
                getStateField(stateful).set(stateful, current.getName());
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void setCurrent(final T stateful, final State<T> current, final State<T> next) throws StaleStateException {
        synchronized (stateful) {
            if (this.getCurrent(stateful).equals(current)) {
                this.setCurrent(stateful, next);
            } else {
                throw new StaleStateException();
            }
        }
    }

    private Field getStateField(final T stateful) {
        if (stateField == null) {
            stateField = locateStateField(stateful);
        }
        return stateField;
    }

    private synchronized Field locateStateField(final T stateful) {
        Field field = null;

        if ((this.stateFieldName != null) && !this.stateFieldName.equals("")) {
            field = ReflectionUtils.getField(stateful.getClass(), this.stateFieldName);
        } else {
            field = ReflectionUtils.getFirstAnnotatedField(stateful.getClass(), org.statefulj.persistence.annotations.State.class);
            if (field != null) {
                this.stateFieldName = field.getName();
            }
        }

        if (field == null) {
            throw new RuntimeException("Unable to locate a State field for stateful: " + stateful);
        }

        field.setAccessible(true);
        return field;
    }
}
