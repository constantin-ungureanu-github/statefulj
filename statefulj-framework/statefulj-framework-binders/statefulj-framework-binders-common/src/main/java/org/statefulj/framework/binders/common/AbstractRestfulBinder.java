package org.statefulj.framework.binders.common;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.stereotype.Component;
import org.statefulj.framework.binders.common.utils.JavassistUtils;
import org.statefulj.framework.core.model.EndpointBinder;
import org.statefulj.framework.core.model.FSMHarness;
import org.statefulj.framework.core.model.ReferenceFactory;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.ParameterAnnotationsAttribute;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.StringMemberValue;

public abstract class AbstractRestfulBinder implements EndpointBinder {
    private static final Logger logger = LoggerFactory.getLogger(AbstractRestfulBinder.class);
    private final Pattern methodPattern = Pattern.compile("(([^:]*):)?(.*)");
    private final String HARNESS_VAR = "harness";
    private final String GET = "GET";
    private final LocalVariableTableParameterNameDiscoverer parmDiscover = new LocalVariableTableParameterNameDiscoverer();

    @Override
    public Class<?> bindEndpoints(final String beanName, final Class<?> statefulControllerClass, final Class<?> idType, final boolean isDomainEntity, final Map<String, Method> eventMapping,
            final ReferenceFactory refFactory) throws CannotCompileException, NotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {

        AbstractRestfulBinder.logger.debug("Building proxy for {}", statefulControllerClass);

        final ClassPool cp = ClassPool.getDefault();
        cp.appendClassPath(new ClassClassPath(getClass()));

        final String proxyClassName = statefulControllerClass.getName() + getSuffix();

        return buildProxy(cp, beanName, proxyClassName, statefulControllerClass, idType, isDomainEntity, eventMapping, refFactory).toClass();
    }

    protected CtClass buildProxy(final ClassPool cp, final String beanName, final String proxyClassName, final Class<?> statefulControllerClass, final Class<?> idType, final boolean isDomainEntity,
            final Map<String, Method> eventMapping, final ReferenceFactory refFactory)
            throws CannotCompileException, NotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {

        final CtClass proxyClass = cp.makeClass(proxyClassName);

        addComponentAnnotation(proxyClass);
        addFSMHarnessReference(proxyClass, refFactory.getFSMHarnessId(), cp);
        addRequestMethods(proxyClass, idType, isDomainEntity, eventMapping, cp);

        return proxyClass;
    }

    protected void addComponentAnnotation(final CtClass proxyClass) {
        JavassistUtils.addClassAnnotation(proxyClass, getComponentClass());
    }

    protected void addRequestMethods(final CtClass proxyClass, final Class<?> idType, final boolean isDomainEntity, final Map<String, Method> eventMapping, final ClassPool cp)
            throws IllegalArgumentException, NotFoundException, IllegalAccessException, InvocationTargetException, CannotCompileException {
        for (final String event : eventMapping.keySet()) {
            addRequestMethod(proxyClass, idType, isDomainEntity, event, eventMapping.get(event), cp);
        }
    }

    protected CtMethod createRequestMethod(final CtClass proxyClass, final String requestMethod, final String requestEvent, final Method method, final ClassPool cp) throws NotFoundException {
        final String methodName = ("$_" + requestMethod + requestEvent.replace("/", "_").replace("{", "").replace("}", "")).toLowerCase();

        AbstractRestfulBinder.logger.debug("Create method {} for {}", methodName, proxyClass.getSimpleName());

        final CtClass returnClass = (method == null) ? CtClass.voidType : cp.get(method.getReturnType().getName());
        final CtMethod ctMethod = new CtMethod(returnClass, methodName, null, proxyClass);
        return ctMethod;
    }

