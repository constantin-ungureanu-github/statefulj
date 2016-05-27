package org.statefulj.framework.binders.camel;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import javax.persistence.Id;

import org.apache.camel.Consume;
import org.apache.camel.component.bean.BeanInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.statefulj.common.utils.ReflectionUtils;
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
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.StringMemberValue;

public class CamelBinder implements EndpointBinder {
    private static final Logger logger = LoggerFactory.getLogger(CamelBinder.class);
    public final static String KEY = "camel";
    private final String CONSUMER_SUFFIX = "CamelBinder";
    private final String HARNESS_VAR = "harness";

    public String getKey() {
        return CamelBinder.KEY;
    }

    public static Object lookupId(Object msg) {
        Object id = null;
        if ((msg instanceof String) || Number.class.isAssignableFrom(msg.getClass())) {
            id = msg;
        } else {
            if (BeanInvocation.class.isAssignableFrom(msg.getClass())) {
                msg = ((BeanInvocation) msg).getArgs()[0];
            }
            Field idField = null;
            try {
                idField = ReflectionUtils.getFirstAnnotatedField(msg.getClass(), Id.class);
            } catch (final Throwable t) {
                // ignore
            }
            if (idField == null) {
                try {
                    idField = ReflectionUtils.getFirstAnnotatedField(msg.getClass(), org.springframework.data.annotation.Id.class);
                } catch (final Throwable t) {
                    // ignore
                }
            }
            if (idField == null) {
                try {
                    idField = msg.getClass().getField("id");
                } catch (final Throwable t) {
                    // ignore
                }
            }
            if (idField != null) {
                try {
                    idField.setAccessible(true);
                    id = idField.get(msg);
                } catch (final IllegalArgumentException e) {
                    throw new RuntimeException(e);
                } catch (final IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return id;
    }

    public Class<?> bindEndpoints(final String beanName, final Class<?> controllerClass, final Class<?> idType, final boolean isDomainEntity, final Map<String, Method> eventMapping,
            final ReferenceFactory refFactory) throws CannotCompileException, NotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        CamelBinder.logger.debug("Building Consumer for {}", controllerClass);

        final ClassPool cp = ClassPool.getDefault();
        cp.appendClassPath(new ClassClassPath(getClass()));

        final String camelProxyClassName = controllerClass.getName() + CONSUMER_SUFFIX;
        final CtClass camelProxyClass = cp.makeClass(camelProxyClassName);

        addFSMHarnessReference(camelProxyClass, refFactory.getFSMHarnessId(), cp);
        addConsumerMethods(camelProxyClass, eventMapping, cp);

        return camelProxyClass.toClass();
    }

    private void addFSMHarnessReference(final CtClass camelProxyClass, final String fsmHarnessId, final ClassPool cp) throws NotFoundException, CannotCompileException {
        final CtClass type = cp.get(FSMHarness.class.getName());
        final CtField field = new CtField(type, HARNESS_VAR, camelProxyClass);

        JavassistUtils.addResourceAnnotation(field, fsmHarnessId);

        camelProxyClass.addField(field);
    }

    private void addConsumerMethods(final CtClass camelProxyClass, final Map<String, Method> eventMapping, final ClassPool cp)
            throws IllegalArgumentException, NotFoundException, IllegalAccessException, InvocationTargetException, CannotCompileException {

        // Build a method for each Event
        //
        for (final String event : eventMapping.keySet()) {
            addConsumerMethod(camelProxyClass, event, eventMapping.get(event), cp);
        }
    }

    private void addConsumerMethod(final CtClass camelProxyClass, final String event, final Method method, final ClassPool cp)
            throws NotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, CannotCompileException {

        // Clone Method from the StatefulController
        //
        final CtMethod ctMethod = createConsumerMethod(camelProxyClass, event, method, cp);

        // Clone method Annotations
        //
        JavassistUtils.addMethodAnnotations(ctMethod, method);

        // Add a RequestMapping annotation
        //
        addConsumeAnnotation(ctMethod, event);

        // Clone the parameters, along with the Annotations
        //
        addMessageParameter(ctMethod, method, cp);

        // Add the Method Body
        //
        addMethodBody(ctMethod, event);

        // Add the Method to the Proxy class
        //
        camelProxyClass.addMethod(ctMethod);
    }

    private CtMethod createConsumerMethod(final CtClass camelProxyClass, final String event, final Method method, final ClassPool cp) throws NotFoundException {
        final String methodName = ("$_" + event.replaceAll("[/:\\.]", "_").replace("{", "").replace("}", "")).toLowerCase();

        CamelBinder.logger.debug("Create method {} for {}", methodName, camelProxyClass.getSimpleName());

        final CtMethod ctMethod = new CtMethod(CtClass.voidType, methodName, null, camelProxyClass);
        return ctMethod;
    }

    private void addConsumeAnnotation(final CtMethod ctMethod, final String uri) {
        final MethodInfo methodInfo = ctMethod.getMethodInfo();
        final ConstPool constPool = methodInfo.getConstPool();

        final Annotation consume = new Annotation(Consume.class.getName(), constPool);
        final StringMemberValue valueVal = new StringMemberValue(constPool);
        valueVal.setValue(uri);
        consume.addMemberValue("uri", valueVal);

        final AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
        attr.addAnnotation(consume);
        methodInfo.addAttribute(attr);
    }

    private void addMethodBody(final CtMethod ctMethod, final String event) throws CannotCompileException, NotFoundException {
        final String methodBody = "{ " + "Object id = org.statefulj.framework.binders.camel.CamelBinder.lookupId($1); " + "$proceed(\"" + event + "\", id, new Object[]{$1, $1});" + "}";

        ctMethod.setBody(methodBody, "this." + HARNESS_VAR, "onEvent");
    }

    private void addMessageParameter(final CtMethod ctMethod, final Method method, final ClassPool cp)
            throws NotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, CannotCompileException {

        // Only one parameter - a message object
        //
        final Class<?> msgClass = ((method != null) && (method.getParameterTypes().length == 3)) ? method.getParameterTypes()[2] : Object.class;
        final CtClass ctParm = cp.get(msgClass.getName());

        // Add the parameter to the method
        //
        ctMethod.addParameter(ctParm);
    }

}
