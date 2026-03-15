package com.classictrashcode.suxaddons.client.utils;

import com.classictrashcode.suxaddons.client.SuxaddonsClient;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class UpdateChecker {

    private static final String RELEASES_API_URL =
            "https://api.github.com/repos/Clasictrashcode/SuxAddons/releases/latest";
    private static final String RELEASES_PAGE_URL =
            "https://github.com/Clasictrashcode/SuxAddons/releases/latest";

    private static boolean hasNotified = false;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void onJoin() {
        if (hasNotified) {
            return;
        }
        checkForUpdate();
    }

    private static void checkForUpdate() {
        CompletableFuture
                .supplyAsync(UpdateChecker::fetchLatestTag)
                .thenAccept(latestTag -> {
                    if (latestTag == null || latestTag.isEmpty()) {
                        return;
                    }

                    String currentVersion = getCurrentVersion();
                    if (currentVersion == null || currentVersion.isEmpty()) {
                        return;
                    }

                    if (isNewerVersion(latestTag, currentVersion)) {
                        Minecraft.getInstance().execute(() -> sendUpdateMessage(latestTag));
                        hasNotified = true;
                    }
                })
                .exceptionally(ex -> null);
    }

    private static String fetchLatestTag() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RELEASES_API_URL))
                    .header("User-Agent", "SuxAddons-UpdateChecker")
                    .GET()
                    .build();

            HttpResponse<String> response =
                    HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return null;
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            if (!json.has("tag_name")) {
                return null;
            }

            return json.get("tag_name").getAsString().replaceFirst("^v", "");

        } catch (Exception e) {
            return null;
        }
    }

    private static String getCurrentVersion() {
        return FabricLoader.getInstance()
                .getModContainer(SuxaddonsClient.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .map(v -> v.replaceFirst("^v", ""))
                .orElse(null);
    }
    
    static boolean isNewerVersion(String latestTag, String currentVersion) {
        String[] latestParts = latestTag.split("\\.");
        String[] currentParts = currentVersion.split("\\.");

        int maxLength = Math.max(latestParts.length, currentParts.length);

        for (int i = 0; i < maxLength; i++) {
            int latest  = parseVersionComponent(latestParts,  i);
            int current = parseVersionComponent(currentParts, i);

            if (latest > current) return true;
            if (latest < current) return false;
        }
        return false;
    }

    private static int parseVersionComponent(String[] parts, int index) {
        if (index >= parts.length) return 0;
        try {
            return Integer.parseInt(parts[index]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void sendUpdateMessage(String latestVersion) {
        Minecraft client = Minecraft.getInstance();
        if (client.gui == null) return;

        MutableComponent prefix = Component.literal("[SuxAddons] ");
        MutableComponent label = Component.literal("A new update is available: ")
                .withStyle(ChatFormatting.WHITE);

        ClickEvent.OpenUrl openUrl = new ClickEvent.OpenUrl(URI.create(RELEASES_PAGE_URL));

        MutableComponent versionLink = Component.literal(latestVersion + " ")
                .withStyle(style -> style
                        .withColor(ChatFormatting.GOLD)
                        .withUnderlined(true)
                        .withClickEvent(openUrl));

        MutableComponent hint = Component.literal("(click to download)")
                .withStyle(style -> style
                        .withColor(ChatFormatting.GRAY)
                        .withItalic(true)
                        .withClickEvent(openUrl));

        client.gui.getChat().addMessage(prefix.append(label).append(versionLink).append(hint));
    }
}