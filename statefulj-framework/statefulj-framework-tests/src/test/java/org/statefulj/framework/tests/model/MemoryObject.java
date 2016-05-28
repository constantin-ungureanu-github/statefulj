package org.statefulj.framework.tests.model;

import org.statefulj.persistence.annotations.State;

public class MemoryObject {
    public final static String ONE_STATE = "one";
    public final static String TWO_STATE = "two";

    @State
    private String state;

    public String getState() {
        return state;
    }
}
