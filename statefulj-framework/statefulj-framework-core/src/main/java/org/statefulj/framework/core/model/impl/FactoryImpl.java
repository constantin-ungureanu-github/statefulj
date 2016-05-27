package org.statefulj.framework.core.model.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.statefulj.framework.core.model.Factory;

public class FactoryImpl<T, CT> implements Factory<T, CT> {
    public T create(final Class<T> clazz, final String event, final CT context) {
        try {
            final Constructor<T> ctr = clazz.getDeclaredConstructor();
            ctr.setAccessible(true);
            return ctr.newInstance();
        } catch (final IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (final InstantiationException e) {
            throw new RuntimeException(e);
        } catch (final IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (final InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (final SecurityException e) {
            throw new RuntimeException(e);
        } catch (final NoSuchMethodException e) {
            throw new RuntimeException(clazz.getCanonicalName() + " does not have a default constructor");
        }
    }

}
