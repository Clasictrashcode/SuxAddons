package com.classictrashcode.suxaddons.client.config.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target; /**
 * Marks a field that depends on another field being enabled
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigDependency {
    String field(); // Field name that must be true
    boolean invert() default false; // If true, depend on field being false
}
