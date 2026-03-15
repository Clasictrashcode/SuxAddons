package com.classictrashcode.suxaddons.client;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * Enum representing available sound alerts for various mod features.
 * Provides built-in Minecraft sounds and support for custom sounds.
 */
public enum SoundAlert {
    NONE("None", null),
    LEVEL_UP("Level Up", SoundEvents.PLAYER_LEVELUP),
    EXPERIENCE_ORB("Experience Orb", SoundEvents.EXPERIENCE_ORB_PICKUP),
    NOTE_PLING("Note Pling", SoundEvents.NOTE_BLOCK_PLING.value()),
    NOTE_BELL("Note Bell", SoundEvents.NOTE_BLOCK_BELL.value()),
    NOTE_CHIME("Note Chime", SoundEvents.NOTE_BLOCK_CHIME.value()),
    BELL("Village Bell", SoundEvents.BELL_BLOCK),
    ANVIL_LAND("Anvil Land", SoundEvents.ANVIL_LAND),
    CHEST_OPEN("Chest Open", SoundEvents.CHEST_OPEN),
    ARROW_HIT("Arrow Hit", SoundEvents.ARROW_HIT_PLAYER),
    ENDERMAN_TELEPORT("Enderman Teleport", SoundEvents.ENDERMAN_TELEPORT),
    FIREWORK_LAUNCH("Firework Launch", SoundEvents.FIREWORK_ROCKET_LAUNCH),
    FIREWORK_BLAST("Firework Blast", SoundEvents.FIREWORK_ROCKET_BLAST),
    TOTEM_USE("Totem of Undying", SoundEvents.TOTEM_USE),
    WITHER_SPAWN("Wither Spawn", SoundEvents.WITHER_SPAWN),
    DRAGON_GROWL("Ender Dragon Growl", SoundEvents.ENDER_DRAGON_GROWL);

    private final String displayName;
    private final SoundEvent soundEvent;

    SoundAlert(String displayName, SoundEvent soundEvent) {
        this.displayName = displayName;
        this.soundEvent = soundEvent;
    }

    /**
     * Gets the display name for the dropdown selector
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the Minecraft SoundEvent (null for NONE or CUSTOM)
     */
    public SoundEvent getSoundEvent() {
        return soundEvent;
    }

    /**
     * Checks if this is a valid playable sound
     */
    public boolean hasSound() {
        return this != NONE;
    }

    /**
     * Gets enum value from display name (for dropdown selection)
     */
    public static SoundAlert fromDisplayName(String displayName) {
        for (SoundAlert alert : values()) {
            if (alert.displayName.equals(displayName)) {
                return alert;
            }
        }
        return NONE;
    }

    /**
     * Gets all display names for dropdown widget
     */
    public static String[] getDisplayNames() {
        SoundAlert[] values = values();
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].displayName;
        }
        return names;
    }

    /**
     * Gets all available sound names including custom sounds from SoundManager
     * @return List of all sound names (built-in + custom)
     */
    public static List<String> getAllSoundNames() {
        List<String> allSounds = new ArrayList<>();

        // Add all built-in sounds
        for (SoundAlert alert : values()) {
            allSounds.add(alert.displayName);
        }

        // Add all custom sounds
        List<String> customSounds = SoundManager.getCustomSoundNames();
        allSounds.addAll(customSounds);

        return allSounds;
    }

    /**
     * Checks if a given sound name is a custom sound (not a built-in enum)
     * @param soundName The display name to check
     * @return true if it's a custom sound, false if it's a built-in enum
     */
    public static boolean isCustomSound(String soundName) {
        for (SoundAlert alert : values()) {
            if (alert.displayName.equals(soundName)) {
                return false;
            }
        }
        return true;
    }
}
