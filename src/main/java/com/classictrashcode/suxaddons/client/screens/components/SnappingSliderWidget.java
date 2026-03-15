package com.classictrashcode.suxaddons.client.screens.components;



import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SnappingSliderWidget extends AbstractSliderButton {
    protected final double min;
    protected final double max;
    protected final double step;
    protected Consumer<Double> onChange;
    protected boolean isInteger;
    protected final Font textRenderer;
    private final com.classictrashcode.suxaddons.client.config.Config.GuiTheme theme;
    private Supplier<Boolean> dropdownOpenCheck;

    public SnappingSliderWidget(int x, int y, int width, int height, double min, double max, double step, double initialValue, com.classictrashcode.suxaddons.client.config.Config.GuiTheme theme) {
        super(x, y, width, height, Component.empty(), valueToNormalized(initialValue, min, max, step));
        this.min = min;
        this.max = max;
        this.step = step;
        this.isInteger = step >= 1.0;
        this.textRenderer = Minecraft.getInstance().font;
        this.theme = theme;
        updateMessage();
    }


    private static double valueToNormalized(double value, double min, double max, double step) {
        double clamped = Math.clamp(value, min, max);
        double snapped = Math.round((clamped - min) / step) * step + min;
        snapped = Math.clamp(snapped, min, max);
        return (snapped - min) / (max - min);
    }

    public double getActualValue() {
        double rawValue = this.value * (max - min) + min;
        double snapped = Math.round((rawValue - min) / step) * step + min;
        return Math.clamp(snapped, min, max);
    }

    public void setActualValue(double value) {
        this.value = valueToNormalized(value, min, max, step);
        updateMessage();
    }

    public void setOnChange(Consumer<Double> onChange) {
        this.onChange = onChange;
    }

    public void setDropdownOpenCheck(Supplier<Boolean> dropdownOpenCheck) {
        this.dropdownOpenCheck = dropdownOpenCheck;
    }

    @Override
    protected void updateMessage() {
        double val = getActualValue();
        if (isInteger) {
            this.setMessage(Component.literal(String.valueOf((int) val)));
        } else {
            this.setMessage(Component.literal(String.format("%.2f", val)));
        }
    }

    @Override
    protected void applyValue() {
        if (onChange != null) {
            onChange.accept(getActualValue());
        }
    }

    @Override
    public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // Get theme colors
        var theme = this.theme;
        int sliderBgColor = theme.parseColor(theme.surfaceElevatedColor);
        int sliderFillColor = theme.parseColor(theme.primaryColor);
        int sliderHoverColor = theme.parseColor(theme.primaryLightColor);
        int textColor = theme.parseColor(theme.textPrimaryColor);
        int textSecondaryColor = theme.parseColor(theme.textSecondaryColor);

        // Get current, min, and max values as formatted strings
        double currentValue = getActualValue();
        String currentStr = isInteger ? String.valueOf((int) currentValue) : String.format("%.2f", currentValue);
        String minStr = isInteger ? String.valueOf((int) min) : String.format("%.2f", min);
        String maxStr = isInteger ? String.valueOf((int) max) : String.format("%.2f", max);

        // Draw background track
        context.fill(this.getX(), this.getY() + this.height / 2 - 2,
                    this.getX() + this.width, this.getY() + this.height / 2 + 2,
                    sliderBgColor);

        // Draw filled portion
        int fillWidth = (int) (this.value * this.width);
        int fillColor = this.isHovered() ? sliderHoverColor : sliderFillColor;
        context.fill(this.getX(), this.getY() + this.height / 2 - 2,
                    this.getX() + fillWidth, this.getY() + this.height / 2 + 2,
                    fillColor);

        // Draw handle/thumb
        int handleX = this.getX() + (int) (this.value * this.width);
        int handleY = this.getY() + this.height / 2;
        int handleSize = 6;

        // Handle shadow
        context.fill(handleX - handleSize + 1, handleY - handleSize + 1,
                    handleX + handleSize + 1, handleY + handleSize + 1,
                    0x80000000);

        // Handle main
        context.fill(handleX - handleSize, handleY - handleSize,
                    handleX + handleSize, handleY + handleSize,
                    this.isHovered() ? sliderHoverColor : sliderFillColor);

        // Handle border
        context.fill(handleX - handleSize, handleY - handleSize,
                    handleX + handleSize, handleY - handleSize + 1,
                    0xFFFFFFFF); // Top
        context.fill(handleX - handleSize, handleY - handleSize,
                    handleX - handleSize + 1, handleY + handleSize,
                    0xFFFFFFFF); // Left

        // Disable shadow when dropdown is open to prevent text bleeding through
        boolean shadow = dropdownOpenCheck == null || !dropdownOpenCheck.get();

        // Draw current value centered above slider
        int currentValueX = this.getX() + this.width / 2 - textRenderer.width(currentStr) / 2;
        int currentValueY = this.getY() - 2;
        context.drawString(textRenderer, Component.literal(currentStr), currentValueX, currentValueY, textColor, shadow);

        // Draw min value at left below slider
        int minValueX = this.getX();
        int minValueY = this.getY() + this.height / 2 + 5;
        context.drawString(textRenderer, Component.literal(minStr), minValueX, minValueY, textSecondaryColor, shadow);

        // Draw max value at right below slider
        int maxValueX = this.getX() + this.width - textRenderer.width(maxStr);
        int maxValueY = this.getY() + this.height / 2 + 5;
        context.drawString(textRenderer, Component.literal(maxStr), maxValueX, maxValueY, textSecondaryColor, shadow);
    }
}