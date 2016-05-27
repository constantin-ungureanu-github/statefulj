package org.statefulj.framework.persistence.jpa;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.persistence.Id;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.statefulj.framework.core.model.PersistenceSupportBeanFactory;
import org.statefulj.framework.core.model.impl.CrudRepositoryFinderImpl;
import org.statefulj.framework.core.model.impl.FactoryImpl;
import org.statefulj.persistence.jpa.JPAPerister;

public class JPAPersistenceSupportBeanFactory implements PersistenceSupportBeanFactory {
    public Class<?> getKey() {
        return JpaRepositoryFactoryBean.class;
    }

    public Class<?> getIdType() {
        return Long.class;
    }

    public Class<? extends Annotation> getIdAnnotationType() {
        return Id.class;
    }

    public BeanDefinition buildFactoryBean(final Class<?> statefulClass) {
        final BeanDefinition factoryBean = BeanDefinitionBuilder.genericBeanDefinition(FactoryImpl.class).getBeanDefinition();
        return factoryBean;
    }

    public BeanDefinition buildFinderBean(final String repoFactoryBeanId) {
        final BeanDefinition finderBean = BeanDefinitionBuilder.genericBeanDefinition(CrudRepositoryFinderImpl.class).getBeanDefinition();
        final ConstructorArgumentValues args = finderBean.getConstructorArgumentValues();
        args.addIndexedArgumentValue(0, new RuntimeBeanReference(repoFactoryBeanId));
        return finderBean;
    }

    public BeanDefinition buildPersisterBean(final Class<?> statefulClass, final String repoBeanId, final BeanDefinition repoBeanDefinitionFactory, final String stateFieldName,
            final String startStateId, final List<RuntimeBeanReference> stateBeans) {
        final BeanDefinition entityMgr = (BeanDefinition) repoBeanDefinitionFactory.getPropertyValues().getPropertyValue("entityManager").getValue();
        final String tmId = (String) repoBeanDefinitionFactory.getPropertyValues().getPropertyValue("transactionManager").getValue();
        final BeanDefinition persisterBean = BeanDefinitionBuilder.genericBeanDefinition(JPAPerister.class).getBeanDefinition();
        final ConstructorArgumentValues args = persisterBean.getConstructorArgumentValues();
        args.addIndexedArgumentValue(0, stateBeans);
        args.addIndexedArgumentValue(1, stateFieldName);
        args.addIndexedArgumentValue(2, new RuntimeBeanReference(startStateId));
        args.addIndexedArgumentValue(3, statefulClass);
        args.addIndexedArgumentValue(4, entityMgr);
        args.addIndexedArgumentValue(5, new RuntimeBeanReference(tmId));
        return persisterBean;
    }

    public BeanDefinition buildFSMHarnessBean(final Class<?> statefulClass, final String fsmBeanId, final String factoryId, final String finderId, final BeanDefinition repoBeanDefinitionFactory) {
        final String tmId = (String) repoBeanDefinitionFactory.getPropertyValues().getPropertyValue("transactionManager").getValue();

        final BeanDefinition fsmHarness = BeanDefinitionBuilder.genericBeanDefinition(JPAFSMHarnessImpl.class).getBeanDefinition();
        final ConstructorArgumentValues args = fsmHarness.getConstructorArgumentValues();
        args.addIndexedArgumentValue(0, new RuntimeBeanReference(fsmBeanId));
        args.addIndexedArgumentValue(1, statefulClass);
        args.addIndexedArgumentValue(2, new RuntimeBeanReference(factoryId));
        args.addIndexedArgumentValue(3, new RuntimeBeanReference(finderId));
        args.addIndexedArgumentValue(4, new RuntimeBeanReference(tmId));
        return fsmHarness;
    }
}
