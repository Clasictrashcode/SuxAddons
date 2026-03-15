package com.classictrashcode.suxaddons.client.utils;


import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedList;
import java.util.Queue;

public class ChatUtils {
    private static final Queue<String> messageQueue = new LinkedList<>();
    private static int cooldown = 0; // ticks remaining until next message allowed
    private static final int MESSAGE_DELAY = 20; // 20 ticks = 1 second

    public static void queueMessage(String message) {
        if (message != null && !message.isEmpty()) {
            messageQueue.offer(message);
        }
    }

    public static void queueMessages(String... messages) {
        for (String message : messages) {
            queueMessage(message);
        }
    }

    public static void tick() {
        if (cooldown > 0) {
            cooldown--;
        }

        Minecraft client = Minecraft.getInstance();
        Player player = client.player;

        if (player == null || messageQueue.isEmpty()) {
            return;
        }

        if (cooldown == 0) {
            String message = messageQueue.poll();
            if (message != null) {
                client.getConnection().sendChat(message);
                cooldown = MESSAGE_DELAY;
            }
        }
    }

    public static void clearQueue() {
        messageQueue.clear();
        cooldown = 0;
    }

    public static int getQueueSize() {
        return messageQueue.size();
    }

    public static boolean hasQueuedMessages() {
        return !messageQueue.isEmpty();
    }
}
