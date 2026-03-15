package com.classictrashcode.suxaddons.client.config.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as requiring a dropdown selector (for enums or predefined options)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigDropdown {
    /**
     * Display name for the dropdown
     */
    String name();

    /**
     * Description/tooltip text
     */
    String description() default "";

    /**
     * Display order
     */
    int order() default 0;
}
