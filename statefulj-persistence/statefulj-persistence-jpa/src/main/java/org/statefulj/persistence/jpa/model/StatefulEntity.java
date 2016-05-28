package org.statefulj.persistence.jpa.model;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.statefulj.persistence.annotations.State;

@MappedSuperclass
public abstract class StatefulEntity {
    @State
    @Column(insertable = true, updatable = false)
    private String state;

    public String getState() {
        return state;
    }
}
