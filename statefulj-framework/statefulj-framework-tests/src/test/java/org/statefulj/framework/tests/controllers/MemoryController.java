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

import static org.statefulj.framework.tests.model.MemoryObject.ONE_STATE;
import static org.statefulj.framework.tests.model.MemoryObject.TWO_STATE;

import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.framework.tests.model.MemoryObject;

@StatefulController(clazz = MemoryObject.class, startState = ONE_STATE)
public class MemoryController {

    @Transition(from = ONE_STATE, event = "one", to = TWO_STATE)
    public MemoryObject oneToTwo(MemoryObject obj, String event) {
        return obj;
    }

    @Transition(from = ONE_STATE, event = "fail", to = TWO_STATE, reload = true)
    public void failReload(MemoryObject obj, String event) {
    }

}
