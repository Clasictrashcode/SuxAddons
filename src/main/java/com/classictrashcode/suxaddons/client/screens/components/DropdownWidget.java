package com.classictrashcode.suxaddons.client.screens.components;

import com.classictrashcode.suxaddons.client.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DropdownWidget extends AbstractWidget {
    private static final int OPTION_HEIGHT = 24;
    private static final int MAX_VISIBLE_OPTIONS = 6;
    private static final int ARROW_SIZE = 4;

    private final List<String> options;
    private String selectedOption;
    private boolean isOpen = false;
    private int hoveredIndex = -1;
    private final Consumer<String> onSelect;
    private final Config.GuiTheme theme;

    // --- SCROLLING FIELDS ---
    private int scrollOffset = 0;
    private boolean isDraggingScrollbar = false;

    public DropdownWidget(int x, int y, int width, int height, List<String> options, String selectedOption, Consumer<String> onSelect, Config.GuiTheme theme) {
        super(x, y, width, height, Component.literal(selectedOption != null ? selectedOption : "Select..."));
        this.options = new ArrayList<>(options);
        this.selectedOption = selectedOption;
        this.onSelect = onSelect;
        this.theme = theme;
    }

    // --- PUBLIC API ---
    public void setSelectedOption(String option) { if (options.contains(option)) { this.selectedOption = option; this.setMessage(Component.literal(option)); } }
    public String getSelectedOption() { return selectedOption; }
    public void setOptions(List<String> newOptions) { this.options.clear(); this.options.addAll(newOptions); if (selectedOption != null && !options.contains(selectedOption)) { selectedOption = options.isEmpty() ? null : options.get(0); if (selectedOption != null) { this.setMessage(Component.literal(selectedOption)); } } }
    public boolean isOpen() { return isOpen; }
    public void close() { isOpen = false; }

    public boolean isMouseOverOpenArea(double mouseX, double mouseY) {
        if (!isOpen) return false;
        int dropdownHeight = Math.min(options.size(), MAX_VISIBLE_OPTIONS) * OPTION_HEIGHT;
        return mouseX >= getX() && mouseX <= getX() + width && mouseY >= getY() + height && mouseY <= getY() + height + dropdownHeight;
    }

    // --- OVERRIDDEN WIDGET METHODS ---

    @Override
    public void onClick(MouseButtonEvent click, boolean isDoubleClick) {
        isOpen = !isOpen;
        if (isOpen) {
            scrollOffset = 0;
        }
    }

    @Override
    protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        Minecraft mc = Minecraft.getInstance();
        hoveredIndex = -1;
        if (isOpen) {
            int dropdownHeight = Math.min(options.size(), MAX_VISIBLE_OPTIONS) * OPTION_HEIGHT;
            if (mouseX >= getX() && mouseX <= getX() + width && mouseY >= getY() + height && mouseY <= getY() + height + dropdownHeight) {
                int visualIndex = (int)((mouseY - (getY() + height)) / OPTION_HEIGHT);
                if (visualIndex >= 0 && visualIndex < MAX_VISIBLE_OPTIONS) {
                    hoveredIndex = visualIndex + scrollOffset;
                    if (hoveredIndex >= options.size()) {
                        hoveredIndex = -1;
                    }
                }
            }
        }

        int buttonColor = this.isHovered() && !isOpen ? theme.parseColor(theme.buttonHoverColor) : theme.parseColor(theme.buttonNormalColor);
        context.fill(getX(), getY(), getX() + width, getY() + height, buttonColor);
        String displayText = selectedOption != null ? selectedOption : "Select...";
        context.drawString(mc.font, Component.literal(displayText), getX() + 8, getY() + (height - 8) / 2, theme.parseColor(theme.textPrimaryColor), false);
        drawArrow(context, theme, getX() + width - 16, getY() + height / 2, isOpen);

        if (isOpen) {
            renderDropdownMenu(context, theme, mc);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean isDoubleClick) {
        if (!this.active || !this.visible) return false;

        if (this.isWithinBounds(click.x(),click.y())) {
            this.onClick(click, isDoubleClick);
            return true;
        }

        if (isOpen) {
            int dropdownHeight = Math.min(options.size(), MAX_VISIBLE_OPTIONS) * OPTION_HEIGHT;

            if (options.size() > MAX_VISIBLE_OPTIONS) {
                int scrollbarWidth = 6;
                int scrollbarX = getX() + width - scrollbarWidth - 2;
                int maxScrollOffset = options.size() - MAX_VISIBLE_OPTIONS;
                float scrollPercentage = maxScrollOffset > 0 ? (float) scrollOffset / maxScrollOffset : 0;
                int thumbHeight = Math.max(20, (int) ((float) dropdownHeight * MAX_VISIBLE_OPTIONS / options.size()));
                int thumbY = getY() + height + (int) ((dropdownHeight - thumbHeight) * scrollPercentage);

                if (click.x() >= scrollbarX && click.x() <= scrollbarX + scrollbarWidth && click.y() >= thumbY && click.y() <= thumbY + thumbHeight) {
                    isDraggingScrollbar = true;
                    return true;
                }
            }

            if (isMouseOverOpenArea(click.x(), click.y())) {
                int visualIndex = (int)((click.y() - (getY() + height)) / OPTION_HEIGHT);
                int clickedIndex = visualIndex + scrollOffset;
                if (clickedIndex >= 0 && clickedIndex < options.size()) {
                    setSelectedOption(options.get(clickedIndex));
                    if (onSelect != null) onSelect.accept(selectedOption);
                    isOpen = false;
                    return true;
                }
            }

            isOpen = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        if (isDraggingScrollbar) {
            int dropdownHeight = Math.min(options.size(), MAX_VISIBLE_OPTIONS) * OPTION_HEIGHT;
            int maxScrollOffset = options.size() - MAX_VISIBLE_OPTIONS;
            int thumbHeight = Math.max(20, (int) ((float) dropdownHeight * MAX_VISIBLE_OPTIONS / options.size()));
            int scrollablePixelArea = dropdownHeight - thumbHeight;

            if (scrollablePixelArea > 0) {
                float scrollAmountPerPixel = (float) maxScrollOffset / scrollablePixelArea;
                scrollOffset += deltaY * scrollAmountPerPixel;
                scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (click.button() == 0) {
            isDraggingScrollbar = false;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isMouseOverOpenArea(mouseX, mouseY)) {
            int maxScrollOffset = options.size() - MAX_VISIBLE_OPTIONS;
            if (maxScrollOffset > 0) {
                scrollOffset -= (int) verticalAmount;
                scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));
                return true;
            }
        }
        return false;
    }

    private void renderDropdownMenu(GuiGraphics context, Config.GuiTheme theme, Minecraft mc) {
        int dropdownHeight = Math.min(options.size(), MAX_VISIBLE_OPTIONS) * OPTION_HEIGHT;
        int menuY = getY() + height;

        context.fill(getX(), menuY, getX() + width, menuY + dropdownHeight, theme.parseColor(theme.surfaceElevatedColor));
        context.fill(getX(), menuY, getX() + width, menuY + 1, theme.parseColor(theme.dividerColor));

        for (int i = 0; i < MAX_VISIBLE_OPTIONS; i++) {
            int optionIndex = i + scrollOffset;
            if (optionIndex >= options.size()) break;
            String option = options.get(optionIndex);
            int optionY = menuY + i * OPTION_HEIGHT;

            if (optionIndex == hoveredIndex) {
                context.fill(getX(), optionY, getX() + width, optionY + OPTION_HEIGHT, theme.parseColor(theme.buttonHoverColor));
            } else if (option.equals(selectedOption)) {
                context.fill(getX(), optionY, getX() + width, optionY + OPTION_HEIGHT, theme.parseColor(theme.buttonActiveColor));
            }
            context.drawString(mc.font, Component.literal(option), getX() + 8, optionY + (OPTION_HEIGHT - 8) / 2, theme.parseColor(theme.textPrimaryColor), false);
            if (i < MAX_VISIBLE_OPTIONS - 1) {
                context.fill(getX(), optionY + OPTION_HEIGHT - 1, getX() + width, optionY + OPTION_HEIGHT, theme.parseColor(theme.dividerColor));
            }
        }

        if (options.size() > MAX_VISIBLE_OPTIONS) {
            renderScrollbar(context, theme, menuY, dropdownHeight);
        }
    }

    private void renderScrollbar(GuiGraphics context, Config.GuiTheme theme, int menuY, int menuHeight) {
        int scrollbarWidth = 6;
        int scrollbarX = getX() + width - scrollbarWidth - 2;
        int maxScrollOffset = options.size() - MAX_VISIBLE_OPTIONS;
        context.fill(scrollbarX, menuY, scrollbarX + scrollbarWidth, menuY + menuHeight, theme.parseColor(theme.surfaceColor));
        float scrollPercentage = maxScrollOffset > 0 ? (float) scrollOffset / maxScrollOffset : 0;
        int thumbHeight = Math.max(20, (int) ((float) menuHeight * MAX_VISIBLE_OPTIONS / options.size()));
        int thumbY = menuY + (int) ((menuHeight - thumbHeight) * scrollPercentage);
        context.fill(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, theme.parseColor(theme.primaryColor));
    }

    private void drawArrow(GuiGraphics context, Config.GuiTheme theme, int centerX, int centerY, boolean pointUp) {
        int arrowColor = theme.parseColor(theme.textPrimaryColor);
        if (pointUp) {
            for (int i = 0; i <= ARROW_SIZE; i++) {
                int y = centerY - ARROW_SIZE / 2 + i;
                int startX = centerX - i;
                int endX = centerX + i + 1;
                context.fill(startX, y, endX, y + 1, arrowColor);
            }
        } else {
            for (int i = 0; i <= ARROW_SIZE; i++) {
                int y = centerY - ARROW_SIZE / 2 + i;
                int startX = centerX - ARROW_SIZE + i;
                int endX = centerX + ARROW_SIZE - i + 1;
                context.fill(startX, y, endX, y + 1, arrowColor);
            }
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE,
                Component.literal("Dropdown: " + (selectedOption != null ? selectedOption : "None selected")));
    }

    private boolean isWithinBounds(double mouseX, double mouseY) {
        return mouseX >= getX() && mouseX <= getX() + width &&
               mouseY >= getY() && mouseY <= getY() + height;
    }
}
