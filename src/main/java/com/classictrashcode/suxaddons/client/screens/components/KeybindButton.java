package com.classictrashcode.suxaddons.client.screens.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class KeybindButton extends net.minecraft.client.gui.components.Button {
    private static final int TRANSITION_SPEED = 8;
    private float hoverProgress = 0.0f;
    private boolean listening = false;
    private int keyCode;
    private Consumer<Integer> onChange;
    private final com.classictrashcode.suxaddons.client.config.Config.GuiTheme theme;

    public KeybindButton(int x, int y, int width, int height, int initialKeyCode, Consumer<Integer> onChange, com.classictrashcode.suxaddons.client.config.Config.GuiTheme theme) {
        super(x, y, width, height, Component.literal(getKeyName(initialKeyCode)), btn -> {},Button.DEFAULT_NARRATION);
        this.keyCode = initialKeyCode;
        this.onChange = onChange;
        this.theme = theme;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
        this.setMessage(Component.literal(getKeyName(keyCode)));
    }


    public int getKeyCode() {
        return this.keyCode;
    }


    public void setListening(boolean listening) {
        this.listening = listening;
        if (listening) {
            this.setMessage(Component.literal("Press any key..."));
        } else {
            this.setMessage(Component.literal(getKeyName(this.keyCode)));
        }
    }

    /**
     * Get whether this button is in listening mode
     */
    public boolean isListening() {
        return this.listening;
    }

    /**
     * Handle key press when in listening mode
     */
    public boolean handleKeyPress(KeyEvent keyInput) {
        if (!this.listening) {
            return false;
        }

        // ESC clears the binding
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.keyCode = -1;
        } else {
            this.keyCode = keyInput.key();
        }

        this.setListening(false);

        if (this.onChange != null) {
            this.onChange.accept(this.keyCode);
        }

        return true;
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
        } else if (listening) {
            // Pulsing effect when listening
            float pulse = (float) (Math.sin(System.currentTimeMillis() / 200.0) * 0.5 + 0.5);
            buttonColor = interpolateColors(
                theme.parseColor(theme.buttonNormalColor),
                theme.parseColor(theme.primaryLightColor),
                pulse * 0.6f
            );
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

        // Draw button background with rounded corners
        drawRoundedRect(context, this.getX(), this.getY(), this.width, this.height, 3, buttonColor);

        // Draw listening indicator border if active
        if (listening) {
            int borderColor = theme.parseColor(theme.primaryLightColor);
            drawRoundedRectBorder(context, this.getX(), this.getY(), this.width, this.height, 3, borderColor, 2);
        }

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

    @Override
    public void onClick(MouseButtonEvent click, boolean isDoubleClick) {
        if (this.active) {
            this.setListening(true);
            this.playDownSound(Minecraft.getInstance().getSoundManager());
        }
    }

    private void drawRoundedRect(GuiGraphics context, int x, int y, int width, int height, int radius, int color) {
        // Main rectangle
        context.fill(x + radius, y, x + width - radius, y + height, color);
        context.fill(x, y + radius, x + radius, y + height - radius, color);
        context.fill(x + width - radius, y + radius, x + width, y + height - radius, color);

        // Corners
        context.fill(x + radius, y, x + width - radius, y + radius, color);
        context.fill(x + radius, y + height - radius, x + width - radius, y + height, color);
    }

    private void drawRoundedRectBorder(GuiGraphics context, int x, int y, int width, int height, int radius, int color, int thickness) {
        // Top border
        context.fill(x + radius, y - thickness, x + width - radius, y, color);
        // Bottom border
        context.fill(x + radius, y + height, x + width - radius, y + height + thickness, color);
        // Left border
        context.fill(x - thickness, y + radius, x, y + height - radius, color);
        // Right border
        context.fill(x + width, y + radius, x + width + thickness, y + height - radius, color);

        // Corner fills (simplified)
        context.fill(x, y, x + radius, y + radius, color);
        context.fill(x + width - radius, y, x + width, y + radius, color);
        context.fill(x, y + height - radius, x + radius, y + height, color);
        context.fill(x + width - radius, y + height - radius, x + width, y + height, color);
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

    private static String getKeyName(int keyCode) {
        if (keyCode == -1) return "Not Set";

        String keyName = GLFW.glfwGetKeyName(keyCode, 0);
        if (keyName == null) {
            return switch (keyCode) {
                case GLFW.GLFW_KEY_SPACE -> "SPACE";
                case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
                case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
                case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
                case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
                case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
                case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
                case GLFW.GLFW_KEY_TAB -> "TAB";
                case GLFW.GLFW_KEY_ENTER -> "ENTER";
                case GLFW.GLFW_KEY_BACKSPACE -> "BACKSPACE";
                case GLFW.GLFW_KEY_DELETE -> "DELETE";
                case GLFW.GLFW_KEY_INSERT -> "INSERT";
                case GLFW.GLFW_KEY_HOME -> "HOME";
                case GLFW.GLFW_KEY_END -> "END";
                case GLFW.GLFW_KEY_PAGE_UP -> "PAGE UP";
                case GLFW.GLFW_KEY_PAGE_DOWN -> "PAGE DOWN";
                case GLFW.GLFW_KEY_ESCAPE -> "ESC";
                default -> "KEY " + keyCode;
            };
        }
        return keyName.toUpperCase();
    }
}
