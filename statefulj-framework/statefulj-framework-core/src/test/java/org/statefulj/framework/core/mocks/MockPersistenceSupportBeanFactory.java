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
package org.statefulj.framework.core.mocks;

import java.lang.annotation.Annotation;
import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.statefulj.framework.core.model.PersistenceSupportBeanFactory;

public class MockPersistenceSupportBeanFactory implements PersistenceSupportBeanFactory {

    public Class<?> getKey() {
        return MockRepositoryFactoryBeanSupport.class;
    }

    public Class<?> getIdType() {
        return Object.class;
    }

    public Class<? extends Annotation> getIdAnnotationType() {
        return null;
    }

    public BeanDefinition buildFactoryBean(final Class<?> statefulClass) {
        return mockDef();
    }

    public BeanDefinition buildFinderBean(final String repoBeanId) {
        return mockDef();
    }

    public BeanDefinition buildPersisterBean(final Class<?> statefulClass, final String repoBeanId, final BeanDefinition repoBeanDefinitionFactory, final String stateFieldName,
            final String startStateId, final List<RuntimeBeanReference> stateBeans) {
        return mockDef();
    }

    public BeanDefinition buildFSMHarnessBean(final Class<?> statefulClass, final String fsmBeanId, final String factoryId, final String finderId, final BeanDefinition repoBeanDefinitionFactory) {
        return mockDef();
    }

    private BeanDefinition mockDef() {
        return BeanDefinitionBuilder.genericBeanDefinition(Object.class).getBeanDefinition();
    }
}
