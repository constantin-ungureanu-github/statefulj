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
package org.statefulj.framework.core.model.impl;

import java.lang.annotation.Annotation;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.statefulj.framework.core.model.PersistenceSupportBeanFactory;
import org.statefulj.persistence.memory.MemoryPersisterImpl;

/**
 * Fallback Persister factory. The MemoryPersister is only used when the Entity does not have a supported Repository and the StatefulController does not have either the factoryId, finderId or
 * persisterId specified. The Memory Persister only supports the instantiation of the Stateful Enitity and the persistence of State - it does not support finding of the Stateful Entity.
 */
public class MemoryPersistenceSupportBeanFactoryImpl implements PersistenceSupportBeanFactory {
    public Class<?> getKey() {
        return null;
    }

    public Class<?> getIdType() {
        return null;
    }

    public Class<? extends Annotation> getIdAnnotationType() {
        return null;
    }

    public BeanDefinition buildFactoryBean(final Class<?> statefulClass) {
        final BeanDefinition factoryBean = BeanDefinitionBuilder.genericBeanDefinition(FactoryImpl.class).getBeanDefinition();
        return factoryBean;
    }

    public BeanDefinition buildFinderBean(final String repoBeanId) {
        throw new NotImplementedException("Memory persister does not support the Finder Bean");
    }

    public BeanDefinition buildPersisterBean(final Class<?> statefulClass, final String repoBeanId, final BeanDefinition repoBeanDefinitionFactory, final String stateFieldName,
            final String startStateId, final List<RuntimeBeanReference> stateBeans) {
        final BeanDefinition persisterBean = BeanDefinitionBuilder.genericBeanDefinition(MemoryPersisterImpl.class).getBeanDefinition();
        final ConstructorArgumentValues args = persisterBean.getConstructorArgumentValues();
        args.addIndexedArgumentValue(0, stateBeans);
        args.addIndexedArgumentValue(1, new RuntimeBeanReference(startStateId));
        args.addIndexedArgumentValue(2, stateFieldName);
        return persisterBean;
    }

    public BeanDefinition buildFSMHarnessBean(final Class<?> statefulClass, final String fsmBeanId, final String factoryId, final String finderId, final BeanDefinition repoBeanDefinitionFactory) {
        throw new NotImplementedException("Memory persister does not support usage of the FSMHarness");
    }
}
