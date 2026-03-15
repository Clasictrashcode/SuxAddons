package com.classictrashcode.suxaddons.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EntityUtils {

    private static Level getLevel() {
        return Minecraft.getInstance().level;
    }
    // Generic entity lookups

    public static Entity getEntityByUUID(UUID uuid) {
        if (uuid == null) return null;
        Level level = getLevel();
        if (level == null) return null;
        for (Entity entity : ((ClientLevel) level).entitiesForRendering()) {
            if (uuid.equals(entity.getUUID())) {
                return entity;
            }
        }
        return null;
    }

    public static Entity getEntityById(int entityId) {
        Level level = getLevel();
        if (level == null) return null;
        return level.getEntity(entityId);
    }
    // 5x5 chunk area (80 block radius) centred on the player
    private static final int DEFAULT_CHUNK_RADIUS = 5;

    public static List<ArmorStand> getAllArmorStands() {
        return getAllArmorStandsInRange(DEFAULT_CHUNK_RADIUS);
    }

    public static List<ArmorStand> getAllArmorStandsInRange(double chunkRadius) {
        Level level = getLevel();
        if (level == null) return Collections.emptyList();
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return Collections.emptyList();

        double blockRadius = chunkRadius * 16.0;
        Vec3 playerPos = client.player.position();
        AABB searchBox = AABB.ofSize(playerPos, blockRadius * 2, 512, blockRadius * 2);
        return level.getEntitiesOfClass(ArmorStand.class, searchBox, armorStand -> true);
    }

    public static Optional<ArmorStand> getArmorStandByUUID(UUID uuid) {
        if (uuid == null) return Optional.empty();
        Entity entity = getEntityByUUID(uuid);
        if (entity instanceof ArmorStand armorStand) {
            return Optional.of(armorStand);
        }
        return Optional.empty();
    }

    public static ArmorStand getArmorStandById(int entityId) {
        Entity entity = getEntityById(entityId);
        if (entity instanceof ArmorStand armorStand) {
            return armorStand;
        }
        return null;
    }
    // ArmorStand proximity queries
    public static List<ArmorStand> getArmorStandsNear(Vec3 center, double radius) {
        Level level = getLevel();
        if (level == null) return Collections.emptyList();
        if (radius <= 0) return Collections.emptyList();

        // AABB coarse filter, then exact distance check to exclude box corners
        AABB searchBox = AABB.ofSize(center, radius * 2, radius * 2, radius * 2);
        double radiusSq = radius * radius;
        return level.getEntitiesOfClass(
                ArmorStand.class,
                searchBox,
                armorStand -> armorStand.position().distanceToSqr(center) <= radiusSq
        );
    }
    public static List<ArmorStand> getArmorStandsNearPlayer(double radius) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return Collections.emptyList();
        return getArmorStandsNear(client.player.position(), radius);
    }
    // ArmorStand name-tag helpers

    public static String getArmorStandName(ArmorStand armorStand) {
        if (armorStand == null) return "";
        Component customName = armorStand.getCustomName();
        if (customName == null) return "";
        return customName.getString();
    }

    public static List<ArmorStand> getArmorStandsWithName(String substring) {
        if (substring == null) return Collections.emptyList();
        String lowerSubstring = substring.toLowerCase();
        return getAllArmorStands().stream()
                .filter(armorStand -> {
                    String name = getArmorStandName(armorStand);
                    return !name.isEmpty() && name.toLowerCase().contains(lowerSubstring);
                })
                .toList();
    }
    public static List<ArmorStand> getArmorStandsWithNameWithinDistance(String substring,double radius) {
        if (substring == null) return Collections.emptyList();
        String lowerSubstring = substring.toLowerCase();
        return getArmorStandsNearPlayer(radius).stream()
                .filter(armorStand -> {
                    String name = getArmorStandName(armorStand);
                    return !name.isEmpty() && name.toLowerCase().contains(lowerSubstring);
                })
                .toList();
    }
}
