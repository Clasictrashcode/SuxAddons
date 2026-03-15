package com.classictrashcode.suxaddons.client.config.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target; /**
 * Defines min/max range for numeric values
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigRange {
    double min();
    double max();
    double step() default 0.1;
}