    protected void addRequestParameters(final boolean referencesId, final Class<?> idType, final boolean isDomainEntity, final CtMethod ctMethod, final Method method, final ClassPool cp)
            throws NotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, CannotCompileException {

        final int fixedParmCnt = (isDomainEntity) ? 1 : 2;

        final String[] parmNames = (method != null) ? parmDiscover.getParameterNames(method) : null;
        final MethodInfo methodInfo = ctMethod.getMethodInfo();
        final ParameterAnnotationsAttribute paramAtrributeInfo = new ParameterAnnotationsAttribute(methodInfo.getConstPool(), ParameterAnnotationsAttribute.visibleTag);

        Annotation[][] paramArrays = null;
        if (method != null) {

            int parmIndex = 0;

            // Does this event reference the stateful object?
            //
            final int additionalParmCnt = (referencesId) ? 2 : 1;
            int annotationCnt = (method.getParameterTypes().length + additionalParmCnt) - fixedParmCnt;
            annotationCnt = Math.max(annotationCnt, additionalParmCnt);

            // Pull the Parameter Annotations from the StatefulController - we're going to skip
            // over the first one (DomainEntity) or two (Controller) - but then we're going to
            // add a parameter for the HttpServletRequest and "id" parameter
            //
            final java.lang.annotation.Annotation[][] parmAnnotations = method.getParameterAnnotations();
            paramArrays = new Annotation[annotationCnt][];

            // Add an Id parameter at the beginning of the method - this will be
            // used by the Harness to fetch the object
            //
            if (referencesId) {
                paramArrays[parmIndex] = addIdParameter(ctMethod, idType, cp);
                parmIndex++;
            }

            // Add an HttpServletRequest - this will be passed in as a context to the finder/factory methods
            //
            paramArrays[parmIndex] = addHttpRequestParameter(ctMethod, cp);
            parmIndex++;

            int parmCnt = 0;
            for (final Class<?> parm : method.getParameterTypes()) {
                if (parmCnt < fixedParmCnt) {
                    parmCnt++;
                    continue;
                }

                final CtClass ctParm = cp.get(parm.getName());
                ctMethod.addParameter(ctParm);

                final String parmName = ((parmNames != null) && (parmNames.length > parmCnt)) ? parmNames[parmCnt] : null;
                paramArrays[parmIndex] = createParameterAnnotations(parmName, ctMethod.getMethodInfo(), parmAnnotations[parmCnt], paramAtrributeInfo.getConstPool());
                parmCnt++;
                parmIndex++;
            }
        } else {
            // NOOP transitions always a require an object Id
            //
            paramArrays = new Annotation[2][];
            paramArrays[0] = addIdParameter(ctMethod, idType, cp);
            paramArrays[1] = addHttpRequestParameter(ctMethod, cp);
        }
        paramAtrributeInfo.setAnnotations(paramArrays);
        methodInfo.addAttribute(paramAtrributeInfo);
    }

    protected void copyParameters(final CtMethod ctMethod, final Method method, final ClassPool cp)
            throws NotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, CannotCompileException {
        final String[] parmNames = (method != null) ? parmDiscover.getParameterNames(method) : null;
        final MethodInfo methodInfo = ctMethod.getMethodInfo();
        final ParameterAnnotationsAttribute paramAtrributeInfo = new ParameterAnnotationsAttribute(methodInfo.getConstPool(), ParameterAnnotationsAttribute.visibleTag);

        final Annotation[][] paramArrays = new Annotation[method.getParameterTypes().length][];
        final java.lang.annotation.Annotation[][] parmAnnotations = method.getParameterAnnotations();
        int parmIndex = 0;
        for (final Class<?> parm : method.getParameterTypes()) {
            final CtClass ctParm = cp.get(parm.getName());
            ctMethod.addParameter(ctParm);

            final String parmName = ((parmNames != null) && (parmNames.length > parmIndex)) ? parmNames[parmIndex] : null;
            paramArrays[parmIndex] = createParameterAnnotations(parmName, ctMethod.getMethodInfo(), parmAnnotations[parmIndex], paramAtrributeInfo.getConstPool());
            parmIndex++;
        }
        paramAtrributeInfo.setAnnotations(paramArrays);
        methodInfo.addAttribute(paramAtrributeInfo);
    }

