package com.classictrashcode.suxaddons.client.hunting;


import com.classictrashcode.suxaddons.client.Logger;
import com.classictrashcode.suxaddons.client.SoundAlert;
import com.classictrashcode.suxaddons.client.SoundManager;
import com.classictrashcode.suxaddons.client.config.ConfigManager;
import com.classictrashcode.suxaddons.client.utils.InventoryUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class AutoFusion {
    public static boolean running = false;
    static int actionStep = 0; // Package-private for AutoFusionOverlay
    private static int stepTimer = 0;
    static int completedCycles = 0; // Package-private for AutoFusionOverlay
    public static boolean isFusionMenu = false;
    public static String currentInventoryTitle;
    private static int nullScreenTicks = 0;


    public static void tick(){
        if (Minecraft.getInstance().screen == null) {
            isFusionMenu = false;
            currentInventoryTitle = "";
            nullScreenTicks++;
            // Only reset after 10 consecutive ticks of null screen (grace period for lag)
            if (nullScreenTicks >= 10) {
                reset();
            }
            return;
        }
        // Screen is not null, reset the grace period counter
        nullScreenTicks = 0;
        if (Minecraft.getInstance().screen.getTitle().getString().contains("Fusion")) {
            isFusionMenu = true;
            currentInventoryTitle = Minecraft.getInstance().screen.getTitle().getString();
        } else {
            isFusionMenu = false;
            currentInventoryTitle = "";
        }
        if (!ConfigManager.getConfig().hunting.autoFusion.enabled || !isFusionMenu) {
            reset();
            return;
        }
        if (!running) return;
        processActionSequence();
    }
    public static void processActionSequence() {
        AbstractContainerMenu screenHandler = Minecraft.getInstance().player.containerMenu;
        stepTimer++;
        int actionDelayTicks = ConfigManager.getConfig().hunting.autoFusion.actionDelayTicks;
        int LongDelayTicks = ConfigManager.getConfig().hunting.autoFusion.longDelayTicks;
        switch (actionStep) {
            case 0:
                if (currentInventoryTitle.contains("Box") && stepTimer >= actionDelayTicks) {
                    if (!screenHandler.getSlot(47).getItem().getDisplayName().getString().contains("Repeat Previous Fusion")) {
                        reset();
                        break;
                    }
                    InventoryUtils.clickSlot(47);
                    stepTimer = 0;
                    actionStep++;
                }
                break;
            case 1:
                if (currentInventoryTitle.contains("Confirm") && stepTimer >= actionDelayTicks) {
                    if (!screenHandler.getSlot(33).getItem().getDisplayName().getString().contains("Fusion")) break;
                    InventoryUtils.clickSlot(33);
                    stepTimer = 0;
                    actionStep++;
                }
                break;
            case 2:
                if(stepTimer >= LongDelayTicks){
                    stepTimer = 0;
                    completedCycles++;
                    actionStep = 0;
                }
                break;
        }
    }
    public static void reset() {
        if (running){
            Logger.inGameLog("AutoFusion","Finished Fusion with "+completedCycles+" Completed!");

            // Play sound alert if enabled
            if (ConfigManager.getConfig().hunting.autoFusion.soundAlertEnabled) {
                String soundTypeName = ConfigManager.getConfig().hunting.autoFusion.soundType;
                float volume = (float) ConfigManager.getConfig().hunting.autoFusion.soundVolume;
                float pitch = (float) ConfigManager.getConfig().hunting.autoFusion.soundPitch;

                // Check if it's a custom sound or built-in
                if (SoundAlert.isCustomSound(soundTypeName)) {
                    // Play custom sound by name
                    SoundManager.playCustomSound(soundTypeName, volume, pitch);
                } else {
                    // Play built-in sound
                    SoundAlert soundAlert = SoundAlert.fromDisplayName(soundTypeName);
                    SoundManager.playSound(soundAlert, volume, pitch);
                }
            }
        }
        running = false;
        actionStep = 0;
        stepTimer = 0;
        completedCycles = 0;
        nullScreenTicks = 0;
    }
}
