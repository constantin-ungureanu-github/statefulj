package org.statefulj.fsm;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.statefulj.fsm.model.Action;
import org.statefulj.fsm.model.State;
import org.statefulj.fsm.model.impl.StateActionPairImpl;
import org.statefulj.fsm.model.impl.StateImpl;
import org.statefulj.persistence.memory.MemoryPersisterImpl;

@SuppressWarnings("unchecked")
public class FSMTest {
    private static class FirstState {
        @org.statefulj.persistence.annotations.State
        String state;
    }

    private static class SecondState {
        @org.statefulj.persistence.annotations.State
        String state;
    }

    @Test
    public void testSimpleFSM() throws TooBusyException, RetryException {

        final FirstState stateful = new FirstState();

        final Action<FirstState> actionA = Mockito.mock(Action.class);
        final Action<FirstState> actionB = Mockito.mock(Action.class);

        final String eventA = "eventA";
        final String eventB = "eventB";

        final State<FirstState> stateA = new StateImpl<>("stateA");
        final State<FirstState> stateB = new StateImpl<>("stateB");
        final State<FirstState> stateC = new StateImpl<>("stateC", true);

        stateA.addTransition(eventA, stateB, actionA);
        stateB.addTransition(eventB, stateC, actionB);

        final List<State<FirstState>> states = new LinkedList<>();
        states.add(stateA);
        states.add(stateB);
        states.add(stateC);

        final Persister<FirstState> persister = new MemoryPersisterImpl<>(stateful, states, stateA);
        final FSM<FirstState> fsm = new FSM<>("SimpleFSM", persister);

        final FirstState arg = new FirstState();
        State<FirstState> current = fsm.onEvent(stateful, eventA, arg);
        Assert.assertEquals(stateB, current);
        Assert.assertFalse(current.isEndState());
        Mockito.verify(actionA).execute(stateful, eventA, arg);
        Mockito.verify(actionB, Mockito.never()).execute(stateful, eventA, arg);

        Mockito.reset(actionA);

        current = fsm.onEvent(stateful, eventA, arg);
        Assert.assertEquals(stateB, current);
        Assert.assertFalse(current.isEndState());
        Mockito.verify(actionA, Mockito.never()).execute(stateful, eventA, arg);
        Mockito.verify(actionB, Mockito.never()).execute(stateful, eventA, arg);

        current = fsm.onEvent(stateful, eventB, arg);
        Assert.assertEquals(stateC, current);
        Assert.assertTrue(current.isEndState());
        Mockito.verify(actionA, Mockito.never()).execute(stateful, eventB, arg);
        Mockito.verify(actionB).execute(stateful, eventB, arg);
    }

    @Test
    public void testNonDeterminsticTransition() throws TooBusyException {
        final FirstState stateful = new FirstState();
        final MutableInt eventCnt = new MutableInt(0);
        final String eventA = "eventA";

        final State<FirstState> stateA = new StateImpl<>("stateA");
        final State<FirstState> stateB = new StateImpl<>("stateB", true);

        stateA.addTransition(eventA, stateful1 -> {
            State<FirstState> next = null;
            if (eventCnt.intValue() < 2) {
                next = stateA;
            } else {
                next = stateB;
            }
            eventCnt.add(1);
            return new StateActionPairImpl<>(next, null);
        });

        final List<State<FirstState>> states = new LinkedList<>();
        states.add(stateA);
        states.add(stateB);

        final Persister<FirstState> persister = new MemoryPersisterImpl<>(stateful, states, stateA);
        final FSM<FirstState> fsm = new FSM<>("NDFSM", persister);

        State<FirstState> current = fsm.onEvent(stateful, eventA);
        Assert.assertEquals(stateA, current);

        current = fsm.onEvent(stateful, eventA);
        Assert.assertEquals(stateA, current);

        current = fsm.onEvent(stateful, eventA);
        Assert.assertEquals(stateB, current);
    }

