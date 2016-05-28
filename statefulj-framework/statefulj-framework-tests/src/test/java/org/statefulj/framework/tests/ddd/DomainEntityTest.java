package org.statefulj.framework.tests.ddd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.statefulj.framework.tests.utils.ReflectionUtils.invoke;

import java.lang.reflect.InvocationTargetException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.statefulj.framework.core.annotations.FSM;
import org.statefulj.framework.core.model.ReferenceFactory;
import org.statefulj.framework.core.model.StatefulFSM;
import org.statefulj.framework.core.model.impl.ReferenceFactoryImpl;
import org.statefulj.fsm.TooBusyException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "/applicationContext-DomainEntityTests.xml" })
public class DomainEntityTest {

    @FSM
    StatefulFSM<DomainEntity> fsm;

    @Resource
    ApplicationContext appContext;

    @Test
    public void testDomainEntityFSM() throws TooBusyException, SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final ReferenceFactory refFactory = new ReferenceFactoryImpl("domainEntity");
        final Object mvcBinder = appContext.getBean(refFactory.getBinderId("springmvc"));

        final HttpServletRequest context = mock(HttpServletRequest.class);
        final DomainEntity entity = invoke(mvcBinder, "$_get_event-x", DomainEntity.class, context, 1);

        assertNotNull(entity);
        assertEquals(1, entity.getValue());
        assertEquals(DomainEntity.STATE_B, entity.getState());

        entity.onEventY(2);
        assertEquals(2, entity.getValue());
        assertEquals(DomainEntity.STATE_A, entity.getState());
    }
}