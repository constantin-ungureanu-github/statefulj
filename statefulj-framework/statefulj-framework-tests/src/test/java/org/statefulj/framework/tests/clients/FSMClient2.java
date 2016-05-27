package org.statefulj.framework.tests.clients;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.statefulj.framework.core.annotations.FSM;
import org.statefulj.framework.core.model.StatefulFSM;
import org.statefulj.framework.tests.model.MemoryObject;
import org.statefulj.framework.tests.model.User;

@Component
public class FSMClient2 {
    public StatefulFSM<User> userStatefulFSM;
    public StatefulFSM<MemoryObject> memoryObjectStatefulFSM;

    @Inject
    public FSMClient2(@FSM("concurrencyController") final StatefulFSM<User> userStatefulFSM, @FSM final StatefulFSM<MemoryObject> memoryObjectStatefulFSM) {
        this.userStatefulFSM = userStatefulFSM;
        this.memoryObjectStatefulFSM = memoryObjectStatefulFSM;
    }
}
