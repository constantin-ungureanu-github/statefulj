package org.statefulj.persistence.common;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.statefulj.common.utils.ReflectionUtils;
import org.statefulj.fsm.Persister;
import org.statefulj.fsm.StaleStateException;
import org.statefulj.fsm.model.State;

public abstract class AbstractPersister<T> implements Persister<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractPersister.class);

    private Field idField;
    private Field stateField;
    private State<T> start;
    private Class<T> clazz;
    private HashMap<String, State<T>> states = new HashMap<String, State<T>>();

    public AbstractPersister(final List<State<T>> states, final String stateFieldName, final State<T> start, final Class<T> clazz) {

        this.clazz = clazz;

        this.idField = findIdField(clazz);

        if (this.idField == null) {
            throw new RuntimeException("No Id field defined");
        }
        this.idField.setAccessible(true);

        this.stateField = findStateField(stateFieldName, clazz);

        if (this.stateField == null) {
            throw new RuntimeException("No State field defined");
        }

        if (!validStateField(this.stateField)) {
            throw new RuntimeException(String.format("State field, %s, of class %s, is not of type %s", this.stateField.getName(), clazz, getStateFieldType()));
        }

        this.stateField.setAccessible(true);

        this.start = start;

        for (final State<T> state : states) {
            this.states.put(state.getName(), state);
        }
    }

    public State<T> getCurrent(final T stateful) {
        State<T> state = null;
        try {
            final String stateKey = this.getState(stateful);
            state = (stateKey == null) ? this.start : this.states.get(stateKey);
        } catch (final NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (final SecurityException e) {
            throw new RuntimeException(e);
        } catch (final IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (final IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        state = (state == null) ? this.start : state;
        return state;
    }

    /**
     * Set the current State. This method will ensure that the state in the db matches the expected current state. If not, it will throw a StateStateException
     *
     * @param stateful
     *            Stateful Entity
     * @param current
     *            Expected current State
     * @param next
     *            The value of the next State
     * @throws StaleStateException
     *             thrown if the value of the State does not equal to the provided current State
     */
    public abstract void setCurrent(T stateful, State<T> current, State<T> next) throws StaleStateException;

    protected abstract boolean validStateField(Field stateField);

    protected abstract Field findIdField(Class<?> clazz);

    protected Field findStateField(final String stateFieldName, final Class<?> clazz) {
        Field stateField = null;
        if (StringUtils.isEmpty(stateFieldName)) {
            stateField = ReflectionUtils.getFirstAnnotatedField(clazz, org.statefulj.persistence.annotations.State.class);
        } else {
            try {
                stateField = clazz.getDeclaredField(stateFieldName);
            } catch (final NoSuchFieldException e) {
                AbstractPersister.logger.error("Unable to locate state field for {}, stateFieldName={}", clazz.getName(), stateFieldName);
            } catch (final SecurityException e) {
                AbstractPersister.logger.error("Security exception trying to locate state field for {}, stateFieldName={}", clazz.getName(), stateFieldName);
                AbstractPersister.logger.error("Exception", e);
            }
        }
        return stateField;
    }

    protected abstract Class<?> getStateFieldType();

    protected Field getIdField() {
        return idField;
    }

    protected void setIdField(final Field idField) {
        this.idField = idField;
    }

    protected Field getStateField() {
        return stateField;
    }

    protected void setStateField(final Field stateField) {
        this.stateField = stateField;
    }

    protected State<T> getStart() {
        return start;
    }

    protected void setStart(final State<T> start) {
        this.start = start;
    }

    protected Class<T> getClazz() {
        return clazz;
    }

    protected void setClazz(final Class<T> clazz) {
        this.clazz = clazz;
    }

    protected HashMap<String, State<T>> getStates() {
        return states;
    }

    protected void setStates(final HashMap<String, State<T>> states) {
        this.states = states;
    }

    protected Object getId(final T obj) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        return this.idField.get(obj);
    }

    protected String getState(final T obj) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        return (String) this.stateField.get(obj);
    }

    protected void setState(final T obj, String state) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        state = (state == null) ? this.start.getName() : state;
        this.stateField.set(obj, state);
    }

    protected void throwStaleState(final State<T> current, final State<T> next) throws StaleStateException {
        final String err = String.format("Unable to update state, entity.state=%s, db.state=%s", current.getName(), next.getName());
        throw new StaleStateException(err);
    }
}
