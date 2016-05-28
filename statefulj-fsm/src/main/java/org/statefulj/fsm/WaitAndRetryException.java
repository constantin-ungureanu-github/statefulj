package org.statefulj.fsm;

public class WaitAndRetryException extends RetryException {
    private static final long serialVersionUID = 4257475898400867019L;
    private int wait;

    public WaitAndRetryException(final int wait) {
        this.wait = wait;
    }

    public int getWait() {
        return wait;
    }

    public void setWait(final int wait) {
        this.wait = wait;
    }
}
