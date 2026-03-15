package com.classictrashcode.suxaddons.client;

import com.classictrashcode.suxaddons.client.commands.CommandManager;
import com.classictrashcode.suxaddons.client.config.ConfigManager;
import net.fabricmc.api.ClientModInitializer;

public class SuxaddonsClient implements ClientModInitializer {
    public static final String MOD_ID = "suxaddons";
    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        CommandManager.registerCommands();
    }
}
