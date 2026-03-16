package com.classictrashcode.suxaddons.client.commands;

import com.classictrashcode.suxaddons.client.Logger;
import com.classictrashcode.suxaddons.client.utils.TracerRenderer;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class TracerCommand {
    public static final String NAME = "tracer";

    public static LiteralArgumentBuilder<FabricClientCommandSource> register(
            LiteralArgumentBuilder<FabricClientCommandSource> base) {
        return base.then(ClientCommandManager.literal(NAME)
                .then(ClientCommandManager.argument("entity", StringArgumentType.word())
                        .suggests(TracerCommand::suggestEntityTypes)
                        .executes(ctx -> executeSetEntity(ctx, StringArgumentType.getString(ctx, "entity")))
                )
                .then(ClientCommandManager.literal("clear")
                        .executes(ctx -> {
                            TracerRenderer.clearTarget();
                            Logger.inGameLog("Tracer", "Tracer cleared");
                            return 1;
                        })
                )
        );
    }

    private static CompletableFuture<Suggestions> suggestEntityTypes(
            CommandContext<FabricClientCommandSource> ctx, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();
        for (ResourceLocation key : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            String path = key.getPath();
            if (path.startsWith(remaining)) {
                builder.suggest(path);
            }
        }
        return builder.buildFuture();
    }

    private static int executeSetEntity(CommandContext<FabricClientCommandSource> ctx, String input) {
        ResourceLocation location = ResourceLocation.parse("minecraft:" + input);
        Optional<EntityType<?>> maybeType = BuiltInRegistries.ENTITY_TYPE.getOptional(location);
        if (maybeType.isEmpty()) {
            Logger.inGameLog("Tracer", "Unknown entity type: " + input);
            return 0;
        }
        EntityType<?> entityType = maybeType.get();
        TracerRenderer.clearTarget();
        TracerRenderer.setTargetType(entityType);
        TracerRenderer.setEnabled(true);
        Logger.inGameLog("Tracer", "Tracing: " + input);
        return 1;
    }
}
