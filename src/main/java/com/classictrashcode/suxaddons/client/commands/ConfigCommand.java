package com.classictrashcode.suxaddons.client.commands;

import com.classictrashcode.suxaddons.client.Logger;
import com.classictrashcode.suxaddons.client.SuxaddonsClient;
import com.classictrashcode.suxaddons.client.config.Config;
import com.classictrashcode.suxaddons.client.config.ConfigManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.CompletableFuture;

public class ConfigCommand {
    public static final String NAME = "config";
    public static LiteralArgumentBuilder<FabricClientCommandSource> register(LiteralArgumentBuilder<FabricClientCommandSource> base) {
        return base.then(ClientCommandManager.literal(NAME)
                .then(ClientCommandManager.argument("path", StringArgumentType.greedyString())
                        .suggests(ConfigCommand::suggestConfigPaths)
                        .executes(ctx -> {
                            String fullPath = StringArgumentType.getString(ctx, "path");
                            return handleConfigCommand(ctx, fullPath);
                        })
                )
                .executes(ctx -> {
                    // No path provided, list all categories
                    Logger.inGameLog("Config","Available categories:");
                    for (Field field : Config.class.getDeclaredFields()) {
                        field.setAccessible(true);
                        if (!field.getType().isPrimitive() && !field.getType().equals(String.class)) {
                            Logger.inGameLog("  - " + field.getName());
                        }
                    }
                    Logger.inGameLog("Config",String.format("Usage: /%s config <category>.<field> [value]", SuxaddonsClient.MOD_ID));
                    return 1;
                }));
    }
    private static CompletableFuture<Suggestions> suggestConfigPaths(
            CommandContext<FabricClientCommandSource> ctx, SuggestionsBuilder builder) {
        String input = builder.getRemaining();

        // Check if user is typing a value (has space after path)
        int lastSpaceIndex = input.lastIndexOf(' ');
        if (lastSpaceIndex > 0) {
            // User is typing a value after the path
            String path = input.substring(0, lastSpaceIndex).trim();
            String partialValue = input.substring(lastSpaceIndex + 1);

            // Try to navigate to the field to check its type
            ConfigFieldResult result = navigateToField(path);
            if (result != null && result.field.getType() == boolean.class) {
                // Suggest true/false for boolean fields
                String lowerPartial = partialValue.toLowerCase();
                if ("true".startsWith(lowerPartial)) {
                    builder.suggest("true");
                }
                if ("false".startsWith(lowerPartial)) {
                    builder.suggest("false");
                }
            } else if (result != null && result.field.getType().isEnum()) {
                // Suggest enum constant names for enum fields
                String lowerPartial = partialValue.toLowerCase();
                for (Object constant : result.field.getType().getEnumConstants()) {
                    String name = ((Enum<?>) constant).name();
                    if (name.toLowerCase().startsWith(lowerPartial)) {
                        builder.suggest(name);
                    }
                }
            }
            return builder.buildFuture();
        }

        // Find the last complete path segment
        int lastDotIndex = input.lastIndexOf('.');
        String completePath = lastDotIndex >= 0 ? input.substring(0, lastDotIndex) : "";
        String partialSegment = lastDotIndex >= 0 ? input.substring(lastDotIndex + 1) : input;
        String partialLower = partialSegment.toLowerCase();

        // Navigate to the current level in the config structure
        Object current = ConfigManager.getConfig();
        Class<?> currentClass = Config.class;

        // Navigate through complete path segments
        if (!completePath.isEmpty()) {
            String[] segments = completePath.split("\\.");
            for (String segment : segments) {
                try {
                    Field field = findFieldIgnoreCase(currentClass, segment);
                    if (field == null) return builder.buildFuture();

                    field.setAccessible(true);
                    Object next = field.get(current);
                    if (next == null) return builder.buildFuture();

                    current = next;
                    currentClass = next.getClass();
                } catch (Exception e) {
                    return builder.buildFuture();
                }
            }
        }

        // Get all fields at current level and suggest matches
        Field[] fields = currentClass.getDeclaredFields();
        for (Field field : fields) {
            // Skip transient and static fields
            if (Modifier.isTransient(field.getModifiers()) ||
                    Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            String fieldName = field.getName();

            // Check if this field matches the partial input
            if (fieldName.toLowerCase().startsWith(partialLower)) {
                String fullSuggestion = completePath.isEmpty() ?
                        fieldName :
                        completePath + "." + fieldName;

                // Add dot if this is a non-primitive field (has sub-fields)
                Class<?> fieldType = field.getType();
                if (!fieldType.isPrimitive() && !fieldType.equals(String.class)) {
                    builder.suggest(fullSuggestion + ".");
                } else {
                    builder.suggest(fullSuggestion);
                }
            }
        }

        return builder.buildFuture();
    }
    private static int handleConfigCommand(CommandContext<FabricClientCommandSource> ctx, String fullPath) {
        // Split the path and optional value
        String[] parts = fullPath.split("\\s+", 2);
        String path = parts[0];
        String valueArg = parts.length > 1 ? parts[1] : null;

        // Navigate to the field
        ConfigFieldResult result = navigateToField(path);

        if (result == null) {
            Logger.inGameLog("Config", "Invalid config path: " + path);
            return 0;
        }

        // If no value provided and field is an object (not primitive), list sub-fields
        if (valueArg == null && !result.field.getType().isPrimitive() &&
                !result.field.getType().equals(String.class)) {
            listSubFields(ctx, path, result);
            return 1;
        }

        // Handle the field based on its type
        return handleFieldOperation(ctx, path, result, valueArg);
    }
    private static int handleFieldOperation(CommandContext<FabricClientCommandSource> ctx, String path,
                                            ConfigFieldResult result, String valueArg) {
        Class<?> type = result.field.getType();

        try {
            if (type == boolean.class) {
                // Boolean toggle or explicit set
                boolean currentValue = result.field.getBoolean(result.parent);
                boolean newValue;

                if (valueArg == null) {
                    // Toggle
                    newValue = !currentValue;
                } else {
                    // Explicit set
                    newValue = Boolean.parseBoolean(valueArg);
                }

                result.field.setBoolean(result.parent, newValue);
                ConfigManager.save();
                Logger.inGameLog("Config", path + " set to " + newValue);
                return 1;

            } else if (type == int.class) {
                if (valueArg == null) {
                    // Just show current value
                    int currentValue = result.field.getInt(result.parent);
                    Logger.inGameLog("Config", path + " = " + currentValue);
                    Logger.inGameLog("Config",String.format("Usage: /%s config %s <value>",SuxaddonsClient.MOD_ID,path) );
                    return 1;
                }

                try {
                    int newValue = Integer.parseInt(valueArg);
                    result.field.setInt(result.parent, newValue);
                    ConfigManager.save();
                    Logger.inGameLog("Config", path + " set to " + newValue);
                    return 1;
                } catch (NumberFormatException e) {
                    Logger.inGameLog("Config", "Invalid integer value: " + valueArg);
                    return 0;
                }

            } else if (type == float.class || type == Float.class) {
                if (valueArg == null) {
                    // Just show current value
                    float currentValue = result.field.getFloat(result.parent);
                    Logger.inGameLog("Config", path + " = " + currentValue);
                    Logger.inGameLog("Config",String.format("Usage: /%s config %s <value>",SuxaddonsClient.MOD_ID,path) );
                    return 1;
                }

                try {
                    float newValue = Float.parseFloat(valueArg);
                    result.field.setFloat(result.parent, newValue);
                    ConfigManager.save();
                    Logger.inGameLog("Config", path + " set to " + newValue);
                    return 1;
                } catch (NumberFormatException e) {
                    Logger.inGameLog("Config", "Invalid float value: " + valueArg);
                    return 0;
                }

            } else if (type == double.class || type == Double.class) {
                if (valueArg == null) {
                    // Just show current value
                    double currentValue = result.field.getDouble(result.parent);
                    Logger.inGameLog("Config", path + " = " + currentValue);
                    Logger.inGameLog("Config",String.format("Usage: /%s config %s <value>",SuxaddonsClient.MOD_ID,path) );
                    return 1;
                }

                try {
                    double newValue = Double.parseDouble(valueArg);
                    result.field.setDouble(result.parent, newValue);
                    ConfigManager.save();
                    Logger.inGameLog("Config", path + " set to " + newValue);
                    return 1;
                } catch (NumberFormatException e) {
                    Logger.inGameLog("Config", "Invalid double value: " + valueArg);
                    return 0;
                }

            } else if (type == String.class) {
                if (valueArg == null) {
                    // Just show current value
                    String currentValue = (String) result.field.get(result.parent);
                    Logger.inGameLog("Config", path + " = \"" + currentValue + "\"");
                    Logger.inGameLog("Config",String.format("Usage: /%s config %s <value>",SuxaddonsClient.MOD_ID,path) );
                    return 1;
                }

                result.field.set(result.parent, valueArg);
                ConfigManager.save();
                Logger.inGameLog("Config", path + " set to \"" + valueArg + "\"");
                return 1;

            } else if (type.isEnum()) {
                // Enum get or set
                Object currentValue = result.field.get(result.parent);

                if (valueArg == null) {
                    // Print current value and list all valid constants
                    Logger.inGameLog("Config", path + " = " + currentValue);
                    StringBuilder constants = new StringBuilder();
                    for (Object constant : type.getEnumConstants()) {
                        if (!constants.isEmpty()) constants.append(", ");
                        constants.append(((Enum<?>) constant).name());
                    }
                    Logger.inGameLog("Config", "Valid values: " + constants);
                    return 1;
                }

                // Case-insensitive match against enum constants
                Object matched = null;
                for (Object constant : type.getEnumConstants()) {
                    if (((Enum<?>) constant).name().equalsIgnoreCase(valueArg)) {
                        matched = constant;
                        break;
                    }
                }

                if (matched == null) {
                    StringBuilder constants = new StringBuilder();
                    for (Object constant : type.getEnumConstants()) {
                        if (!constants.isEmpty()) constants.append(", ");
                        constants.append(((Enum<?>) constant).name());
                    }
                    Logger.inGameLog("Config", "Invalid value '" + valueArg + "' for " + path);
                    Logger.inGameLog("Config", "Valid values: " + constants);
                    return 0;
                }

                result.field.set(result.parent, matched);
                ConfigManager.save();
                Logger.inGameLog("Config", path + " set to " + matched);
                return 1;

            } else {
                Logger.inGameLog("Config", "Unsupported field type: " + type.getSimpleName());
                Logger.inGameLog("Config", "This field cannot be modified via command");
                return 0;
            }
        } catch (IllegalAccessException e) {
            Logger.inGameLog("Config", "Error accessing field: " + e.getMessage());
            return 0;
        }
    }
    private static class ConfigFieldResult {
        Field field;
        Object parent;

        ConfigFieldResult(Field field, Object parent) {
            this.field = field;
            this.parent = parent;
        }
    }
    private static ConfigFieldResult navigateToField(String path) {
        String[] segments = path.split("\\.");

        Object current = ConfigManager.getConfig();
        Class<?> currentClass = Config.class;

        // Navigate through all segments except the last one
        for (int i = 0; i < segments.length - 1; i++) {
            String segment = segments[i];
            try {
                Field field = findFieldIgnoreCase(currentClass, segment);
                if (field == null) return null;

                field.setAccessible(true);
                Object next = field.get(current);
                if (next == null) return null;

                current = next;
                currentClass = next.getClass();
            } catch (Exception e) {
                return null;
            }
        }

        // Get the final field
        try {
            String finalSegment = segments[segments.length - 1];
            Field finalField = findFieldIgnoreCase(currentClass, finalSegment);
            if (finalField == null) return null;

            finalField.setAccessible(true);
            return new ConfigFieldResult(finalField, current);
        } catch (Exception e) {
            return null;
        }
    }

    private static Field findFieldIgnoreCase(Class<?> clazz, String name) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getName().equalsIgnoreCase(name)) {
                return field;
            }
        }
        return null;
    }

    private static void listSubFields(CommandContext<FabricClientCommandSource> ctx, String path, ConfigFieldResult result) {
        try {
            Object obj = result.field.get(result.parent);
            if (obj == null) {
                Logger.inGameLog("Config", "Object at path '" + path + "' is null");
                return;
            }

            Logger.inGameLog("Config", "Available fields in '" + path + "':");

            Field[] fields = obj.getClass().getDeclaredFields();
            boolean foundAny = false;

            for (Field field : fields) {
                // Skip transient and static fields
                if (Modifier.isTransient(field.getModifiers()) ||
                        Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                field.setAccessible(true);
                Class<?> type = field.getType();
                String typeName = getSimpleTypeName(type);

                try {
                    Object value = field.get(obj);
                    Logger.inGameLog("Config", "  - " + field.getName() + " (" + typeName + "): " + value);
                    foundAny = true;
                } catch (Exception e) {
                    Logger.inGameLog("Config", "  - " + field.getName() + " (" + typeName + ")");
                    foundAny = true;
                }
            }

            if (!foundAny) {
                Logger.inGameLog("Config", "No configurable fields found");
            }
        } catch (Exception e) {
            Logger.inGameLog("Config", "Error listing fields: " + e.getMessage());
        }
    }

    private static String getSimpleTypeName(Class<?> type) {
        if (type == boolean.class) return "boolean";
        if (type == int.class) return "int";
        if (type == float.class) return "float";
        if (type == double.class) return "double";
        if (type == String.class) return "String";
        if (type.isEnum()) return type.getSimpleName();
        return type.getSimpleName();
    }
}
