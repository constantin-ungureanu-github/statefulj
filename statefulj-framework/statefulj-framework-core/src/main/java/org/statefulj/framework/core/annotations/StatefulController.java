package org.statefulj.framework.core.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface StatefulController {

    String value() default "";

    String startState();

    Class<?> clazz();

    String stateField() default "";

    String factoryId() default "";

    String finderId() default "";

    String persisterId() default "";

    String[] blockingStates() default {};

    Transition[] noops() default {};

    int retryAttempts() default 20;

    int retryInterval() default 250;
}
