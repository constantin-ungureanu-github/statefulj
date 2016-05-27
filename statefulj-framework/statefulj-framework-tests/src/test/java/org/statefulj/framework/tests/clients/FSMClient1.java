package org.statefulj.framework.tests.clients;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.statefulj.framework.core.annotations.FSM;
import org.statefulj.framework.core.model.StatefulFSM;
import org.statefulj.framework.tests.model.User;

@Component
public class FSMClient1 {

    public StatefulFSM<User> userStatefulFSM;

    @Inject
    public FSMClient1(@FSM("userController") final StatefulFSM<User> userStatefulFSM) {
        this.userStatefulFSM = userStatefulFSM;
    }
}
