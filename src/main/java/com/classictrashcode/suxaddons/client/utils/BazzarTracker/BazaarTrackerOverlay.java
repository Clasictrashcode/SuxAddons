package com.classictrashcode.suxaddons.client.utils.BazzarTracker;


import com.classictrashcode.suxaddons.client.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;


public class BazaarTrackerOverlay {
    private static final int OVERLAY_WIDTH = 220;
    private static final int PADDING = 10;
    private static final int LINE_HEIGHT = 12;
    private static final int ORDER_CARD_HEIGHT = 54;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int DELETE_BUTTON_SIZE = 20;

    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat QUANTITY_FORMAT = new DecimalFormat("#,###");

    // Scroll state
    private static int scrollOffset = 0;
    private static int maxScrollOffset = 0;

    // Delete button tracking
    private static class DeleteButton {
        int x, y, width, height;
        int orderIndex;
        boolean isSellOrder;

        DeleteButton(int x, int y, int width, int height, int orderIndex, boolean isSellOrder) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.orderIndex = orderIndex;
            this.isSellOrder = isSellOrder;
        }

        boolean isMouseOver(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    private static List<DeleteButton> deleteButtons = new ArrayList<>();
    private static int overlayX = 0;
    private static int overlayY = 0;
    private static int overlayHeight = 0;

    public static void renderOverlay(GuiGraphics context, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        // Only show overlay when in Bazaar-related screens
        if (!shouldShowOverlay()) {
            scrollOffset = 0;
            maxScrollOffset = 0;
            deleteButtons.clear();
            return;
        }

        // Get theme colors from config
        var theme = ConfigManager.getConfig().guiTheme;
        int backgroundColor = theme.parseColor(theme.surfaceColor);
        int borderColor = theme.parseColor(theme.dividerColor);
        int textColor = theme.parseColor(theme.textPrimaryColor);
        int textSecondaryColor = theme.parseColor(theme.textSecondaryColor);
        int successColor = theme.parseColor(theme.successColor);
        int errorColor = theme.parseColor(theme.errorColor);

        // Count total orders
        int totalOrders = BazzarTracker.sellOrders.size() + BazzarTracker.buyOrders.size();

        if (totalOrders == 0) {
            // Don't render if no orders to display
            scrollOffset = 0;
            maxScrollOffset = 0;
            deleteButtons.clear();
            return;
        }

        // Clear delete buttons list for this frame
        deleteButtons.clear();

        // Calculate dynamic overlay height based on available screen space
        int maxOverlayHeight = screenHeight - (PADDING * 2 + 40); // Leave padding top/bottom
        int titleBarHeight = 22;
        int contentAreaHeight = maxOverlayHeight - titleBarHeight;

        // Calculate total content height needed for all orders
        int totalContentHeight = totalOrders * ORDER_CARD_HEIGHT;

        // Set overlay height to max available or content height, whichever is smaller
        overlayHeight = Math.min(maxOverlayHeight, titleBarHeight + totalContentHeight);

        // Calculate overlay position (LEFT side with padding)
        overlayX = PADDING;
        overlayY = PADDING + 20;

        // Draw background with slight transparency
        int bgColorWithAlpha = (backgroundColor & 0x00FFFFFF) | 0xE6000000; // 90% opacity
        context.fill(overlayX, overlayY, overlayX + OVERLAY_WIDTH, overlayY + overlayHeight, bgColorWithAlpha);

        // Draw colored title bar
        int titleBarColor = theme.parseColor(theme.primaryColor);
        context.fill(overlayX, overlayY, overlayX + OVERLAY_WIDTH, overlayY + titleBarHeight, titleBarColor);

        // Draw border
        drawBorder(context, overlayX, overlayY, OVERLAY_WIDTH, overlayHeight, borderColor);

        // Draw title in white on colored bar
        String title = "Bazaar Orders (" + totalOrders + ")";
        context.drawString(
            Minecraft.getInstance().font,
            Component.literal(title),
            overlayX + PADDING,
            overlayY + (titleBarHeight - 8) / 2, // Center vertically in title bar
            0xFFFFFFFF, // White text
            false
        );

        // Calculate max scroll offset
        maxScrollOffset = Math.max(0, totalContentHeight - contentAreaHeight);

        // Clamp scroll offset
        scrollOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset));

        // Enable scissor clipping for content area
        int contentStartY = overlayY + titleBarHeight;
        int contentEndY = overlayY + overlayHeight;
        context.enableScissor(overlayX, contentStartY, overlayX + OVERLAY_WIDTH, contentEndY);

        // Content starts below title bar, offset by scroll
        int currentY = contentStartY + 5 - scrollOffset;
        int orderIndex = 0;

        // Render sell orders first
        for (BazzarTracker.Order order : BazzarTracker.sellOrders) {
            currentY = renderOrder(context, overlayX, currentY, order, true, orderIndex, true,
                                 textColor, textSecondaryColor, successColor, errorColor, borderColor, mouseX, mouseY);
            orderIndex++;
        }

