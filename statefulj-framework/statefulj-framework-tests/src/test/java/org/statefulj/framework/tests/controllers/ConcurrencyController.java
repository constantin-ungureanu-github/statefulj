package org.statefulj.framework.tests.controllers;

import static org.statefulj.framework.tests.model.User.ONE_STATE;
import static org.statefulj.framework.tests.model.User.THREE_STATE;
import static org.statefulj.framework.tests.model.User.TWO_STATE;

import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.framework.tests.model.User;

@StatefulController(clazz = User.class, startState = ONE_STATE, noops = { @Transition(from = TWO_STATE, event = "two", to = THREE_STATE) })
public class ConcurrencyController {

    @Transition(from = ONE_STATE, event = "one", to = TWO_STATE)
    public void oneOneTwo(final User user, final String event, final Object monitor) throws InterruptedException {
        monitor.notify();
        monitor.wait();
    }
}
