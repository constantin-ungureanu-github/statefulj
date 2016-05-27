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

/**
 * Indicates that the evaluated State was inconsistent with the Persisted State
 * 
 * @author Andrew Hall
 *
 */
public class StaleStateException extends RetryException {

    private static final long serialVersionUID = 1L;

    public StaleStateException() {
        super();
    }

    public StaleStateException(String err) {
        super(err);
    }
}
