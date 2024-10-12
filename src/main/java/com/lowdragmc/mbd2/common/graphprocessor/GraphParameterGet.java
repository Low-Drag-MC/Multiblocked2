package com.lowdragmc.mbd2.common.graphprocessor;

import com.lowdragmc.lowdraglib.gui.graphprocessor.data.parameter.ExposedParameter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface GraphParameterGet {
    /**
     * The identity name of the parameter
     */
    String identity() default "";
    /**
     * The display name of the parameter
     */
    String displayName() default "";
    /**
     * The type of the parameter
     */
    Class type() default ExposedParameter.class;
    /**
     * The description of the parameter
     */
    String[] tips() default {};
}
