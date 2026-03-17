package com.classictrashcode.suxaddons.client.hunting;


import com.classictrashcode.suxaddons.client.Logger;
import com.classictrashcode.suxaddons.client.utils.BazzarTracker.BazaarAPI;
import com.classictrashcode.suxaddons.client.utils.TracerRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class CinderBatTracker {
    private static final String CINDERBAT_SHARD_ID = "SHARD_CINDER_BAT";

    public static int RESET_TIME = 2400;
    private static boolean isTracking = false;
    public static int NumberOfCinderbatsCaught = 0;
    public static int NumberOfCinderbatShardsCaught = 0;
    public static int trackedTime = 0;
    public static void tick(){
        if (!isTracking) return;
        if (trackedTime % 1200 == 0) {
            BazaarAPI.updateData();
        }

        if (TracerRenderer.currentTarget == null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.player != null) {
                Entity nearest = null;
                double nearestDistSq = Double.MAX_VALUE;
                for (Entity entity : mc.level.entitiesForRendering()) {
                    if (entity.getType() != EntityType.BAT) continue;
                    if (!entity.isAlive() || entity.isInvisible()) continue;
                    double distSq = entity.distanceToSqr(mc.player);
                    if (distSq < nearestDistSq) { nearestDistSq = distSq; nearest = entity; }
                }
                if (nearest != null) TracerRenderer.pinEntity(nearest);
            }
        }

        trackedTime++;
        RESET_TIME--;

        if(RESET_TIME <= 0){
            stopTracker();
        }
    }

    public static void onMessage(String message){
        if (message.contains("You caught") && message.contains("Cinderbat Shard")) {
            Logger.DebugLog("CinderBatTracker", "Shard message detected: " + message);

            java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                    "You caught\\s+(?:x(\\d+)|(\\d+)|a)\\s+Cinderbat\\s+Shard[s]?",
                    java.util.regex.Pattern.CASE_INSENSITIVE
            );
            java.util.regex.Matcher m = p.matcher(message);

            int amount = 1;
            if (m.find()) {
                String xNum = m.group(1);
                String plainNum = m.group(2);
                try {
                    if (xNum != null) amount = Integer.parseInt(xNum);
                    else if (plainNum != null) amount = Integer.parseInt(plainNum);
                } catch (NumberFormatException e) {
                    Logger.DebugLog("CinderBatTracker", "Failed to parse shard amount: " + e.getMessage());
                }
            } else {
                Logger.DebugLog("CinderBatTracker", "Shard regex did not match, defaulting to 1");
            }

            isTracking = true;
            RESET_TIME = 2400;
            NumberOfCinderbatsCaught++;
            NumberOfCinderbatShardsCaught += amount;
            Logger.DebugLog("CinderBatTracker", "Cinderbat caught! Shards: +" + amount + " (total: " + NumberOfCinderbatShardsCaught + ") | Bats: " + NumberOfCinderbatsCaught);
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.player != null) {
                Entity current = TracerRenderer.currentTarget;
                Entity next = null;
                double nextDistSq = Double.MAX_VALUE;
                for (Entity entity : mc.level.entitiesForRendering()) {
                    if (entity.getType() != EntityType.BAT) continue;
                    if (!entity.isAlive() || entity.isInvisible()) continue;
                    if (entity == current) continue;
                    double distSq = entity.distanceToSqr(mc.player);
                    if (distSq < nextDistSq) { nextDistSq = distSq; next = entity; }
                }
                if (next != null) TracerRenderer.pinEntity(next);
                else TracerRenderer.clearTarget();
            }
        }
    }

    public static boolean isTracking() {
        return isTracking;
    }

    public static void stopTracker(){
        NumberOfCinderbatsCaught = 0;
        NumberOfCinderbatShardsCaught = 0;
        isTracking = false;
        RESET_TIME = 2400;
        trackedTime = 0;
        TracerRenderer.clearTarget();
    }

    public static double getTotalCoinsFromSell() {
        return NumberOfCinderbatShardsCaught * BazaarAPI.getInstaSellPrice(CINDERBAT_SHARD_ID);
    }

    public static double getTotalCoinsFromBuy() {
        return NumberOfCinderbatShardsCaught * BazaarAPI.getInstaBuyPrice(CINDERBAT_SHARD_ID);
    }

    public static double getCoinsPerHourSell() {
        double hours = trackedTime / 72000.0;
        return hours > 0 ? getTotalCoinsFromSell() / hours : 0;
    }

    public static double getCoinsPerHourBuy() {
        double hours = trackedTime / 72000.0;
        return hours > 0 ? getTotalCoinsFromBuy() / hours : 0;
    }
}
