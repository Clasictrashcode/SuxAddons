package com.classictrashcode.suxaddons.client.hunting;


import com.classictrashcode.suxaddons.client.config.ConfigManager;
import com.classictrashcode.suxaddons.client.utils.BazzarTracker.BazaarAPI;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

@SuppressWarnings("deprecation")
public class CinderBatTrackerHud implements HudRenderCallback {
    public static int hudX = 10;
    public static int hudY = 10;

    private static final int PADDING = 4;
    private static final int LINE_HEIGHT = 10;
    private static final int BACKGROUND_COLOR = 0x80000000;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    public void drawBorder(int hudX, int hudY, int bgWidth, int bgHeight, int hex, GuiGraphics drawContext){
        drawContext.hLine(hudX, hudX + bgWidth, hudY, hex); // top
        drawContext.hLine(hudX, hudX + bgWidth, hudY + bgHeight, hex); // Bottom
        drawContext.vLine(hudX, hudY, hudY + bgHeight, hex);   // Left
        drawContext.vLine(hudX + bgWidth, hudY, hudY + bgHeight, hex); // Right
    }

    private String formatCoins(double coins) {
        if (coins >= 1_000_000) {
            return String.format("%.2fM", coins / 1_000_000);
        } else if (coins >= 1_000) {
            return String.format("%.1fk", coins / 1_000);
        } else {
            return String.format("%.0f", coins);
        }
    }

    @Override
    public void onHudRender(GuiGraphics drawContext, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) return;
        if (!ConfigManager.getConfig().hunting.cinderBatTracker.enabled) return;
        if (!CinderBatTracker.isTracking()) return;

        var theme = ConfigManager.getConfig().guiTheme;
        int titleBarColor = theme.parseColor(theme.primaryColor);

        if (BazaarAPI.getInstaBuyPrice("SHARD_CINDER_BAT") == 0) {
            BazaarAPI.updateData();
        }

        int seconds = CinderBatTracker.trackedTime / 20;
        int mins = seconds / 60;
        int secs = seconds % 60;
        String timeStr = String.format("%d:%02d", mins, secs);

        double hours = seconds / 3600.0;
        String catchRate = hours > 0 ? String.format("%.1f", CinderBatTracker.NumberOfCinderbatsCaught / hours) : "N/A";
        String shardRate = hours > 0 ? String.format("%.1f", CinderBatTracker.NumberOfCinderbatShardsCaught / hours) : "N/A";

        double coinsSell = CinderBatTracker.getTotalCoinsFromSell();
        double coinsBuy = CinderBatTracker.getTotalCoinsFromBuy();
        double coinsPerHourSell = CinderBatTracker.getCoinsPerHourSell();
        double coinsPerHourBuy = CinderBatTracker.getCoinsPerHourBuy();

        String coinsSellStr = formatCoins(coinsSell);
        String coinsBuyStr = formatCoins(coinsBuy);
        String coinsPerHourSellStr = formatCoins(coinsPerHourSell);
        String coinsPerHourBuyStr = formatCoins(coinsPerHourBuy);

        String title = "Cinderbat Tracker";
        String[] contentLines = {
                "§7Time: §f" + timeStr,
                "§7Catches: §a" + CinderBatTracker.NumberOfCinderbatsCaught + " §7(§a" + catchRate + "/hr§7)",
                "§7Shards: §e" + CinderBatTracker.NumberOfCinderbatShardsCaught + " §7(§e" + shardRate + "/hr§7)",
                "§7Coins (Sell): §6" + coinsSellStr + " §7(§6" + coinsPerHourSellStr + "/hr§7)",
                "§7Coins (Buy): §6" + coinsBuyStr + " §7(§6" + coinsPerHourBuyStr + "/hr§7)",
                "§7Shard Price: §6" + formatCoins(BazaarAPI.getInstaSellPrice("SHARD_CINDER_BAT")) + "§7/§6" + formatCoins(BazaarAPI.getInstaBuyPrice("SHARD_CINDER_BAT"))
        };

        int maxWidth = client.font.width(title);
        for (String line : contentLines) {
            int width = client.font.width(line);
            if (width > maxWidth) maxWidth = width;
        }

        int bgWidth = maxWidth + (PADDING * 2);
        int titleBarHeight = 14;
        int contentHeight = (contentLines.length * LINE_HEIGHT) + (PADDING * 2);
        int bgHeight = titleBarHeight + contentHeight;

        drawContext.fill(hudX, hudY, hudX + bgWidth, hudY + bgHeight, BACKGROUND_COLOR);
        drawContext.fill(hudX, hudY, hudX + bgWidth, hudY + titleBarHeight, titleBarColor);
        drawBorder(hudX, hudY, bgWidth, bgHeight, 0xFF555555, drawContext);
        drawContext.drawString(
                client.font,
                Component.literal(title),
                hudX + PADDING,
                hudY + (titleBarHeight - 8) / 2,
                TEXT_COLOR
        );
        int currentY = hudY + titleBarHeight + PADDING;
        for (String line : contentLines) {
            drawContext.drawString(
                    client.font,
                    Component.literal(line),
                    hudX + PADDING,
                    currentY,
                    TEXT_COLOR,
                    true
            );
            currentY += LINE_HEIGHT;
        }
    }
}
