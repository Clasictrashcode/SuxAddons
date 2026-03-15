package com.classictrashcode.suxaddons.client.screens.components;



import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.function.BiFunction;
import java.util.function.Consumer;


public class TextField extends EditBox {
    private static final int BORDER_THICKNESS = 1;
    private static final int PADDING = 4;

    private Consumer<String> onChange;
    private final Font textRenderer;
    protected final com.classictrashcode.suxaddons.client.config.Config.GuiTheme theme;
    private Component placeholder;
    private int firstCharacterIndex;
    private BiFunction<String, Integer, FormattedCharSequence> renderTextProvider;
    private Integer selectionStart;
    private boolean shouldDrawSelection;


    public TextField(Font textRenderer, int x, int y, int width, int height, com.classictrashcode.suxaddons.client.config.Config.GuiTheme theme) {
        this(textRenderer, x, y, width, height, "", theme);
    }

    public TextField(Font textRenderer, int x, int y, int width, int height, String initialValue, com.classictrashcode.suxaddons.client.config.Config.GuiTheme theme) {
        super(textRenderer, x, y, width, height, Component.empty());
        this.textRenderer = textRenderer;
        this.theme = theme;
        this.setValue(initialValue);
        this.setMaxLength(256);
        // Default change listener
        this.setResponder(text -> {
            if (this.onChange != null) {
                this.onChange.accept(text);
            }
        });
    }

    public void setOnChange(Consumer<String> onChange) {
        this.onChange = onChange;
    }

    public void setPlaceholder(Component placeholder) {
        this.placeholder = placeholder;
    }

    @Override
    public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        var theme = this.theme;

        // Determine colors based on state
        int bgColor = theme.parseColor(theme.textFieldBackgroundColor);
        int borderColor = this.isFocused() ?
            theme.parseColor(theme.textFieldFocusColor) :
            theme.parseColor(theme.textFieldBorderColor);
        int textColor = theme.parseColor(theme.textPrimaryColor);

        // Draw border
        context.fill(
            this.getX() - BORDER_THICKNESS,
            this.getY() - BORDER_THICKNESS,
            this.getX() + this.width + BORDER_THICKNESS,
            this.getY() + this.height + BORDER_THICKNESS,
            borderColor
        );

        // Draw background
        context.fill(
            this.getX(),
            this.getY(),
            this.getX() + this.width,
            this.getY() + this.height,
            bgColor
        );

        // Store original position and dimensions to account for padding
        int originalX = this.getX();
        int originalY = this.getY();
        int originalWidth = this.width;

        // Add padding by adjusting position
        this.setX(originalX + PADDING);
        this.setY(originalY);
        this.width = originalWidth - (PADDING * 2);

        // Restore original position and dimensions
        this.setX(originalX);
        this.width = originalWidth;

        //Render the Text without using the Super method

        // Draw placeholder if empty and not focused
        if (this.getValue().isEmpty() && !this.isFocused() && this.placeholder != null) {
            int placeholderColor = theme.parseColor(theme.textSecondaryColor);
            int placeholderX = this.getX() + PADDING;
            int placeholderY = this.getY() + (this.height - 8) / 2;
            context.drawString(textRenderer, this.placeholder, placeholderX, placeholderY, placeholderColor, false);
        }
        if (!this.getValue().isEmpty()) {
            int textX = this.getX() + PADDING;
            int textY = this.getY() + (this.height - 8) / 2;
            context.drawString(textRenderer, Component.literal(this.getValue()), textX, textY, textColor, false);
        }
        long time = System.currentTimeMillis();
        boolean showCursor = ((time / 500L) % 2L) == 0L;
        int cursor = this.getX() + PADDING + textRenderer.width(this.getValue().substring(0, this.getCursorPosition()));
        if (this.isFocused() && showCursor) {
            // Draw cursor
            context.fill(cursor , this.getY() + 4, cursor + 1, this.getY() + this.height - 4, textColor);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean isDoubleClick) {
        double mouseX = click.x();
        double mouseY = click.y();
        // Adjust click detection to account for our custom rendering
        if (mouseX >= this.getX() && mouseX < this.getX() + this.width &&
            mouseY >= this.getY() && mouseY < this.getY() + this.height) {
            return super.mouseClicked(click, isDoubleClick);
        }
        return false;
    }
}
