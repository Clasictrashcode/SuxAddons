package com.classictrashcode.suxaddons.client;

import com.classictrashcode.suxaddons.client.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import static com.classictrashcode.suxaddons.client.SuxaddonsClient.MOD_ID;

public class Logger {
    public static void DebugLog(String source,String message) {
        if (ConfigManager.getConfig().debug.debugMode){
            String formatedMessage = String.format("[%s][%s][Debug]: %s",MOD_ID,source,message);
            System.out.println(formatedMessage);
            inGameLog(formatedMessage);
        }
    }
    public static void inGameLog(String source,String message) {
        if (message == null) {
            return;
        }
        String formatedMessage = String.format("[%s][%s]: %s",MOD_ID,source,message);
        inGameLog(formatedMessage);
    }
    public static void inGameLog(String message) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            client.gui.getChat().addMessage(Component.literal(message));
        });
    }
}
