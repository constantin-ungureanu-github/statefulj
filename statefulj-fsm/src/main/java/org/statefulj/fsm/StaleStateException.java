package org.statefulj.fsm;

public class StaleStateException extends RetryException {
    private static final long serialVersionUID = -152318137915951158L;

    public StaleStateException() {
        super();
    }

    public StaleStateException(final String err) {
        super(err);
    }
}
