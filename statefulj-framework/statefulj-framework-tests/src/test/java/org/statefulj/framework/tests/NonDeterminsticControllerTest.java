package org.statefulj.framework.tests;

import static org.junit.Assert.assertEquals;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.statefulj.framework.core.annotations.FSM;
import org.statefulj.framework.core.model.StatefulFSM;
import org.statefulj.framework.tests.dao.UserRepository;
import org.statefulj.framework.tests.model.User;
import org.statefulj.fsm.TooBusyException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "/applicationContext-StatefulControllerTests.xml" })
public class NonDeterminsticControllerTest {

    @Resource
    UserRepository userRepo;

    @FSM("nonDetermisticController")
    StatefulFSM<User> nonDetermisticFSM;

    @Test
    public void testNonDeterministicTransitions() throws TooBusyException {

        final User user = userRepo.save(new User());

        String retVal = (String) nonDetermisticFSM.onEvent(user, "non-determinstic", true);

        assertEquals("onTrue", retVal);
        assertEquals(User.TWO_STATE, user.getState());

        retVal = (String) nonDetermisticFSM.onEvent(user, "non-determinstic", false);

        assertEquals("onFalse", retVal);
        assertEquals(User.THREE_STATE, user.getState());
    }
}
