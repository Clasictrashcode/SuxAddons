package com.classictrashcode.suxaddons.client.config;

import com.classictrashcode.suxaddons.client.config.annotations.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ConfigRegistry {

    private final Config config;
    private final List<CategoryEntry> categories = new ArrayList<>();

    public ConfigRegistry(Config config) {
        this.config = config;
        scan();
    }

    private void scan() {
        // Scan all fields in Config class
        for (Field field : Config.class.getDeclaredFields()) {
            field.setAccessible(true);

            ConfigCategory categoryAnnotation = field.getAnnotation(ConfigCategory.class);
            if (categoryAnnotation != null) {
                try {
                    Object categoryObject = field.get(config);
                    CategoryEntry entry = new CategoryEntry(
                            categoryAnnotation.name(),
                            categoryAnnotation.order(),
                            categoryAnnotation.formatting(),
                            categoryAnnotation.separator(),
                            categoryObject,
                            field
                    );

                    // Scan fields within this category
                    scanCategoryFields(categoryObject, entry);

                    categories.add(entry);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        // Sort categories by order
        categories.sort(Comparator.comparingInt(c -> c.order));
    }

    private void scanCategoryFields(Object categoryObject, CategoryEntry category) {
        if (categoryObject == null) return;

        for (Field field : categoryObject.getClass().getDeclaredFields()) {
            field.setAccessible(true);

            // Check for ConfigOption
            ConfigOption optionAnnotation = field.getAnnotation(ConfigOption.class);
            if (optionAnnotation != null) {
                ConfigOptionEntry entry = new ConfigOptionEntry(
                        optionAnnotation.name(),
                        optionAnnotation.description(),
                        optionAnnotation.order(),
                        field,
                        categoryObject,
                        optionAnnotation.type(),
                        field.getAnnotation(ConfigRange.class),
                        field.getAnnotation(ConfigDependency.class)
                );
                category.options.add(entry);
            }

            // Check for ConfigSubSettings
            ConfigSubSettings subSettingsAnnotation = field.getAnnotation(ConfigSubSettings.class);
            if (subSettingsAnnotation != null) {
                try {
                    Object subSettingsObject = field.get(categoryObject);
                    SubSettingsEntry entry = new SubSettingsEntry(
                            subSettingsAnnotation.name(),
                            subSettingsAnnotation.description(),
                            subSettingsAnnotation.order(),
                            field,
                            categoryObject,
                            subSettingsObject
                    );
                    category.subSettings.add(entry);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

            // Check for ConfigKeybind
            ConfigKeybind keybindAnnotation = field.getAnnotation(ConfigKeybind.class);
            if (keybindAnnotation != null) {
                ConfigKeybindEntry entry = new ConfigKeybindEntry(
                        keybindAnnotation.name(),
                        keybindAnnotation.description(),
                        keybindAnnotation.order(),
                        field,
                        categoryObject,
                        field.getAnnotation(ConfigDependency.class)
                );
                category.keybinds.add(entry);
            }

            // Check for ConfigList
            ConfigList listAnnotation = field.getAnnotation(ConfigList.class);
            if (listAnnotation != null) {
                try {
                    List<?> listInstance = (List<?>) field.get(categoryObject);
                    ConfigListEntry entry = new ConfigListEntry(
                            listAnnotation.name(),
                            listAnnotation.description(),
                            listAnnotation.order(),
                            field,
                            categoryObject,
                            listInstance,
                            listAnnotation
                    );
                    category.lists.add(entry);
                } catch (IllegalAccessException | ClassCastException e) {
                    e.printStackTrace();
                }
            }
        }

        // Sort all entries by order
        category.options.sort(Comparator.comparingInt(e -> e.order));
        category.subSettings.sort(Comparator.comparingInt(e -> e.order));
        category.keybinds.sort(Comparator.comparingInt(e -> e.order));
        category.lists.sort(Comparator.comparingInt(e -> e.order));
    }

    public List<CategoryEntry> getCategories() {
        return categories;
    }

    public CategoryEntry getCategoryByName(String name) {
        return categories.stream()
                .filter(c -> c.name.equals(name))
                .findFirst()
                .orElse(null);
    }

    // ==================== ENTRY CLASSES ====================

    public static class CategoryEntry {
        public final String name;
        public final int order;
        public final String formatting;
        public final boolean separator;
        public final Object instance;
        public final Field field;
        public final List<ConfigOptionEntry> options = new ArrayList<>();
        public final List<SubSettingsEntry> subSettings = new ArrayList<>();
        public final List<ConfigKeybindEntry> keybinds = new ArrayList<>();
        public final List<ConfigListEntry> lists = new ArrayList<>();

        public CategoryEntry(String name, int order, String formatting, boolean separator, Object instance, Field field) {
            this.name = name;
            this.order = order;
            this.formatting = formatting;
            this.separator = separator;
            this.instance = instance;
            this.field = field;
        }

        public List<Object> getAllEntries() {
            List<Object> all = new ArrayList<>();
            all.addAll(options);
            all.addAll(subSettings);
            all.addAll(keybinds);
            all.addAll(lists);
            all.sort(Comparator.comparingInt(e -> {
                if (e instanceof ConfigOptionEntry) return ((ConfigOptionEntry) e).order;
                if (e instanceof SubSettingsEntry) return ((SubSettingsEntry) e).order;
                if (e instanceof ConfigKeybindEntry) return ((ConfigKeybindEntry) e).order;
                if (e instanceof ConfigListEntry) return ((ConfigListEntry) e).order;
                return 0;
            }));
            return all;
        }
    }

    public static class ConfigOptionEntry {
        public final String name;
        public final String description;
        public final int order;
        public final Field field;
        public final Object parent;
        public final WidgetType widgetType;
        public final ConfigRange range;
        public final ConfigDependency dependency;

        public ConfigOptionEntry(String name, String description, int order, Field field, Object parent,
                                 WidgetType widgetType, ConfigRange range, ConfigDependency dependency) {
            this.name = name;
            this.description = description;
            this.order = order;
            this.field = field;
            this.parent = parent;
            this.widgetType = widgetType;
            this.range = range;
            this.dependency = dependency;
        }

        public Object getValue() {
            try {
                return field.get(parent);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return null;
            }
        }

        public void setValue(Object value) {
            try {
                field.set(parent, value);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        public boolean isDependencySatisfied() {
            if (dependency == null) return true;

            try {
                Field depField = parent.getClass().getDeclaredField(dependency.field());
                depField.setAccessible(true);
                boolean depValue = depField.getBoolean(parent);
                return dependency.invert() ? !depValue : depValue;
            } catch (Exception e) {
                return true; // If we can't check, assume satisfied
            }
        }
    }

    public static class SubSettingsEntry {
        public final String name;
        public final String description;
        public final int order;
        public final Field field;
        public final Object parent;
        public final Object instance;

        public SubSettingsEntry(String name, String description, int order, Field field, Object parent, Object instance) {
            this.name = name;
            this.description = description;
            this.order = order;
            this.field = field;
            this.parent = parent;
            this.instance = instance;
        }

        public boolean isEnabled() {
            // Check if there's an "enabled" field in the sub-settings
            try {
                Field enabledField = instance.getClass().getDeclaredField("enabled");
                enabledField.setAccessible(true);
                return enabledField.getBoolean(instance);
            } catch (Exception e) {
                return false;
            }
        }
    }

    public static class ConfigKeybindEntry {
        public final String name;
        public final String description;
        public final int order;
        public final Field field;
        public final Object parent;
        public final ConfigDependency dependency;

        public ConfigKeybindEntry(String name, String description, int order, Field field, Object parent, ConfigDependency dependency) {
            this.name = name;
            this.description = description;
            this.order = order;
            this.field = field;
            this.parent = parent;
            this.dependency = dependency;
        }

        public int getValue() {
            try {
                return field.getInt(parent);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return -1;
            }
        }

        public void setValue(int value) {
            try {
                field.setInt(parent, value);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        public boolean isDependencySatisfied() {
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

    public static class ConfigListEntry {
        public final String name;
        public final String description;
        public final int order;
        public final Field field;
        public final Object parent;
        public final List<?> listInstance;
        public final ConfigList annotation;
        public final List<EntryFieldInfo> entryFields;

        public ConfigListEntry(String name, String description, int order, Field field, Object parent, List<?> listInstance, ConfigList annotation) {
            this.name = name;
            this.description = description;
            this.order = order;
            this.field = field;
            this.parent = parent;
            this.listInstance = listInstance;
            this.annotation = annotation;
            this.entryFields = scanEntryClass(annotation.entryClass());
        }

        /**
         * Scans the entry class for annotated fields
         */
        private List<EntryFieldInfo> scanEntryClass(Class<?> entryClass) {
            List<EntryFieldInfo> fields = new ArrayList<>();

            for (Field field : entryClass.getDeclaredFields()) {
                field.setAccessible(true);

                // Check for various annotations
                ConfigOption optionAnnotation = field.getAnnotation(ConfigOption.class);
                ConfigKeybind keybindAnnotation = field.getAnnotation(ConfigKeybind.class);
                ConfigRange rangeAnnotation = field.getAnnotation(ConfigRange.class);

                if (optionAnnotation != null || keybindAnnotation != null) {
                    String fieldName = optionAnnotation != null ? optionAnnotation.name() : keybindAnnotation.name();
                    int fieldOrder = optionAnnotation != null ? optionAnnotation.order() : keybindAnnotation.order();

                    EntryFieldInfo info = new EntryFieldInfo(
                            fieldName,
                            field,
                            keybindAnnotation != null,
                            rangeAnnotation,
                            fieldOrder
                    );
                    fields.add(info);
                }
            }

            // Sort by order
            fields.sort(Comparator.comparingInt(f -> f.order));
            return fields;
        }

        public List<?> getList() {
            try {
                return (List<?>) field.get(parent);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        }

        public void setList(List<?> list) {
            try {
                field.set(parent, list);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        /**
         * Information about a field in an entry class
         */
        public static class EntryFieldInfo {
            public final String name;
            public final Field field;
            public final boolean isKeybind;
            public final ConfigRange range;
            public final int order;

            public EntryFieldInfo(String name, Field field, boolean isKeybind, ConfigRange range, int order) {
                this.name = name;
                this.field = field;
                this.isKeybind = isKeybind;
                this.range = range;
                this.order = order;
            }

            public Object getValue(Object entryInstance) {
                try {
                    return field.get(entryInstance);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            public void setValue(Object entryInstance, Object value) {
                try {
                    field.set(entryInstance, value);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}