package org.statefulj.framework.core.model.impl;

import java.beans.Introspector;
import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;
import org.statefulj.framework.core.model.ReferenceFactory;

public class ReferenceFactoryImpl implements ReferenceFactory {
    private final String ctrl;

    public ReferenceFactoryImpl(final String ctrl) {
        this.ctrl = ctrl;
    }

    public String getBinderId(String key) {
        key = (!StringUtils.isEmpty(key)) ? "." + key : "";
        return Introspector.decapitalize(ctrl + ".binder" + key);
    }

    public String getFinderId() {
        return Introspector.decapitalize(ctrl + ".finder");
    }

    public String getFSMHarnessId() {
        return Introspector.decapitalize(ctrl + ".fsmHarness");
    }

    public String getPersisterId() {
        return Introspector.decapitalize(ctrl + ".persister");
    }

    public String getFactoryId() {
        return Introspector.decapitalize(ctrl + ".factory");
    }

    public String getStatefulFSMId() {
        return Introspector.decapitalize(ctrl + ".statefulFSM");
    }

    public String getFSMId() {
        return Introspector.decapitalize(ctrl + ".fsm");
    }

    public String getStateId(final String state) {
        return Introspector.decapitalize(ctrl + ".state." + state);
    }

    public String getTransitionId(final int cnt) {
        return Introspector.decapitalize(ctrl + ".transition." + cnt);
    }

    public String getActionId(final Method method) {
        String id = Introspector.decapitalize(ctrl + ".action." + method.getName());
        for (final Class<?> clazz : method.getParameterTypes()) {
            id += "." + clazz.getName();
        }
        return id;
    }
}
