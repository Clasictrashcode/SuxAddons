package com.classictrashcode.suxaddons.client;

import com.classictrashcode.suxaddons.client.commands.CommandManager;
import com.classictrashcode.suxaddons.client.config.ConfigManager;
import com.classictrashcode.suxaddons.client.macros.ChatMacros;
import com.classictrashcode.suxaddons.client.utils.ChatUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class SuxaddonsClient implements ClientModInitializer {
    public static final String MOD_ID = "suxaddons";
    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        CommandManager.registerCommands();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ChatUtils.tick();
            ChatMacros.tick(client.getWindow().handle());
        });
    }
}
