package com.classictrashcode.suxaddons.client.screens.components;


import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class Button extends net.minecraft.client.gui.components.Button {
    private static final int TRANSITION_SPEED = 8; // Higher = faster transition
    private float hoverProgress = 0.0f;
    private boolean selected = false;
    private final com.classictrashcode.suxaddons.client.config.Config.GuiTheme theme;

    public Button(int x, int y, int width, int height, Component message, net.minecraft.client.gui.components.Button.OnPress onPress, com.classictrashcode.suxaddons.client.config.Config.GuiTheme theme) {
        super(x, y, width, height, message, onPress ,Button.DEFAULT_NARRATION);
        this.theme = theme;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    public boolean isSelected() {
        return this.selected;
    }

    @Override
    protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        var theme = this.theme;
        // Update hover progress for smooth transitions
        if (this.isHovered() && this.active) {
            hoverProgress = Math.min(1.0f, hoverProgress + delta * TRANSITION_SPEED);
        } else {
            hoverProgress = Math.max(0.0f, hoverProgress - delta * TRANSITION_SPEED);
        }
        // Determine button color based on state
        int buttonColor;
        if (!this.active) {
            buttonColor = theme.parseColor(theme.buttonDisabledColor);
        } else if (selected) {
            buttonColor = theme.parseColor(theme.buttonActiveColor);
        } else {
            // Interpolate between normal and hover colors
            buttonColor = interpolateColors(
                theme.parseColor(theme.buttonNormalColor),
                theme.parseColor(theme.buttonHoverColor),
                hoverProgress
            );
        }

        int textColor = this.active ?
            theme.parseColor(theme.textPrimaryColor) :
            theme.parseColor(theme.textSecondaryColor);

        // Draw button background as a plain rectangle (no rounded corners)
        context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, buttonColor);

        // Draw button text centered
        int textX = this.getX() + this.width / 2 - this.getWidth(this.getMessage()) / 2;
        int textY = this.getY() + (this.height - 8) / 2;
        context.drawString(
                Minecraft.getInstance().font,
            this.getMessage(),
            textX,
            textY,
            textColor,
            true
        );
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

    private int getWidth(Component text) {
        return Minecraft.getInstance().font.width(text.getString());
    }
}