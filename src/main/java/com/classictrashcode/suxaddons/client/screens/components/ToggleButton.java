package com.classictrashcode.suxaddons.client.screens.components;

import com.classictrashcode.suxaddons.client.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.function.Consumer;

/**
 * Square sliding toggle switch for boolean values.
 * Features smooth animations, square knob design, and clear visual distinction between ON and OFF states.
 */
public class ToggleButton extends AbstractWidget {
    private static final int WIDTH = 40;
    private static final int HEIGHT = 18;
    private static final int KNOB_SIZE = 14; // Square knob slightly smaller than height
    private static final int KNOB_PADDING = 2;
    private static final float ANIMATION_SPEED = 0.15f; // Animation speed (0.0 - 1.0)

    private boolean value;
    private float animationProgress; // 0.0 = OFF position, 1.0 = ON position
    private Consumer<Boolean> onChange;
    private final Config.GuiTheme theme;

    public ToggleButton(int x, int y, boolean initialValue, Consumer<Boolean> onChange, Config.GuiTheme theme) {
        super(x, y, WIDTH, HEIGHT, Component.empty());
        this.value = initialValue;
        this.animationProgress = initialValue ? 1.0f : 0.0f;
        this.onChange = onChange;
        this.theme = theme;
    }


    public void setValue(boolean value) {
        this.value = value;
    }

    public boolean getValue() {
        return this.value;
    }


    public void setOnChange(Consumer<Boolean> onChange) {
        this.onChange = onChange;
    }

    @Override
    protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        var theme = this.theme;

        // Update animation progress
        float targetProgress = value ? 1.0f : 0.0f;
        if (animationProgress < targetProgress) {
            animationProgress = Math.min(targetProgress, animationProgress + ANIMATION_SPEED);
        } else if (animationProgress > targetProgress) {
            animationProgress = Math.max(targetProgress, animationProgress - ANIMATION_SPEED);
        }

        // Interpolate background color between OFF and ON
        int bgColor = interpolateColors(
            theme.parseColor(theme.toggleOffColor),
            theme.parseColor(theme.toggleOnColor),
            animationProgress
        );

        // Add hover effect (brighten slightly)
        if (this.isHovered()) {
            bgColor = brightenColor(bgColor, 0.15f);
        }

        int knobColor = interpolateColors(
            theme.parseColor(theme.surfaceElevatedColor), // OFF - lighter color
            theme.parseColor(theme.surfaceColor),         // ON - darker color
            animationProgress
        );

        // Draw toggle background as a rectangle
        context.fill(this.getX(), this.getY(), this.getX() + WIDTH, this.getY() + HEIGHT, bgColor);

        // Calculate knob position based on animation progress
        int knobTravel = WIDTH - KNOB_SIZE - (KNOB_PADDING * 2);
        int knobX = this.getX() + KNOB_PADDING + (int) (knobTravel * animationProgress);
        int knobY = this.getY() + KNOB_PADDING;

        // Draw square knob with subtle shadow for depth
        int shadowColor = 0x40000000; // Semi-transparent black
        context.fill(knobX + 1, knobY + 1, knobX + KNOB_SIZE + 1, knobY + KNOB_SIZE + 1, shadowColor);
        context.fill(knobX, knobY, knobX + KNOB_SIZE, knobY + KNOB_SIZE, knobColor);
    }


    private void drawRoundedRect(GuiGraphics context, int x, int y, int width, int height, int radius, int color) {
        // Main center rectangle
        context.fill(x + radius, y, x + width - radius, y + height, color);

        // Left and right rectangles
        context.fill(x, y + radius, x + radius, y + height - radius, color);
        context.fill(x + width - radius, y + radius, x + width, y + height - radius, color);

        // Fill corners with circles (approximated with filled rectangles for simplicity)
        // Top-left corner
        fillCorner(context, x + radius, y + radius, radius, color, true, true);
        // Top-right corner
        fillCorner(context, x + width - radius, y + radius, radius, color, false, true);
        // Bottom-left corner
        fillCorner(context, x + radius, y + height - radius, radius, color, true, false);
        // Bottom-right corner
        fillCorner(context, x + width - radius, y + height - radius, radius, color, false, false);
    }


    private void fillCorner(GuiGraphics context, int centerX, int centerY, int radius, int color, boolean left, boolean top) {
        int xStart = left ? centerX - radius : centerX;
        int xEnd = left ? centerX : centerX + radius;
        int yStart = top ? centerY - radius : centerY;
        int yEnd = top ? centerY : centerY + radius;
        context.fill(xStart, yStart, xEnd, yEnd, color);
    }

    private int interpolateColors(int color1, int color2, float progress) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * progress);
        int r = (int) (r1 + (r2 - r1) * progress);
        int g = (int) (g1 + (g2 - g1) * progress);
        int b = (int) (b1 + (b2 - b1) * progress);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int brightenColor(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        r = Math.min(255, (int) (r + (255 - r) * factor));
        g = Math.min(255, (int) (g + (255 - g) * factor));
        b = Math.min(255, (int) (b + (255 - b) * factor));

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public void onClick(MouseButtonEvent click, boolean isDoubleClick) {
        this.value = !this.value;
        if (this.onChange != null) {
            this.onChange.accept(this.value);
        }
        this.playDownSound(Minecraft.getInstance().getSoundManager());
    }


    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE,
                Component.literal("Toggle: " + (value ? "ON" : "OFF")));
    }

    @Override
    public AbstractWidget.@NotNull NarrationPriority narrationPriority() {
        return AbstractWidget.NarrationPriority.HOVERED;
    }
}
