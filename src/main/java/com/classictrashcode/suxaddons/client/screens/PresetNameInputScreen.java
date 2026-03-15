package com.classictrashcode.suxaddons.client.screens;


import com.classictrashcode.suxaddons.client.config.ConfigManager;
import com.classictrashcode.suxaddons.client.config.ThemePresetManager;
import com.classictrashcode.suxaddons.client.screens.components.Button;
import com.classictrashcode.suxaddons.client.screens.components.TextField;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public class PresetNameInputScreen extends Screen {
    private final Screen parent;
    private final boolean isRename;
    private final com.classictrashcode.suxaddons.client.config.Config.GuiTheme theme;
    private TextField nameInput;
    private String errorMessage = null;

    public PresetNameInputScreen(Screen parent, boolean isRename, com.classictrashcode.suxaddons.client.config.Config.GuiTheme theme) {
        super(Component.literal(isRename ? "Rename Preset" : "Save Custom Preset"));
        this.parent = parent;
        this.isRename = isRename;
        this.theme = theme;
    }

    @Override
    protected void init() {
        super.init();

        int dialogWidth = 400;
        int dialogHeight = 150;
        int dialogX = this.width / 2 - dialogWidth / 2;
        int dialogY = this.height / 2 - dialogHeight / 2;

        // Create text input field
        int inputWidth = 340;
        int inputX = dialogX + (dialogWidth - inputWidth) / 2;
        int inputY = dialogY + 50;

        nameInput = new TextField(this.font, inputX, inputY, inputWidth, 20, theme);
        nameInput.setPlaceholder(Component.literal("Enter preset name..."));
        nameInput.setMaxLength(50);
        this.addRenderableWidget(nameInput);
        this.setInitialFocus(nameInput);

        // Create buttons
        int buttonY = dialogY + 95;
        int centerX = this.width / 2;

        Button cancelButton = new Button(centerX - 110, buttonY, 100, 24, Component.literal("Cancel"), button -> {
            this.onClose();
        }, theme);
        this.addRenderableWidget(cancelButton);

        Button saveButton = new Button(centerX + 10, buttonY, 100, 24, Component.literal("Save"), button -> {
            savePreset();
        }, theme);
        this.addRenderableWidget(saveButton);
    }

    private void savePreset() {
        String name = nameInput.getValue().trim();

        // Validate name
        if (name.isEmpty()) {
            errorMessage = "Preset name cannot be empty";
            return;
        }

        // Check if name conflicts with built-in preset
        if (ThemePresetManager.isBuiltIn(name)) {
            errorMessage = "Cannot overwrite built-in preset";
            return;
        }

        // Check if name already exists (for non-rename operations)
        if (!isRename && ThemePresetManager.presetExists(name)) {
            errorMessage = "Preset name already exists";
            return;
        }

        // Save the preset
        boolean success = ThemePresetManager.saveCurrentAsPreset(name, this.theme);

        if (success) {
            // Update current preset reference in the staged theme object
            this.theme.currentPreset = name;
            this.onClose();
        } else {
            errorMessage = "Failed to save preset";
        }
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // Dim the background
        context.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        var theme = ConfigManager.getConfig().guiTheme;

        // Draw dialog background
        int dialogWidth = 400;
        int dialogHeight = 150;
        int dialogX = this.width / 2 - dialogWidth / 2;
        int dialogY = this.height / 2 - dialogHeight / 2;

        context.fill(dialogX, dialogY, dialogX + dialogWidth, dialogY + dialogHeight,
            theme.parseColor(theme.surfaceColor));

        // Draw border
        context.fill(dialogX, dialogY, dialogX + dialogWidth, dialogY + 2,
            theme.parseColor(theme.primaryColor));

        // Draw title
        Component title = Component.literal(isRename ? "Rename Preset" : "Save Custom Preset").withStyle(ChatFormatting.BOLD);
        int titleX = dialogX + dialogWidth / 2 - this.font.width(title.getString()) / 2;
        context.drawString(this.font, title, titleX, dialogY + 15,
            theme.parseColor(theme.textPrimaryColor), false);

        // Draw description
        Component description = Component.literal("Enter a name for your custom theme preset:");
        int descX = dialogX + dialogWidth / 2 - this.font.width(description) / 2;
        context.drawString(this.font, description, descX, dialogY + 32,
            theme.parseColor(theme.textSecondaryColor), false);

        // Draw error message if present
        if (errorMessage != null) {
            Component error = Component.literal(errorMessage);
            int errorX = dialogX + dialogWidth / 2 - this.font.width(error) / 2;
            context.drawString(this.font, error, errorX, dialogY + 75,
                theme.parseColor(theme.errorColor), false);
        }

        // Render widgets
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(KeyEvent keyInput) {
        // Handle Enter key to save
        int keyCode = keyInput.key();
        if (keyCode == 257) { // Enter key
            savePreset();
            return true;
        }

        // Handle Escape key to cancel
        if (keyCode == 256) { // Escape key
            this.onClose();
            return true;
        }

        return super.keyPressed(keyInput);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
