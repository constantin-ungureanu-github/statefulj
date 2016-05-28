package org.statefulj.framework.core.model;

import java.lang.annotation.Annotation;
import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;

public interface PersistenceSupportBeanFactory {

    Class<?> getKey();

    Class<?> getIdType();

    Class<? extends Annotation> getIdAnnotationType();

    BeanDefinition buildFactoryBean(Class<?> statefulClass);

    BeanDefinition buildFinderBean(String repoBeanId);

    BeanDefinition buildPersisterBean(Class<?> statefulClass, String repoBeanId, BeanDefinition repoBeanDefinitionFactory, String stateFieldName, String startStateId,
            List<RuntimeBeanReference> stateBeans);

    BeanDefinition buildFSMHarnessBean(Class<?> statefulClass, String fsmBeanId, String factoryId, String finderId, BeanDefinition repoBeanDefinitionFactory);
}
