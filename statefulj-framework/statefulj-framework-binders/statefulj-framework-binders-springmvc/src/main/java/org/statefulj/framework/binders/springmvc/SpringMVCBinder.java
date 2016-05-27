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
package org.statefulj.framework.binders.springmvc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.statefulj.framework.binders.common.AbstractRestfulBinder;
import org.statefulj.framework.binders.common.utils.JavassistUtils;
import org.statefulj.framework.core.model.ReferenceFactory;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.EnumMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.StringMemberValue;

public class SpringMVCBinder extends AbstractRestfulBinder {

    public final static String KEY = "springmvc";
    private static final Logger logger = LoggerFactory.getLogger(SpringMVCBinder.class);
    private final String MVC_SUFFIX = "MVCBinder";
    private final String CONTROLLER_VAR = "controller";
    private final Class<?>[] proxyable = new Class<?>[] { ExceptionHandler.class, InitBinder.class };

    public String getKey() {
        return SpringMVCBinder.KEY;
    }

    @Override
    protected CtClass buildProxy(final ClassPool cp, final String beanName, final String proxyClassName, final Class<?> statefulControllerClass, final Class<?> idType, final boolean isDomainEntity,
            final Map<String, Method> eventMapping, final ReferenceFactory refFactory)
            throws CannotCompileException, NotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {

        SpringMVCBinder.logger.debug("Building proxy for {}", statefulControllerClass);

        final CtClass proxyClass = super.buildProxy(cp, beanName, proxyClassName, statefulControllerClass, idType, isDomainEntity, eventMapping, refFactory);

        // Add the member variable referencing the StatefulController
        //
        addControllerReference(proxyClass, statefulControllerClass, beanName, cp);

        // Copy over all the Class level Annotations
        //
        JavassistUtils.copyTypeAnnotations(statefulControllerClass, proxyClass);

        // Copy Proxy methods that bypass the FSM
        //
        addProxyMethods(proxyClass, statefulControllerClass, cp);

        return proxyClass;

    }

    @Override
    protected Class<?> getComponentClass() {
        return Controller.class;
    }

    /**
     * Clone all the parameter Annotations from the StatefulController to the Proxy
     *
     * @param methodInfo
     * @param parmIndex
     * @param annotations
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    @Override
    protected Annotation[] createParameterAnnotations(final String parmName, final MethodInfo methodInfo, final java.lang.annotation.Annotation[] annotations, final ConstPool parameterConstPool)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        final List<Annotation> ctParmAnnotations = new LinkedList<Annotation>();

        for (final java.lang.annotation.Annotation annotation : annotations) {
            final Annotation clone = JavassistUtils.cloneAnnotation(parameterConstPool, annotation);

            // Special case: since Javaassist doesn't allow me to set the name of the parameter,
            // I need to ensure that RequestParam's value is set to the parm name if there isn't already
            // a value set
            //
            if (RequestParam.class.isAssignableFrom(annotation.annotationType())) {
                if ("".equals(((RequestParam) annotation).value()) && !StringUtils.isEmpty(parmName)) {
                    final MemberValue value = JavassistUtils.createMemberValue(parameterConstPool, parmName);
                    clone.addMemberValue("value", value);
                }
            }

            ctParmAnnotations.add(clone);
        }
        return ctParmAnnotations.toArray(new Annotation[] {});
    }

    @Override
    protected void addEndpointMapping(final CtMethod ctMethod, final String method, final String request) {
        final MethodInfo methodInfo = ctMethod.getMethodInfo();
        final ConstPool constPool = methodInfo.getConstPool();

        final AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
        final Annotation requestMapping = new Annotation(RequestMapping.class.getName(), constPool);

        final ArrayMemberValue valueVals = new ArrayMemberValue(constPool);
        final StringMemberValue valueVal = new StringMemberValue(constPool);
        valueVal.setValue(request);
        valueVals.setValue(new MemberValue[] { valueVal });

        requestMapping.addMemberValue("value", valueVals);

        final ArrayMemberValue methodVals = new ArrayMemberValue(constPool);
        final EnumMemberValue methodVal = new EnumMemberValue(constPool);
        methodVal.setType(RequestMethod.class.getName());
        methodVal.setValue(method);
        methodVals.setValue(new MemberValue[] { methodVal });

        requestMapping.addMemberValue("method", methodVals);
        attr.addAnnotation(requestMapping);
        methodInfo.addAttribute(attr);
    }

    @Override
    protected String getSuffix() {
        return MVC_SUFFIX;
    }

    @Override
    protected Class<?> getPathAnnotationClass() {
        return PathVariable.class;
    }

    @SuppressWarnings("unchecked")
    private void addProxyMethods(final CtClass mvcProxyClass, final Class<?> ctrlClass, final ClassPool cp)
            throws IllegalArgumentException, NotFoundException, IllegalAccessException, InvocationTargetException, CannotCompileException {

        for (final Class<?> annotation : proxyable) {
            final List<Method> methods = JavassistUtils.getMethodsAnnotatedWith(ctrlClass, (Class<java.lang.annotation.Annotation>) annotation);
            for (final Method method : methods) {
                addProxyMethod(mvcProxyClass, method, cp);
            }
        }
    }

    private void addProxyMethod(final CtClass mvcProxyClass, final Method method, final ClassPool cp)
            throws NotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, CannotCompileException {

        // Create Method
        //
        final CtClass returnClass = cp.get(method.getReturnType().getName());
        final String methodName = "$_" + method.getName();

        SpringMVCBinder.logger.debug("Adding proxy method {}", methodName);

        final CtMethod ctMethod = new CtMethod(returnClass, methodName, null, mvcProxyClass);

        // Clone method Annotations
        //
        JavassistUtils.addMethodAnnotations(ctMethod, method);

        // Copy parameters one-for-one
        //
        copyParameters(ctMethod, method, cp);

        // Add the Method
        //
        addProxyMethodBody(ctMethod, method);

        // Add the Method to the Proxy class
        //
        mvcProxyClass.addMethod(ctMethod);
    }

    private void addControllerReference(final CtClass proxyClass, final Class<?> controllerClass, final String beanName, final ClassPool cp) throws NotFoundException, CannotCompileException {
        final CtClass type = cp.get(controllerClass.getName());
        final CtField field = new CtField(type, getControllerVar(), proxyClass);

        JavassistUtils.addResourceAnnotation(field, beanName);

        proxyClass.addField(field);
    }

    private void addProxyMethodBody(final CtMethod ctMethod, final Method method) throws CannotCompileException, NotFoundException {
        final String returnType = ctMethod.getReturnType().getName();

        final String returnStmt = (returnType.equals("void")) ? "" : "return (" + returnType + ")";

        final String methodBody = "{ " + returnStmt + "$proceed($$); }";

        ctMethod.setBody(methodBody, "this." + getControllerVar(), method.getName());
    }

    private String getControllerVar() {
        return CONTROLLER_VAR;
    }
}
