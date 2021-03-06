package org.statefulj.fsm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.statefulj.fsm.model.Action;
import org.statefulj.fsm.model.State;
import org.statefulj.fsm.model.StateActionPair;
import org.statefulj.fsm.model.Transition;

public class FSM<T> {
    private static final Logger logger = LoggerFactory.getLogger(FSM.class);

    private static final int DEFAULT_RETRIES = 100;
    private static final int DEFAULT_RETRY_INTERVAL = 100;
    private int retryAttempts = DEFAULT_RETRIES;
    private int retryInterval = DEFAULT_RETRY_INTERVAL;
    private Persister<T> persister;
    private String name = "FSM";

    public FSM(final String name) {
        this.name = name;
    }

    public FSM(final Persister<T> persister) {
        this.persister = persister;
    }

    public FSM(final String name, final Persister<T> persister) {
        this.name = name;
        this.persister = persister;
    }

    public FSM(final String name, final Persister<T> persister, final int retryAttempts, final int retryInterval) {
        this.name = name;
        this.persister = persister;
        this.retryAttempts = retryAttempts;
        this.retryInterval = retryInterval;
    }

    public State<T> onEvent(final T stateful, final String event, final Object... args) throws TooBusyException {
        int attempts = 0;

        while ((retryAttempts == -1) || (attempts < retryAttempts)) {
            try {
                State<T> current = this.getCurrentState(stateful);

                final Transition<T> transition = this.getTransition(event, current);

                if (transition != null) {
                    current = this.transition(stateful, current, event, transition, args);
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("{}({})::{}({})->{}/noop", name, stateful.getClass().getSimpleName(), current.getName(), event, current.getName());
                    }

                    if (current.isBlocking()) {
                        this.setCurrent(stateful, current, current);
                        throw new WaitAndRetryException(retryInterval);
                    }
                }

                return current;

            } catch (final RetryException re) {
                FSM.logger.warn("{}({})::Retrying event", name, stateful);

                if (WaitAndRetryException.class.isInstance(re)) {
                    try {
                        Thread.sleep(((WaitAndRetryException) re).getWait());
                    } catch (final InterruptedException ie) {
                        throw new RuntimeException(ie);
                    }
                }
                attempts++;
            }
        }

        FSM.logger.error("{}({})::Unable to process event", this.name, stateful);
        throw new TooBusyException();
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }

    public void setRetryAttempts(final int retries) {
        this.retryAttempts = retries;
    }

    public int getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(final int retryInterval) {
        this.retryInterval = retryInterval;
    }

    public Persister<T> getPersister() {
        return persister;
    }

    public void setPersister(final Persister<T> persister) {
        this.persister = persister;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public State<T> getCurrentState(final T obj) {
        return this.persister.getCurrent(obj);
    }

    protected Transition<T> getTransition(final String event, final State<T> current) {
        return current.getTransition(event);
    }

    protected State<T> transition(final T stateful, final State<T> current, final String event, final Transition<T> transition, final Object... args) throws RetryException {
        final StateActionPair<T> pair = transition.getStateActionPair(stateful);
        setCurrent(stateful, current, pair.getState());
        executeAction(pair.getAction(), stateful, event, current.getName(), pair.getState().getName(), args);
        return pair.getState();
    }

    protected void setCurrent(final T stateful, final State<T> current, final State<T> next) throws StaleStateException {
        persister.setCurrent(stateful, current, next);
    }

    protected void executeAction(final Action<T> action, final T stateful, final String event, final String from, final String to, final Object... args) throws RetryException {
        if (logger.isDebugEnabled()) {
            logger.debug("{}({})::{}({})->{}/{}", this.name, stateful.getClass().getSimpleName(), from, event, to, (action == null) ? "noop" : action.toString());
        }

        if (action != null) {
            action.execute(stateful, event, args);
        }
    }
}
