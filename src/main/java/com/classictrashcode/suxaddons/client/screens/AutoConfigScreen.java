package com.classictrashcode.suxaddons.client.screens;


import com.classictrashcode.suxaddons.client.SoundAlert;
import com.classictrashcode.suxaddons.client.config.*;
import com.classictrashcode.suxaddons.client.config.annotations.WidgetType;
import com.classictrashcode.suxaddons.client.screens.components.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AutoConfigScreen extends Screen {
    private final Screen parent;
    private String searchQuery = "";
    private KeybindButton activeKeybindButton = null;
    private String currentCategory = "";
    private final Map<String, Button> tabButtons = new HashMap<>();
    private final List<WidgetEntry> contentWidgets = new ArrayList<>();
    private DropdownWidget themePresetDropdown = null;
    private final List<DropdownWidget> allDropdowns = new ArrayList<>();

    // Staged config for uncommitted changes
    private Config stagedConfig;
    private ConfigRegistry stagedRegistry;

    // Scroll state
    private int scrollOffset = 0;
    private int maxScrollOffset = 0;
    private boolean isDraggingScrollbar = false;

    // Layout constants
    private static final int HEADER_HEIGHT = 90;
    private static final int TAB_BAR_HEIGHT = 40;
    private static final int FOOTER_HEIGHT = 50;
    private static final int CONTENT_PADDING = 20;
    private static final int CARD_SPACING = 8;
    private static final int CARD_PADDING = 16;
    private static final int CARD_WIDTH = 600;
    private static final int SCROLLBAR_WIDTH = 6;

    public AutoConfigScreen(Screen parent) {
        super(Component.literal("Configuration"));
        this.parent = parent;

        ConfigManager.load();
        ThemePresetManager.initialize();

        // Create staged copy of config for uncommitted changes
        stagedConfig = ConfigManager.createStagedConfig();
        stagedRegistry = new ConfigRegistry(stagedConfig);
    }

    @Override
    protected void init() {
        // Clear previous state
        contentWidgets.clear();
        tabButtons.clear();
        this.clearWidgets();
        scrollOffset = 0;
        themePresetDropdown = null;

        ConfigRegistry registry = stagedRegistry;

        // Create search bar
        int searchWidth = 360;
        int searchX = this.width / 2 - searchWidth / 2;
        int searchY = 55;

        TextField searchBox = new TextField(this.font, searchX, searchY, searchWidth, 20, stagedConfig.guiTheme);
        searchBox.setPlaceholder(Component.literal("Search settings..."));
        searchBox.setOnChange(query -> {
            this.searchQuery = query.toLowerCase();
            refreshContent();
        });
        this.addRenderableWidget(searchBox);

        // Create tab buttons
        int tabsStartX = Math.max(20, this.width / 2 - 400);
        int tabX = tabsStartX;
        int tabY = HEADER_HEIGHT + 10;

        for (ConfigRegistry.CategoryEntry category : registry.getCategories()) {
            if (category.separator) {
                tabX += 20;
                continue;
            }

            int tabWidth = this.font.width(category.name) + 20;
            String categoryName = category.name; // Capture for lambda
            Button tabBtn = new Button(tabX, tabY, tabWidth, 24, Component.literal(category.name), button -> {
                showCategory(categoryName);
                updateTabSelection(categoryName);
            }, stagedConfig.guiTheme);

            tabButtons.put(category.name, tabBtn);
            this.addRenderableWidget(tabBtn);
            tabX += tabWidth + 4;
        }

        // Create footer buttons
        int footerY = this.height - FOOTER_HEIGHT + 15;
        int centerX = this.width / 2;

        Button cancelBtn = new Button(centerX - 130, footerY, 120, 24, Component.literal("Cancel"), button -> {
            // Discard staged changes
            this.onClose();
        }, stagedConfig.guiTheme);
        this.addRenderableWidget(cancelBtn);

        Button saveBtn = new Button(centerX + 10, footerY, 120, 24, Component.literal("Save & Close"), button -> {
            // Apply staged config and save
            ConfigManager.applyStagedConfig(stagedConfig);
            ConfigManager.save();
            this.onClose();
        }, stagedConfig.guiTheme);
        this.addRenderableWidget(saveBtn);

        // Show first category by default
        if (!registry.getCategories().isEmpty()) {
            String firstCategory = registry.getCategories().stream()
                .filter(c -> !c.separator)
                .findFirst()
                .map(c -> c.name)
                .orElse("");

            if (!firstCategory.isEmpty()) {
                currentCategory = firstCategory;
                showCategory(firstCategory);
                updateTabSelection(firstCategory);
            }
        }
    }

    private void updateTabSelection(String categoryName) {
        // Use the custom Button's setSelected method for visual feedback
        tabButtons.forEach((name, btn) -> {
            btn.setSelected(name.equals(categoryName));
        });
    }

    private void showCategory(String categoryName) {
        currentCategory = categoryName;
        refreshContent();
    }

    private void refreshContent() {
        // Remove old content widgets
        contentWidgets.forEach(entry -> this.removeWidget(entry.widget));
        contentWidgets.clear();
        allDropdowns.clear();
        scrollOffset = 0;

        ConfigRegistry registry = stagedRegistry;
        ConfigRegistry.CategoryEntry category = registry.getCategoryByName(currentCategory);

        if (category == null) return;

        int contentStartY = HEADER_HEIGHT + TAB_BAR_HEIGHT + CONTENT_PADDING;
        int contentX = this.width / 2 - CARD_WIDTH / 2;
        int currentY = contentStartY;

        if (category.name.equals("About")) {
            buildAboutContent(contentX, currentY);
        } else {
            buildCategoryContent(category, contentX, currentY);
        }

        // Calculate max scroll offset
        int contentEndY = contentWidgets.isEmpty() ? currentY :
            contentWidgets.get(contentWidgets.size() - 1).y + contentWidgets.get(contentWidgets.size() - 1).height;
        int availableHeight = this.height - contentStartY - FOOTER_HEIGHT - CONTENT_PADDING;
        maxScrollOffset = Math.max(0, contentEndY - contentStartY - availableHeight);
    }

    private void buildAboutContent(int x, int startY) {
        int y = startY;

        // About section doesn't have interactive widgets, will be rendered in render()
        // Just reserve space for rendering
        WidgetEntry spacer = new WidgetEntry(null, x, y, CARD_WIDTH, 250);
        contentWidgets.add(spacer);
    }

    private void buildCategoryContent(ConfigRegistry.CategoryEntry category, int x, int startY) {
        List<Object> entries = filterEntries(category.getAllEntries());

        if (entries.isEmpty() && !searchQuery.isEmpty()) {
            // No results - will be rendered in render()
            WidgetEntry spacer = new WidgetEntry(null, x, startY, CARD_WIDTH, 60);
            contentWidgets.add(spacer);
            return;
        }

        int y = startY;

        // Special handling for GUI Theme category - add preset controls at the top
        if (category.name.equals("GUI Theme") && searchQuery.isEmpty()) {
            y = buildThemePresetControls(x, y);
        }

        for (Object entry : entries) {
            int cardHeight = 44; // Default card height
            boolean entryRendered = false;

            if (entry instanceof ConfigRegistry.ConfigOptionEntry option) {
                if (!option.isDependencySatisfied()) continue;

                WidgetType type = option.widgetType == WidgetType.AUTO ? inferWidgetType(option) : option.widgetType;

                // Skip entries with unsupported types (null type means skip)
                if (type == null) continue;

                if (type == WidgetType.TOGGLE) {
                    ToggleButton widget = createToggleButton(option, x, y);
                    if (widget != null) {
                        addContentWidget(widget, x, y, CARD_WIDTH, cardHeight);
                        entryRendered = true;
                    }
                } else if (type == WidgetType.SLIDER) {
                    SnappingSliderWidget widget = createSlider(option, x, y);
                    if (widget != null) {
                        addContentWidget(widget, x, y, CARD_WIDTH, cardHeight);
                        entryRendered = true;
                    }
                } else if (type == WidgetType.TEXT_FIELD || type == WidgetType.COLOR) {
                    AbstractWidget widget = createTextField(option, x, y);
                    if (widget != null) {
                        addContentWidget(widget, x, y, CARD_WIDTH, cardHeight);
                        entryRendered = true;
                    }
                } else if (type == WidgetType.DROPDOWN) {
                    DropdownWidget widget = createDropdown(option, x, y);
                    if (widget != null) {
                        addContentWidget(widget, x, y, CARD_WIDTH, cardHeight);
                        allDropdowns.add(widget);
                        entryRendered = true;
                    }
                }

            } else if (entry instanceof ConfigRegistry.SubSettingsEntry subSettings) {
                AbstractWidget[] widgets = createSubSettingsButtons(subSettings, x, y);
                for (AbstractWidget widget : widgets) {
                    if (widget != null) {
                        addContentWidget(widget, x, y, CARD_WIDTH, cardHeight);
                        entryRendered = true;
                    }
                }

            } else if (entry instanceof ConfigRegistry.ConfigKeybindEntry keybind) {
                if (!keybind.isDependencySatisfied()) continue;

                KeybindButton widget = createKeybindButton(keybind, x, y);
                if (widget != null) {
                    addContentWidget(widget, x, y, CARD_WIDTH, cardHeight);
                    entryRendered = true;
                }

            } else if (entry instanceof ConfigRegistry.ConfigListEntry listEntry) {
                // Build list UI (title card + add button + entries + add button)
                y = buildListContent(listEntry, x, y);
                continue; // Skip the y increment since buildListContent returns new y
            }

            if (entryRendered) {
                y += cardHeight + CARD_SPACING;
            }
        }
    }

    private ToggleButton createToggleButton(ConfigRegistry.ConfigOptionEntry option, int cardX, int cardY) {
        // Type safety check - only create toggle button for boolean fields
        if (option.field.getType() != boolean.class && option.field.getType() != Boolean.class) {
            return null; // Skip non-boolean fields
        }

        boolean value = (boolean) option.getValue();

        int btnX = cardX + CARD_WIDTH - 48 - CARD_PADDING;
        int btnY = cardY + 13; // Centered: (44 - 18) / 2 = 13

        ToggleButton toggle = new ToggleButton(btnX, btnY, value, newValue -> {
            option.setValue(newValue);
            // Don't save yet - changes are staged
            refreshContent(); // Refresh for dependencies
        }, stagedConfig.guiTheme);

        return toggle;
    }

    private SnappingSliderWidget createSlider(ConfigRegistry.ConfigOptionEntry option, int cardX, int cardY) {
        final double min = option.range != null ? option.range.min() : 0;
        final double max = option.range != null ? option.range.max() : 100;
        final double step = option.range != null ? option.range.step() : 1;
        double value = Double.parseDouble(String.valueOf(option.getValue()));

        int sliderX = cardX + CARD_WIDTH - 200 - 60 - CARD_PADDING;
        int sliderY = cardY + 10; // Centered: (44 - 24) / 2 = 10

        SnappingSliderWidget slider = new SnappingSliderWidget(sliderX, sliderY, 200, 24, min, max, step, value, stagedConfig.guiTheme);
        slider.setOnChange(snappedValue -> {
            if (option.field.getType() == int.class) {
                option.setValue((int) Math.round(snappedValue));
            } else if (option.field.getType() == float.class) {
                option.setValue(Float.valueOf(snappedValue.floatValue()));
            } else {
                option.setValue(snappedValue);
            }
            // Don't save yet - changes are staged
        });

        // Set dropdown open check to hide slider text when dropdown is open
        slider.setDropdownOpenCheck(this::hasOpenDropdown);

        return slider;
    }

    /**
     * Checks if any dropdown is currently open
     * @return true if any dropdown is expanded/open
     */
    private boolean hasOpenDropdown() {
        for (DropdownWidget dropdown : allDropdowns) {
            if (dropdown != null && dropdown.isOpen()) {
                return true;
            }
        }
        return false;
    }

    private AbstractWidget createTextField(ConfigRegistry.ConfigOptionEntry option, int cardX, int cardY) {
        // Type safety check - only create text field for String fields
        if (option.field.getType() != String.class) {
            return null;
        }

        String value = (String) option.getValue();
        if (value == null) {
            value = "";
        }

        // Check if this is a color field (use COLOR widget type or name contains "color")
        boolean isColorField = option.widgetType == WidgetType.COLOR || option.name.toLowerCase().contains("color");

        int textFieldX = cardX + CARD_WIDTH - 200 - 60 - CARD_PADDING;
        int textFieldY = cardY + 12; // Centered: (44 - 20) / 2 = 12
        int textFieldWidth = 200;

        if (isColorField) {
            ColorTextField textField = new ColorTextField(
                this.font, textFieldX, textFieldY, textFieldWidth, 20,
                value, true, stagedConfig.guiTheme
            );

            textField.setOnChange(newValue -> {
                option.setValue(newValue);
                // Don't save yet - changes are staged
            });

            return textField;
        } else {
            TextField textField = new TextField(this.font, textFieldX, textFieldY, textFieldWidth, 20, value, stagedConfig.guiTheme);

            textField.setOnChange(newValue -> {
                option.setValue(newValue);
                // Don't save yet - changes are staged
            });

            return textField;
        }
    }

    private DropdownWidget createDropdown(ConfigRegistry.ConfigOptionEntry option, int cardX, int cardY) {
        // Type safety check - only create dropdown for String fields marked as DROPDOWN
        if (option.field.getType() != String.class) {
            return null;
        }

        String currentValue = (String) option.getValue();
        if (currentValue == null) {
            currentValue = "";
        }

        // Get dropdown options - for now, we'll specifically handle SoundAlert
        List<String> options = new ArrayList<>();

        // Check if this is a sound type field by checking field name
        if (option.name.toLowerCase().contains("sound") && option.name.toLowerCase().contains("type")) {
            // Populate with all sound names (built-in + custom)
            options = SoundAlert.getAllSoundNames();
        } else {
            // Generic fallback - just use current value
            options.add(currentValue.isEmpty() ? "None" : currentValue);
        }

        int dropdownX = cardX + CARD_WIDTH - 200 - 60 - CARD_PADDING;
        int dropdownY = cardY + 10; // Centered: (44 - 24) / 2 = 10
        int dropdownWidth = 200;

        DropdownWidget dropdown = new DropdownWidget(
            dropdownX, dropdownY, dropdownWidth, 24,
            options,
            currentValue.isEmpty() ? options.get(0) : currentValue,
            selectedValue -> {
                option.setValue(selectedValue);
                // Changes are staged, not saved immediately
            },
            stagedConfig.guiTheme
        );

        return dropdown;
    }

    private AbstractWidget[] createSubSettingsButtons(ConfigRegistry.SubSettingsEntry subSettings, int cardX, int cardY) {
        List<AbstractWidget> buttons = new ArrayList<>();

        int btnX = cardX + CARD_WIDTH - CARD_PADDING;
        int btnY = cardY + 10; // Centered: (44 - 24) / 2 = 10

        // Settings button
        btnX -= 100;
        Button settingsBtn = new Button(btnX, btnY, 100, 24, Component.literal("Configure"), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new AutoSubSettingsScreen(this, subSettings, stagedConfig.guiTheme));
            }
        }, stagedConfig.guiTheme);
        buttons.add(settingsBtn);

        // Toggle button if has enabled field
        boolean hasEnabled = hasEnabledField(subSettings.instance);
        if (hasEnabled) {
            btnX -= 52; // Adjusted for ToggleButton width (48px)
            boolean value = subSettings.isEnabled();
            ToggleButton toggleBtn = new ToggleButton(btnX, btnY + 3, value, newValue -> { // +3 to align with 24px button
                toggleEnabled(subSettings.instance);
                // Don't save yet - changes are staged
                refreshContent();
            }, stagedConfig.guiTheme);
            buttons.add(toggleBtn);
        }

        return buttons.toArray(new AbstractWidget[0]);
    }

    private KeybindButton createKeybindButton(ConfigRegistry.ConfigKeybindEntry keybind, int cardX, int cardY) {
        int keyCode = keybind.getValue();

        int btnX = cardX + CARD_WIDTH - 120 - CARD_PADDING;
        int btnY = cardY + 10; // Centered: (44 - 24) / 2 = 10

        KeybindButton button = new KeybindButton(btnX, btnY, 120, 24, keyCode, newKeyCode -> {
            keybind.setValue(newKeyCode);
            // Don't save yet - changes are staged
        }, stagedConfig.guiTheme) {
            @Override
            public void onClick(MouseButtonEvent click, boolean isDoubleClick) {
                super.onClick(click, isDoubleClick);
                // Track this button when it starts listening
                if (this.isListening()) {
                    activeKeybindButton = this;
                }
            }
        };

        return button;
    }

    private void addContentWidget(AbstractWidget widget, int x, int y, int width, int height) {
        contentWidgets.add(new WidgetEntry(widget, x, y, width, height));
        this.addRenderableWidget(widget);
    }

    /**
     * Build theme preset controls (dropdown + save/delete buttons) for GUI Theme category
     * @return The new Y position after adding the controls
     */
    private int buildThemePresetControls(int x, int y) {
        int cardHeight = 80;

        // Create dropdown for preset selection
        Config.GuiTheme theme = stagedConfig.guiTheme;
        String currentPreset = theme.currentPreset != null ? theme.currentPreset : "Dark Mode (Green)";

        int dropdownX = x + CARD_PADDING;
        int dropdownY = y + 28; // Centered in 80px card: (80 - 24) / 2 = 28
        int dropdownWidth = 300;

        themePresetDropdown = new DropdownWidget(
            dropdownX, dropdownY, dropdownWidth, 24,
            ThemePresetManager.getPresetNames(),
            currentPreset,
            presetName -> {
                // Apply selected preset to staged config
                ThemePreset preset = ThemePresetManager.getPreset(presetName);
                if (preset != null) {
                    preset.applyTo(stagedConfig.guiTheme);
                    stagedConfig.guiTheme.currentPreset = presetName;
                }
                refreshContent(); // Refresh to update color fields
            },
            stagedConfig.guiTheme
        );
        themePresetDropdown.setSelectedOption(theme.currentPreset);
        addContentWidget(themePresetDropdown, x, y, CARD_WIDTH, cardHeight);
        allDropdowns.add(themePresetDropdown);

        // Create "Save As..." button
        int saveButtonX = dropdownX + dropdownWidth + 8;
        int saveButtonY = dropdownY;
        Button saveButton = new Button(saveButtonX, saveButtonY, 120, 24, Component.literal("Save As..."), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new PresetNameInputScreen(this, false, stagedConfig.guiTheme));
            }
        }, stagedConfig.guiTheme);
        addContentWidget(saveButton, x, y, CARD_WIDTH, cardHeight);

        int deleteButtonX = saveButtonX + 128;
        Button deleteButton = new Button(deleteButtonX, saveButtonY, 80, 24, Component.literal("Delete"), button -> {
            String selected = themePresetDropdown.getSelectedOption();
            if (selected != null && !ThemePresetManager.isBuiltIn(selected)) {
                ThemePresetManager.deletePreset(selected);
                // Select the first available preset after deletion
                List<String> remaining = ThemePresetManager.getPresetNames();
                if (!remaining.isEmpty()) {
                    String newPreset = remaining.get(0);
                    ThemePreset preset = ThemePresetManager.getPreset(newPreset);
                    if (preset != null) {
                        preset.applyTo(stagedConfig.guiTheme);
                        stagedConfig.guiTheme.currentPreset = newPreset;
                    }
                }
                refreshContent();
            }
        }, stagedConfig.guiTheme);
        addContentWidget(deleteButton, x, y, CARD_WIDTH, cardHeight);

        // Update delete button state based on selected preset
        String selected = themePresetDropdown.getSelectedOption();
        deleteButton.active = selected != null && !ThemePresetManager.isBuiltIn(selected);

        // Add label text area (will be rendered in renderThemePresetCard)
        WidgetEntry labelEntry = new WidgetEntry(null, x, y, CARD_WIDTH, cardHeight);
        contentWidgets.add(labelEntry);

        return y + cardHeight + CARD_SPACING;
    }

    /**
     * Build UI for a ConfigList entry (title + add buttons + list entries)
     * @return New Y position after building the list
     */
    private int buildListContent(ConfigRegistry.ConfigListEntry listEntry, int x, int y) {
        int startY = y;
        int cardHeight = 44;
        boolean isHorizontal = listEntry.annotation.layout().equals("horizontal");

        // Title card with list name
        contentWidgets.add(new WidgetEntry(null, x, y, CARD_WIDTH, cardHeight));
        y += cardHeight + CARD_SPACING;

        // Render each list entry
        List<Object> list = (List<Object>) listEntry.getList();
        int maxSize = listEntry.annotation.maxSize();

        for (int i = 0; i < list.size(); i++) {
            final int index = i;
            Object entryInstance = list.get(i);

            int entryCardHeight = isHorizontal ? 70 : (44 * listEntry.entryFields.size() + 40);
            contentWidgets.add(new WidgetEntry(null, x, y, CARD_WIDTH, entryCardHeight));

            // --- Action Buttons ---
            int actionButtonY = y + 10;
            int currentX = x + CARD_WIDTH - CARD_PADDING;

            currentX -= 60;
            Button deleteBtn = new Button(currentX, actionButtonY, 60, 24, Component.literal("Delete"), btn -> {
                list.remove(index);
                refreshContent();
            }, stagedConfig.guiTheme);
            addContentWidget(deleteBtn, x, y, CARD_WIDTH, entryCardHeight);

            if (index < list.size() - 1) {
                currentX -= 34;
                Button downBtn = new Button(currentX, actionButtonY, 30, 24, Component.literal("▼"), btn -> {
                    Object temp = list.get(index);
                    list.set(index, list.get(index + 1));
                    list.set(index + 1, temp);
                    refreshContent();
                }, stagedConfig.guiTheme);
                addContentWidget(downBtn, x, y, CARD_WIDTH, entryCardHeight);
            }

            if (index > 0) {
                currentX -= 34;
                Button upBtn = new Button(currentX, actionButtonY, 30, 24, Component.literal("▲"), btn -> {
                    Object temp = list.get(index);
                    list.set(index, list.get(index - 1));
                    list.set(index - 1, temp);
                    refreshContent();
                }, stagedConfig.guiTheme);
                addContentWidget(upBtn, x, y, CARD_WIDTH, entryCardHeight);
            }

            // --- Fields ---
            int fieldY = y + 40;
            if (isHorizontal) {
                int totalFieldWidth = listEntry.entryFields.stream().mapToInt(f -> (f.isKeybind ? 120 : 150)).sum() + Math.max(0, listEntry.entryFields.size() - 1) * 8;
                int fieldX = x + CARD_PADDING + (CARD_WIDTH - 2 * CARD_PADDING - totalFieldWidth) / 2;

                for (ConfigRegistry.ConfigListEntry.EntryFieldInfo fieldInfo : listEntry.entryFields) {
                    int widgetHeight = fieldInfo.isKeybind ? 24 : 20;
                    AbstractWidget fieldWidget = createListEntryFieldWidget(fieldInfo, entryInstance, fieldX, y + (entryCardHeight - widgetHeight) / 2);
                    addContentWidget(fieldWidget, x, y, CARD_WIDTH, entryCardHeight);
                    fieldX += (fieldInfo.isKeybind ? 120 : 150) + 8;
                }
            } else {
                for (ConfigRegistry.ConfigListEntry.EntryFieldInfo fieldInfo : listEntry.entryFields) {
                    int widgetHeight = fieldInfo.isKeybind ? 24 : 20;
                    int widgetX = x + CARD_WIDTH - (fieldInfo.isKeybind ? 120 : 150) - CARD_PADDING;
                    AbstractWidget fieldWidget = createListEntryFieldWidget(fieldInfo, entryInstance, widgetX, fieldY + (44 - widgetHeight) / 2);
                    addContentWidget(fieldWidget, x, y, CARD_WIDTH, entryCardHeight);
                    fieldY += 44;
                }
            }

            y += entryCardHeight + CARD_SPACING;
        }

        // Bottom "Add" button
        boolean canAddMore = maxSize == -1 || list.size() < maxSize;
        if (canAddMore) {
            Button bottomAddBtn = new Button(x + CARD_WIDTH / 2 - 60, y + (cardHeight - 24) / 2, 120, 24, Component.literal("+ Add"), btn -> {
                addListEntry(listEntry);
                refreshContent();
            }, stagedConfig.guiTheme);
            addContentWidget(bottomAddBtn, x, y, CARD_WIDTH, cardHeight);
            y += cardHeight + CARD_SPACING;
        }

        return y;
    }

    /**
     * Get the title for a list entry
     */
    private String getListEntryTitle(ConfigRegistry.ConfigListEntry listEntry, Object entryInstance, int index) {
        String titleField = listEntry.annotation.entryTitleField();

        if (!titleField.isEmpty()) {
            // Use specified field for title
            for (ConfigRegistry.ConfigListEntry.EntryFieldInfo fieldInfo : listEntry.entryFields) {
                if (fieldInfo.field.getName().equals(titleField)) {
                    Object value = fieldInfo.getValue(entryInstance);
                    if (value != null) {
                        return value.toString();
                    }
                }
            }
        }

        // Auto-generate title
        String prefix = listEntry.annotation.titlePrefix();
        return prefix + " #" + (index + 1);
    }

    /**
     * Add a new entry to a list
     */
    @SuppressWarnings("unchecked")
    private void addListEntry(ConfigRegistry.ConfigListEntry listEntry) {
        try {
            Class<?> entryClass = listEntry.annotation.entryClass();
            Object newEntry = entryClass.getDeclaredConstructor().newInstance();
            List<Object> list = (List<Object>) listEntry.getList();
            list.add(newEntry);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Create a widget for a field in a list entry
     */
    private AbstractWidget createListEntryFieldWidget(ConfigRegistry.ConfigListEntry.EntryFieldInfo fieldInfo,
                                                        Object entryInstance, int x, int y) {
        if (fieldInfo.isKeybind) {
            // Keybind button
            int keyCode = (int) fieldInfo.getValue(entryInstance);
            KeybindButton btn = new KeybindButton(x, y, 120, 24, keyCode, newKeyCode -> {
                fieldInfo.setValue(entryInstance, newKeyCode);
            }, stagedConfig.guiTheme) {
                @Override
                public void onClick(MouseButtonEvent click, boolean isDoubleClick) {
                    super.onClick(click, isDoubleClick);
                    if (this.isListening()) {
                        activeKeybindButton = this;
                    }
                }
            };
            return btn;
        } else {
            // Text field (for strings and other types)
            Object value = fieldInfo.getValue(entryInstance);
            String strValue = value != null ? value.toString() : "";

            TextField textField = new TextField(this.font, x, y, 150, 20, strValue, stagedConfig.guiTheme);
            textField.setOnChange(newValue -> {
                // Convert string back to appropriate type
                Class<?> fieldType = fieldInfo.field.getType();
                try {
                    if (fieldType == String.class) {
                        fieldInfo.setValue(entryInstance, newValue);
                    } else if (fieldType == int.class || fieldType == Integer.class) {
                        fieldInfo.setValue(entryInstance, Integer.parseInt(newValue));
                    } else if (fieldType == double.class || fieldType == Double.class) {
                        fieldInfo.setValue(entryInstance, Double.parseDouble(newValue));
                    } else if (fieldType == float.class || fieldType == Float.class) {
                        fieldInfo.setValue(entryInstance, Float.parseFloat(newValue));
                    }
                } catch (NumberFormatException e) {
                    // Invalid input, ignore
                }
            });
            return textField;
        }
    }

    private List<Object> filterEntries(List<Object> entries) {
        if (searchQuery.isEmpty()) return entries;

        List<Object> filtered = new ArrayList<>();
        for (Object entry : entries) {
            String name = "";
            if (entry instanceof ConfigRegistry.ConfigOptionEntry option) {
                name = option.name;
            } else if (entry instanceof ConfigRegistry.SubSettingsEntry subSettings) {
                name = subSettings.name;
            } else if (entry instanceof ConfigRegistry.ConfigKeybindEntry keybind) {
                name = keybind.name;
            } else if (entry instanceof ConfigRegistry.ConfigListEntry listEntry) {
                name = listEntry.name;
            }

            if (name.toLowerCase().contains(searchQuery)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    private WidgetType inferWidgetType(ConfigRegistry.ConfigOptionEntry option) {
        Class<?> fieldType = option.field.getType();
        if (fieldType == boolean.class || fieldType == Boolean.class) {
            return WidgetType.TOGGLE;
        } else if (fieldType == int.class || fieldType == Integer.class ||
                   fieldType == float.class || fieldType == Float.class ||
                   fieldType == double.class || fieldType == Double.class) {
            return WidgetType.SLIDER;
        } else if (fieldType == String.class) {
            return WidgetType.TEXT_FIELD;
        }
        return WidgetType.TOGGLE;
    }

    private boolean hasEnabledField(Object obj) {
        try {
            obj.getClass().getDeclaredField("enabled");
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    private void toggleEnabled(Object obj) {
        try {
            var field = obj.getClass().getDeclaredField("enabled");
            field.setAccessible(true);
            field.setBoolean(obj, !field.getBoolean(obj));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getKeyName(int keyCode) {
        if (keyCode == -1) return "Not Set";
        if (keyCode <= -100) return "MOUSE " + (-(keyCode + 100) + 1);

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

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // Override to prevent blur effect - we render our own custom background
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        var theme = stagedConfig.guiTheme;

        // Background
        context.fill(0, 0, this.width, this.height, theme.parseColor(theme.backgroundColor));

        // Header
        renderHeader(context, theme);

        // Tab bar background
        context.fill(0, HEADER_HEIGHT, this.width, HEADER_HEIGHT + TAB_BAR_HEIGHT,
                    theme.parseColor(theme.surfaceElevatedColor));

        // Content area with scrolling
        renderContent(context, theme, mouseX, mouseY);

        // Footer
        renderFooter(context, theme);

        // Render widgets with scissor clipping for scrollable content
        int contentStartY = HEADER_HEIGHT + TAB_BAR_HEIGHT;
        int contentEndY = this.height - FOOTER_HEIGHT;

        // Enable scissor for content widgets
        context.enableScissor(0, contentStartY, this.width, contentEndY);

        // Render content widgets (including closed dropdowns) with scissor clipping
        for (WidgetEntry entry : contentWidgets) {
            if (entry.widget != null) {
                if (entry.widget instanceof DropdownWidget dd && dd.isOpen()) continue;
                entry.widget.render(context, mouseX, mouseY, delta);
            }
        }

        context.disableScissor();

        // Render non-content widgets (search, tabs, footer buttons) without scissor
        for (var child : this.children()) {
            if (child instanceof AbstractWidget widget) {
                boolean isContentWidget = false;
                for (WidgetEntry entry : contentWidgets) {
                    if (entry.widget == widget) {
                        isContentWidget = true;
                        break;
                    }
                }

                if (!isContentWidget) {
                    widget.render(context, mouseX, mouseY, delta);
                }
            }
        }

        // Render only OPEN dropdowns last so their menu appears on top of everything
        for (DropdownWidget dropdown : allDropdowns) {
            if (dropdown != null && dropdown.isOpen()) {
                dropdown.render(context, mouseX, mouseY, delta);
            }
        }
    }

    private void renderHeader(GuiGraphics context, Config.GuiTheme theme) {
        context.fill(0, 0, this.width, HEADER_HEIGHT, theme.parseColor(theme.surfaceColor));

        // Title
        Component title = Component.literal("Configuration").withStyle(ChatFormatting.BOLD);
        int titleX = this.width / 2 - this.font.width(title) / 2;
        context.drawString(this.font, title, titleX, 20, theme.parseColor(theme.textPrimaryColor), false);

        // Subtitle
        Component subtitle = Component.literal("Sux Addons Settings");
        int subtitleX = this.width / 2 - this.font.width(subtitle) / 2;
        context.drawString(this.font, subtitle, subtitleX, 35, theme.parseColor(theme.textSecondaryColor), false);
    }

    private void renderContent(GuiGraphics context, Config.GuiTheme theme, int mouseX, int mouseY) {
        int contentStartY = HEADER_HEIGHT + TAB_BAR_HEIGHT;
        int contentEndY = this.height - FOOTER_HEIGHT;
        int contentHeight = contentEndY - contentStartY;

        // Enable scissor for clipping - parameters are (x1, y1, x2, y2) where x2/y2 are bottom-right coordinates
        context.enableScissor(0, contentStartY, this.width, contentEndY);

        ConfigRegistry registry = stagedRegistry;
        ConfigRegistry.CategoryEntry category = registry.getCategoryByName(currentCategory);

        if (category != null) {
            renderCategoryContent(context, theme, category, contentStartY);
        }

        context.disableScissor();

        // Render scrollbar if needed
        if (maxScrollOffset > 0) {
            renderScrollbar(context, theme, contentStartY, contentHeight);
        }
    }


    private void renderCategoryContent(GuiGraphics context, Config.GuiTheme theme, ConfigRegistry.CategoryEntry category, int contentStartY) {
        List<Object> entries = filterEntries(category.getAllEntries());

        int x = this.width / 2 - CARD_WIDTH / 2;
        int y = contentStartY + CONTENT_PADDING - scrollOffset;

        if (entries.isEmpty() && !searchQuery.isEmpty()) {
            // No results card
            context.fill(x, y, x + CARD_WIDTH, y + 60, theme.parseColor(theme.surfaceElevatedColor));
            Component noResults = Component.literal("No settings found matching: \"" + searchQuery + "\"");
            int textX = x + CARD_WIDTH / 2 - this.font.width(noResults) / 2;
            context.drawString(this.font, noResults, textX, y + 22, theme.parseColor(theme.textSecondaryColor), false);
            return;
        }

        // Special rendering for GUI Theme category - show preset controls card at top
        if (category.name.equals("GUI Theme") && searchQuery.isEmpty()) {
            context.fill(x, y, x + CARD_WIDTH, y + 80, theme.parseColor(theme.surfaceElevatedColor));

            // Draw "Theme Presets" label
            Component presetLabel = Component.literal("Theme Presets");
            context.drawString(this.font, presetLabel, x + CARD_PADDING, y + CARD_PADDING - 5,
                theme.parseColor(theme.textSecondaryColor), false);

            y += 80 + CARD_SPACING;
        }

        for (Object entry : entries) {
            String entryName = "";
            boolean shouldRender = true;
            boolean isListEntry = false;

            if (entry instanceof ConfigRegistry.ConfigOptionEntry option) {
                if (!option.isDependencySatisfied()) {
                    shouldRender = false;
                } else {
                    WidgetType type = option.widgetType == WidgetType.AUTO ? inferWidgetType(option) : option.widgetType;
                    if (type == null) {
                        shouldRender = false;
                    } else {
                        entryName = option.name;
                    }
                }
            } else if (entry instanceof ConfigRegistry.SubSettingsEntry subSettings) {
                entryName = subSettings.name;
            } else if (entry instanceof ConfigRegistry.ConfigKeybindEntry keybind) {
                if (!keybind.isDependencySatisfied()) {
                    shouldRender = false;
                } else {
                    entryName = keybind.name;
                }
            } else if (entry instanceof ConfigRegistry.ConfigListEntry listEntry) {
                // Render list entries separately
                y = renderListEntry(context, theme, listEntry, x, y);
                continue; // Skip normal rendering
            }

            if (shouldRender && !entryName.isEmpty()) {
                // Draw card background
                context.fill(x, y, x + CARD_WIDTH, y + 44, theme.parseColor(theme.surfaceElevatedColor));

                // Draw entry name (centered vertically)
                context.drawString(this.font, Component.literal(entryName),
                               x + CARD_PADDING, y + 18, // Centered: (44 - 9) / 2 ≈ 18
                               theme.parseColor(theme.textPrimaryColor), false);

                y += 44 + CARD_SPACING;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private int renderListEntry(GuiGraphics context, Config.GuiTheme theme, ConfigRegistry.ConfigListEntry listEntry, int x, int y) {
        int startY = y;
        int cardHeight = 44;
        boolean isHorizontal = listEntry.annotation.layout().equals("horizontal");

        // --- Container Background ---
        int listHeight = 0;
        listHeight += cardHeight + CARD_SPACING; // Title
        List<Object> list = (List<Object>) listEntry.getList();
        for (int i = 0; i < list.size(); i++) {
            listHeight += (isHorizontal ? 70 : (44 * listEntry.entryFields.size() + 40)) + CARD_SPACING;
        }
        int maxSize = listEntry.annotation.maxSize();
        boolean canAddMore = maxSize == -1 || list.size() < maxSize;
        if (canAddMore) {
            listHeight += cardHeight + CARD_SPACING;
        }
        context.fill(x - 2, startY - 2, x + CARD_WIDTH + 2, startY + listHeight + 2, theme.parseColor(theme.surfaceColor));


        // Title card
        context.fill(x, y, x + CARD_WIDTH, y + cardHeight, theme.parseColor(theme.surfaceElevatedColor));
        context.drawString(this.font, Component.literal(listEntry.name), x + CARD_PADDING, y + (cardHeight - 8) / 2, theme.parseColor(theme.textPrimaryColor), false);
        y += cardHeight + CARD_SPACING;

        // Each list entry card
        for (int i = 0; i < list.size(); i++) {
            Object entryInstance = list.get(i);
            int entryCardHeight = isHorizontal ? 70 : (44 * listEntry.entryFields.size() + 40);

            // Entry card background
            context.fill(x, y, x + CARD_WIDTH, y + entryCardHeight, theme.parseColor(theme.surfaceElevatedColor));

            // Entry title
            String entryTitle = getListEntryTitle(listEntry, entryInstance, i);
            context.drawString(this.font, Component.literal(entryTitle), x + CARD_PADDING, y + 10, theme.parseColor(theme.textPrimaryColor), false);

            // Field labels (if not horizontal)
            if (!isHorizontal) {
                int labelY = y + 40;
                for (ConfigRegistry.ConfigListEntry.EntryFieldInfo fieldInfo : listEntry.entryFields) {
                    context.drawString(this.font, Component.literal(fieldInfo.name), x + CARD_PADDING, labelY + (44 - 8) / 2, theme.parseColor(theme.textSecondaryColor), false);
                    labelY += 44;
                }
            }

            y += entryCardHeight + CARD_SPACING;
        }

        // Bottom Add button card
        if (canAddMore) {
            context.fill(x, y, x + CARD_WIDTH, y + cardHeight, theme.parseColor(theme.surfaceElevatedColor));
            y += cardHeight + CARD_SPACING;
        }

        return y;
    }

    private void renderScrollbar(GuiGraphics context, Config.GuiTheme theme, int startY, int height) {
        int scrollbarX = this.width - SCROLLBAR_WIDTH - 10;
        int scrollbarHeight = height - 20;

        // Scrollbar track
        context.fill(scrollbarX, startY + 10, scrollbarX + SCROLLBAR_WIDTH, startY + 10 + scrollbarHeight,
                    theme.parseColor(theme.surfaceElevatedColor));

        // Scrollbar thumb
        float scrollPercentage = maxScrollOffset > 0 ? (float) scrollOffset / maxScrollOffset : 0;
        int thumbHeight = Math.max(30, scrollbarHeight / 3);
        int thumbY = startY + 10 + (int) ((scrollbarHeight - thumbHeight) * scrollPercentage);

        context.fill(scrollbarX, thumbY, scrollbarX + SCROLLBAR_WIDTH, thumbY + thumbHeight,
                    theme.parseColor(theme.primaryColor));
    }

    private void renderFooter(GuiGraphics context, Config.GuiTheme theme) {
        int footerY = this.height - FOOTER_HEIGHT;
        context.fill(0, footerY, this.width, this.height, theme.parseColor(theme.surfaceColor));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean isDoubleClick) {
        if (click.button() >= 2) {
            if (activeKeybindButton != null && activeKeybindButton.isListening()) {
                boolean handled = activeKeybindButton.handleMouseButton(click);
                if (handled) {
                    activeKeybindButton = null;
                    return true;
                }
            }
            for (WidgetEntry entry : contentWidgets) {
                if (entry.widget instanceof KeybindButton keybindBtn && keybindBtn.isListening()) {
                    boolean handled = keybindBtn.handleMouseButton(click);
                    if (handled) {
                        return true;
                    }
                }
            }
        }

        // If ANY dropdown is open, give it priority for click handling
        for (DropdownWidget dropdown : allDropdowns) {
            if (dropdown != null && dropdown.isOpen()) {
                // Let the dropdown handle the click first
                boolean handled = dropdown.mouseClicked(click, isDoubleClick);
                if (handled) {
                    return true;
                }
                // If not handled, close the dropdown
                dropdown.close();
                return true;
            }
        }

        return super.mouseClicked(click, isDoubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Prioritize dropdown scrolling if ANY dropdown is open and the mouse is over it
        for (DropdownWidget dropdown : allDropdowns) {
            if (dropdown != null && dropdown.isMouseOverOpenArea(mouseX, mouseY)) {
                return dropdown.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
            }
        }

        // If not scrolling dropdown, scroll the main content
        if (maxScrollOffset > 0) {
            // Ensure we are not scrolling while hovering over header or footer
            int contentStartY = HEADER_HEIGHT + TAB_BAR_HEIGHT;
            int contentEndY = this.height - FOOTER_HEIGHT;
            if (mouseY >= contentStartY && mouseY < contentEndY) {
                scrollOffset = (int) Math.max(0, Math.min(maxScrollOffset, scrollOffset - verticalAmount * 20));
                updateWidgetPositions();
                return true;
            }
        }

        // Fallback to default behavior if no other scrolling is handled
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void updateWidgetPositions() {
        for (WidgetEntry entry : contentWidgets) {
            if (entry.widget != null) {
                // Update widget position based on card position and widget's offset from card top
                int newY = entry.baseY - scrollOffset + entry.widgetOffsetY;
                entry.widget.setY(newY);
            }
        }
    }

    @Override
    public boolean keyPressed(KeyEvent keyInput) {
        // Check if any keybind button is listening
        if (activeKeybindButton != null && activeKeybindButton.isListening()) {
            boolean handled = activeKeybindButton.handleKeyPress(keyInput);
            if (handled) {
                activeKeybindButton = null;
                return true;
            }
        }

        // Check all content widgets for listening keybind buttons
        for (WidgetEntry entry : contentWidgets) {
            if (entry.widget instanceof KeybindButton keybindBtn) {
                if (keybindBtn.isListening()) {
                    boolean handled = keybindBtn.handleKeyPress(keyInput);
                    if (handled) {
                        return true;
                    }
                }
            }
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

    private static class WidgetEntry {
        AbstractWidget widget;
        int x;
        int y; // Rendered Y position (card Y)
        int baseY; // Base Y before scroll offset (card Y)
        int width;
        int height;
        int widgetOffsetY; // Widget's Y offset from card top

        WidgetEntry(AbstractWidget widget, int x, int y, int width, int height) {
            this.widget = widget;
            this.x = x;
            this.y = y;
            this.baseY = y;
            this.width = width;
            this.height = height;
            // Store the widget's offset from card top
            this.widgetOffsetY = widget != null ? widget.getY() - y : 0;
        }
    }
}
