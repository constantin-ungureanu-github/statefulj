package org.statefulj.framework.core;

import org.alternative.AltTestRepositoryFactoryBeanSupport;
import org.alternative.AltTestUserController;
import org.alternative.AltTestUserRepository;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.statefulj.framework.core.controllers.FailedMemoryController;
import org.statefulj.framework.core.controllers.MemoryController;
import org.statefulj.framework.core.controllers.NoRetryController;
import org.statefulj.framework.core.controllers.UserController;
import org.statefulj.framework.core.dao.UserRepository;
import org.statefulj.framework.core.mocks.MockBeanDefinitionRegistryImpl;
import org.statefulj.framework.core.mocks.MockProxy;
import org.statefulj.framework.core.mocks.MockRepositoryFactoryBeanSupport;
import org.statefulj.framework.core.model.ReferenceFactory;
import org.statefulj.framework.core.model.impl.ReferenceFactoryImpl;
import org.statefulj.persistence.memory.MemoryPersisterImpl;

public class StatefulFactoryTest {

    @Test
    public void testFSMConstruction() throws ClassNotFoundException, NoSuchMethodException, SecurityException {

        final BeanDefinitionRegistry registry = new MockBeanDefinitionRegistryImpl();

        final BeanDefinition userRepo = BeanDefinitionBuilder.genericBeanDefinition(MockRepositoryFactoryBeanSupport.class).getBeanDefinition();
        userRepo.getPropertyValues().add("repositoryInterface", UserRepository.class.getName());

        registry.registerBeanDefinition("userRepo", userRepo);

        final BeanDefinition userController = BeanDefinitionBuilder.genericBeanDefinition(UserController.class).getBeanDefinition();

        registry.registerBeanDefinition("userController", userController);

        final ReferenceFactory refFactory = new ReferenceFactoryImpl("userController");
        final StatefulFactory factory = new StatefulFactory();

        factory.postProcessBeanDefinitionRegistry(registry);

        final BeanDefinition userControllerMVCProxy = registry.getBeanDefinition(refFactory.getBinderId("mock"));

        Assert.assertNotNull(userControllerMVCProxy);

        final Class<?> proxyClass = Class.forName(userControllerMVCProxy.getBeanClassName());

        Assert.assertNotNull(proxyClass);

        Assert.assertEquals(MockProxy.class, proxyClass);

        // Verify that FIVE_STATE is blocking
        //
        final BeanDefinition stateFive = registry.getBeanDefinition(refFactory.getStateId(UserController.FIVE_STATE));

        Assert.assertEquals(true, stateFive.getConstructorArgumentValues().getArgumentValue(2, Boolean.class).getValue());

        final BeanDefinition fsm = registry.getBeanDefinition(refFactory.getFSMId());
        Assert.assertNotNull(fsm);
        Assert.assertEquals(20, fsm.getConstructorArgumentValues().getArgumentValue(2, Integer.class).getValue());
        Assert.assertEquals(250, fsm.getConstructorArgumentValues().getArgumentValue(3, Integer.class).getValue());
    }

    @Test
    public void testFSMConstructionWithNonDefaultRetry() throws ClassNotFoundException, NoSuchMethodException, SecurityException {

        final BeanDefinitionRegistry registry = new MockBeanDefinitionRegistryImpl();

        final BeanDefinition userRepo = BeanDefinitionBuilder.genericBeanDefinition(MockRepositoryFactoryBeanSupport.class).getBeanDefinition();
        userRepo.getPropertyValues().add("repositoryInterface", UserRepository.class.getName());

        registry.registerBeanDefinition("userRepo", userRepo);

        final BeanDefinition noRetryController = BeanDefinitionBuilder.genericBeanDefinition(NoRetryController.class).getBeanDefinition();

        registry.registerBeanDefinition("noRetryController", noRetryController);

        final ReferenceFactory refFactory = new ReferenceFactoryImpl("noRetryController");

        final StatefulFactory factory = new StatefulFactory();

        factory.postProcessBeanDefinitionRegistry(registry);

        final BeanDefinition fsm = registry.getBeanDefinition(refFactory.getFSMId());
        Assert.assertNotNull(fsm);
        Assert.assertEquals(1, fsm.getConstructorArgumentValues().getArgumentValue(2, Integer.class).getValue());
        Assert.assertEquals(1, fsm.getConstructorArgumentValues().getArgumentValue(3, Integer.class).getValue());
    }

