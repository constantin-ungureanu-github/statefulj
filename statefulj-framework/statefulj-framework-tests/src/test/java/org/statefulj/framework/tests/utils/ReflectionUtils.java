package org.statefulj.framework.tests.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

public class ReflectionUtils {

    @SuppressWarnings("unchecked")
    public static <T> T invoke(final Object obj, final String methodName, final Class<T> returnType, final Object... parms)
            throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        final ArrayList<Class<?>> parmClasses = new ArrayList<>();
        for (final Object parm : parms) {
            if (HttpServletRequest.class.isAssignableFrom(parm.getClass())) {
                parmClasses.add(HttpServletRequest.class);
            } else {
                parmClasses.add(parm.getClass());
            }
        }
        final Method method = obj.getClass().getDeclaredMethod(methodName, parmClasses.toArray(new Class<?>[] {}));
        return (T) method.invoke(obj, parms);
    }

    public static void invoke(final Object obj, final String methodName, final Object... parms)
            throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        final ArrayList<Class<?>> parmClasses = new ArrayList<>();
        for (final Object parm : parms) {
            if (HttpServletRequest.class.isAssignableFrom(parm.getClass())) {
                parmClasses.add(HttpServletRequest.class);
            } else {
                parmClasses.add(parm.getClass());
            }
        }

        Method method = null;
        for (Method m : obj.getClass().getDeclaredMethods()) {
            if (m.getName().equals(methodName) && (m.getParameterTypes().length == parmClasses.size())) {
                method = m;
                int i = 0;
                for (final Class<?> parmClass : m.getParameterTypes()) {
                    if (!parmClass.isAssignableFrom(parmClasses.get(i))) {
                        m = null;
                        break;
                    }
                    i++;
                }
            }
            if (method != null) {
                break;
            }
        }
        if (method == null) {
            throw new RuntimeException("Couldn't find method " + methodName);
        }
        method.invoke(obj, parms);
    }
}
