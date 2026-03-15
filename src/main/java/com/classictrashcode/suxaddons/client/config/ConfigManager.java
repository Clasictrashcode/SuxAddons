package com.classictrashcode.suxaddons.client.config;

import com.classictrashcode.suxaddons.client.macros.ChatMacros;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_DIR = new File("config/suxaddons");
    private static final File CONFIG_FILE = new File(CONFIG_DIR, "config.json");
    private static Config config = new Config();
    private static ConfigRegistry registry;

    public static void load() {
        if (!CONFIG_DIR.exists()) {
            CONFIG_DIR.mkdirs();
            System.out.println("[SuxAddons] Created config directory: " + CONFIG_DIR.getAbsolutePath());
        }
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                config = GSON.fromJson(json, Config.class);
            } catch (IOException e) {
                System.err.println("[SuxAddons] Failed to load config: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("[SuxAddons] No config found, creating default config...");
            save();
        }
        registry = new ConfigRegistry(config);
    }

    private static boolean getBoolean(JsonObject json, String key, boolean defaultValue) {
        return json.has(key) ? json.get(key).getAsBoolean() : defaultValue;
    }

    private static int getInt(JsonObject json, String key, int defaultValue) {
        return json.has(key) ? json.get(key).getAsInt() : defaultValue;
    }

    private static float getFloat(JsonObject json, String key, float defaultValue) {
        return json.has(key) ? json.get(key).getAsFloat() : defaultValue;
    }

    public static void save() {
        try {
            if (!CONFIG_DIR.exists()) {
                CONFIG_DIR.mkdirs();
            }

            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            System.err.println("[SuxAddons] Failed to save config: " + e.getMessage());
            e.printStackTrace();
        }
        ChatMacros.onConfigChanged();
    }

    public static Config getConfig() {
        return config;
    }

    public static ConfigRegistry getRegistry() {
        if (registry == null) {
            registry = new ConfigRegistry(config);
        }
        return registry;
    }

    public static Config createStagedConfig() {
        String json = GSON.toJson(config);
        Config staged = GSON.fromJson(json, Config.class);
        return staged;
    }
    public static void applyStagedConfig(Config stagedConfig) {
        String json = GSON.toJson(stagedConfig);
        config = GSON.fromJson(json, Config.class);

        // Rebuild registry with new config
        registry = new ConfigRegistry(config);
    }
}