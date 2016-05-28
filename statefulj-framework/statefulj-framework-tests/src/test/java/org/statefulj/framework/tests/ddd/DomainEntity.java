package org.statefulj.framework.tests.ddd;

import static org.statefulj.framework.tests.ddd.DomainEntity.STATE_A;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.springframework.context.annotation.Scope;
import org.statefulj.framework.core.annotations.FSM;
import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.framework.core.annotations.Transitions;
import org.statefulj.framework.core.model.StatefulFSM;
import org.statefulj.fsm.TooBusyException;
import org.statefulj.persistence.jpa.model.StatefulEntity;

@Entity
@Scope("prototype")
@StatefulController(clazz = DomainEntity.class, startState = STATE_A)
public class DomainEntity extends StatefulEntity {
    public final static String STATE_A = "A";
    public final static String STATE_B = "B";

    private final static String EVENT_X = "event-x";
    private final static String EVENT_Y = "event-y";
    private final static String SPRING_EVENT_X = "springmvc:/" + EVENT_X;
    private final static String SPRING_EVENT_Y = "springmvc:/" + EVENT_Y;

    @Id
    private Long id;

    private int value;

    @FSM
    @Transient
    private StatefulFSM<DomainEntity> fsm;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public int getValue() {
        return value;
    }

    public void setValue(final int value) {
        this.value = value;
    }

    public void onEventX(final int value) throws TooBusyException {
        fsm.onEvent(this, EVENT_X, value);
    }

    public void onEventY(final int value) throws TooBusyException {
        fsm.onEvent(this, EVENT_Y, value);
    }

    @Transitions({ @Transition(from = STATE_A, event = EVENT_X, to = STATE_B), @Transition(from = STATE_A, event = SPRING_EVENT_X, to = STATE_B), })
    protected DomainEntity actionAXB(final String event, final Integer value) {
        this.value = value;
        return this;
    }

    @Transitions({ @Transition(from = STATE_B, event = EVENT_Y, to = STATE_A), @Transition(from = STATE_B, event = SPRING_EVENT_Y, to = STATE_A), })
    protected DomainEntity actionBYA(final String event, final Integer value) {
        this.value = value;
        return this;
    }
}
