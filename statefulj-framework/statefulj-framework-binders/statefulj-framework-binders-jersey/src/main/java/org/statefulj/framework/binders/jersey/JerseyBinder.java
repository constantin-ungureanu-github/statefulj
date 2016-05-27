package org.statefulj.framework.binders.jersey;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.statefulj.framework.binders.common.AbstractRestfulBinder;
import org.statefulj.framework.binders.common.utils.JavassistUtils;
import org.statefulj.framework.core.model.ReferenceFactory;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.StringMemberValue;

public class JerseyBinder extends AbstractRestfulBinder {
    private static final Logger logger = LoggerFactory.getLogger(JerseyBinder.class);
    public final static String KEY = "jersey";
    private final String JERSEY_SUFFIX = "JerseyBinder";

    public String getKey() {
        return JerseyBinder.KEY;
    }

    @Override
    public Class<?> bindEndpoints(final String beanName, final Class<?> statefulControllerClass, final Class<?> idType, final boolean isDomainEntity, final Map<String, Method> eventMapping,
            final ReferenceFactory refFactory) throws CannotCompileException, NotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {

        final Class<?> binding = super.bindEndpoints(beanName, statefulControllerClass, idType, isDomainEntity, eventMapping, refFactory);

        BindingsRegistry.addBinding(binding);

        return binding;
    }

    @Override
    protected CtClass buildProxy(final ClassPool cp, final String beanName, final String proxyClassName, final Class<?> statefulControllerClass, final Class<?> idType, final boolean isDomainEntity,
            final Map<String, Method> eventMapping, final ReferenceFactory refFactory)
            throws CannotCompileException, NotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {

        JerseyBinder.logger.debug("Building proxy for {}", statefulControllerClass);

        final CtClass proxyClass = super.buildProxy(cp, beanName, proxyClassName, statefulControllerClass, idType, isDomainEntity, eventMapping, refFactory);

        JavassistUtils.addClassAnnotation(proxyClass, Path.class, "value", "");

        return proxyClass;
    }

    @Override
    protected void addEndpointMapping(final CtMethod ctMethod, final String method, final String request) {

        final MethodInfo methodInfo = ctMethod.getMethodInfo();
        final ConstPool constPool = methodInfo.getConstPool();

        final AnnotationsAttribute annoAttr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
        final Annotation pathMapping = new Annotation(Path.class.getName(), constPool);

        final StringMemberValue valueVal = new StringMemberValue(constPool);
        valueVal.setValue(request);

        pathMapping.addMemberValue("value", valueVal);

        annoAttr.addAnnotation(pathMapping);

        final String verbClassName = "javax.ws.rs." + method;
        final Annotation verb = new Annotation(verbClassName, constPool);
        annoAttr.addAnnotation(verb);

        methodInfo.addAttribute(annoAttr);
    }

    @Override
    protected Annotation[] addHttpRequestParameter(final CtMethod ctMethod, final ClassPool cp) throws NotFoundException, CannotCompileException {

        super.addHttpRequestParameter(ctMethod, cp);

        return new Annotation[] { new Annotation(ctMethod.getMethodInfo().getConstPool(), cp.getCtClass(Context.class.getName())) };
    }

    @Override
    protected String getSuffix() {
        return JERSEY_SUFFIX;
    }

    @Override
    protected Class<?> getPathAnnotationClass() {
        return PathParam.class;
    }
}
