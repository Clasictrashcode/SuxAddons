package com.classictrashcode.suxaddons.client.commands;

import com.classictrashcode.suxaddons.client.screens.AutoConfigScreen;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.util.concurrent.atomic.AtomicBoolean;

public class ConfigScreenCommand {
    public static LiteralArgumentBuilder<FabricClientCommandSource> register(LiteralArgumentBuilder<FabricClientCommandSource> base) {
        return base.executes(ctx -> {
            //Had to do it this way because the Game sets the screen to null after sending a command.
            AtomicBoolean ran = new AtomicBoolean(false);
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (!ran.getAndSet(true)) {
                    client.setScreen(new AutoConfigScreen(null));
                }
            });
            return 1;
        });
    }
}
