package com.classictrashcode.suxaddons.client;

import com.classictrashcode.suxaddons.client.commands.CommandManager;
import com.classictrashcode.suxaddons.client.config.ConfigManager;
import com.classictrashcode.suxaddons.client.hunting.*;
import com.classictrashcode.suxaddons.client.macros.ChatMacros;
import com.classictrashcode.suxaddons.client.utils.BazzarTracker.BazzarTracker;
import com.classictrashcode.suxaddons.client.utils.ChatUtils;
import com.classictrashcode.suxaddons.client.utils.InventoryUtils;
import com.classictrashcode.suxaddons.client.utils.UpdateChecker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class SuxaddonsClient implements ClientModInitializer {
    public static final String MOD_ID = "suxaddons";
    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        SoundManager.initialize();// Initialize sound manager for custom sounds\
        CommandManager.registerCommands();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ChatUtils.tick();
            InventoryUtils.tick();
            HideonLeafTracker.tick();
            CinderBatTracker.tick();
            LassoPuller.tick();
            BazzarTracker.tick();
            AutoFusion.tick();
            ChatMacros.tick(client.getWindow().handle());
        });
        HudRenderCallback.EVENT.register(new HideonLeafTrackerHud());
        HudRenderCallback.EVENT.register(new CinderBatTrackerHud());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> UpdateChecker.onJoin());
    }
}