        // Render buy orders
        int buyOrderIndex = 0;
        for (BazzarTracker.Order order : BazzarTracker.buyOrders) {
            currentY = renderOrder(context, overlayX, currentY, order, false, buyOrderIndex, false,
                                 textColor, textSecondaryColor, successColor, errorColor, borderColor, mouseX, mouseY);
            buyOrderIndex++;
        }

        // Disable scissor clipping
        context.disableScissor();

        // Render scrollbar if needed
        if (maxScrollOffset > 0) {
            renderScrollbar(context, theme, contentStartY, contentAreaHeight);
        }
    }


    private static int renderOrder(GuiGraphics context, int overlayX, int startY,
                                   BazzarTracker.Order order, boolean isSellOrder, int orderIndex, boolean isSellList,
                                   int textColor, int textSecondaryColor,
                                   int successColor, int errorColor, int borderColor,
                                   int mouseX, int mouseY) {
        int currentY = startY;
        var textRenderer = Minecraft.getInstance().font;

        // Draw delete button (top-right corner)
        int deleteButtonX = overlayX + OVERLAY_WIDTH - PADDING - DELETE_BUTTON_SIZE;
        int deleteButtonY = currentY;

        // Track delete button for click detection
        DeleteButton deleteBtn = new DeleteButton(
            deleteButtonX, deleteButtonY, DELETE_BUTTON_SIZE, DELETE_BUTTON_SIZE,
            orderIndex, isSellList
        );
        deleteButtons.add(deleteBtn);

        // Check if mouse is hovering over delete button
        boolean isHovering = deleteBtn.isMouseOver(mouseX, mouseY);
        int deleteButtonColor = isHovering ?
            ConfigManager.getConfig().guiTheme.parseColor("#FF5252") : // Bright red on hover
            ConfigManager.getConfig().guiTheme.parseColor("#D32F2F");   // Dark red normal

        // Draw delete button background
        context.fill(deleteButtonX, deleteButtonY,
                    deleteButtonX + DELETE_BUTTON_SIZE, deleteButtonY + DELETE_BUTTON_SIZE,
                    deleteButtonColor);

        // Draw "X" text centered in button
        String deleteText = "×";
        int deleteTextWidth = textRenderer.width(deleteText);
        context.drawString(
            textRenderer,
            Component.literal(deleteText),
            deleteButtonX + (DELETE_BUTTON_SIZE - deleteTextWidth) / 2,
            deleteButtonY + (DELETE_BUTTON_SIZE - 8) / 2, // Center vertically (text height ~8px)
            0xFFFFFFFF, // White
            false
        );

        // Draw order type badge
        String orderTypeBadge = isSellOrder ? "SELL" : "BUY";
        int badgeColor = isSellOrder ?
            ConfigManager.getConfig().guiTheme.parseColor("#FF9800") : // Orange for sell
            ConfigManager.getConfig().guiTheme.parseColor("#2196F3");   // Blue for buy

        context.drawString(
            textRenderer,
            Component.literal(orderTypeBadge),
            overlayX + PADDING,
            currentY,
            badgeColor,
            false
        );

        // Draw status next to delete button (right aligned with some spacing)
        String statusText = getStatusText(order.status);
        int statusColor = getStatusColor(order.status, successColor, textSecondaryColor, errorColor);
        int statusWidth = textRenderer.width(statusText);
        context.drawString(
            textRenderer,
            Component.literal(statusText),
            overlayX + OVERLAY_WIDTH - PADDING - DELETE_BUTTON_SIZE - 4 - statusWidth,
            currentY,
            statusColor,
            false
        );

        // Draw item name (truncated if too long)
        currentY += LINE_HEIGHT;
        String itemName = order.itemName;
        int maxWidth = OVERLAY_WIDTH - (PADDING * 2);

        // Truncate item name if too long
        if (textRenderer.width(itemName) > maxWidth) {
            while (textRenderer.width(itemName + "...") > maxWidth && itemName.length() > 0) {
                itemName = itemName.substring(0, itemName.length() - 1);
            }
            itemName += "...";
        }

        context.drawString(
            textRenderer,
            Component.literal(itemName),
            overlayX + PADDING,
            currentY,
            textColor,
            false
        );

        // Draw quantity
        currentY += LINE_HEIGHT;
        context.drawString(
            textRenderer,
            Component.literal("Qty: " + QUANTITY_FORMAT.format(order.quantity)),
            overlayX + PADDING,
            currentY,
            textSecondaryColor,
            false
        );

        // Draw price per unit (right aligned on same line)
        String priceText = PRICE_FORMAT.format(order.pricePerUnit) + " coins";
        int priceWidth = textRenderer.width(priceText);
        context.drawString(
            textRenderer,
            Component.literal(priceText),
            overlayX + OVERLAY_WIDTH - PADDING - priceWidth,
            currentY,
            textSecondaryColor,
            false
        );

        // Draw separator line between orders
        currentY += LINE_HEIGHT + 3;
        context.fill(
            overlayX + PADDING,
            currentY,
            overlayX + OVERLAY_WIDTH - PADDING,
            currentY + 1,
            borderColor
        );

        return currentY + 4;
    }

    private static boolean shouldShowOverlay() {
        if (!ConfigManager.getConfig().utilities.bazaarTracker.enabled) {
            return false;
        }

        String title = BazzarTracker.currentInventoryTitle;
        if (title == null) {
            return false;
        }

        // Show in Bazaar menu, order screens, and product screens
        return title.contains("Bazaar");
    }

    private static String getStatusText(BazzarTracker.OrderStatus status) {
        switch (status) {
            case SEARCHING:
                return "SEARCHING";
            case BEST:
                return "BEST";
            case MATCHED:
                return "MATCHED";
            case OUTDATED:
                return "OUTDATED";
            default:
                return "UNKNOWN";
        }
    }

    private static int getStatusColor(BazzarTracker.OrderStatus status,
                                     int successColor, int secondaryColor, int errorColor) {
        switch (status) {
            case SEARCHING:
                return secondaryColor;
            case BEST:
                return successColor;
            case MATCHED:
                return successColor;
            case OUTDATED:
                return errorColor;
            default:
                return secondaryColor;
        }
    }

    private static void drawBorder(GuiGraphics context, int x, int y, int width, int height, int color) {
        // Top
        context.fill(x, y, x + width, y + 1, color);
        // Bottom
        context.fill(x, y + height - 1, x + width, y + height, color);
        // Left
        context.fill(x, y, x + 1, y + height, color);
        // Right
        context.fill(x + width - 1, y, x + width, y + height, color);
    }


    private static void renderScrollbar(GuiGraphics context, com.classictrashcode.suxaddons.client.config.Config.GuiTheme theme,
                                       int contentStartY, int contentHeight) {
        int scrollbarX = overlayX + OVERLAY_WIDTH - SCROLLBAR_WIDTH - 4;
        int scrollbarHeight = contentHeight - 10;
        int scrollbarY = contentStartY + 5;

        // Scrollbar track (subtle background)
        context.fill(scrollbarX, scrollbarY,
                    scrollbarX + SCROLLBAR_WIDTH, scrollbarY + scrollbarHeight,
                    theme.parseColor(theme.surfaceElevatedColor));

        // Scrollbar thumb
        if (maxScrollOffset > 0) {
            float scrollPercentage = (float) scrollOffset / maxScrollOffset;
            int totalContentHeight = maxScrollOffset + contentHeight;
            int thumbHeight = Math.max(20, (int) ((float) contentHeight / totalContentHeight * scrollbarHeight));
            int thumbY = scrollbarY + (int) ((scrollbarHeight - thumbHeight) * scrollPercentage);

            context.fill(scrollbarX, thumbY,
                        scrollbarX + SCROLLBAR_WIDTH, thumbY + thumbHeight,
                        theme.parseColor(theme.primaryColor));
        }
    }
    public static boolean handleMouseScroll(double mouseX, double mouseY, double verticalAmount) {
        // Only handle scroll if overlay is visible and mouse is over it
        if (!shouldShowOverlay()) {
            return false;
        }

        // Check if mouse is within overlay bounds
        if (mouseX >= overlayX && mouseX <= overlayX + OVERLAY_WIDTH &&
            mouseY >= overlayY && mouseY <= overlayY + overlayHeight) {
            if (maxScrollOffset > 0) {
                // Scroll by 20 pixels per scroll unit
                scrollOffset = (int) Math.max(0, Math.min(maxScrollOffset, scrollOffset - verticalAmount * 20));
                return true;
            }
        }


        return false;
    }
    public static boolean handleMouseClick(double mouseX, double mouseY) {
        // Only handle clicks if overlay is visible
        if (!shouldShowOverlay()) {
            return false;
        }

        // Check if any delete button was clicked
        for (DeleteButton button : deleteButtons) {
            if (button.isMouseOver(mouseX, mouseY)) {
                // Remove the order from the appropriate list
                if (button.isSellOrder) {
                    if (button.orderIndex >= 0 && button.orderIndex < BazzarTracker.sellOrders.size()) {
                        BazzarTracker.sellOrders.remove(button.orderIndex);
                    }
                } else {
                    if (button.orderIndex >= 0 && button.orderIndex < BazzarTracker.buyOrders.size()) {
                        BazzarTracker.buyOrders.remove(button.orderIndex);
                    }
                }

                // Adjust scroll if we're at the bottom and removed the last item
                int totalOrders = BazzarTracker.sellOrders.size() + BazzarTracker.buyOrders.size();
                int totalContentHeight = totalOrders * ORDER_CARD_HEIGHT;
                int newMaxScroll = Math.max(0, totalContentHeight - (overlayHeight - 22));
                if (scrollOffset > newMaxScroll) {
                    scrollOffset = newMaxScroll;
                }

                return true; // Click was handled
            }
        }

        return false; // Click was not on a delete button
    }
}
