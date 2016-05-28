package org.statefulj.framework.tests;

import java.lang.reflect.InvocationTargetException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.statefulj.framework.core.annotations.FSM;
import org.statefulj.framework.core.model.FSMHarness;
import org.statefulj.framework.core.model.ReferenceFactory;
import org.statefulj.framework.core.model.StatefulFSM;
import org.statefulj.framework.core.model.impl.ReferenceFactoryImpl;
import org.statefulj.framework.tests.clients.FSMClient1;
import org.statefulj.framework.tests.clients.FSMClient2;
import org.statefulj.framework.tests.dao.UserRepository;
import org.statefulj.framework.tests.model.MemoryObject;
import org.statefulj.framework.tests.model.User;
import org.statefulj.framework.tests.utils.ReflectionUtils;
import org.statefulj.fsm.Persister;
import org.statefulj.fsm.StaleStateException;
import org.statefulj.fsm.TooBusyException;
import org.statefulj.fsm.model.State;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "/applicationContext-StatefulControllerTests.xml" })
public class StatefulControllerTest {
    @Resource
    ApplicationContext appContext;

    @Resource
    UserRepository userRepo;

    @Resource
    JpaTransactionManager transactionManager;

    @Resource(name = "userController.fsmHarness")
    FSMHarness userFSMHarness;

    @FSM("userController")
    StatefulFSM<User> userFSM;

    @FSM("concurrencyController")
    StatefulFSM<User> concurrencyFSM;

    @FSM("overloadedMethodController")
    StatefulFSM<User> overloadFSM;

    @FSM
    StatefulFSM<MemoryObject> memoryFSM;

    @Resource
    FSMClient1 fsmClient1;

    @Resource
    FSMClient2 fsmClient2;

    @Test
    public void testConstructorInjectionWithDisambiquation() {
        Assert.assertNotNull(fsmClient1.userStatefulFSM);
    }

    @Test
    public void testConstructorInjection() {
        Assert.assertNotNull(fsmClient2.userStatefulFSM);
        Assert.assertNotNull(fsmClient2.memoryObjectStatefulFSM);
    }

    @Test
    public void testInMemoryController() throws TooBusyException {
        MemoryObject memObject = new MemoryObject();
        memObject = (MemoryObject) memoryFSM.onEvent(memObject, "one");

        Assert.assertNotNull(memObject);
        Assert.assertEquals(MemoryObject.TWO_STATE, memObject.getState());
    }

    @Test(expected = RuntimeException.class)
    public void testFailedReloadForInMemoryController() throws TooBusyException {
        final MemoryObject memObject = new MemoryObject();
        memoryFSM.onEvent(memObject, "fail");
    }

    @Test
    public void testStateTransitions() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, TooBusyException {
        Assert.assertNotNull(userFSM);

        final ReferenceFactory refFactory = new ReferenceFactoryImpl("userController");
        final Object mvcBinder = appContext.getBean(refFactory.getBinderId("springmvc"));
        final Object jerseyBinder = appContext.getBean(refFactory.getBinderId("jersey"));
        final Object camelBinder = appContext.getBean(refFactory.getBinderId("camel"));
        Assert.assertNotNull(mvcBinder);
        Assert.assertNotNull(camelBinder);

        final HttpServletRequest context = Mockito.mock(HttpServletRequest.class);
        User user = ReflectionUtils.invoke(mvcBinder, "$_get_first", User.class, context);

        Assert.assertNotNull(user);
        Assert.assertTrue(user.getId() > 0);
        Assert.assertEquals(User.TWO_STATE, user.getState());

        user = ReflectionUtils.invoke(mvcBinder, "$_get_id_any", User.class, user.getId(), context);

        Assert.assertNotNull(user);
        Assert.assertTrue(user.getId() > 0);
        Assert.assertEquals(User.TWO_STATE, user.getState());

        user = ReflectionUtils.invoke(mvcBinder, "$_post_id_second", User.class, user.getId(), context);

        Assert.assertTrue(user.getId() > 0);
        Assert.assertEquals(User.THREE_STATE, user.getState());

        user = ReflectionUtils.invoke(mvcBinder, "$_get_id_any", User.class, user.getId(), context);

        Assert.assertNotNull(user);
        Assert.assertTrue(user.getId() > 0);
        Assert.assertEquals(User.THREE_STATE, user.getState());

        final Object nulObj = ReflectionUtils.invoke(mvcBinder, "$_get_id_four", User.class, user.getId(), context);

        Assert.assertNull(nulObj);
        user = userRepo.findOne(user.getId());
        Assert.assertEquals(User.FOUR_STATE, user.getState());

        userFSMHarness.onEvent("five", user.getId(), new Object[] { context });
        user = userRepo.findOne(user.getId());
        Assert.assertEquals(User.FIVE_STATE, user.getState());

        Assert.assertEquals(mvcBinder.getClass().getMethod("$_handleError", Exception.class),
                org.statefulj.common.utils.ReflectionUtils.getFirstAnnotatedMethod(mvcBinder.getClass(), ExceptionHandler.class));

        final String retVal = ReflectionUtils.invoke(mvcBinder, "$_handleError", String.class, new Exception());
        Assert.assertEquals("called", retVal);

        ReflectionUtils.invoke(camelBinder, "$_camelone", user.getId());
        ReflectionUtils.invoke(camelBinder, "$_six", user.getId());
        user = userRepo.findOne(user.getId());
        Assert.assertEquals(User.SIX_STATE, user.getState());

        final User retUser = ReflectionUtils.invoke(jerseyBinder, "$_get_id_one", User.class, user.getId(), context);
        Assert.assertNotNull(retUser);
    }

    @Test
    public void testOverloadedMethod() throws TooBusyException {
        Assert.assertNotNull(overloadFSM);

        final User user = new User();
        String response = (String) overloadFSM.onEvent(user, "one");
        Assert.assertEquals("method1", response);

        response = (String) overloadFSM.onEvent(user, "two", "foo");
        Assert.assertEquals("method2", response);

        response = (String) overloadFSM.onEvent(user, "three", 1);
        Assert.assertEquals("method3", response);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = TooBusyException.class)
    public void testBlockedState() throws TooBusyException, StaleStateException {
        Assert.assertNotNull(userFSM);
        final ReferenceFactory refFactory = new ReferenceFactoryImpl("userController");

        User user = new User();
        user = userRepo.save(user);

        final State<User> stateSix = (State<User>) appContext.getBean(refFactory.getStateId(User.SIX_STATE));
        final Persister<User> persister = (Persister<User>) appContext.getBean(refFactory.getPersisterId());
        persister.setCurrent(user, persister.getCurrent(user), stateSix);

        Assert.assertEquals(stateSix, persister.getCurrent(user));
        final org.statefulj.framework.core.fsm.FSM<User, ?> fsm = (org.statefulj.framework.core.fsm.FSM<User, ?>) appContext.getBean(refFactory.getFSMId());
        fsm.setRetryAttempts(1);
        fsm.onEvent(user, "block.me");
    }
}
