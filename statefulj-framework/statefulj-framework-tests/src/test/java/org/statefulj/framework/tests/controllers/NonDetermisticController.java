package org.statefulj.framework.tests.controllers;

import static org.statefulj.framework.tests.model.User.ONE_STATE;
import static org.statefulj.framework.tests.model.User.THREE_STATE;
import static org.statefulj.framework.tests.model.User.TWO_STATE;

import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.framework.tests.model.User;

@StatefulController(clazz = User.class, startState = ONE_STATE)
public class NonDetermisticController {
    @Transition(event = "non-determinstic")
    String onEvent(final User user, final String event, final Boolean flag) {
        if (flag) {
            return "event:true";
        } else {
            return "event:false";
        }
    }

    @Transition(event = "true", to = TWO_STATE)
    private String onTrue(final User user, final String event, final Boolean flag) {
        return "onTrue";
    }

    @Transition(event = "false", to = THREE_STATE)
    private String onFalse(final User user, final String event, final Boolean flag) {
        return "onFalse";
    }
}
