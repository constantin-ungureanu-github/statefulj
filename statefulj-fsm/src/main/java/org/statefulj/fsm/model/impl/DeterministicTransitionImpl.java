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
package org.statefulj.fsm.model.impl;

import org.statefulj.fsm.model.Action;
import org.statefulj.fsm.model.State;
import org.statefulj.fsm.model.StateActionPair;
import org.statefulj.fsm.model.Transition;

public class DeterministicTransitionImpl<T> implements Transition<T> {

    private StateActionPair<T> stateActionPair;

    public DeterministicTransitionImpl(final State<T> from, final State<T> to, final String event) {
        stateActionPair = new StateActionPairImpl<T>(to, null);
        from.addTransition(event, this);
    }

    public DeterministicTransitionImpl(final State<T> from, final State<T> to, final String event, final Action<T> action) {
        stateActionPair = new StateActionPairImpl<T>(to, action);
        from.addTransition(event, this);
    }

    public DeterministicTransitionImpl(final State<T> to, final Action<T> action) {
        stateActionPair = new StateActionPairImpl<T>(to, action);
    }

    public DeterministicTransitionImpl(final State<T> to) {
        stateActionPair = new StateActionPairImpl<T>(to, null);
    }

    public StateActionPair<T> getStateActionPair(final T stateful) {
        return stateActionPair;
    }

    public void setStateActionPair(final StateActionPair<T> stateActionPair) {
        this.stateActionPair = stateActionPair;
    }

    @Override
    public String toString() {
        return "DeterministicTransition[state=" + this.stateActionPair.getState().getName() + ", action=" + this.stateActionPair.getAction() + "]";
    }
}
