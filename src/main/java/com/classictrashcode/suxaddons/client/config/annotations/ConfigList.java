package com.classictrashcode.suxaddons.client.config.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a List field as a configurable dynamic list with entries
 * Each entry is defined by the entryClass which contains annotated fields
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigList {
    /**
     * Display name for the list section
     */
    String name();

    /**
     * Optional description
     */
    String description() default "";

    /**
     * Ordering value (lower = earlier in GUI)
     */
    int order() default 0;

    /**
     * Class defining the structure of list entries
     * Must be a class with annotated fields (@ConfigOption, @ConfigKeybind, etc.)
     */
    Class<?> entryClass();

    /**
     * Optional field name from entryClass to use as entry title
     * If empty, auto-generates titles like "Entry #1", "Entry #2"
     */
    String entryTitleField() default "";

    /**
     * Prefix for auto-generated titles (default: "Entry")
     * Only used if entryTitleField is empty
     */
    String titlePrefix() default "Entry";

    /**
     * Layout direction for fields within each entry
     * "horizontal" = side-by-side, "vertical" = stacked
     */
    String layout() default "horizontal";

    /**
     * Maximum number of entries allowed
     * -1 = unlimited
     */
    int maxSize() default -1;
}