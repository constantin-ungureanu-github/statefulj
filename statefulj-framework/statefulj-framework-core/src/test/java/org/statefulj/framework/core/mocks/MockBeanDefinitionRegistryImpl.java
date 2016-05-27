package org.statefulj.framework.core.mocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

public class MockBeanDefinitionRegistryImpl implements BeanDefinitionRegistry {
    private final Map<String, BeanDefinition> registry = new HashMap<String, BeanDefinition>();

    public String[] getAliases(final String arg0) {
        return null;
    }

    public boolean isAlias(final String arg0) {
        return false;
    }

    public void registerAlias(final String arg0, final String arg1) {
    }

    public void removeAlias(final String arg0) {
    }

    public void registerBeanDefinition(final String beanName, final BeanDefinition beanDefinition) throws BeanDefinitionStoreException {
        registry.put(beanName, beanDefinition);
    }

    public void removeBeanDefinition(final String beanName) throws NoSuchBeanDefinitionException {
        registry.remove(beanName);
    }

    public BeanDefinition getBeanDefinition(final String beanName) throws NoSuchBeanDefinitionException {
        return registry.get(beanName);
    }

    public boolean containsBeanDefinition(final String beanName) {
        return registry.containsKey(beanName);
    }

    public String[] getBeanDefinitionNames() {
        final ArrayList<String> keys = new ArrayList<String>();
        keys.addAll(registry.keySet());
        return keys.toArray(new String[] {});
    }

    public int getBeanDefinitionCount() {
        return registry.size();
    }

    public boolean isBeanNameInUse(final String beanName) {
        return false;
    }
}
