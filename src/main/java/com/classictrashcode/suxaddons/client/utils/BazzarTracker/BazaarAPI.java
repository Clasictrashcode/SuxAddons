package com.classictrashcode.suxaddons.client.utils.BazzarTracker;

import com.classictrashcode.suxaddons.client.config.ConfigManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class BazaarAPI {
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final String BAZAAR_URL = "https://api.hypixel.net/v2/skyblock/bazaar";

    private static final Map<String, BazaarProduct> products = new HashMap<>();
    private static long lastUpdate = System.currentTimeMillis() ;
    private static long UPDATE_INTERVAL = ConfigManager.getConfig().utilities.bazaarTracker.updateInterval * 1000L;
    public static class BazaarProduct {
        public double instaBuyPrice;
        public double instaSellPrice;
        public double buyVolume;
        public double sellVolume;
        public int buyOrdersAsInt;
        public int sellOrdersAsInt;
        public List<BazaarOrder> buyOrders;
        public List<BazaarOrder> sellOrders;


        public BazaarProduct(double instaBuyPrice, double instaSellPrice, double buyVolume, double sellVolume,
                             int buyOrdersAsInt, int sellOrdersAsInt,
                             List<BazaarOrder> buyOrders, List<BazaarOrder> sellOrders) {
            this.instaBuyPrice = instaBuyPrice;
            this.instaSellPrice = instaSellPrice;
            this.buyVolume = buyVolume;
            this.sellVolume = sellVolume;
            this.buyOrdersAsInt = buyOrdersAsInt;
            this.sellOrdersAsInt = sellOrdersAsInt;
            this.buyOrders = buyOrders;
            this.sellOrders = sellOrders;
        }
    }
    public static class BazaarOrder {
        public int amount;
        public double pricePerUnit;
        public int orders;

        public BazaarOrder(int amount, double pricePerUnit, int orders) {
            this.amount = amount;
            this.pricePerUnit = pricePerUnit;
            this.orders = orders;
        }
    }


    public static CompletableFuture<Void> updateData() {
        long now = System.currentTimeMillis();
        if (now - lastUpdate < UPDATE_INTERVAL) {
            return CompletableFuture.completedFuture(null);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BAZAAR_URL))
                .GET()
                .build();

        UPDATE_INTERVAL = ConfigManager.getConfig().utilities.bazaarTracker.updateInterval * 1000L;

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    try {
                        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                        JsonObject productsJson = json.getAsJsonObject("products");

                        if (productsJson != null) {
                            products.clear();

                            for (String productId : productsJson.keySet()) {
                                JsonObject productData = productsJson.getAsJsonObject(productId);
                                JsonObject quickStatus = productData.getAsJsonObject("quick_status");

                                if (quickStatus != null) {
                                    double buyPrice = quickStatus.get("buyPrice").getAsDouble();
                                    double sellPrice = quickStatus.get("sellPrice").getAsDouble();
                                    double buyVolume = quickStatus.get("buyVolume").getAsDouble();
                                    double sellVolume = quickStatus.get("sellVolume").getAsDouble();
                                    int buyOrdersAsInt = quickStatus.get("buyOrders").getAsInt();
                                    int sellOrdersAsInt = quickStatus.get("sellOrders").getAsInt();
                                    JsonArray buyOrders = productData.getAsJsonArray("sell_summary");
                                    JsonArray sellOrders = productData.getAsJsonArray("buy_summary");
                                    List<BazaarOrder> buyOrderList = new java.util.ArrayList<>();
                                    for (JsonElement element : buyOrders) {
                                        JsonObject obj = element.getAsJsonObject();
                                        int amount = obj.get("amount").getAsInt();
                                        double pricePerUnit = obj.get("pricePerUnit").getAsDouble();
                                        int orders = obj.get("orders").getAsInt();
                                        buyOrderList.add(new BazaarOrder(amount, pricePerUnit, orders));
                                    }
                                    List<BazaarOrder> sellOrderList = new java.util.ArrayList<>();
                                    for (JsonElement element : sellOrders) {
                                        JsonObject obj = element.getAsJsonObject();
                                        int amount = obj.get("amount").getAsInt();
                                        double pricePerUnit = obj.get("pricePerUnit").getAsDouble();
                                        int orders = obj.get("orders").getAsInt();
                                        sellOrderList.add(new BazaarOrder(amount, pricePerUnit, orders));
                                    }
                                    products.put(productId, new BazaarProduct(buyPrice, sellPrice, buyVolume, sellVolume, buyOrdersAsInt, sellOrdersAsInt,buyOrderList,sellOrderList));
                                }
                            }

                            lastUpdate = now;

                            if (ConfigManager.getConfig().debug.debugMode) {
                                System.out.println("[BazaarAPI] Updated " + products.size() + " products");
                            }
                        }
                    } catch (Exception e) {
                        if (ConfigManager.getConfig().debug.debugMode) {
                            System.out.println("[BazaarAPI] Failed to parse bazaar data: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                });
    }

    public static BazaarProduct getProduct(String productId) {
        return products.get(productId);
    }

    public static double getInstaBuyPrice(String productId) {
        BazaarProduct product = products.get(productId);
        return product != null ? product.instaBuyPrice : 0;
    }

    public static double getInstaSellPrice(String productId) {
        BazaarProduct product = products.get(productId);
        return product != null ? product.instaSellPrice : 0;
    }

    public static boolean hasProduct(String productId) {
        return products.containsKey(productId);
    }

    public static Map<String, BazaarProduct> getAllProducts() {
        return new HashMap<>(products);
    }

    public static long getLastUpdateTime() {
        return lastUpdate;
    }

    public static boolean needsUpdate() {
        return System.currentTimeMillis() - lastUpdate >= UPDATE_INTERVAL;
    }
}
