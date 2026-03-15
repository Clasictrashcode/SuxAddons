package com.classictrashcode.suxaddons.client;



import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SoundManager {
    private static final String CUSTOM_SOUNDS_FOLDER = "config/suxaddons/sounds";
    private static final Map<String, File> customSoundFiles = new HashMap<>();
    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) return;

        // Create custom sounds directory if it doesn't exist
        File soundsDir = new File(CUSTOM_SOUNDS_FOLDER);
        if (!soundsDir.exists()) {
            soundsDir.mkdirs();
        }

        // Load custom sounds from the directory
        loadCustomSounds(soundsDir);
        initialized = true;
    }

    private static void loadCustomSounds(File soundsDir) {
        // Accept multiple audio formats
        File[] files = soundsDir.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".wav") || lower.endsWith(".ogg") ||
                   lower.endsWith(".aiff") || lower.endsWith(".au");
        });

        if (files == null || files.length == 0) {
            System.out.println("[SuxAddons] No custom sound files found in " + CUSTOM_SOUNDS_FOLDER);
            System.out.println("[SuxAddons] Supported formats: WAV (recommended), OGG, AIFF, AU");
            return;
        }

        System.out.println("[SuxAddons] Loading " + files.length + " custom sound(s) for direct playback:");

        for (File soundFile : files) {
            // Remove extension from filename
            String fileName = soundFile.getName();
            String soundName = fileName.substring(0, fileName.lastIndexOf('.'));
            String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toUpperCase();

            System.out.println("[SuxAddons]   - " + soundName + " [" + extension + "] (" + soundFile.length() + " bytes)");

            // Store the file reference for direct playback
            customSoundFiles.put(soundName, soundFile);
        }

        System.out.println("[SuxAddons] Custom sounds loaded successfully. They will be played directly from disk.");
        if (files.length > 0) {
            boolean hasOgg = false;
            for (File f : files) {
                if (f.getName().toLowerCase().endsWith(".ogg")) {
                    hasOgg = true;
                    break;
                }
            }
            if (hasOgg) {
                System.out.println("[SuxAddons] NOTE: OGG support depends on system codecs. If OGG sounds don't play, convert to WAV.");
            }
        }
    }


    public static void playSound(SoundAlert alert, float volume, float pitch) {
        if (alert == null || alert == SoundAlert.NONE) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        SoundEvent soundEvent = alert.getSoundEvent();

        if (soundEvent == null) {
            return;
        }

        // Play the sound at the player's position
        client.getSoundManager().play(
                SimpleSoundInstance.forUI(soundEvent, pitch, volume)
        );
    }

    public static void playSound(SoundAlert alert) {
        playSound(alert, 1.0f, 1.0f);
    }

    public static boolean hasCustomSounds() {
        return !customSoundFiles.isEmpty();
    }

    public static int getCustomSoundCount() {
        return customSoundFiles.size();
    }
    public static List<String> getCustomSoundNames() {
        return new ArrayList<>(customSoundFiles.keySet());
    }

    public static void playCustomSound(String soundName, float volume, float pitch) {
        File soundFile = customSoundFiles.get(soundName);
        if (soundFile == null) {
            System.err.println("[SuxAddons] Custom sound '" + soundName + "' not found");
            return;
        }

        if (!soundFile.exists()) {
            System.err.println("[SuxAddons] Custom sound file does not exist: " + soundFile.getAbsolutePath());
            return;
        }

        // Play the sound in a separate thread to avoid blocking
        new Thread(() -> {
            try {
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioInputStream);

                // Set volume (convert from 0.0-1.0 to decibels)
                FloatControl volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float min = volumeControl.getMinimum();
                float max = volumeControl.getMaximum();
                float volumeDb;

                if (volume <= 0.0f) {
                    volumeDb = min;
                } else if (volume >= 1.0f) {
                    volumeDb = max;
                } else {
                    // Convert linear volume (0-1) to decibels (logarithmic scale)
                    // Formula: dB = 20 * log10(volume)
                    volumeDb = (float) (20.0 * Math.log10(volume));
                    volumeDb = Math.max(min, Math.min(max, volumeDb));
                }

                volumeControl.setValue(volumeDb);

                // Note: Pitch adjustment is not trivial with Java Sound API and would require
                // resampling the audio data. For now, we skip pitch adjustment for custom sounds.
                if (pitch != 1.0f) {
                    System.out.println("[SuxAddons] Note: Pitch adjustment is not supported for custom sounds (requested: " + pitch + ")");
                }

                // Start playback
                clip.start();

                // Wait for the clip to finish and then clean up
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                        try {
                            audioInputStream.close();
                        } catch (IOException e) {
                            // Ignore close errors
                        }
                    }
                });

                Logger.DebugLog("SoundManager","Playing custom sound: " + soundName + " (volume: " + volume + ")");

            } catch (UnsupportedAudioFileException e) {
                Logger.DebugLog("SoundManager","Unsupported audio format for '" + soundName + "': " + e.getMessage());
                System.err.println("Make sure the file is in OGG Vorbis format");
            } catch (IOException e) {
                Logger.DebugLog("SoundManager","Failed to read custom sound file '" + soundName + "': " + e.getMessage());
            } catch (LineUnavailableException e) {
                Logger.DebugLog("SoundManager","Audio line unavailable for '" + soundName + "': " + e.getMessage());
            } catch (Exception e) {
                Logger.DebugLog("SoundManager","Unexpected error playing custom sound '" + soundName + "': " + e.getMessage());
                e.printStackTrace();
            }
        }, "SuxAddons-CustomSound-" + soundName).start();
    }

    public static void reload() {
        customSoundFiles.clear();
        initialized = false;
        initialize();
    }
}
