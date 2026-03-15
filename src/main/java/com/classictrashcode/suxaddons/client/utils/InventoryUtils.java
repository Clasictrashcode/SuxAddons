package com.classictrashcode.suxaddons.client.utils;

import com.classictrashcode.suxaddons.client.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;


public class InventoryUtils {

    public static void clickSlot(int slot) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null) return;
        client.gameMode.handleInventoryMouseClick(
                client.player.containerMenu.containerId,
                slot,
                0,
                ClickType.PICKUP,
                client.player
        );
    }

    public static InteractionResult useHand() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null) return InteractionResult.FAIL;
        InteractionHand hand = client.player.getUsedItemHand();
        return client.gameMode.useItem(client.player, hand);
    }

    public static void switchSlot(Minecraft client, Inventory inv, int targetSlot) {
        if (targetSlot < 0 || targetSlot >= inv.getContainerSize()) {
            Logger.DebugLog("InventoryUtils", "Invalid target slot: " + targetSlot);
            return;
        }
        if (client.player == null) return;
        Logger.DebugLog("InventoryUtils", "Switching to slot " + targetSlot);
        client.execute(() -> {
            client.player.getInventory().setSelectedSlot(targetSlot);
        });
    }
}