package com.classictrashcode.suxaddons.client.config;


public class ThemePreset {
    public String name;
    public boolean builtIn; // Built-in presets cannot be deleted

    public String backgroundColor;
    public String surfaceColor;
    public String surfaceElevatedColor;
    public String primaryColor;
    public String primaryDarkColor;
    public String primaryLightColor;
    public String textPrimaryColor;
    public String textSecondaryColor;
    public String dividerColor;
    public String errorColor;
    public String successColor;
    public String buttonNormalColor;
    public String buttonHoverColor;
    public String buttonActiveColor;
    public String buttonDisabledColor;
    public String toggleOnColor;
    public String toggleOffColor;
    public String toggleKnobColor;
    public String textFieldBackgroundColor;
    public String textFieldBorderColor;
    public String textFieldFocusColor;

    public ThemePreset() {
    }

    public ThemePreset(String name, Config.GuiTheme theme, boolean builtIn) {
        this.name = name;
        this.builtIn = builtIn;
        this.backgroundColor = theme.backgroundColor;
        this.surfaceColor = theme.surfaceColor;
        this.surfaceElevatedColor = theme.surfaceElevatedColor;
        this.primaryColor = theme.primaryColor;
        this.primaryDarkColor = theme.primaryDarkColor;
        this.primaryLightColor = theme.primaryLightColor;
        this.textPrimaryColor = theme.textPrimaryColor;
        this.textSecondaryColor = theme.textSecondaryColor;
        this.dividerColor = theme.dividerColor;
        this.errorColor = theme.errorColor;
        this.successColor = theme.successColor;
        this.buttonNormalColor = theme.buttonNormalColor;
        this.buttonHoverColor = theme.buttonHoverColor;
        this.buttonActiveColor = theme.buttonActiveColor;
        this.buttonDisabledColor = theme.buttonDisabledColor;
        this.toggleOnColor = theme.toggleOnColor;
        this.toggleOffColor = theme.toggleOffColor;
        this.toggleKnobColor = theme.toggleKnobColor;
        this.textFieldBackgroundColor = theme.textFieldBackgroundColor;
        this.textFieldBorderColor = theme.textFieldBorderColor;
        this.textFieldFocusColor = theme.textFieldFocusColor;
    }
    public void applyTo(Config.GuiTheme theme) {
        theme.backgroundColor = this.backgroundColor;
        theme.surfaceColor = this.surfaceColor;
        theme.surfaceElevatedColor = this.surfaceElevatedColor;
        theme.primaryColor = this.primaryColor;
        theme.primaryDarkColor = this.primaryDarkColor;
        theme.primaryLightColor = this.primaryLightColor;
        theme.textPrimaryColor = this.textPrimaryColor;
        theme.textSecondaryColor = this.textSecondaryColor;
        theme.dividerColor = this.dividerColor;
        theme.errorColor = this.errorColor;
        theme.successColor = this.successColor;
        theme.buttonNormalColor = this.buttonNormalColor;
        theme.buttonHoverColor = this.buttonHoverColor;
        theme.buttonActiveColor = this.buttonActiveColor;
        theme.buttonDisabledColor = this.buttonDisabledColor;
        theme.toggleOnColor = this.toggleOnColor;
        theme.toggleOffColor = this.toggleOffColor;
        theme.toggleKnobColor = this.toggleKnobColor;
        theme.textFieldBackgroundColor = this.textFieldBackgroundColor;
        theme.textFieldBorderColor = this.textFieldBorderColor;
        theme.textFieldFocusColor = this.textFieldFocusColor;
    }
}