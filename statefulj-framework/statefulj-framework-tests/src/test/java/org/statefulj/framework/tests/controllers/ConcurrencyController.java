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
package org.statefulj.framework.tests.controllers;

import static org.statefulj.framework.tests.model.User.ONE_STATE;
import static org.statefulj.framework.tests.model.User.THREE_STATE;
import static org.statefulj.framework.tests.model.User.TWO_STATE;

import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.framework.tests.model.User;

@StatefulController(clazz = User.class, startState = ONE_STATE, noops = { @Transition(from = TWO_STATE, event = "two", to = THREE_STATE) })
public class ConcurrencyController {

    @Transition(from = ONE_STATE, event = "one", to = TWO_STATE)
    public void oneOneTwo(User user, String event, Object monitor) throws InterruptedException {
        monitor.notify();
        monitor.wait();
    }
}
