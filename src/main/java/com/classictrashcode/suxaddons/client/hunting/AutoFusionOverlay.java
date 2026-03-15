package com.classictrashcode.suxaddons.client.hunting;


import com.classictrashcode.suxaddons.client.config.Config;
import com.classictrashcode.suxaddons.client.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class AutoFusionOverlay {
    private static final int OVERLAY_WIDTH = 200;
    private static final int OVERLAY_HEIGHT = 140;
    private static final int PADDING = 10;
    private static final int LINE_HEIGHT = 12;

    // Button dimensions
    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_HEIGHT = 24;

    // State for button hover detection
    private static int buttonX;
    private static int buttonY;

    public static void renderOverlay(GuiGraphics context, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        if (!AutoFusion.isFusionMenu || !ConfigManager.getConfig().hunting.autoFusion.enabled) {
            return;
        }

        // Get theme colors from config
        var theme = ConfigManager.getConfig().guiTheme;
        int backgroundColor = theme.parseColor(theme.surfaceColor);
        int borderColor = theme.parseColor(theme.dividerColor);
        int textColor = theme.parseColor(theme.textPrimaryColor);
        int textSecondaryColor = theme.parseColor(theme.textSecondaryColor);

        int overlayX = PADDING;
        int overlayY = PADDING + 20; // Add extra padding from top

        // Draw background with slight transparency
        int bgColorWithAlpha = (backgroundColor & 0x00FFFFFF) | 0xE6000000; // 90% opacity
        context.fill(overlayX, overlayY, overlayX + OVERLAY_WIDTH, overlayY + OVERLAY_HEIGHT, bgColorWithAlpha);

        // Draw colored title bar
        int titleBarHeight = 22;
        int titleBarColor = theme.parseColor(theme.primaryColor);
        context.fill(overlayX, overlayY, overlayX + OVERLAY_WIDTH, overlayY + titleBarHeight, titleBarColor);

        // Draw border
        drawBorder(context, overlayX, overlayY, OVERLAY_WIDTH, OVERLAY_HEIGHT, borderColor);

        // Draw title in white on colored bar
        context.drawString(
            Minecraft.getInstance().font,
            Component.literal("Auto Fusion"),
            overlayX + PADDING,
            overlayY + (titleBarHeight - 8) / 2, // Center vertically in title bar
            0xFFFFFFFF, // White text
            false
        );

        // Content starts below title bar
        int currentY = overlayY + titleBarHeight + 8;

        // Draw status
        int statusColor = AutoFusion.running ? theme.parseColor(theme.successColor) : textSecondaryColor;
        context.drawString(
            Minecraft.getInstance().font,
            Component.literal("Status: "),
            overlayX + PADDING,
            currentY,
            textColor,
            false
        );
        context.drawString(
            Minecraft.getInstance().font,
            Component.literal(AutoFusion.running ? "Running" : "Stopped"),
            overlayX + PADDING + Minecraft.getInstance().font.width("Status: "),
            currentY,
            statusColor,
            false
        );

        // Draw completed cycles
        currentY += LINE_HEIGHT + 2;
        context.drawString(
            Minecraft.getInstance().font,
            Component.literal("Cycles: " + AutoFusion.completedCycles),
            overlayX + PADDING,
            currentY,
            textColor,
            false
        );

        // Draw current step
        currentY += LINE_HEIGHT + 2;
        int actionStep = AutoFusion.actionStep;
        context.drawString(
            Minecraft.getInstance().font,
            Component.literal("Step: " + actionStep + "/2"),
            overlayX + PADDING,
            currentY,
            textColor,
            false
        );

        // Draw delay settings
        currentY += LINE_HEIGHT + 4;
        int actionDelay = ConfigManager.getConfig().hunting.autoFusion.actionDelayTicks;
        int longDelay = ConfigManager.getConfig().hunting.autoFusion.longDelayTicks;
        context.drawString(
            Minecraft.getInstance().font,
            Component.literal("Action Delay: " + actionDelay + "t"),
            overlayX + PADDING,
            currentY,
            textSecondaryColor,
            false
        );
        currentY += LINE_HEIGHT;
        context.drawString(
            Minecraft.getInstance().font,
            Component.literal("Long Delay: " + longDelay + "t"),
            overlayX + PADDING,
            currentY,
            textSecondaryColor,
            false
        );

        // Draw toggle button
        currentY += LINE_HEIGHT + 6;
        buttonX = overlayX + (OVERLAY_WIDTH - BUTTON_WIDTH) / 2;
        buttonY = currentY;

        boolean hovered = isMouseOver(mouseX, mouseY, buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT);
        drawToggleButton(context, buttonX, buttonY, hovered);
    }

    private static void drawBorder(GuiGraphics context, int x, int y, int width, int height, int color) {
        // Top
        context.fill(x, y, x + width, y + 1, color);
        // Bottom
        context.fill(x, y + height - 1, x + width, y + height, color);
        // Left
        context.fill(x, y, x + 1, y + height, color);
        // Right
        context.fill(x + width - 1, y, x + width, y + height, color);
    }
    private static void drawToggleButton(GuiGraphics context, int x, int y, boolean hovered) {
        var theme = ConfigManager.getConfig().guiTheme;

        int buttonColor;
        if (AutoFusion.running) {
            // Stop button - use error color
            buttonColor = hovered ?
                theme.parseColor(theme.errorColor) :
                darken(theme.parseColor(theme.errorColor), 0.8f);
        } else {
            // Start button - use success color
            buttonColor = hovered ?
                theme.parseColor(theme.buttonHoverColor) :
                theme.parseColor(theme.buttonNormalColor);
        }

        // Draw button background
        context.fill(x, y, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, buttonColor);

        // Draw button border
        int borderColor = theme.parseColor(theme.dividerColor);
        drawBorder(context, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, borderColor);

        // Draw button text
        String buttonText = AutoFusion.running ? "Stop" : "Start";
        int textWidth = Minecraft.getInstance().font.width(buttonText);
        int textX = x + (BUTTON_WIDTH - textWidth) / 2;
        int textY = y + (BUTTON_HEIGHT - 8) / 2;

        context.drawString(
            Minecraft.getInstance().font,
            Component.literal(buttonText),
            textX,
            textY,
            0xFFFFFFFF,
            false
        );
    }

    private static boolean isMouseOver(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public static boolean handleClick(int mouseX, int mouseY) {
        if (!AutoFusion.isFusionMenu) {
            return false;
        }

        if (isMouseOver(mouseX, mouseY, buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
            if (!ConfigManager.getConfig().hunting.autoFusion.enabled) {
                return true; // Consume click but don't toggle
            }

            AutoFusion.running = !AutoFusion.running;
            if (!AutoFusion.running){
                AutoFusion.reset();
            }
            return true;
        }

        return false;
    }

    private static int darken(int color, float factor) {
        int alpha = (color >> 24) & 0xFF;
        int red = (int) (((color >> 16) & 0xFF) * factor);
        int green = (int) (((color >> 8) & 0xFF) * factor);
        int blue = (int) ((color & 0xFF) * factor);

        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
}
