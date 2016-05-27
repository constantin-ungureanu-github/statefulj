package org.statefulj.framework.tests;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "/applicationContext-AbstractBeanTests.xml" })
public class AbstractBeanTest {

    @Resource
    ApplicationContext appContext;

    @Test
    public void testInitWithAbstractBean() {
        final Object nonAbstractBean = appContext.getBean("nonAbstractBean");
        Assert.assertNotNull(nonAbstractBean);
    }
}
