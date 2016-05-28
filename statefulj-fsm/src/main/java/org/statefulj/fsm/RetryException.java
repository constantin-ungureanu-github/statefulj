package org.statefulj.fsm;

public class RetryException extends Exception {
    private static final long serialVersionUID = 6759057957512004480L;

    public RetryException() {
        super();
    }

    public RetryException(final String msg) {
        super(msg);
    }
}
