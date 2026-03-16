package com.classictrashcode.suxaddons.client.utils;

import com.classictrashcode.suxaddons.client.config.ConfigManager;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

@SuppressWarnings("deprecation")
public class TracerRenderer implements HudRenderCallback {
    private static EntityType<?> targetType = null;
    private static UUID pinnedEntityId = null;
    private static boolean enabled = false;

    public static Entity currentTarget = null;

    public static EntityType<?> getTargetType() { return targetType; }
    public static void setTargetType(EntityType<?> type) { targetType = type; }

    public static void pinEntity(Entity entity) {
        pinnedEntityId = entity.getUUID();
        targetType = null;
        enabled = true;
    }

    public static void clearTarget() {
        if (currentTarget != null) {
            currentTarget.setGlowingTag(false);
            currentTarget = null;
        }
        targetType = null;
        pinnedEntityId = null;
    }

    public static void setEnabled(boolean value) { enabled = value; }
    public static boolean isEnabled() { return enabled; }

    @Override
    public void onHudRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        var cfg = ConfigManager.getConfig().utilities.tracer;
        enabled = cfg.enabled;

        if (!enabled) {
            if (currentTarget != null) {
                currentTarget.setGlowingTag(false);
                currentTarget = null;
            }
            return;
        }
        if (pinnedEntityId == null && targetType == null) return;

        Entity nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        if (pinnedEntityId != null) {
            Entity pinned = EntityUtils.getEntityByUUID(pinnedEntityId);
            if (pinned == null || !pinned.isAlive()) {
                pinnedEntityId = null;
                if (currentTarget != null) {
                    currentTarget.setGlowingTag(false);
                    currentTarget = null;
                }
                return;
            }
            nearest = pinned;
            nearestDistSq = pinned.distanceToSqr(mc.player);
        } else {
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity.getType() != targetType) continue;
                if (!entity.isAlive()) continue;
                if (entity == mc.player) continue;
                double distSq = entity.distanceToSqr(mc.player);
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearest = entity;
                }
            }
        }

        if (nearest == null) {
            if (currentTarget != null) {
                currentTarget.setGlowingTag(false);
                currentTarget = null;
            }
            return;
        }

        if (nearest != currentTarget) {
            if (currentTarget != null) {
                currentTarget.setGlowingTag(false);
            }
            currentTarget = nearest;
        }
        currentTarget.setGlowingTag(true);

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        double ex = nearest.xo + (nearest.getX() - nearest.xo) * partialTick;
        double ey = nearest.yo + (nearest.getY() - nearest.yo) * partialTick + (nearest.getBbHeight() * 0.5);
        double ez = nearest.zo + (nearest.getZ() - nearest.zo) * partialTick;

        Vec3 ndc = mc.gameRenderer.projectPointToScreen(new Vec3(ex, ey, ez));
        if (ndc.z < -1.0) return;

        int guiW = mc.getWindow().getGuiScaledWidth();
        int guiH = mc.getWindow().getGuiScaledHeight();
        float cx = guiW / 2.0f;
        float cy = guiH / 2.0f;

        float sx = (float) ((ndc.x + 1.0) / 2.0 * guiW);
        float sy = (float) ((1.0 - ndc.y) / 2.0 * guiH);

        float dx = sx - cx;
        float dy = sy - cy;
        if (ndc.z > 1.0) { dx = -dx; dy = -dy; }

        float halfW = guiW / 2.0f - 1;
        float halfH = guiH / 2.0f - 1;
        if (dx != 0 || dy != 0) {
            float scaleX = (dx != 0) ? halfW / Math.abs(dx) : Float.MAX_VALUE;
            float scaleY = (dy != 0) ? halfH / Math.abs(dy) : Float.MAX_VALUE;
            float scale = Math.min(1.0f, Math.min(scaleX, scaleY));
            sx = cx + dx * scale;
            sy = cy + dy * scale;
        }

        var theme = ConfigManager.getConfig().guiTheme;
        int color = theme.parseColor(theme.primaryColor);
        float halfWidth = Math.max(1, cfg.lineWidth) * 0.5f;

        drawThickLine(guiGraphics, cx, cy, sx, sy, color, halfWidth);

        double distMeters = Math.sqrt(nearestDistSq);
        String label = nearest.getType().getDescription().getString()
                + " §7" + String.format("%.1fm", distMeters);
        int labelX = (int) (cx - mc.font.width(label) / 2.0f);
        int labelY = (int) (cy + 12);
        guiGraphics.drawString(mc.font, Component.literal(label), labelX, labelY, color, true);
    }

    private static void drawThickLine(GuiGraphics g,
                                      float x0, float y0,
                                      float x1, float y1,
                                      int color, float halfWidth) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.5f) return;

        float angle = (float) Math.atan2(dy, dx);

        g.pose().pushMatrix();
        g.pose().translate(x0, y0);
        g.pose().rotate(angle);
        g.fill(0, (int) -halfWidth, (int) len, (int) halfWidth, color);
        g.pose().popMatrix();
    }
}
