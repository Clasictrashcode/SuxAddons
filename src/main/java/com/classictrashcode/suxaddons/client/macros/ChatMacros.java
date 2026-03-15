package com.classictrashcode.suxaddons.client.macros;

import com.classictrashcode.suxaddons.client.config.Config;
import com.classictrashcode.suxaddons.client.config.ConfigManager;
import com.classictrashcode.suxaddons.client.utils.ChatUtils;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatMacros {
    private static Map<Integer, String> macros = new HashMap<>();
    private static final Map<Integer, Boolean> lastPressed = new HashMap<>();
    static {
        syncFromConfig();
    }
    public static void syncFromConfig() {
        macros.clear();
        Config config = ConfigManager.getConfig();
        if (config != null && config.macros != null && config.macros.chatMacros != null && config.macros.chatMacros.macroList != null) {
            List<Config.ChatMacrosSettings.MacroEntry> entries = config.macros.chatMacros.macroList;
            for (Config.ChatMacrosSettings.MacroEntry entry : entries) {
                if (entry.keyCode != GLFW.GLFW_KEY_UNKNOWN && entry.message != null && !entry.message.isEmpty()) {
                    macros.put(entry.keyCode, entry.message);
                }
            }
        }
    }

    public static void onConfigChanged() {
        syncFromConfig();
    }

    public static void tick(long window){
        if (!ConfigManager.getConfig().macros.chatMacros.enabled){
            return;
        }
        for (Map.Entry<Integer, String> entry : macros.entrySet()) {
            int keyCode = entry.getKey();
            String message = entry.getValue();
            boolean currentlyPressed = GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
            boolean previouslyPressed = lastPressed.getOrDefault(keyCode, false);
            if (currentlyPressed && !previouslyPressed && Minecraft.getInstance().screen == null) {
                System.out.println("Macro triggered: " + message);
                ChatUtils.queueMessage(message);
            }
            lastPressed.put(keyCode, currentlyPressed);
        }
    }
}
