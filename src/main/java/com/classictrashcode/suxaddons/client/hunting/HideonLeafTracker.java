package com.classictrashcode.suxaddons.client.hunting;


import com.classictrashcode.suxaddons.client.config.ConfigManager;
import com.classictrashcode.suxaddons.client.utils.BazzarTracker.BazaarAPI;

public class HideonLeafTracker {
    private static final String HIDEONLEAF_SHARD_ID = "SHARD_HIDEONLEAF";

    public static int RESET_TIME = 2400;
    private static boolean isTracking = false;
    public static int NumberOfHideonleafsCaught = 0;
    public static int NumberOfHideonleafShardsCaught = 0;
    public static int trackedTime = 0;

    public static void tick(){
        if (!isTracking) return;

        if (trackedTime % 1200 == 0) { // Every minute
            BazaarAPI.updateData();
        }

        trackedTime++;
        RESET_TIME--;

        if(RESET_TIME <= 0){
            stopTracker();
        }
    }

    public static void onMessage(String message){
        if (message.contains("Hideonleaf lost the fight")){
            isTracking = true;
            NumberOfHideonleafsCaught++;
            RESET_TIME = 2400;
            if (ConfigManager.getConfig().debug.debugMode) {
                System.out.println("[HideonLeafTracker] Hideonleaf caught! Total: " + NumberOfHideonleafsCaught);
            }
        }

        if (message.contains("You caught") && message.contains("Hideonleaf Shard")) {
            if (ConfigManager.getConfig().debug.debugMode) {
                System.out.println("[HideonLeafTracker] Shard message detected: " + message);
            }

            java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                    "You caught\\s+(?:x(\\d+)|a|(\\d+))\\s+Hideonleaf\\s+Shard[s]?",
                    java.util.regex.Pattern.CASE_INSENSITIVE
            );
            java.util.regex.Matcher m = p.matcher(message);

            if (m.find()) {
                String xNum = m.group(1);
                String plainNum = m.group(2);
                try {
                    int amount;
                    if (xNum != null) amount = Integer.parseInt(xNum);
                    else if (plainNum != null) amount = Integer.parseInt(plainNum);
                    else amount = 1;

                    NumberOfHideonleafShardsCaught += amount;

                    if (ConfigManager.getConfig().debug.debugMode) {
                        System.out.println("[HideonLeafTracker] Parsed shard amount: " + amount + " | Total shards: " + NumberOfHideonleafShardsCaught);
                    }
                } catch (NumberFormatException e) {
                    if (ConfigManager.getConfig().debug.debugMode) {
                        System.out.println("[HideonLeafTracker] Failed to parse shard amount: " + e.getMessage());
                    }
                }
            } else {
                if (ConfigManager.getConfig().debug.debugMode) {
                    System.out.println("[HideonLeafTracker] Shard regex did not match");
                }
            }
        }
    }

    public static boolean isTracking() {
        return isTracking;
    }

    public static void stopTracker(){
        NumberOfHideonleafsCaught = 0;
        NumberOfHideonleafShardsCaught = 0;
        isTracking = false;
        RESET_TIME = 2400;
        trackedTime = 0;
    }

    public static double getTotalCoinsFromSell() {
        return NumberOfHideonleafShardsCaught * BazaarAPI.getInstaSellPrice(HIDEONLEAF_SHARD_ID);
    }

    public static double getTotalCoinsFromBuy() {
        return NumberOfHideonleafShardsCaught * BazaarAPI.getInstaBuyPrice(HIDEONLEAF_SHARD_ID);
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
