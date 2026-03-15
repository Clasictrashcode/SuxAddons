package com.classictrashcode.suxaddons.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThemePresetManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_DIR = new File("config/suxaddons");
    private static final File PRESET_FILE = new File(CONFIG_DIR, "theme_presets.json");

    private static final Map<String, ThemePreset> presets = new HashMap<>();
    private static boolean initialized = false;


    public static void initialize() {
        if (initialized) return;

        createBuiltInPresets();

        loadCustomPresets();

        initialized = true;
    }

    private static void createBuiltInPresets() {
        // Dark Mode (Green) - current default theme
        ThemePreset darkModeGreen = new ThemePreset();
        darkModeGreen.name = "Dark Mode (Green)";
        darkModeGreen.builtIn = true;
        darkModeGreen.backgroundColor = "#121212";
        darkModeGreen.surfaceColor = "#1E1E1E";
        darkModeGreen.surfaceElevatedColor = "#252525";
        darkModeGreen.primaryColor = "#2E7D32";
        darkModeGreen.primaryDarkColor = "#1B5E20";
        darkModeGreen.primaryLightColor = "#4CAF50";
        darkModeGreen.textPrimaryColor = "#E0E0E0";
        darkModeGreen.textSecondaryColor = "#9E9E9E";
        darkModeGreen.dividerColor = "#424242";
        darkModeGreen.errorColor = "#D32F2F";
        darkModeGreen.successColor = "#388E3C";
        darkModeGreen.buttonNormalColor = "#2E7D32";
        darkModeGreen.buttonHoverColor = "#4CAF50";
        darkModeGreen.buttonActiveColor = "#1B5E20";
        darkModeGreen.buttonDisabledColor = "#424242";
        darkModeGreen.toggleOnColor = "#4CAF50";
        darkModeGreen.toggleOffColor = "#616161";
        darkModeGreen.toggleKnobColor = "#FFFFFF";
        darkModeGreen.textFieldBackgroundColor = "#252525";
        darkModeGreen.textFieldBorderColor = "#424242";
        darkModeGreen.textFieldFocusColor = "#4CAF50";
        presets.put(darkModeGreen.name, darkModeGreen);

        // Dark Mode (Blue)
        ThemePreset darkModeBlue = new ThemePreset();
        darkModeBlue.name = "Dark Mode (Blue)";
        darkModeBlue.builtIn = true;
        darkModeBlue.backgroundColor = "#121212";
        darkModeBlue.surfaceColor = "#1E1E1E";
        darkModeBlue.surfaceElevatedColor = "#252525";
        darkModeBlue.primaryColor = "#1976D2";
        darkModeBlue.primaryDarkColor = "#0D47A1";
        darkModeBlue.primaryLightColor = "#42A5F5";
        darkModeBlue.textPrimaryColor = "#E0E0E0";
        darkModeBlue.textSecondaryColor = "#9E9E9E";
        darkModeBlue.dividerColor = "#424242";
        darkModeBlue.errorColor = "#D32F2F";
        darkModeBlue.successColor = "#1976D2";
        darkModeBlue.buttonNormalColor = "#1976D2";
        darkModeBlue.buttonHoverColor = "#42A5F5";
        darkModeBlue.buttonActiveColor = "#0D47A1";
        darkModeBlue.buttonDisabledColor = "#424242";
        darkModeBlue.toggleOnColor = "#42A5F5";
        darkModeBlue.toggleOffColor = "#616161";
        darkModeBlue.toggleKnobColor = "#FFFFFF";
        darkModeBlue.textFieldBackgroundColor = "#252525";
        darkModeBlue.textFieldBorderColor = "#424242";
        darkModeBlue.textFieldFocusColor = "#42A5F5";
        presets.put(darkModeBlue.name, darkModeBlue);

        // Dark Mode (Pink) - soft rose/pink accents
        ThemePreset darkModePink = new ThemePreset();
        darkModePink.name = "Dark Mode (Pink)";
        darkModePink.builtIn = true;
        darkModePink.backgroundColor = "#121212";
        darkModePink.surfaceColor = "#1E1E1E";
        darkModePink.surfaceElevatedColor = "#252525";
        darkModePink.primaryColor = "#E57B9E"; // Soft rose pink
        darkModePink.primaryDarkColor = "#D16B8C"; // Muted darker pink
        darkModePink.primaryLightColor = "#F5A3BE"; // Pastel light pink
        darkModePink.textPrimaryColor = "#E0E0E0";
        darkModePink.textSecondaryColor = "#9E9E9E";
        darkModePink.dividerColor = "#424242";
        darkModePink.errorColor = "#D32F2F";
        darkModePink.successColor = "#E57B9E"; // Soft pink
        darkModePink.buttonNormalColor = "#E57B9E"; // Soft rose
        darkModePink.buttonHoverColor = "#F5A3BE"; // Pastel pink hover
        darkModePink.buttonActiveColor = "#D16B8C"; // Deeper rose active
        darkModePink.buttonDisabledColor = "#424242";
        darkModePink.toggleOnColor = "#F5A3BE"; // Pastel pink on
        darkModePink.toggleOffColor = "#616161";
        darkModePink.toggleKnobColor = "#FFFFFF";
        darkModePink.textFieldBackgroundColor = "#252525";
        darkModePink.textFieldBorderColor = "#424242";
        darkModePink.textFieldFocusColor = "#E57B9E"; // Soft rose focus
        presets.put(darkModePink.name, darkModePink);

        // Light Mode - Flash Bang
        ThemePreset lightMode = new ThemePreset();
        lightMode.name = "Light Mode (Flash Bang)";
        lightMode.builtIn = true;
        lightMode.backgroundColor = "#E8E8E8";
        lightMode.surfaceColor = "#F0F0F0";
        lightMode.surfaceElevatedColor = "#FAFAFA";
        lightMode.primaryColor = "#5C6BC0";
        lightMode.primaryDarkColor = "#3949AB";
        lightMode.primaryLightColor = "#7986CB";
        lightMode.textPrimaryColor = "#2C2C2C";
        lightMode.textSecondaryColor = "#616161";
        lightMode.dividerColor = "#BDBDBD";
        lightMode.errorColor = "#C62828";
        lightMode.successColor = "#558B2F";
        lightMode.buttonNormalColor = "#5C6BC0";
        lightMode.buttonHoverColor = "#7986CB";
        lightMode.buttonActiveColor = "#3949AB";
        lightMode.buttonDisabledColor = "#9E9E9E";
        lightMode.toggleOnColor = "#7986CB";
        lightMode.toggleOffColor = "#9E9E9E";
        lightMode.toggleKnobColor = "#FFFFFF";
        lightMode.textFieldBackgroundColor = "#FAFAFA";
        lightMode.textFieldBorderColor = "#BDBDBD";
        lightMode.textFieldFocusColor = "#5C6BC0";
        presets.put(lightMode.name, lightMode);
    }

    private static void loadCustomPresets() {
        if (!PRESET_FILE.exists()) {
            return; // No custom presets file yet
        }

        try (FileReader reader = new FileReader(PRESET_FILE)) {
            Type listType = new TypeToken<List<ThemePreset>>() {}.getType();
            List<ThemePreset> customPresets = GSON.fromJson(reader, listType);

            if (customPresets != null) {
                for (ThemePreset preset : customPresets) {
                    if (!preset.builtIn) {
                        presets.put(preset.name, preset);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[SuxAddons] Failed to load custom theme presets: " + e.getMessage());
        }
    }

    private static void saveCustomPresets() {
        // Get only custom presets (non-built-in)
        List<ThemePreset> customPresets = new ArrayList<>();
        for (ThemePreset preset : presets.values()) {
            if (!preset.builtIn) {
                customPresets.add(preset);
            }
        }

        try {
            File parent = PRESET_FILE.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            try (FileWriter writer = new FileWriter(PRESET_FILE)) {
                GSON.toJson(customPresets, writer);
            }
        } catch (IOException e) {
            System.err.println("[SuxAddons] Failed to save custom theme presets: " + e.getMessage());
        }
    }

    public static List<String> getPresetNames() {
        initialize();
        return new ArrayList<>(presets.keySet());
    }
    public static ThemePreset getPreset(String name) {
        initialize();
        return presets.get(name);
    }
    public static boolean applyPreset(String name) {
        initialize();
        ThemePreset preset = presets.get(name);
        if (preset != null) {
            preset.applyTo(ConfigManager.getConfig().guiTheme);
            ConfigManager.save();
            return true;
        }
        return false;
    }
    public static boolean saveCurrentAsPreset(String name, Config.GuiTheme themeToSave) {
        initialize();

        // Don't allow overwriting built-in presets
        ThemePreset existing = presets.get(name);
        if (existing != null && existing.builtIn) {
            return false;
        }

        // Create new preset from current theme
        ThemePreset newPreset = new ThemePreset(name, themeToSave, false);

        presets.put(name, newPreset);
        saveCustomPresets();
        return true;
    }
    public static boolean deletePreset(String name) {
        initialize();
        ThemePreset preset = presets.get(name);

        if (preset == null || preset.builtIn) {
            return false; // Cannot delete built-in or non-existent presets
        }

        presets.remove(name);
        saveCustomPresets();
        return true;
    }

    public static boolean presetExists(String name) {
        initialize();
        return presets.containsKey(name);
    }

    public static boolean isBuiltIn(String name) {
        initialize();
        ThemePreset preset = presets.get(name);
        return preset != null && preset.builtIn;
    }
}