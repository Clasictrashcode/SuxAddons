package com.classictrashcode.suxaddons.client.config.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a nested config class as a category that should appear as a tab
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigCategory {
    String name();
    int order() default 0;
    String formatting() default "WHITE"; // Minecraft Formatting enum name
    boolean separator() default false; // Add separator before this tab
}

