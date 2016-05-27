package org.statefulj.framework.core.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Autowired;

@Target({ PARAMETER, FIELD, METHOD })
@Retention(RUNTIME)
@Documented
@Autowired
public @interface FSM {
    public String value() default "";
}