    @Test
    public void testAlternativePackages() throws ClassNotFoundException {
        final BeanDefinitionRegistry registry = new MockBeanDefinitionRegistryImpl();

        final BeanDefinition testUserRepo = BeanDefinitionBuilder.genericBeanDefinition(AltTestRepositoryFactoryBeanSupport.class).getBeanDefinition();
        testUserRepo.getPropertyValues().add("repositoryInterface", AltTestUserRepository.class.getName());

        registry.registerBeanDefinition("testUserRepo", testUserRepo);

        final BeanDefinition testUserController = BeanDefinitionBuilder.genericBeanDefinition(AltTestUserController.class).getBeanDefinition();

        registry.registerBeanDefinition("testUserController", testUserController);

        final ReferenceFactory refFactory = new ReferenceFactoryImpl("testUserController");
        final StatefulFactory factory = new StatefulFactory("org.alternative");

        factory.postProcessBeanDefinitionRegistry(registry);

        final BeanDefinition testUserControllerMVCProxy = registry.getBeanDefinition(refFactory.getBinderId("test"));

        Assert.assertNotNull(testUserControllerMVCProxy);

        final Class<?> proxyClass = Class.forName(testUserControllerMVCProxy.getBeanClassName());

        Assert.assertNotNull(proxyClass);

        Assert.assertEquals(MockProxy.class, proxyClass);

        final BeanDefinition stateOne = registry.getBeanDefinition(refFactory.getStateId(AltTestUserController.ONE_STATE));

        Assert.assertNotNull(stateOne);

        final BeanDefinition persister = registry.getBeanDefinition(refFactory.getPersisterId());
        Assert.assertNotNull(persister);
    }

    @Test
    public void testMemoryPersistor() throws ClassNotFoundException {
        final BeanDefinitionRegistry registry = new MockBeanDefinitionRegistryImpl();

        final BeanDefinition memoryController = BeanDefinitionBuilder.genericBeanDefinition(MemoryController.class).getBeanDefinition();

        registry.registerBeanDefinition("memoryController", memoryController);

        final ReferenceFactory refFactory = new ReferenceFactoryImpl("memoryController");
        final StatefulFactory factory = new StatefulFactory();

        factory.postProcessBeanDefinitionRegistry(registry);

        final BeanDefinition fsm = registry.getBeanDefinition(refFactory.getFSMId());
        Assert.assertNotNull(fsm);

        final BeanDefinition persister = registry.getBeanDefinition(refFactory.getPersisterId());
        Assert.assertNotNull(persister);
        Assert.assertEquals(MemoryPersisterImpl.class.getName(), persister.getBeanClassName());

        final BeanDefinition harness = registry.getBeanDefinition(refFactory.getFSMHarnessId());
        Assert.assertNull(harness);
    }

    @Test(expected = RuntimeException.class)
    public void testMemoryFailurePersistor() throws ClassNotFoundException {
        final BeanDefinitionRegistry registry = new MockBeanDefinitionRegistryImpl();
        final BeanDefinition failedMemoryController = BeanDefinitionBuilder.genericBeanDefinition(FailedMemoryController.class).getBeanDefinition();

        registry.registerBeanDefinition("failedMemoryController", failedMemoryController);
        final StatefulFactory factory = new StatefulFactory();
        factory.postProcessBeanDefinitionRegistry(registry);
    }
}
