package org.statefulj.framework.tests.controllers;

import static org.statefulj.framework.tests.model.MemoryObject.ONE_STATE;
import static org.statefulj.framework.tests.model.MemoryObject.TWO_STATE;

import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.framework.tests.model.MemoryObject;

@StatefulController(clazz = MemoryObject.class, startState = ONE_STATE)
public class MemoryController {
    @Transition(from = ONE_STATE, event = "one", to = TWO_STATE)
    public MemoryObject oneToTwo(final MemoryObject obj, final String event) {
        return obj;
    }

    @Transition(from = ONE_STATE, event = "fail", to = TWO_STATE, reload = true)
    public void failReload(final MemoryObject obj, final String event) {
    }
}
