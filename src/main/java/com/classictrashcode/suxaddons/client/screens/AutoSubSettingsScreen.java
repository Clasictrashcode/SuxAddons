package com.classictrashcode.suxaddons.client.screens;

import com.classictrashcode.suxaddons.client.SoundAlert;
import com.classictrashcode.suxaddons.client.config.Config;
import com.classictrashcode.suxaddons.client.config.ConfigRegistry;
import com.classictrashcode.suxaddons.client.config.annotations.*;
import com.classictrashcode.suxaddons.client.screens.components.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class AutoSubSettingsScreen extends Screen {
    private final AutoConfigScreen parent;
    private final ConfigRegistry.SubSettingsEntry subSettings;
    private final Config.GuiTheme theme;
    private KeybindButton activeKeybindButton = null;
    private final List<WidgetEntry> contentWidgets = new ArrayList<>();
    private final List<DropdownWidget> allDropdowns = new ArrayList<>();

    // Scroll state
    private int scrollOffset = 0;
    private int maxScrollOffset = 0;

    // Layout constants
    private static final int HEADER_HEIGHT = 80;
    private static final int FOOTER_HEIGHT = 50;
    private static final int CONTENT_PADDING = 20;
    private static final int CARD_SPACING = 8;
    private static final int CARD_PADDING = 16;
    private static final int CARD_WIDTH = 600;
    private static final int SCROLLBAR_WIDTH = 6;

    public AutoSubSettingsScreen(Screen parent, ConfigRegistry.SubSettingsEntry subSettings, Config.GuiTheme theme) {
        super(Component.literal(subSettings.name));
        this.parent = (AutoConfigScreen) parent;
        this.subSettings = subSettings;
        this.theme = theme;
    }

    @Override
    protected void init() {
        // Clear previous state
        contentWidgets.clear();
        this.clearWidgets();
        scrollOffset = 0;

        // Create footer buttons
        int footerY = this.height - FOOTER_HEIGHT + 15;
        int centerX = this.width / 2;

        Button backBtn = new Button(centerX - 60, footerY, 120, 24, Component.literal("Done"), button -> {
            // Changes are already in parent's staged config, just close
            this.onClose();
        }, theme);
        this.addRenderableWidget(backBtn);

        // Build content
        refreshContent();
    }

    private void refreshContent() {
        // Remove old content widgets
        contentWidgets.forEach(entry -> {
            if (entry.widget != null) {
                this.removeWidget(entry.widget);
            }
        });
        contentWidgets.clear();
        allDropdowns.clear();
        scrollOffset = 0;

        List<FieldEntry> fields = scanFields(subSettings.instance);
        fields.sort(Comparator.comparingInt(f -> f.order));

        int contentStartY = HEADER_HEIGHT + CONTENT_PADDING;
        int contentX = this.width / 2 - CARD_WIDTH / 2;
        int y = contentStartY;

        for (FieldEntry fieldEntry : fields) {
            if (!fieldEntry.isDependencySatisfied()) {
                continue;
            }

            // Handle list entries
            if (fieldEntry.isListEntry()) {
                y = buildListContent(fieldEntry, contentX, y);
                continue;
            }

            int cardHeight = 44;
            boolean widgetCreated = false;

            if (fieldEntry.widgetType == WidgetType.TOGGLE || fieldEntry.widgetType == WidgetType.AUTO) {
                WidgetType type = fieldEntry.widgetType == WidgetType.AUTO ?
                    inferWidgetType(fieldEntry.field) : fieldEntry.widgetType;

                // Skip unsupported types (null means skip)
                if (type == null) continue;

                if (type == WidgetType.TOGGLE) {
                    ToggleButton widget = createToggleButton(fieldEntry, contentX, y);
                    if (widget != null) {
                        addContentWidget(widget, contentX, y, CARD_WIDTH, cardHeight);
                        widgetCreated = true;
                    }
                } else if (type == WidgetType.SLIDER) {
                    SnappingSliderWidget widget = createSlider(fieldEntry, contentX, y);
                    if (widget != null) {
                        addContentWidget(widget, contentX, y, CARD_WIDTH, cardHeight);
                        widgetCreated = true;
                    }
                } else if (type == WidgetType.TEXT_FIELD || type == WidgetType.COLOR) {
                    AbstractWidget widget = createTextField(fieldEntry, contentX, y);
                    if (widget != null) {
                        addContentWidget(widget, contentX, y, CARD_WIDTH, cardHeight);
                        widgetCreated = true;
                    }
                } else if (type == WidgetType.DROPDOWN) {
                    DropdownWidget widget = createDropdown(fieldEntry, contentX, y);
                    if (widget != null) {
                        addContentWidget(widget, contentX, y, CARD_WIDTH, cardHeight);
                        allDropdowns.add(widget);
                        widgetCreated = true;
                    }
                }
            } else if (fieldEntry.widgetType == WidgetType.SLIDER) {
                SnappingSliderWidget widget = createSlider(fieldEntry, contentX, y);
                if (widget != null) {
                    addContentWidget(widget, contentX, y, CARD_WIDTH, cardHeight);
                    widgetCreated = true;
                }
            } else if (fieldEntry.widgetType == WidgetType.TEXT_FIELD || fieldEntry.widgetType == WidgetType.COLOR) {
                AbstractWidget widget = createTextField(fieldEntry, contentX, y);
                if (widget != null) {
                    addContentWidget(widget, contentX, y, CARD_WIDTH, cardHeight);
                    widgetCreated = true;
                }
            } else if (fieldEntry.widgetType == WidgetType.DROPDOWN) {
                DropdownWidget widget = createDropdown(fieldEntry, contentX, y);
                if (widget != null) {
                    addContentWidget(widget, contentX, y, CARD_WIDTH, cardHeight);
                    allDropdowns.add(widget);
                    widgetCreated = true;
                }
            } else if (fieldEntry.widgetType == WidgetType.KEYBIND) {
                KeybindButton widget = createKeybindButton(fieldEntry, contentX, y);
                if (widget != null) {
                    addContentWidget(widget, contentX, y, CARD_WIDTH, cardHeight);
                    widgetCreated = true;
                }
            }

            if (widgetCreated) {
                y += cardHeight + CARD_SPACING;
            }
        }

        // Calculate max scroll offset
        int contentEndY = contentWidgets.isEmpty() ? y :
            contentWidgets.getLast().y + contentWidgets.getLast().height;
        int availableHeight = this.height - contentStartY - FOOTER_HEIGHT - CONTENT_PADDING;
        maxScrollOffset = Math.max(0, contentEndY - contentStartY - availableHeight);
    }

    private ToggleButton createToggleButton(FieldEntry fieldEntry, int cardX, int cardY) {
        try {
            boolean value = fieldEntry.field.getBoolean(fieldEntry.parent);

            int btnX = cardX + CARD_WIDTH - 48 - CARD_PADDING;
            int btnY = cardY + 13; // Centered: (44 - 18) / 2 = 13

            ToggleButton toggle = new ToggleButton(btnX, btnY, value, newValue -> {
                try {
                    fieldEntry.field.setBoolean(fieldEntry.parent, newValue);
                    // Don't save yet - changes are staged in parent screen
                    refreshContent(); // Refresh for dependencies
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }, theme);

            return toggle;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    private SnappingSliderWidget createSlider(FieldEntry fieldEntry, int cardX, int cardY) {
        try {
            final double min = fieldEntry.range != null ? fieldEntry.range.min() : 0;
            final double max = fieldEntry.range != null ? fieldEntry.range.max() : 100;
            final double step = fieldEntry.range != null ? fieldEntry.range.step() : 1;
            double value = Double.parseDouble(String.valueOf(fieldEntry.field.get(fieldEntry.parent)));

            int sliderX = cardX + CARD_WIDTH - 200 - 60 - CARD_PADDING;
            int sliderY = cardY + 10; // Centered: (44 - 24) / 2 = 10

            SnappingSliderWidget slider = new SnappingSliderWidget(sliderX, sliderY, 200, 24, min, max, step, value, theme);
            slider.setOnChange(snappedValue -> {
                try {
                    if (fieldEntry.field.getType() == int.class) {
                        fieldEntry.field.setInt(fieldEntry.parent, (int) Math.round(snappedValue));
                    } else if (fieldEntry.field.getType() == float.class) {
                        fieldEntry.field.setFloat(fieldEntry.parent, snappedValue.floatValue());
                    } else if (fieldEntry.field.getType() == double.class) {
                        fieldEntry.field.setDouble(fieldEntry.parent, snappedValue);
                    }
                    // Don't save yet - changes are staged in parent screen
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            });

            // Set dropdown open check to hide slider text when dropdown is open
            slider.setDropdownOpenCheck(this::hasOpenDropdown);

            return slider;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
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

    private KeybindButton createKeybindButton(FieldEntry fieldEntry, int cardX, int cardY) {
        try {
            int keyCode = fieldEntry.field.getInt(fieldEntry.parent);

            int btnX = cardX + CARD_WIDTH - 120 - CARD_PADDING;
            int btnY = cardY + 10; // Centered: (44 - 24) / 2 = 10

            KeybindButton button = new KeybindButton(btnX, btnY, 120, 24, keyCode, newKeyCode -> {
                try {
                    fieldEntry.field.setInt(fieldEntry.parent, newKeyCode);
                    // Don't save yet - changes are staged in parent screen
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }, theme) {
                @Override
                public void onClick(MouseButtonEvent click, boolean isDoubleClick) {
                    super.onClick(click,isDoubleClick);
                    // Track this button when it starts listening
                    if (this.isListening()) {
                        activeKeybindButton = this;
                    }
                }
            };

            return button;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    private AbstractWidget createTextField(FieldEntry fieldEntry, int cardX, int cardY) {
        try {
            // Type safety check - only create text field for String fields
            if (fieldEntry.field.getType() != String.class) {
                return null;
            }

            String value = (String) fieldEntry.field.get(fieldEntry.parent);
            if (value == null) {
                value = "";
            }

            // Check if this is a color field (use COLOR widget type or name contains "color")
            boolean isColorField = fieldEntry.widgetType == WidgetType.COLOR || fieldEntry.name.toLowerCase().contains("color");

            int textFieldX = cardX + CARD_WIDTH - 200 - 60 - CARD_PADDING;
            int textFieldY = cardY + 12; // Centered: (44 - 20) / 2 = 12
            int textFieldWidth = 200;

            if (isColorField) {
                ColorTextField textField = new ColorTextField(
                    this.font, textFieldX, textFieldY, textFieldWidth, 20,
                    value, true, theme
                );

                textField.setOnChange(newValue -> {
                    try {
                        fieldEntry.field.set(fieldEntry.parent, newValue);
                        // Don't save yet - changes are staged in parent screen
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                });

                return textField;
            } else {
                TextField textField = new TextField(this.font, textFieldX, textFieldY, textFieldWidth, 20, value, theme);

                textField.setOnChange(newValue -> {
                    try {
                        fieldEntry.field.set(fieldEntry.parent, newValue);
                        // Don't save yet - changes are staged in parent screen
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                });

                return textField;
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    private DropdownWidget createDropdown(FieldEntry fieldEntry, int cardX, int cardY) {
        try {
            // Type safety check - only create dropdown for String fields marked as DROPDOWN
            if (fieldEntry.field.getType() != String.class) {
                return null;
            }

            String currentValue = (String) fieldEntry.field.get(fieldEntry.parent);
            if (currentValue == null) {
                currentValue = "";
            }

            // Get dropdown options - for now, we'll specifically handle SoundAlert
            List<String> options = new ArrayList<>();

            // Check if this is a sound type field by checking field name
            if (fieldEntry.name.toLowerCase().contains("sound") && fieldEntry.name.toLowerCase().contains("type")) {
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
                currentValue.isEmpty() ? options.getFirst() : currentValue,
                selectedValue -> {
                    try {
                        fieldEntry.field.set(fieldEntry.parent, selectedValue);
                        // Don't save yet - changes are staged in parent screen
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                },
                theme
            );

            return dropdown;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Build UI for a ConfigList entry (title + add buttons + list entries)
     */
    @SuppressWarnings("unchecked")
    private int buildListContent(FieldEntry fieldEntry, int x, int y) {
        try {
            ConfigList annotation = fieldEntry.listAnnotation;
            List<Object> list = (List<Object>) fieldEntry.field.get(fieldEntry.parent);

            int cardHeight = 44;
            boolean isHorizontal = annotation.layout().equals("horizontal");
            List<ConfigRegistry.ConfigListEntry.EntryFieldInfo> entryFields = scanEntryClass(annotation.entryClass());

            // Title card
            contentWidgets.add(new WidgetEntry(null, x, y, CARD_WIDTH, cardHeight));
            y += cardHeight + CARD_SPACING;

            // Render each list entry
            int maxSize = annotation.maxSize();

            for (int i = 0; i < list.size(); i++) {
                final int index = i;
                Object entryInstance = list.get(i);

                int entryCardHeight = isHorizontal ? 70 : (44 * entryFields.size() + 40);
                contentWidgets.add(new WidgetEntry(null, x, y, CARD_WIDTH, entryCardHeight));

                // --- Action Buttons ---
                int actionButtonY = y + 10;
                int currentX = x + CARD_WIDTH - CARD_PADDING;

                currentX -= 60;
                Button deleteBtn = new Button(currentX, actionButtonY, 60, 24, Component.literal("Delete"), btn -> {
                    list.remove(index);
                    refreshContent();
                }, theme);
                addContentWidget(deleteBtn, x, y, CARD_WIDTH, entryCardHeight);

                if (index < list.size() - 1) {
                    currentX -= 34;
                    Button downBtn = new Button(currentX, actionButtonY, 30, 24, Component.literal("▼"), btn -> {
                        Object temp = list.get(index);
                        list.set(index, list.get(index + 1));
                        list.set(index + 1, temp);
                        refreshContent();
                    }, theme);
                    addContentWidget(downBtn, x, y, CARD_WIDTH, entryCardHeight);
                }

                if (index > 0) {
                    currentX -= 34;
                    Button upBtn = new Button(currentX, actionButtonY, 30, 24, Component.literal("▲"), btn -> {
                        Object temp = list.get(index);
                        list.set(index, list.get(index - 1));
                        list.set(index - 1, temp);
                        refreshContent();
                    }, theme);
                    addContentWidget(upBtn, x, y, CARD_WIDTH, entryCardHeight);
                }

                // --- Fields ---
                int fieldY = y + 40;
                if (isHorizontal) {
                    int totalFieldWidth = entryFields.stream().mapToInt(f -> (f.isKeybind ? 120 : 150)).sum() + Math.max(0, entryFields.size() - 1) * 8;
                    int fieldX = x + CARD_PADDING + (CARD_WIDTH - 2 * CARD_PADDING - totalFieldWidth) / 2;

                    for (ConfigRegistry.ConfigListEntry.EntryFieldInfo fieldInfo : entryFields) {
                        int widgetHeight = fieldInfo.isKeybind ? 24 : 20;
                        AbstractWidget fieldWidget = createListEntryFieldWidget(fieldInfo, entryInstance, fieldX, y + (entryCardHeight - widgetHeight) / 2);
                        addContentWidget(fieldWidget, x, y, CARD_WIDTH, entryCardHeight);
                        fieldX += (fieldInfo.isKeybind ? 120 : 150) + 8;
                    }
                } else {
                    for (ConfigRegistry.ConfigListEntry.EntryFieldInfo fieldInfo : entryFields) {
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
                    addListEntry(fieldEntry);
                    refreshContent();
                }, theme);
                addContentWidget(bottomAddBtn, x, y, CARD_WIDTH, cardHeight);
                y += cardHeight + CARD_SPACING;
            }

            return y;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return y;
        }
    }

    @SuppressWarnings("unchecked")
    private int renderListEntry(GuiGraphics context, Config.GuiTheme theme, FieldEntry fieldEntry, int x, int y) {
        try {
            ConfigList annotation = fieldEntry.listAnnotation;
            List<Object> list = (List<Object>) fieldEntry.field.get(fieldEntry.parent);

            int cardHeight = 44;
            boolean isHorizontal = annotation.layout().equals("horizontal");
            List<ConfigRegistry.ConfigListEntry.EntryFieldInfo> entryFields = scanEntryClass(annotation.entryClass());

            // --- Container Background ---
            int listHeight = 0;
            listHeight += cardHeight + CARD_SPACING; // Title
            for (int i = 0; i < list.size(); i++) {
                listHeight += (isHorizontal ? 70 : (44 * entryFields.size() + 40)) + CARD_SPACING;
            }
            int maxSize = annotation.maxSize();
            boolean canAddMore = maxSize == -1 || list.size() < maxSize;
            if (canAddMore) {
                listHeight += cardHeight + CARD_SPACING;
            }
            context.fill(x - 2, y - 2, x + CARD_WIDTH + 2, y + listHeight + 2, theme.parseColor(theme.surfaceColor));

            // Title card
            context.fill(x, y, x + CARD_WIDTH, y + cardHeight, theme.parseColor(theme.surfaceElevatedColor));
            context.drawString(this.font, Component.literal(fieldEntry.name),
                           x + CARD_PADDING, y + (cardHeight - 8) / 2, theme.parseColor(theme.textPrimaryColor), false);
            y += cardHeight + CARD_SPACING;

            // Each list entry card
            for (int i = 0; i < list.size(); i++) {
                Object entryInstance = list.get(i);
                int entryCardHeight = isHorizontal ? 70 : (44 * entryFields.size() + 40);

                // Entry card background
                context.fill(x, y, x + CARD_WIDTH, y + entryCardHeight, theme.parseColor(theme.surfaceElevatedColor));

                // Entry title
                String entryTitle = getListEntryTitle(annotation, entryInstance, i, entryFields);
                context.drawString(this.font, Component.literal(entryTitle),
                               x + CARD_PADDING, y + 10, theme.parseColor(theme.textPrimaryColor), false);

                // Field labels (if not horizontal)
                if (!isHorizontal) {
                    int labelY = y + 40;
                    for (ConfigRegistry.ConfigListEntry.EntryFieldInfo fieldInfo : entryFields) {
                        context.drawString(this.font, Component.literal(fieldInfo.name),
                                       x + CARD_PADDING, labelY + (44 - 8) / 2, theme.parseColor(theme.textSecondaryColor), false);
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
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return y;
        }
    }

    private List<ConfigRegistry.ConfigListEntry.EntryFieldInfo> scanEntryClass(Class<?> entryClass) {
        List<ConfigRegistry.ConfigListEntry.EntryFieldInfo> fields = new ArrayList<>();

        for (Field field : entryClass.getDeclaredFields()) {
            field.setAccessible(true);

            ConfigOption optionAnnotation = field.getAnnotation(ConfigOption.class);
            ConfigKeybind keybindAnnotation = field.getAnnotation(ConfigKeybind.class);
            ConfigRange rangeAnnotation = field.getAnnotation(ConfigRange.class);

            if (optionAnnotation != null || keybindAnnotation != null) {
                String fieldName = optionAnnotation != null ? optionAnnotation.name() : keybindAnnotation.name();
                int fieldOrder = optionAnnotation != null ? optionAnnotation.order() : keybindAnnotation.order();

                ConfigRegistry.ConfigListEntry.EntryFieldInfo info = new ConfigRegistry.ConfigListEntry.EntryFieldInfo(
                        fieldName,
                        field,
                        keybindAnnotation != null,
                        rangeAnnotation,
                        fieldOrder
                );
                fields.add(info);
            }
        }

        fields.sort(Comparator.comparingInt(f -> f.order));
        return fields;
    }

    private String getListEntryTitle(ConfigList annotation, Object entryInstance, int index, List<ConfigRegistry.ConfigListEntry.EntryFieldInfo> entryFields) {
        String titleField = annotation.entryTitleField();

        if (!titleField.isEmpty()) {
            for (ConfigRegistry.ConfigListEntry.EntryFieldInfo fieldInfo : entryFields) {
                if (fieldInfo.field.getName().equals(titleField)) {
                    Object value = fieldInfo.getValue(entryInstance);
                    if (value != null) {
                        return value.toString();
                    }
                }
            }
        }

        String prefix = annotation.titlePrefix();
        return prefix + " #" + (index + 1);
    }

    private void addListEntry(FieldEntry fieldEntry) {
        try {
            Class<?> entryClass = fieldEntry.listAnnotation.entryClass();
            Object newEntry = entryClass.getDeclaredConstructor().newInstance();
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) fieldEntry.field.get(fieldEntry.parent);
            list.add(newEntry);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private AbstractWidget createListEntryFieldWidget(ConfigRegistry.ConfigListEntry.EntryFieldInfo fieldInfo,
                                                        Object entryInstance, int x, int y) {
        if (fieldInfo.isKeybind) {
            int keyCode = (int) fieldInfo.getValue(entryInstance);
            KeybindButton btn = new KeybindButton(x, y, 120, 24, keyCode, newKeyCode -> {
                fieldInfo.setValue(entryInstance, newKeyCode);
            }, theme) {
                @Override
                public void onClick(MouseButtonEvent click, boolean isDoubleClick) {
                    super.onClick(click,isDoubleClick);
                    if (this.isListening()) {
                        activeKeybindButton = this;
                    }
                }
            };
            return btn;
        } else {
            Object value = fieldInfo.getValue(entryInstance);
            String strValue = value != null ? value.toString() : "";

            TextField textField = new TextField(this.font, x, y, 150, 20, strValue, theme);
            textField.setOnChange(newValue -> {
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

    private void addContentWidget(AbstractWidget widget, int x, int y, int width, int height) {
        if (widget != null) {
            contentWidgets.add(new WidgetEntry(widget, x, y, width, height));
            this.addRenderableWidget(widget);
        }
    }

    private List<FieldEntry> scanFields(Object obj) {
        List<FieldEntry> fields = new ArrayList<>();
        Class<?> clazz = obj.getClass();
        Field[] declared = clazz.getDeclaredFields();

        for (Field field : declared) {
            field.setAccessible(true);

            ConfigOption option = field.getAnnotation(ConfigOption.class);
            if (option != null) {
                fields.add(new FieldEntry(
                    field, obj, option.name(), option.description(),
                    option.order(), option.type(),
                    field.getAnnotation(ConfigRange.class),
                    field.getAnnotation(ConfigDependency.class),
                    null // No ConfigList annotation
                ));
                continue;
            }

            ConfigKeybind keybind = field.getAnnotation(ConfigKeybind.class);
            if (keybind != null) {
                fields.add(new FieldEntry(
                    field, obj, keybind.name(), keybind.description(),
                    keybind.order(), WidgetType.KEYBIND, null,
                    field.getAnnotation(ConfigDependency.class),
                    null // No ConfigList annotation
                ));
                continue;
            }

            ConfigList listAnnotation = field.getAnnotation(ConfigList.class);
            if (listAnnotation != null) {
                fields.add(new FieldEntry(
                    field, obj, listAnnotation.name(), listAnnotation.description(),
                    listAnnotation.order(), WidgetType.AUTO, null, null,
                    listAnnotation // Pass the ConfigList annotation
                ));
            }
        }

        return fields;
    }

    private WidgetType inferWidgetType(Field field) {
        Class<?> type = field.getType();
        if (type == boolean.class || type == Boolean.class) {
            return WidgetType.TOGGLE;
        } else if (type == int.class || type == Integer.class ||
                   type == float.class || type == Double.class ||
                   type == double.class || type == Double.class) {
            return WidgetType.SLIDER;
        } else if (type == String.class) {
            return WidgetType.TEXT_FIELD;
        }
        return WidgetType.TOGGLE;
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
        var theme = this.theme;

        // Background
        context.fill(0, 0, this.width, this.height, theme.parseColor(theme.backgroundColor));

        // Header
        renderHeader(context, theme);

        // Content area with scrolling
        renderContent(context, theme);

        // Footer
        renderFooter(context, theme);

        // Render widgets with scissor clipping for scrollable content
        int contentStartY = HEADER_HEIGHT;
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

        // Render non-content widgets (footer buttons) without scissor
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
        context.fill(0, 0, this.width, HEADER_HEIGHT, theme.parseColor(theme.surfaceElevatedColor));

        // Title
        Component title = Component.literal(subSettings.name).withStyle(ChatFormatting.BOLD);
        int titleX = this.width / 2 - this.font.width(title) / 2;
        context.drawString(this.font, title, titleX, 25, theme.parseColor(theme.textPrimaryColor), false);

        // Description if present
        if (!subSettings.description.isEmpty()) {
            Component description = Component.literal(subSettings.description);
            int descX = this.width / 2 - this.font.width(description) / 2;
            context.drawString(this.font, description, descX, 45, theme.parseColor(theme.textSecondaryColor), false);
        }
    }

    private void renderContent(GuiGraphics context, Config.GuiTheme theme) {
        int contentStartY = HEADER_HEIGHT;
        int contentEndY = this.height - FOOTER_HEIGHT;
        int contentHeight = contentEndY - contentStartY;

        // Enable scissor for clipping
        context.enableScissor(0, contentStartY, this.width, contentEndY);

        // Render field names and cards properly
        List<FieldEntry> fields = scanFields(subSettings.instance);
        fields.sort(Comparator.comparingInt(f -> f.order));

        int x = this.width / 2 - CARD_WIDTH / 2;
        int y = contentStartY + CONTENT_PADDING - scrollOffset;

        for (FieldEntry fieldEntry : fields) {
            if (!fieldEntry.isDependencySatisfied()) {
                continue;
            }

            // Handle list entries
            if (fieldEntry.isListEntry()) {
                y = renderListEntry(context, theme, fieldEntry, x, y);
                continue;
            }

            // Check widget type
            WidgetType type = fieldEntry.widgetType == WidgetType.AUTO ?
                inferWidgetType(fieldEntry.field) : fieldEntry.widgetType;
            if (type == null && fieldEntry.widgetType != WidgetType.KEYBIND) {
                continue;
            }

            // Draw card background
            context.fill(x, y, x + CARD_WIDTH, y + 44, theme.parseColor(theme.surfaceElevatedColor));

            // Draw field name
            context.drawString(this.font, Component.literal(fieldEntry.name),
                           x + CARD_PADDING, y + CARD_PADDING + 6,
                           theme.parseColor(theme.textPrimaryColor), false);

            y += 44 + CARD_SPACING;
        }

        context.disableScissor();

        // Render scrollbar if needed
        if (maxScrollOffset > 0) {
            renderScrollbar(context, theme, contentStartY, contentHeight);
        }
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

        if (maxScrollOffset > 0) {
            scrollOffset = (int) Math.max(0, Math.min(maxScrollOffset, scrollOffset - verticalAmount * 20));
            updateWidgetPositions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void updateWidgetPositions() {
        int contentStartY = HEADER_HEIGHT + CONTENT_PADDING;

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

    private static class FieldEntry {
        Field field;
        Object parent;
        String name;
        String description;
        int order;
        WidgetType widgetType;
        ConfigRange range;
        ConfigDependency dependency;
        ConfigList listAnnotation;

        FieldEntry(Field field, Object parent, String name, String description,
                   int order, WidgetType widgetType, ConfigRange range, ConfigDependency dependency,
                   ConfigList listAnnotation) {
            this.field = field;
            this.parent = parent;
            this.name = name;
            this.description = description;
            this.order = order;
            this.widgetType = widgetType;
            this.range = range;
            this.dependency = dependency;
            this.listAnnotation = listAnnotation;
        }

        boolean isListEntry() {
            return listAnnotation != null;
        }

        boolean isDependencySatisfied() {
            if (dependency == null) return true;

            try {
                Field depField = parent.getClass().getDeclaredField(dependency.field());
                depField.setAccessible(true);
                boolean depValue = depField.getBoolean(parent);
                return dependency.invert() ? !depValue : depValue;
            } catch (Exception e) {
                return true;
            }
        }
    }

    private static class WidgetEntry {
        AbstractWidget widget;
        int x;
        int y;
        int baseY;
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