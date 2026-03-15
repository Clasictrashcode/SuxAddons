package com.classictrashcode.suxaddons.client.config.annotations;

/**
 * Widget types for auto-generation
 */
public enum WidgetType {
    AUTO,       // Automatically determine from field type
    TOGGLE,     // Boolean toggle button
    SLIDER,     // Numeric slider
    TEXT_FIELD, // Text input
    KEYBIND,    // Keybind selector
    DROPDOWN,   // Dropdown/cycle button for enums
    COLOR       // Color picker
}
