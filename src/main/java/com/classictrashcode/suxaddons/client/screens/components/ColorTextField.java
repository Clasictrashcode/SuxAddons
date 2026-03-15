package com.classictrashcode.suxaddons.client.screens.components;



import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * A custom text field widget for hex color input with visual color preview.
 * Matches the visual style of SnappingSliderWidget.
 */
public class ColorTextField extends TextField {
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final int COLOR_PREVIEW_SIZE = 16;
    private static final int COLOR_PREVIEW_PADDING = 4;

    private Consumer<String> onChange;
    private boolean isValid = true;
    private final boolean showColorPreview;
    private final Font textRenderer;

    public ColorTextField(Font textRenderer, int x, int y, int width, int height,
                         String initialValue, boolean showColorPreview, com.classictrashcode.suxaddons.client.config.Config.GuiTheme theme) {
        super(textRenderer, x, y, width, height, String.valueOf(Component.empty()), theme);
        this.textRenderer = textRenderer;
        this.showColorPreview = showColorPreview;
        this.setValue(initialValue);
        this.setMaxLength(7); // "#" + 6 hex digits

        // Validate on text change
        this.setResponder(text -> {
            this.isValid = validateHexColor(text);
            if (this.isValid && onChange != null) {
                onChange.accept(text);
            }
        });
    }

    public void setOnChange(Consumer<String> onChange) {
        this.onChange = onChange;
    }

    private boolean validateHexColor(String text) {
        return HEX_COLOR_PATTERN.matcher(text).matches();
    }

    private int parseColor(String hex) {
        try {
            String cleaned = hex.replace("#", "");
            return (int) Long.parseLong(cleaned, 16) | 0xFF000000;
        } catch (NumberFormatException e) {
            return 0xFFFF0000; // Red on error
        }
    }

    @Override
    public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        var theme = this.theme;

        // Calculate offsets for color preview
        int textFieldX = this.getX();
        int textFieldWidth = this.width;

        if (showColorPreview) {
            textFieldX += COLOR_PREVIEW_SIZE + COLOR_PREVIEW_PADDING * 2;
            textFieldWidth -= COLOR_PREVIEW_SIZE + COLOR_PREVIEW_PADDING * 2;
        }

        // Draw background with themed colors
        int bgColor = theme.parseColor(theme.surfaceElevatedColor);
        int borderColor = isValid ?
            theme.parseColor(theme.dividerColor) :
            theme.parseColor(theme.errorColor);

        // Background
        context.fill(textFieldX - 1, this.getY() - 1,
                    textFieldX + textFieldWidth + 1, this.getY() + this.height + 1,
                    borderColor);
        context.fill(textFieldX, this.getY(),
                    textFieldX + textFieldWidth, this.getY() + this.height,
                    bgColor);

        // Draw color preview square if enabled
        if (showColorPreview) {
            int previewX = this.getX() + COLOR_PREVIEW_PADDING;
            int previewY = this.getY() + (this.height - COLOR_PREVIEW_SIZE) / 2;

            // Preview border
            context.fill(previewX - 1, previewY - 1,
                        previewX + COLOR_PREVIEW_SIZE + 1, previewY + COLOR_PREVIEW_SIZE + 1,
                        theme.parseColor(theme.dividerColor));

            // Preview color
            int previewColor = isValid ? parseColor(this.getValue()) : 0xFF424242;
            context.fill(previewX, previewY,
                        previewX + COLOR_PREVIEW_SIZE, previewY + COLOR_PREVIEW_SIZE,
                        previewColor);
        }

        // Temporarily adjust position for text rendering
        int originalX = this.getX();
        int originalWidth = this.width;

        if (showColorPreview) {
            this.setX(textFieldX);
            this.width = textFieldWidth;
        }

        // Render the text field (cursor, selection, text)
        super.renderWidget(context, mouseX, mouseY, delta);

        // Restore original position
        if (showColorPreview) {
            this.setX(originalX);
            this.width = originalWidth;
        }

        // Draw error indicator if invalid
        if (!isValid) {
            int errorTextColor = theme.parseColor(theme.errorColor);
            Component errorText = Component.literal("Invalid hex color");
            int errorX = this.getX() + this.width + 8;
            int errorY = this.getY() + (this.height - textRenderer.lineHeight) / 2;
            context.drawString(textRenderer, errorText, errorX, errorY, errorTextColor, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean isDoubleClick) {
        // Adjust click detection for color preview offset
        double mouseX = click.x();
        double mouseY = click.y();
        if (showColorPreview) {
            int textFieldX = this.getX() + COLOR_PREVIEW_SIZE + COLOR_PREVIEW_PADDING * 2;
            int textFieldWidth = this.width - COLOR_PREVIEW_SIZE - COLOR_PREVIEW_PADDING * 2;

            if (mouseX >= textFieldX && mouseX < textFieldX + textFieldWidth &&
                mouseY >= this.getY() && mouseY < this.getY() + this.height) {
                return super.mouseClicked(click, isDoubleClick);
            }
            return false;
        }
        return super.mouseClicked(click, isDoubleClick);
    }

    /**
     * Get whether the current text is a valid hex color
     */
    public boolean isValid() {
        return isValid;
    }
}
