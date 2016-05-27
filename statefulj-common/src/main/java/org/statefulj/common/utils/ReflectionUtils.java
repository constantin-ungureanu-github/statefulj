package org.statefulj.common.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReflectionUtils {
    private static Pattern fieldNamePattern = Pattern.compile("[g|s]et(.)(.*)");

    public static Field getFirstAnnotatedField(final Class<?> clazz, final Class<? extends Annotation> annotationClass) {
        Field match = null;
        if (clazz != null) {
            for (final Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(annotationClass)) {
                    match = field;
                    break;
                }
            }
            if (match == null) {
                match = ReflectionUtils.getFirstAnnotatedField(clazz.getSuperclass(), annotationClass);
            }
        }

        return match;
    }

    public static List<Field> getAllAnnotatedFields(final Class<?> clazz, final Class<? extends Annotation> annotationClass) {
        final List<Field> fields = new LinkedList<Field>();
        if (clazz != null) {
            fields.addAll(ReflectionUtils.getAllAnnotatedFields(clazz.getSuperclass(), annotationClass));
            for (final Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(annotationClass)) {
                    fields.add(field);
                }
            }
        }

        return fields;
    }

    public static Method getFirstAnnotatedMethod(final Class<?> clazz, final Class<? extends Annotation> annotationClass) {
        Method match = null;
        if (clazz != null) {
            for (final Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(annotationClass)) {
                    match = method;
                    break;
                }
            }
            if (match == null) {
                match = ReflectionUtils.getFirstAnnotatedMethod(clazz.getSuperclass(), annotationClass);
            }
        }

        return match;
    }

    public static Class<?> getFirstAnnotatedClass(final Class<?> clazz, final Class<? extends Annotation> annotationClass) {

        if (clazz == null) {
            return null;
        }

        Class<?> annotatedClass = (clazz.isAnnotationPresent(annotationClass)) ? clazz : null;

        if (annotatedClass == null) {
            annotatedClass = ReflectionUtils.getFirstAnnotatedClass(clazz.getSuperclass(), annotationClass);
        }

        return annotatedClass;
    }

    public static <T extends Annotation> T getFirstClassAnnotation(final Class<?> clazz, final Class<T> annotationClass) {

        if (clazz == null) {
            return null;
        }

        T annotation = clazz.getAnnotation(annotationClass);

        if (annotation == null) {
            annotation = ReflectionUtils.getFirstClassAnnotation(clazz.getSuperclass(), annotationClass);
        }

        return annotation;
    }

    public static boolean isAnnotationPresent(final Class<?> clazz, final Class<? extends Annotation> annotationClass) {

        if (clazz == null) {
            return false;
        }

        boolean annotationPresent = clazz.isAnnotationPresent(annotationClass);

        if (!annotationPresent) {
            annotationPresent = ReflectionUtils.isAnnotationPresent(clazz.getSuperclass(), annotationClass);
        }

        return annotationPresent;
    }

    public static boolean isGetter(final Method method) {
        if (!method.getName().startsWith("get")) {
            return false;
        }
        if (method.getParameterTypes().length != 0) {
            return false;
        }
        if (void.class.equals(method.getReturnType())) {
            return false;
        }
        return true;
    }

    public static boolean isSetter(final Method method) {
        if (!method.getName().startsWith("set")) {
            return false;
        }
        if (method.getParameterTypes().length != 1) {
            return false;
        }
        return true;
    }

    public static String toFieldName(final Method getterOrSetter) {
        final Matcher matcher = ReflectionUtils.fieldNamePattern.matcher(getterOrSetter.getName());
        return (matcher.matches()) ? matcher.group(1).toLowerCase() + matcher.group(2) : null;
    }

    public static Field getReferencedField(final Class<?> clazz, final Class<? extends Annotation> annotationClass) {
        Field field = ReflectionUtils.getFirstAnnotatedField(clazz, annotationClass);
        if (field == null) {
            final Method method = ReflectionUtils.getFirstAnnotatedMethod(clazz, annotationClass);
            if ((method != null) && (ReflectionUtils.isGetter(method) || ReflectionUtils.isSetter(method))) {
                final String fieldName = ReflectionUtils.toFieldName(method);
                try {
                    field = (fieldName != null) ? clazz.getDeclaredField(fieldName) : null;
                } catch (final Exception e) {
                    // Ignore
                }
                if (field == null) {
                    try {
                        field = (fieldName != null) ? clazz.getField(fieldName) : null;
                    } catch (final Exception e) {
                        // Ignore
                    }
                }
            }
        }
        return field;
    }

    public static Field getField(final Class<?> clazz, final String fieldName) {
        Field field = null;

        for (Class<?> current = clazz; (current != null) && (field == null); current = current.getSuperclass()) {
            try {
                field = current.getDeclaredField(fieldName);
            } catch (final NoSuchFieldException e) {
                // Ignore
            }
        }

        return field;
    }
}
