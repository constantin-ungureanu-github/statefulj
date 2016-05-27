/***
 *
 * Copyright 2014 Andrew Hall
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
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
import org.statefulj.fsm.model.StateActionPair;
import org.statefulj.fsm.model.Transition;
import org.statefulj.fsm.model.impl.StateActionPairImpl;
import org.statefulj.fsm.model.impl.StateImpl;
import org.statefulj.fsm.model.impl.WaitAndRetryActionImpl;
import org.statefulj.persistence.memory.MemoryPersisterImpl;

public class FSMTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testSimpleFSM() throws TooBusyException, RetryException {
        // Stateful
        //
        final Foo stateful = new Foo();

        // Set up the Actions as Mock so we can inspect them
        //
        final Action<Foo> actionA = Mockito.mock(Action.class);
        final Action<Foo> actionB = Mockito.mock(Action.class);

        // Events
        //
        final String eventA = "eventA";
        final String eventB = "eventB";

        // States
        //
        final State<Foo> stateA = new StateImpl<Foo>("stateA");
        final State<Foo> stateB = new StateImpl<Foo>("stateB");
        final State<Foo> stateC = new StateImpl<Foo>("stateC", true); // End State

        // Transitions
        //
        stateA.addTransition(eventA, stateB, actionA);
        stateB.addTransition(eventB, stateC, actionB);

        // FSM
        //
        final List<State<Foo>> states = new LinkedList<State<Foo>>();
        states.add(stateA);
        states.add(stateB);
        states.add(stateC);

        final Persister<Foo> persister = new MemoryPersisterImpl<Foo>(stateful, states, stateA);
        final FSM<Foo> fsm = new FSM<Foo>("SimpleFSM", persister);

        // Verify that on eventA, we transition to StateB and verify
        // that we call actionA with the correct arg
        //
        final Foo arg = new Foo();
        State<Foo> current = fsm.onEvent(stateful, eventA, arg);
        Assert.assertEquals(stateB, current);
        Assert.assertFalse(current.isEndState());
        Mockito.verify(actionA).execute(stateful, eventA, arg);
        Mockito.verify(actionB, Mockito.never()).execute(stateful, eventA, arg);

        Mockito.reset(actionA);

        // Verify that on eventA from StateB, that nothing happened
        //
        current = fsm.onEvent(stateful, eventA, arg);
        Assert.assertEquals(stateB, current);
        Assert.assertFalse(current.isEndState());
        Mockito.verify(actionA, Mockito.never()).execute(stateful, eventA, arg);
        Mockito.verify(actionB, Mockito.never()).execute(stateful, eventA, arg);

        // Verify that on eventB from StateB, we transition to stateC - the endState, and
        // call actionB with the correct args
        //
        current = fsm.onEvent(stateful, eventB, arg);
        Assert.assertEquals(stateC, current);
        Assert.assertTrue(current.isEndState());
        Mockito.verify(actionA, Mockito.never()).execute(stateful, eventB, arg);
        Mockito.verify(actionB).execute(stateful, eventB, arg);

    }

    @Test
    public void testNonDeterminsticTransition() throws TooBusyException {

        // Stateful
        //
        final Foo stateful = new Foo();

        final MutableInt eventCnt = new MutableInt(0);

        // Events
        //
        final String eventA = "eventA";

        // States
        //
        final State<Foo> stateA = new StateImpl<Foo>("stateA");
        final State<Foo> stateB = new StateImpl<Foo>("stateB", true);

        // Non-Deterministic Transition
        //
        stateA.addTransition(eventA, new Transition<Foo>() {

            public StateActionPair<Foo> getStateActionPair(final Foo stateful) {
                State<Foo> next = null;
                if (eventCnt.intValue() < 2) {
                    next = stateA;
                } else {
                    next = stateB;
                }
                eventCnt.add(1);
                return new StateActionPairImpl<Foo>(next, null);
            }
        });

        // FSM
        //
        final List<State<Foo>> states = new LinkedList<State<Foo>>();
        states.add(stateA);
        states.add(stateB);

        final Persister<Foo> persister = new MemoryPersisterImpl<Foo>(stateful, states, stateA);
        final FSM<Foo> fsm = new FSM<Foo>("NDFSM", persister);

        // Verify that the first two eventA returns stateA
        //
        State<Foo> current = fsm.onEvent(stateful, eventA);
        Assert.assertEquals(stateA, current);

        current = fsm.onEvent(stateful, eventA);
        Assert.assertEquals(stateA, current);

        // Third should return stateB
        //
        current = fsm.onEvent(stateful, eventA);
        Assert.assertEquals(stateB, current);

    }

    @Test
    public void testConcurrency() throws TooBusyException, InterruptedException, RetryException {
        // Stateful
        //
        final Foo stateful = new Foo();

        // Events
        //
        final String eventA = "eventA";
        final String eventB = "eventB";

        final FSM<Foo> fsm = new FSM<Foo>("ConcurrentFSM");
        final WaitAndRetryActionImpl<Foo> wra = new WaitAndRetryActionImpl<Foo>(100); // wait 100 ms and retry

        // This action will wait for 250ms then invoke the fsm with eventA.
        // This should transition the FSM back to stateA
        //
        final Action<Foo> waitAction = new Action<Foo>() {

            public void execute(final Foo stateful, final String event, final Object... args) throws RetryException {
                try {
                    Thread.sleep(250);
                    fsm.onEvent(stateful, eventA, args);
                } catch (final InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (final TooBusyException tbe) {
                    throw new RuntimeException(tbe);
                }

            }
        };

        // Add Spies
        //
        final WaitAndRetryActionImpl<Foo> wraSpy = Mockito.spy(wra);
        final Action<Foo> waitActionSpy = Mockito.spy(waitAction);

        // States
        //
        final State<Foo> stateA = new StateImpl<Foo>("stateA");
        final State<Foo> statePending = new StateImpl<Foo>("statePending");
        final State<Foo> stateEnd = new StateImpl<Foo>("stateEnd");

        // Transitions
        //
        stateA.addTransition(eventA, statePending, waitActionSpy);
        stateA.addTransition(eventB, stateEnd);

        statePending.addTransition(eventA, stateA);
        statePending.addTransition(eventB, statePending, wraSpy);

        // Create the Persister
        //
        final List<State<Foo>> states = new LinkedList<State<Foo>>();
        states.add(stateA);
        states.add(statePending);
        states.add(stateEnd);

        final Persister<Foo> persister = new MemoryPersisterImpl<Foo>(states, stateA);

        // Set the Persister
        //
        fsm.setPersister(persister);

        // Kick off a thread that will invoke the fsm with eventA
        //
        final Foo args = null;
        new Thread(new Runnable() {

            public void run() {
                try {
                    fsm.onEvent(stateful, eventA, args);
                } catch (final TooBusyException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        // Give eventA some time
        //
        Thread.sleep(5);

        // Fire off eventB
        //
        final State<Foo> current = fsm.onEvent(stateful, eventB, args);

        // Verify that we are at the end state
        //
        Assert.assertEquals(stateEnd, current);

        // Make sure we retried eventB at least 2 times (possibly more)
        //
        Mockito.verify(wraSpy, Mockito.atLeast(2)).execute(stateful, eventB, args);

        // Make sure we only called waitAction once
        //
        Mockito.verify(waitActionSpy, Mockito.times(1)).execute(stateful, eventA, args);

    }

    @Test(expected = TooBusyException.class)
    public void testTooBusy() throws TooBusyException {

        // Stateful
        //
        final Foo stateful = new Foo();

        // Events
        //
        final String eventA = "eventA";

        // Actions
        //
        final Action<Foo> throwAction = new Action<Foo>() {

            public void execute(final Foo stateful, final String event, final Object... args) throws RetryException {
                throw new RetryException();
            }
        };

        // States
        //
        final State<Foo> stateA = new StateImpl<Foo>("stateA");

        // Transitions
        //
        stateA.addTransition(eventA, stateA, throwAction);

        // FSM
        //
        final List<State<Foo>> states = new LinkedList<State<Foo>>();
        states.add(stateA);

        final Persister<Foo> persister = new MemoryPersisterImpl<Foo>(stateful, states, stateA);
        final FSM<Foo> fsm = new FSM<Foo>("TooBusy", persister);
        fsm.setRetryAttempts(1);

        // Boom
        //
        fsm.onEvent(stateful, eventA);
    }

    @Test
    public void testRetryInterval() {

        // Stateful
        //
        final Foo stateful = new Foo();

        // Events
        //
        final String eventA = "eventA";

        // States
        //
        final State<Foo> stateA = new StateImpl<Foo>("stateA", false, true); // blocking

        // FSM
        //
        final List<State<Foo>> states = new LinkedList<State<Foo>>();
        states.add(stateA);

        final Persister<Foo> persister = new MemoryPersisterImpl<Foo>(stateful, states, stateA);
        final FSM<Foo> fsm = new FSM<Foo>("TooBusy", persister);
        fsm.setRetryAttempts(1);
        fsm.setRetryInterval(500);

        // Run
        //
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
        final Foo stateful = new Foo();
        final String eventA = "eventA";
        final State<Foo> stateA = new StateImpl<Foo>("stateA", false, true);
        final List<State<Foo>> states = new LinkedList<State<Foo>>();
        states.add(stateA);
        final Persister<Foo> persister = new MemoryPersisterImpl<Foo>(stateful, states, stateA);
        final FSM<Foo> fsm = new FSM<Foo>("TooBusy", persister);
        fsm.setRetryAttempts(1);
        fsm.onEvent(stateful, eventA);
    }

    @Test(expected = TooBusyException.class)
    public void testRetryFailureOnTransition() throws TooBusyException {
        final Foo stateful = new Foo();
        final String eventA = "eventA";
        final State<Foo> stateA = new StateImpl<Foo>("stateA");
        stateA.addTransition(eventA, new Transition<Foo>() {

            public StateActionPair<Foo> getStateActionPair(final Foo stateful) throws RetryException {
                throw new RetryException();
            }

        });

        final List<State<Foo>> states = new LinkedList<State<Foo>>();
        states.add(stateA);

        final Persister<Foo> persister = new MemoryPersisterImpl<Foo>(stateful, states, stateA);
        final FSM<Foo> fsm = new FSM<Foo>("TooBusy", persister);
        fsm.setRetryAttempts(1);

        fsm.onEvent(stateful, eventA);
    }

    @Test
    public void testStateFieldName() {
        final Foo2 stateful = new Foo2();

        final State<Foo2> stateA = new StateImpl<Foo2>("stateA");
        final State<Foo2> stateB = new StateImpl<Foo2>("stateB");

        final List<State<Foo2>> states = new LinkedList<State<Foo2>>();
        states.add(stateA);
        states.add(stateB);

        final MemoryPersisterImpl<Foo2> persister = new MemoryPersisterImpl<Foo2>(stateful, states, stateA, "state");

        persister.setCurrent(stateful, stateB);

        Assert.assertEquals(stateB, persister.getCurrent(stateful));
    }

    @Test
    public void testTransitionOutOfBlocking() throws TooBusyException {
        final Foo stateful = new Foo();

        final String eventA = "eventA";
        final String eventB = "eventB";

        final State<Foo> stateA = new StateImpl<Foo>("stateA", false, true);
        final State<Foo> stateB = new StateImpl<Foo>("stateB");

        stateA.addTransition(eventB, stateB);

        final List<State<Foo>> states = new LinkedList<State<Foo>>();
        states.add(stateA);
        states.add(stateB);

        final Persister<Foo> persister = new MemoryPersisterImpl<Foo>(stateful, states, stateA);
        final FSM<Foo> fsm = new FSM<Foo>("TooBusy", persister);
        fsm.setRetryAttempts(1000);

        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(500);
                    fsm.onEvent(stateful, eventB);
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        final State<Foo> state = fsm.onEvent(stateful, eventA);
        Assert.assertEquals(stateB, state);
    }
}
