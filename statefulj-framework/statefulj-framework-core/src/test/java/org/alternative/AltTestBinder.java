package org.alternative;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.statefulj.framework.core.mocks.MockProxy;
import org.statefulj.framework.core.model.EndpointBinder;
import org.statefulj.framework.core.model.ReferenceFactory;

import javassist.CannotCompileException;
import javassist.NotFoundException;

public class AltTestBinder implements EndpointBinder {
    public String getKey() {
        return "test";
    }

    public Class<?> bindEndpoints(final String beanId, final Class<?> stateControllerClass, final Class<?> idType, final boolean isDomainEntity, final Map<String, Method> eventMapping,
            final ReferenceFactory refFactory) throws CannotCompileException, NotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {

        return MockProxy.class;
    }
}
