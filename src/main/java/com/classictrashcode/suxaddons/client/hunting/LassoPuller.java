package com.classictrashcode.suxaddons.client.hunting;

import com.classictrashcode.suxaddons.client.Logger;
import com.classictrashcode.suxaddons.client.config.Config;
import com.classictrashcode.suxaddons.client.config.ConfigManager;
import com.classictrashcode.suxaddons.client.utils.EntityUtils;
import com.classictrashcode.suxaddons.client.utils.InventoryUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Random;

public  class LassoPuller {
    private static int coolDown = ConfigManager.getConfig().hunting.autoLassoPuller.coolDownBetweenPulls;
    private static Random random = new Random();
    public static void tick(){
        if (!ConfigManager.getConfig().hunting.autoLassoPuller.enabled) return;
        if (coolDown >= 1) {
            coolDown--;
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        ItemStack heldItem = client.player.getMainHandItem();
        if (!heldItem.getItem().equals(Items.LEAD) || !heldItem.getDisplayName().getString().contains(" Lasso")) return;
        List<ArmorStand> ReelArmorStand = EntityUtils.getArmorStandsWithNameWithinDistance("REEL",16);
        if (ReelArmorStand.isEmpty()) return;
        coolDown = ConfigManager.getConfig().hunting.autoLassoPuller.coolDownBetweenPulls;
        int min = ConfigManager.getConfig().hunting.autoLassoPuller.minDelay;
        int max = ConfigManager.getConfig().hunting.autoLassoPuller.maxDelay;
        int delay = min + random.nextInt(max - min);
        InventoryUtils.useHandDelayed(delay);
    }
}