    @Test(expected = TooBusyException.class)
    public void testTooBusy() throws TooBusyException {
        final FirstState stateful = new FirstState();
        final String eventA = "eventA";

        final Action<FirstState> throwAction = (stateful1, event, args) -> {
            throw new RetryException();
        };

        final State<FirstState> stateA = new StateImpl<>("stateA");

        stateA.addTransition(eventA, stateA, throwAction);

        final List<State<FirstState>> states = new LinkedList<>();
        states.add(stateA);

        final Persister<FirstState> persister = new MemoryPersisterImpl<>(stateful, states, stateA);
        final FSM<FirstState> fsm = new FSM<>("TooBusy", persister);
        fsm.setRetryAttempts(1);

        fsm.onEvent(stateful, eventA);
    }

    @Test
    public void testRetryInterval() {
        final FirstState stateful = new FirstState();
        final String eventA = "eventA";
        final State<FirstState> stateA = new StateImpl<>("stateA", false, true);

        final List<State<FirstState>> states = new LinkedList<>();
        states.add(stateA);

        final Persister<FirstState> persister = new MemoryPersisterImpl<>(stateful, states, stateA);
        final FSM<FirstState> fsm = new FSM<>("TooBusy", persister);
        fsm.setRetryAttempts(1);
        fsm.setRetryInterval(500);

        final long start = Calendar.getInstance().getTimeInMillis();
        try {
            fsm.onEvent(stateful, eventA);
        } catch (final TooBusyException e) {
        }

        final long end = Calendar.getInstance().getTimeInMillis();
        Assert.assertTrue((end - start) > 499);
        Assert.assertTrue((end - start) < 600);
    }

    @Test(expected = TooBusyException.class)
    public void testBlocking() throws TooBusyException {
        final FirstState stateful = new FirstState();
        final String eventA = "eventA";
        final State<FirstState> stateA = new StateImpl<>("stateA", false, true);
        final List<State<FirstState>> states = new LinkedList<>();
        states.add(stateA);
        final Persister<FirstState> persister = new MemoryPersisterImpl<>(stateful, states, stateA);
        final FSM<FirstState> fsm = new FSM<>("TooBusy", persister);
        fsm.setRetryAttempts(1);
        fsm.onEvent(stateful, eventA);
    }

    @Test(expected = TooBusyException.class)
    public void testRetryFailureOnTransition() throws TooBusyException {
        final FirstState stateful = new FirstState();
        final String eventA = "eventA";
        final State<FirstState> stateA = new StateImpl<>("stateA");
        stateA.addTransition(eventA, stateful1 -> {
            throw new RetryException();
        });

        final List<State<FirstState>> states = new LinkedList<>();
        states.add(stateA);

        final Persister<FirstState> persister = new MemoryPersisterImpl<>(stateful, states, stateA);
        final FSM<FirstState> fsm = new FSM<>("TooBusy", persister);
        fsm.setRetryAttempts(1);

        fsm.onEvent(stateful, eventA);
    }

    @Test
    public void testStateFieldName() {
        final SecondState stateful = new SecondState();

        final State<SecondState> stateA = new StateImpl<>("stateA");
        final State<SecondState> stateB = new StateImpl<>("stateB");

        final List<State<SecondState>> states = new LinkedList<>();
        states.add(stateA);
        states.add(stateB);

        final MemoryPersisterImpl<SecondState> persister = new MemoryPersisterImpl<>(stateful, states, stateA, "state");
        persister.setCurrent(stateful, stateB);
        Assert.assertEquals(stateB, persister.getCurrent(stateful));
    }

    @Test
    public void testTransitionOutOfBlocking() throws TooBusyException {
        final State<FirstState> stateA = new StateImpl<>("stateA", false, true);
        final State<FirstState> stateB = new StateImpl<>("stateB");
        final String eventA = "eventA";
        final String eventB = "eventB";
        final FirstState stateful = new FirstState();

        stateA.addTransition(eventB, stateB);

        final List<State<FirstState>> states = new LinkedList<>();
        states.add(stateA);
        states.add(stateB);

        final Persister<FirstState> persister = new MemoryPersisterImpl<>(stateful, states, stateA);
        final FSM<FirstState> fsm = new FSM<>("TooBusy", persister);
        fsm.setRetryAttempts(1000);

        new Thread(() -> {
            try {
                Thread.sleep(500);
                fsm.onEvent(stateful, eventB);
            } catch (final Exception e) {
            }
        }).start();

        final State<FirstState> state = fsm.onEvent(stateful, eventA);
        Assert.assertEquals(stateB, state);
    }
}
