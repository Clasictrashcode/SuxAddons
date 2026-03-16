package com.classictrashcode.suxaddons.client.commands;

import com.classictrashcode.suxaddons.client.SuxaddonsClient;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public class CommandManager {
    public static void registerCommands(){
        System.out.printf("[%s] Registering Commands%n", SuxaddonsClient.MOD_ID);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> base =
                    ClientCommandManager.literal(SuxaddonsClient.MOD_ID);
            base = ConfigScreenCommand.register(base);
            base = ConfigCommand.register(base);
            base = TracerCommand.register(base);
            dispatcher.register(base);
        });
    }
}
