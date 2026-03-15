package com.classictrashcode.suxaddons.client.utils.BazzarTracker;

import com.classictrashcode.suxaddons.client.Logger;
import com.classictrashcode.suxaddons.client.SoundAlert;
import com.classictrashcode.suxaddons.client.SoundManager;
import com.classictrashcode.suxaddons.client.config.Config;
import com.classictrashcode.suxaddons.client.config.ConfigManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BazzarTracker {
    public static String currentInventoryTitle;
    private static int tickCounter = 0;
    private static Order selectedOrder = null;
    private static String selectedOrderType = null;
    private static Map<String, String> nameToIdMap;
    static {
        loadBaazarConversion();
    }
    public enum OrderStatus {
        SEARCHING,  // Initial state, waiting for first API check
        BEST,       // Best price on the market (no competition)
        MATCHED,    // Matched with other orders at the same price
        OUTDATED    // No longer competitive (worse price than market)
    }

    public static List<Order> sellOrders = new ArrayList<>();
    public static List<Order> buyOrders = new ArrayList<>();

    public static class Order {
        public int quantity;
        public String itemName;
        public String itemId;
        public OrderStatus status;
        public double pricePerUnit;
        public transient boolean dirty = false; // Used for screen reconciliation

        public Order(int quantity, String itemName, double pricePerUnit) {
            this.quantity = quantity;
            this.itemName = itemName;
            this.itemId = getItemIdFromName(itemName);
            this.status = OrderStatus.SEARCHING;
            this.pricePerUnit = pricePerUnit;
        }
        public boolean equalsIdentity(Order other) {
            if (other == null) return false;
            if (this == other) return true;

            return this.quantity == other.quantity
                    && Double.compare(this.pricePerUnit, other.pricePerUnit) == 0
                    && Objects.equals(this.itemName, other.itemName)
                    && Objects.equals(this.itemId, other.itemId);
        }
    }

    public static void onSlotClick(int slotIndex) {
        if (!ConfigManager.getConfig().utilities.bazaarTracker.enabled) return;
        if (Minecraft.getInstance().player == null) return;

        AbstractContainerMenu handler = Minecraft.getInstance().player.containerMenu;
        if (currentInventoryTitle == null) return;

        // Handle confirming new orders
        if (currentInventoryTitle.contains("Confirm") && (currentInventoryTitle.contains("Offer") || currentInventoryTitle.contains("Order"))) {
            if (slotIndex != 13) return;

            try {
                ItemStack confirmStack = handler.getSlot(13).getItem();
                if (confirmStack.isEmpty()) return;

                List<Component> itemLore = confirmStack.getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.ADVANCED);
                int quantity = 0;
                String productName = "";
                double pricePerUnit = 0.0;
                String itemName = confirmStack.getDisplayName().getString();

                // Parse order details from tooltip
                for (Component line : itemLore) {
                    String lineStr = line.getString();

                    if (lineStr.startsWith("Price per unit:")) {
                        String[] parts = lineStr.split(" ");
                        if (parts.length >= 4) {
                            pricePerUnit = parseDouble(parts[3].replace(",", ""));
                        }
                    }

                    if (lineStr.startsWith("Selling:")) {
                        Pattern pattern = Pattern.compile("Selling:\\s+(\\d+)x\\s+(.+)");
                        Matcher matcher = pattern.matcher(lineStr);
                        if (matcher.find()) {
                            quantity = parseInt(matcher.group(1));
                            productName = matcher.group(2).trim();
                        }
                    }

                    if (lineStr.startsWith("Order:")) {
                        Pattern pattern = Pattern.compile("Order:\\s+(\\d+)x\\s+(.+)");
                        Matcher matcher = pattern.matcher(lineStr);
                        if (matcher.find()) {
                            quantity = parseInt(matcher.group(1));
                            productName = matcher.group(2).trim();
                        }
                    }
                }

                // Validate parsed data
                if (quantity <= 0 || productName.isEmpty() || pricePerUnit <= 0) {
                    debugLog("Failed to parse order details from confirmation screen");
                    return;
                }
                productName = stripEmojis(productName);
                Order newOrder = new Order(quantity, productName, pricePerUnit);

                if (itemName.contains("Buy")) {
                    buyOrders.add(newOrder);
                    Logger.inGameLog(
                        "BazaarTracker",
                        "§aAdded buy order: " + quantity + "x §f" + productName + "§a at §6" + pricePerUnit + " coins/unit§a.");
                } else {
                    sellOrders.add(newOrder);
                    Logger.inGameLog(
                        "BazaarTracker",
                        "§aAdded sell order: " + quantity + "x §f" + productName + "§a at §6" + pricePerUnit + " coins/unit§a.");
                }
            } catch (Exception e) {
                debugLog("Error tracking new order: " + e.getMessage());
                if (ConfigManager.getConfig().debug.debugMode) {
                    e.printStackTrace();
                }
            }
        }

        // Handle selecting orders for removal in Bazaar Orders screen
        if (currentInventoryTitle.contains("Bazaar Orders")) {
            try {
                ItemStack orderStack = handler.getSlot(slotIndex).getItem();
                if (orderStack.isEmpty()) return;

                List<Component> itemLore = orderStack.getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.ADVANCED);
                int quantity = 0;
                double pricePerUnit = 0.0;

                // Parse order details from tooltip
                for (Component line : itemLore) {
                    String lineStr = line.getString();

                    if (lineStr.startsWith("Price per unit:")) {
                        String[] parts = lineStr.split(" ");
                        if (parts.length >= 4) {
                            pricePerUnit = parseDouble(parts[3].replace(",", ""));
                        }
                    }

                    if (lineStr.startsWith("Offer amount:")) {
                        Pattern pattern = Pattern.compile("Offer amount:\\s+([\\d,]+)x");
                        Matcher matcher = pattern.matcher(lineStr);
                        if (matcher.find()) {
                            quantity = parseInt(matcher.group(1).replace(",", ""));
                        }
                    }
                    if (lineStr.startsWith("Order amount:")) {
                        Pattern pattern = Pattern.compile("Order amount:\\s+([\\d,]+)x");
                        Matcher matcher = pattern.matcher(lineStr);
                        if (matcher.find()) {
                            quantity = parseInt(matcher.group(1).replace(",", ""));
                        }
                    }
                }


                String orderName = orderStack.getDisplayName().getString();
                String productName = stripEmojis(orderName.replaceFirst("^\\S+\\s*", "")); // Remove "SELL " or "BUY " and emojis

                if (quantity <= 0 || productName.isEmpty() || pricePerUnit <= 0) {
                    debugLog("Failed to parse order details from Bazaar Orders screen");
                    return;
                }

                // Select order for removal based on type
                if (orderName.contains("SELL ")) {
                    Order matchedOrder = findMatchingOrder(sellOrders, productName, quantity, pricePerUnit);
                    if (matchedOrder != null) {
                        debugLog("§eSelected to be removed sell order: " + quantity + "x §f" + productName + "§e at §6" + pricePerUnit + " coins/unit§e.");
                        selectedOrder = matchedOrder;
                        selectedOrderType = "SELL";
                    }
                } else if (orderName.contains("BUY ")) {
                    Order matchedOrder = findMatchingOrder(buyOrders, productName, quantity, pricePerUnit);
                    if (matchedOrder != null) {
                        Logger.inGameLog(
                            "BazaarTracker",
                            "§eSelected to be removed buy order: " + quantity + "x §f" + productName + "§e at §6" + pricePerUnit + " coins/unit§e."
                        );
                        selectedOrder = matchedOrder;
                        selectedOrderType = "BUY";
                    }
                }
            } catch (Exception e) {
                debugLog("Error selecting order for removal: " + e.getMessage());
                if (ConfigManager.getConfig().debug.debugMode) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static Order findMatchingOrder(List<Order> orders, String itemName, int quantity, double pricePerUnit) {
        for (Order order : orders) {
            if (order.itemName.equals(itemName) &&
                order.quantity == quantity &&
                Math.abs(order.pricePerUnit - pricePerUnit) < 0.01) { // Use tolerance for double comparison
                return order;
            }
        }
        return null;
    }
    private static void loadBaazarConversion() {
        try {
            InputStream is = BazzarTracker.class.getClassLoader().getResourceAsStream("BazaarConversion.json");
            if (is == null) {
                debugLog("BaazarConversion.json not found in resources, attempting to load from project directory");
                // fallback to file in project (adjust path as needed)
                Path path = Path.of("src", "main", "resources", "BazaarConversion.json");
                is = Files.newInputStream(path);
            }

            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> raw = gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), type);

            // normalize keys to lowercase for case-insensitive lookup
            nameToIdMap = raw.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            //log the Size of the loaded map
            debugLog("Loaded BaazarConversion.json with " + nameToIdMap.size() + " entries");
        } catch (Exception e) {
            // handle logging / fallback
            e.printStackTrace();
            nameToIdMap = Map.of();// empty map as fallback
            debugLog("Failed to load BaazarConversion.json: " + e.getMessage());
        }
    }
    private static String stripEmojis(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Remove emoji characters (includes symbols like ❁, ✦, etc.)
        // This regex keeps: letters, digits, spaces, hyphens, apostrophes, and basic punctuation
        String cleaned = text.replaceAll("[^\\p{L}\\p{N}\\s'-]", "").trim();

        // Normalize multiple spaces to single space
        cleaned = cleaned.replaceAll("\\s+", " ");

        debugLog("Stripped emojis: '" + text + "' -> '" + cleaned + "'");
        return cleaned;
    }

    public static String getItemIdFromName(String itemName) {
        if (itemName == null || itemName.isEmpty()) {
            debugLog("Empty or null item name provided");
            return null;
        }

        // Trim whitespace, strip emojis, and normalize the item name
        String normalizedName = stripEmojis(itemName.trim());

        debugLog("Looking up item name '" + normalizedName + "'");

        // Direct lookup first
        if (nameToIdMap.containsKey(normalizedName)) {
            String key = nameToIdMap.get(normalizedName);
            debugLog("Found direct match: " + key);
            return key;
        }

        // Case-insensitive fallback
        for (Map.Entry<String, String> entry : nameToIdMap.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(normalizedName)) {
                String key = entry.getValue();
                debugLog("Found case-insensitive match: " + key);
                return key;
            }
        }

        debugLog("Item name '" + normalizedName + "' not found in conversion map");
        return null;
    }

    public static void onMessage(String message) {
        if (!ConfigManager.getConfig().utilities.bazaarTracker.enabled) return;
        if (!message.startsWith("[Bazaar]")) return;

        try {
            // Handle cancelled orders
            if (message.contains("Cancelled")) {
                if (selectedOrder != null && selectedOrderType != null) {
                    boolean removed = false;

                    if (selectedOrderType.equals("SELL")) {
                        removed = sellOrders.remove(selectedOrder);
                        if (removed) {
                            Logger.inGameLog(
                                "BazaarTracker",
                                "§cRemoved sell order: " + selectedOrder.quantity + "x §f" + selectedOrder.itemName +
                                "§c at §6" + selectedOrder.pricePerUnit + " coins/unit§c.");
                        }
                    } else if (selectedOrderType.equals("BUY")) {
                        removed = buyOrders.remove(selectedOrder);
                        if (removed) {
                            Logger.inGameLog(
                                "BazaarTracker",
                                "§cRemoved buy order: " + selectedOrder.quantity + "x §f" + selectedOrder.itemName +
                                "§c at §6" + selectedOrder.pricePerUnit + " coins/unit§c.");
                        }
                    }

                    if (!removed) {
                        debugLog("Failed to remove selected order - order not found in list");
                    }

                    selectedOrder = null;
                    selectedOrderType = null;
                }
                return;
            }

            // Handle filled sell orders
            if (message.contains("Your Sell Offer for") && message.contains("was filled!")) {
                Pattern pattern = Pattern.compile("for (\\d+)x (.+) was filled!");
                Matcher matcher = pattern.matcher(message);
                if (matcher.find()) {
                    int amount = parseInt(matcher.group(1));
                    String itemName = stripEmojis(matcher.group(2).trim());
                    String itemId = getItemIdFromName(itemName);

                    // Remove the first matching order (FIFO - first in, first out)
                    boolean removed = sellOrders.removeIf(order ->
                        order.itemId.equals(itemId) && order.quantity == amount
                    );

                    if (removed) {
                        Logger.inGameLog(
                            "BazaarTracker",
                            "§cSell order filled and removed: " + amount + "x §f" + itemName + "§c.");
                    } else {
                        debugLog("Received fill notification for untracked sell order: " + amount + "x " + itemName);
                    }
                }
            }

            // Handle filled buy orders
            if (message.contains("Your Buy Order for") && message.contains("was filled!")) {
                Pattern pattern = Pattern.compile("for (\\d+)x (.+) was filled!");
                Matcher matcher = pattern.matcher(message);
                if (matcher.find()) {
                    int amount = parseInt(matcher.group(1));
                    String itemName = stripEmojis(matcher.group(2).trim());
                    String itemId = getItemIdFromName(itemName);

                    // Remove the first matching order (FIFO - first in, first out)
                    boolean removed = buyOrders.removeIf(order ->
                        order.itemId.equals(itemId) && order.quantity == amount
                    );

                    if (removed) {
                        Logger.inGameLog(
                            "BazaarTracker",
                            "§cBuy order filled and removed: " + amount + "x §f" + itemName + "§c.");
                    } else {
                        debugLog("Received fill notification for untracked buy order: " + amount + "x " + itemName);
                    }
                }
            }
        } catch (Exception e) {
            debugLog("Error processing bazaar message: " + e.getMessage());
            if (ConfigManager.getConfig().debug.debugMode) {
                e.printStackTrace();
            }
        }
    }

    public static void tick() {
        if (!ConfigManager.getConfig().utilities.bazaarTracker.enabled) return;
        currentInventoryTitle = Minecraft.getInstance().screen != null ? Minecraft.getInstance().screen.getTitle().getString():"";
        tickCounter++;
        if (tickCounter%5 == 0 &&
            Minecraft.getInstance().screen != null &&
                Minecraft.getInstance().screen.getTitle().getString().contains("Bazaar Orders")) {
            // Refresh Orders from Screen every 5 ticks when in Bazaar Orders Screen using dirty flag pattern
            assert Minecraft.getInstance().player != null;
            AbstractContainerMenu screenHandler = Minecraft.getInstance().player.containerMenu;

            // Mark all tracked orders as dirty before parsing screen
            sellOrders.forEach(order -> order.dirty = true);
            buyOrders.forEach(order -> order.dirty = true);

            // Parse screen orders and mark matching tracker orders as not dirty
            for (Slot slot : screenHandler.slots) {
                ItemStack stack = slot.getItem();
                if (stack.isEmpty() || !(stack.getDisplayName().getString().contains("SELL") || stack.getDisplayName().getString().contains("BUY"))) {
                    continue;
                }

                List<Component> itemLore = stack.getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.ADVANCED);
                int quantity = 0;
                String productName = stripEmojis(stack.getDisplayName().getString().replaceFirst("^\\S+\\s*", ""));
                double pricePerUnit = 0.0;
                boolean isFilled = false;

                // Parse order details from tooltip
                for (Component line : itemLore) {
                    String lineStr = line.getString();

                    if (lineStr.startsWith("Price per unit:")) {
                        String[] parts = lineStr.split(" ");
                        if (parts.length >= 4) {
                            pricePerUnit = parseDouble(parts[3].replace(",", ""));
                        }
                    }

                    if (lineStr.startsWith("Offer amount:")) {
                        Pattern pattern = Pattern.compile("Offer amount:\\s+([\\d,]+)x");
                        Matcher matcher = pattern.matcher(lineStr);
                        if (matcher.find()) {
                            quantity = parseInt(matcher.group(1).replace(",", ""));
                        }
                    }

                    if (lineStr.startsWith("Order amount:")) {
                        Pattern pattern = Pattern.compile("Order amount:\\s+([\\d,]+)x");
                        Matcher matcher = pattern.matcher(lineStr);
                        if (matcher.find()) {
                            quantity = parseInt(matcher.group(1).replace(",", ""));
                        }
                    }

                    if (lineStr.startsWith("Filled:") && lineStr.contains("100%")) {
                        isFilled = true;
                        debugLog("Detected filled order: " + lineStr);
                    }
                }

                if (quantity <= 0 || productName.isEmpty() || pricePerUnit <= 0) {
                    debugLog("Failed to parse order details from Bazaar Orders screen");
                    continue;
                }

                // Skip filled orders
                if (isFilled) {
                    debugLog("Skipping filled order for: " + productName);
                    continue;
                }

                final int finalQuantity = quantity;
                final String finalProductName = productName;
                final double finalPricePerUnit = pricePerUnit;

                if (stack.getDisplayName().getString().contains("SELL ")) {
                    Order matchingOrder = sellOrders.stream()
                        .filter(o -> o.dirty && o.itemName.equals(finalProductName)
                            && o.quantity == finalQuantity
                            && Math.abs(o.pricePerUnit - finalPricePerUnit) < 0.01)
                        .findFirst()
                        .orElse(null);

                    if (matchingOrder != null) {
                        matchingOrder.dirty = false; // Mark as found
                    } else {
                        // New order not in tracker - add it
                        Order newOrder = new Order(finalQuantity, finalProductName, finalPricePerUnit);
                        newOrder.dirty = false;
                        sellOrders.add(newOrder);
                        debugLog("Added new sell order from screen: " + finalQuantity + "x " + finalProductName + " at " + finalPricePerUnit);
                    }
                } else if (stack.getDisplayName().getString().contains("BUY ")) {
                    Order matchingOrder = buyOrders.stream()
                        .filter(o -> o.dirty && o.itemName.equals(finalProductName)
                            && o.quantity == finalQuantity
                            && Math.abs(o.pricePerUnit - finalPricePerUnit) < 0.01)
                        .findFirst()
                        .orElse(null);

                    if (matchingOrder != null) {
                        matchingOrder.dirty = false; // Mark as found
                    } else {
                        // New order not in tracker - add it
                        Order newOrder = new Order(finalQuantity, finalProductName, finalPricePerUnit);
                        newOrder.dirty = false;
                        buyOrders.add(newOrder);
                        debugLog("Added new buy order from screen: " + finalQuantity + "x " + finalProductName + " at " + finalPricePerUnit);
                    }
                }
            }

            int removedSell = (int) sellOrders.stream().filter(o -> o.dirty).count();
            int removedBuy = (int) buyOrders.stream().filter(o -> o.dirty).count();
            sellOrders.removeIf(order -> order.dirty);
            buyOrders.removeIf(order -> order.dirty);

            if (removedSell > 0 || removedBuy > 0) {
                debugLog("Removed " + removedSell + " sell orders and " + removedBuy + " buy orders no longer on screen");
            }
        }
        if (tickCounter < 20 * ConfigManager.getConfig().utilities.bazaarTracker.updateInterval) return;
        tickCounter = 0;

        try {
            BazaarAPI.updateData();

            // Process both sell and buy orders
            processOrders(sellOrders, true);
            processOrders(buyOrders, false);
        } catch (Exception e) {
            debugLog("Error in tick processing: " + e.getMessage());
            if (ConfigManager.getConfig().debug.debugMode) {
                e.printStackTrace();
            }
        }
    }

    private static void processOrders(List<Order> orders, boolean isSellOrder) {
        Iterator<Order> iterator = orders.iterator();
        String orderType = isSellOrder ? "sell" : "buy";

        while (iterator.hasNext()) {
            Order order = iterator.next();

            try {
                debugLog("Checking " + orderType + " order: " + order.itemName +
                        " (" + order.itemId + "), qty=" + order.quantity + ", price/unit=" + order.pricePerUnit);

                BazaarAPI.BazaarProduct product = BazaarAPI.getProduct(order.itemId);

                // Remove orders for products that don't exist in the API
                if (product == null) {
                    iterator.remove();
                    Logger.inGameLog(
                        "BazaarTracker",
                        "§cRemoved " + orderType + " order for unknown itemID: §f" + order.itemId + "§c.");
                    debugLog("Product not found for itemID: " + order.itemId);
                    continue;
                }

                // Get the appropriate order list based on order type
                List<BazaarAPI.BazaarOrder> bazaarOrders = isSellOrder ? product.sellOrders : product.buyOrders;

                // Remove orders if there are no matching orders in the bazaar
                if (bazaarOrders == null || bazaarOrders.isEmpty()) {
                    iterator.remove();
                    Logger.inGameLog(
                        "BazaarTracker",
                        "§cRemoved " + orderType + " order for §f" + order.itemId + "§c - no " + orderType + " orders on market.");
                    debugLog("No " + orderType + " orders for itemID: " + order.itemId);
                    continue;
                }

                BazaarAPI.BazaarOrder bestOrder = bazaarOrders.getFirst();

                debugLog("Best order price/unit: " + bestOrder.pricePerUnit + ", my price/unit: " + order.pricePerUnit);

                // Determine if order is outdated:
                // For sell orders: outdated if best price is LOWER than ours (market undercut us)
                // For buy orders: outdated if best price is HIGHER than ours (market outbid us)
                boolean isOutdated = isSellOrder ?
                        (bestOrder.pricePerUnit < order.pricePerUnit) :
                        (bestOrder.pricePerUnit > order.pricePerUnit);

                updateOrderStatus(order, bestOrder, isOutdated, orderType);

            } catch (Exception e) {
                debugLog("Error processing order " + order.itemName + ": " + e.getMessage());
                if (ConfigManager.getConfig().debug.debugMode) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void updateOrderStatus(Order order, BazaarAPI.BazaarOrder bestOrder, boolean isOutdated, String orderType) {
        OrderStatus previousStatus = order.status;
        Config.BazaarTracker config = ConfigManager.getConfig().utilities.bazaarTracker;

        if (isOutdated) {
            // Order is no longer competitive
            if (order.status != OrderStatus.OUTDATED) {
                order.status = OrderStatus.OUTDATED;
                Logger.inGameLog(
                    "BazaarTracker",
                    "§cYour " + orderType + " order for " + order.quantity + "x §f" + order.itemName +
                            "§c at §6" + order.pricePerUnit + " coins/unit§c is outdated. Best price: §6" +
                            bestOrder.pricePerUnit + " coins/unit§c.");
                // Play OUTDATED sound alert
                playSoundAlert(config.soundAlertOutdatedEnabled, config.soundTypeOutdated,
                        config.soundVolumeOutdated, config.soundPitchOutdated);
            }
        } else {
            // Order is competitive - determine if BEST or MATCHED
            boolean hasMultipleOrders = bestOrder.orders > 1;

            if (hasMultipleOrders) {
                // Multiple orders at the best price (including ours)
                if (order.status != OrderStatus.MATCHED && order.pricePerUnit == bestOrder.pricePerUnit) {
                    order.status = OrderStatus.MATCHED;
                    Logger.inGameLog(
                        "BazaarTracker",
                        "§eYour " + orderType + " order for " + order.quantity + "x §f" + order.itemName +
                                "§e at §6" + order.pricePerUnit + " coins/unit§e is matched with §6" + bestOrder.orders +
                                "§e orders at the same price.");
                    // Play MATCHED sound alert
                    playSoundAlert(config.soundAlertMatchedEnabled, config.soundTypeMatched,
                            config.soundVolumeMatched, config.soundPitchMatched);
                }
            } else {
                // Only one order at this price (ours)
                if (order.status != OrderStatus.BEST) {
                    order.status = OrderStatus.BEST;

                    // Customize message based on previous status
                    String statusMessage;
                    if (previousStatus == OrderStatus.MATCHED) {
                        statusMessage = "§aYour " + orderType + " order for " + order.quantity + "x §f" + order.itemName +
                                "§a at §6" + order.pricePerUnit + " coins/unit§a is no longer matched with other orders.";
                    } else if (previousStatus == OrderStatus.OUTDATED) {
                        statusMessage = "§aYour " + orderType + " order for " + order.quantity + "x §f" + order.itemName +
                                "§a at §6" + order.pricePerUnit + " coins/unit§a is now competitive again.";
                    } else {
                        statusMessage = "§aYour " + orderType + " order for " + order.quantity + "x §f" + order.itemName +
                                "§a at §6" + order.pricePerUnit + " coins/unit§a is the best on the market.";
                    }

                    Logger.inGameLog("BazaarTracker", statusMessage);
                    // Play BEST sound alert
                    playSoundAlert(config.soundAlertBestEnabled, config.soundTypeBest,
                            config.soundVolumeBest, config.soundPitchBest);
                }
            }
        }
    }
    private static void playSoundAlert(boolean enabled, String soundTypeName, double volume, double pitch) {
        if (!enabled) return;

        float vol = (float) volume;
        float pit = (float) pitch;

        // Check if it's a custom sound or built-in
        if (SoundAlert.isCustomSound(soundTypeName)) {
            // Play custom sound by name
            SoundManager.playCustomSound(soundTypeName, vol, pit);
        } else {
            // Play built-in sound
            SoundAlert soundAlert = SoundAlert.fromDisplayName(soundTypeName);
            SoundManager.playSound(soundAlert, vol, pit);
        }
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            debugLog("Failed to parse integer: " + value);
            return 0;
        }
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            debugLog("Failed to parse double: " + value);
            return 0.0;
        }
    }

    private static void debugLog(String message) {
        Logger.DebugLog("BazaarTracker",message);
    }
}