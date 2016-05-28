package org.statefulj.framework.tests.controllers;

import static org.statefulj.framework.tests.model.User.ONE_STATE;

import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.framework.tests.model.User;

@StatefulController(value = "overloadedMethodController", clazz = User.class, startState = ONE_STATE)
public class OverloadedMethodController {
    @Transition(event = "one")
    public String method(final User user, final String event) {
        return "method1";
    }

    @Transition(event = "two")
    public String method(final User user, final String event, final String parm) {
        return "method2";
    }

    @Transition(event = "three")
    public String method(final User user, final String event, final Integer parm) {
        return "method3";
    }
}
