package org.statefulj.framework.tests.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.statefulj.persistence.jpa.model.StatefulEntity;

@Entity
@Table(name = "users")
public class User extends StatefulEntity {
    public static final String ONE_STATE = "one";
    public static final String TWO_STATE = "two";
    public static final String THREE_STATE = "three";
    public static final String FOUR_STATE = "four";
    public static final String FIVE_STATE = "five";
    public static final String SIX_STATE = "six";
    public static final String SEVEN_STATE = "seven";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    Long id;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }
}