    protected Annotation[] addHttpRequestParameter(final CtMethod ctMethod, final ClassPool cp) throws NotFoundException, CannotCompileException {
        // Map the HttpServletRequest class
        //
        final CtClass ctParm = cp.get(HttpServletRequest.class.getName());

        // Add the parameter to the method
        //
        ctMethod.addParameter(ctParm);

        return new Annotation[] {};

    }

    protected Pair<String, String> parseMethod(final String event) {
        final Matcher matcher = getMethodPattern().matcher(event);
        if (!matcher.matches()) {
            throw new RuntimeException("Unable to parse event=" + event);
        }
        return new ImmutablePair<>(matcher.group(2), matcher.group(3));
    }

    protected void addRequestMethodBody(final boolean referencesId, final CtMethod ctMethod, final String event) throws CannotCompileException, NotFoundException {
        final String nullObjId = (referencesId) ? "\"" : "\", null";
        final String returnType = ctMethod.getReturnType().getName();
        final String returnStmt = (returnType.equals("void")) ? "" : "return (" + returnType + ")";
        final String methodBody = "{ " + returnStmt + "$proceed(\"" + event + nullObjId + ", $args); }";

        ctMethod.setBody(methodBody, "this." + HARNESS_VAR, "onEvent");
    }

    protected void addFSMHarnessReference(final CtClass proxyClass, final String fsmHarnessId, final ClassPool cp) throws NotFoundException, CannotCompileException {
        final CtClass type = cp.get(FSMHarness.class.getName());
        final CtField field = new CtField(type, HARNESS_VAR, proxyClass);

        JavassistUtils.addResourceAnnotation(field, fsmHarnessId);

        proxyClass.addField(field);
    }

    protected void addRequestMethod(final CtClass proxyClass, final Class<?> idType, final boolean isDomainEntity, final String event, final Method method, final ClassPool cp)
            throws NotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, CannotCompileException {

        final Pair<String, String> methodEndpoint = parseMethod(event);
        String requestMethod = methodEndpoint.getLeft();
        final String requestEvent = methodEndpoint.getRight();
        requestMethod = (requestMethod == null) ? GET : requestMethod.toUpperCase();

        final boolean referencesId = (event.indexOf("{id}") > 0);
        final CtMethod ctMethod = createRequestMethod(proxyClass, requestMethod, requestEvent, method, cp);

        JavassistUtils.addMethodAnnotations(ctMethod, method);

        addEndpointMapping(ctMethod, requestMethod, requestEvent);
        addRequestParameters(referencesId, idType, isDomainEntity, ctMethod, method, cp);
        addRequestMethodBody(referencesId, ctMethod, event);

        proxyClass.addMethod(ctMethod);
    }

    protected Annotation[] addIdParameter(final CtMethod ctMethod, final Class<?> idType, final ClassPool cp) throws NotFoundException, CannotCompileException {
        final CtClass ctParm = cp.get(idType.getName());
        ctMethod.addParameter(ctParm);

        final MethodInfo methodInfo = ctMethod.getMethodInfo();
        final ConstPool constPool = methodInfo.getConstPool();
        final Annotation annot = new Annotation(getPathAnnotationClass().getName(), constPool);

        final StringMemberValue valueVal = new StringMemberValue("id", constPool);
        annot.addMemberValue("value", valueVal);

        return new Annotation[] { annot };
    }

    protected Annotation[] createParameterAnnotations(final String parmName, final MethodInfo methodInfo, final java.lang.annotation.Annotation[] annotations, final ConstPool parameterConstPool)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        final List<Annotation> ctParmAnnotations = new LinkedList<>();

        for (final java.lang.annotation.Annotation annotation : annotations) {
            final Annotation clone = JavassistUtils.cloneAnnotation(parameterConstPool, annotation);
            ctParmAnnotations.add(clone);
        }
        return ctParmAnnotations.toArray(new Annotation[] {});
    }

    protected Pattern getMethodPattern() {
        return methodPattern;
    }

    protected Class<?> getComponentClass() {
        return Component.class;
    }

    protected abstract void addEndpointMapping(CtMethod ctMethod, String method, String request);

    protected abstract Class<?> getPathAnnotationClass();

    protected abstract String getSuffix();
}
