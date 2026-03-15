package com.classictrashcode.suxaddons.client.config;

import com.classictrashcode.suxaddons.client.config.annotations.*;
import org.jetbrains.annotations.Range;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class Config {

    @ConfigCategory(name = "Utilities")
    public Utilities utilities = new Utilities();
    @ConfigCategory(name = "Hunting", order = 2)
    public hunting hunting = new hunting();
    @ConfigCategory(name = "Macros", order = 3,formatting = "RED")
    public Macros macros = new Macros();
    @ConfigCategory(name = "Debug", order = 4)
    public Debug debug = new Debug();
    @ConfigCategory(name = "GUI Theme", order = 5)
    public GuiTheme guiTheme = new GuiTheme();

    public static class GuiTheme {
        // Current preset name (saved to config, managed by dropdown)
        public String currentPreset = "Dark Mode (Green)";

        @ConfigOption(name = "Background Color", description = "Main background color (hex)")
        public String backgroundColor = "#121212";

        @ConfigOption(name = "Surface Color", description = "Card/surface color (hex)", order = 1)
        public String surfaceColor = "#1E1E1E";

        @ConfigOption(name = "Surface Elevated Color", description = "Elevated surface color (hex)", order = 2)
        public String surfaceElevatedColor = "#252525";

        @ConfigOption(name = "Primary Color", description = "Primary accent color (hex)", order = 3)
        public String primaryColor = "#2E7D32";

        @ConfigOption(name = "Primary Dark Color", description = "Dark primary color (hex)", order = 4)
        public String primaryDarkColor = "#1B5E20";

        @ConfigOption(name = "Primary Light Color", description = "Light primary color (hex)", order = 5)
        public String primaryLightColor = "#4CAF50";

        @ConfigOption(name = "Text Primary Color", description = "Primary text color (hex)", order = 6)
        public String textPrimaryColor = "#E0E0E0";

        @ConfigOption(name = "Text Secondary Color", description = "Secondary text color (hex)", order = 7)
        public String textSecondaryColor = "#9E9E9E";

        @ConfigOption(name = "Divider Color", description = "Divider/border color (hex)", order = 8)
        public String dividerColor = "#424242";

        @ConfigOption(name = "Error Color", description = "Error state color (hex)", order = 9)
        public String errorColor = "#D32F2F";

        @ConfigOption(name = "Success Color", description = "Success state color (hex)", order = 10)
        public String successColor = "#388E3C";

        @ConfigOption(name = "Button Normal Color", description = "Normal button color (hex)", order = 11)
        public String buttonNormalColor = "#2E7D32";

        @ConfigOption(name = "Button Hover Color", description = "Hovered button color (hex)", order = 12)
        public String buttonHoverColor = "#4CAF50";

        @ConfigOption(name = "Button Active Color", description = "Active/pressed button color (hex)", order = 13)
        public String buttonActiveColor = "#1B5E20";

        @ConfigOption(name = "Button Disabled Color", description = "Disabled button color (hex)", order = 14)
        public String buttonDisabledColor = "#424242";

        @ConfigOption(name = "Toggle ON Color", description = "Toggle background when enabled (hex)", order = 15)
        public String toggleOnColor = "#4CAF50";

        @ConfigOption(name = "Toggle OFF Color", description = "Toggle background when disabled (hex)", order = 16)
        public String toggleOffColor = "#616161";

        @ConfigOption(name = "Toggle Knob Color", description = "Toggle knob/circle color (hex)", order = 17)
        public String toggleKnobColor = "#FFFFFF";

        @ConfigOption(name = "Text Field Background Color", description = "Text field background (hex)", order = 18)
        public String textFieldBackgroundColor = "#252525";

        @ConfigOption(name = "Text Field Border Color", description = "Text field border (hex)", order = 19)
        public String textFieldBorderColor = "#424242";

        @ConfigOption(name = "Text Field Focus Color", description = "Text field border when focused (hex)", order = 20)
        public String textFieldFocusColor = "#4CAF50";

        public int parseColor(String hex) {
            try {
                String cleaned = hex.replace("#", "");
                return (int) Long.parseLong(cleaned, 16) | 0xFF000000;
            } catch (NumberFormatException e) {
                return 0xFF121212; // Default to dark background on error
            }
        }
    }

    public static class Utilities {
        @ConfigSubSettings(name = "Bazaar Tracker", description = "Track bazaar Orders", order = 2)
        public BazaarTracker bazaarTracker = new BazaarTracker();
    }
    public static class BazaarTracker {
        @ConfigOption(name = "Enabled")
        public boolean enabled = false;
        @ConfigOption(name = "update Interval (seconds)", order = 1)
        @ConfigRange(min = 5, max = 60, step = 1)
        public int updateInterval = 15;

        // Sound Alert - OUTDATED
        @ConfigOption(name = "Sound Alert (Outdated)", order = 2)
        public boolean soundAlertOutdatedEnabled = false;

        @ConfigOption(name = "Outdated Sound Type", order = 3, type = WidgetType.DROPDOWN)
        @ConfigDependency(field = "soundAlertOutdatedEnabled")
        public String soundTypeOutdated = "Note Bass";

        @ConfigOption(name = "Outdated Volume", order = 4)
        @ConfigRange(min = 0.0, max = 1.0)
        @ConfigDependency(field = "soundAlertOutdatedEnabled")
        public double soundVolumeOutdated = 1.0;

        @ConfigOption(name = "Outdated Pitch", order = 5)
        @ConfigRange(min = 0.5, max = 2.0)
        @ConfigDependency(field = "soundAlertOutdatedEnabled")
        public double soundPitchOutdated = 0.5;

        // Sound Alert - MATCHED
        @ConfigOption(name = "Sound Alert (Matched)", order = 6)
        public boolean soundAlertMatchedEnabled = false;

        @ConfigOption(name = "Matched Sound Type", order = 7, type = WidgetType.DROPDOWN)
        @ConfigDependency(field = "soundAlertMatchedEnabled")
        public String soundTypeMatched = "Note Pling";

        @ConfigOption(name = "Matched Volume", order = 8)
        @ConfigRange(min = 0.0, max = 1.0)
        @ConfigDependency(field = "soundAlertMatchedEnabled")
        public double soundVolumeMatched = 1.0;

        @ConfigOption(name = "Matched Pitch", order = 9)
        @ConfigRange(min = 0.5, max = 2.0)
        @ConfigDependency(field = "soundAlertMatchedEnabled")
        public double soundPitchMatched = 1.0;

        // Sound Alert - BEST
        @ConfigOption(name = "Sound Alert (Best)", order = 10)
        public boolean soundAlertBestEnabled = false;

        @ConfigOption(name = "Best Sound Type", order = 11, type = WidgetType.DROPDOWN)
        @ConfigDependency(field = "soundAlertBestEnabled")
        public String soundTypeBest = "Level Up";

        @ConfigOption(name = "Best Volume", order = 12)
        @ConfigRange(min = 0.0, max = 1.0)
        @ConfigDependency(field = "soundAlertBestEnabled")
        public double soundVolumeBest = 1.0;

        @ConfigOption(name = "Best Pitch", order = 13)
        @ConfigRange(min = 0.5, max = 2.0)
        @ConfigDependency(field = "soundAlertBestEnabled")
        public double soundPitchBest = 1.0;
    }
    public static class PlayerScale {
        @ConfigOption(name = "Enabled")
        public boolean enabled = false;

        @ConfigOption(name = "Only Scale Player", order = 1)
        public boolean onlyScalePlayer = false;

        @ConfigOption(name = "Scale X", order = 2)
        @ConfigRange(min = 0.1, max = 2.0, step = 0.05)
        public float scaleX = 0.6f;

        @ConfigOption(name = "Scale Y", order = 3)
        @ConfigRange(min = 0.1, max = 2.0, step = 0.05)
        public float scaleY = 0.6f;

        @ConfigOption(name = "Scale Z", order = 4)
        @ConfigRange(min = 0.1, max = 2.0, step = 0.05)
        public float scaleZ = 0.6f;
    }
    public static class Wardrobe {
        @ConfigOption(name = "Enabled")
        public boolean enabled = false;

        @ConfigKeybind(name = "Slot 1", order = 1)
        public int slot1 = -1;
        @ConfigKeybind(name = "Slot 2", order = 2)
        public int slot2 = -1;
        @ConfigKeybind(name = "Slot 3", order = 3)
        public int slot3 = -1;
        @ConfigKeybind(name = "Slot 4", order = 4)
        public int slot4 = -1;
        @ConfigKeybind(name = "Slot 5", order = 5)
        public int slot5 = -1;
        @ConfigKeybind(name = "Slot 6", order = 6)
        public int slot6 = -1;
        @ConfigKeybind(name = "Slot 7", order = 7)
        public int slot7 = -1;
        @ConfigKeybind(name = "Slot 8", order = 8)
        public int slot8 = -1;
        @ConfigKeybind(name = "Slot 9", order = 9)
        public int slot9 = -1;
    }
    public static class Macros {
        @ConfigSubSettings(name = "Chat Macros", description = "Configure chat macro keybinds and messages", order = 3)
        public ChatMacrosSettings chatMacros = new ChatMacrosSettings();
    }
    public static class ChatMacrosSettings {
        @ConfigOption(name = "Enabled")
        public boolean enabled = false;
        @ConfigList(
            name = "Macros",
            description = "Chat macros with keybind and message",
            entryClass = MacroEntry.class,
            titlePrefix = "Macro",
                maxSize = 50,
            order = 1
        )
        public List<MacroEntry> macroList = new ArrayList<>();

        public static class MacroEntry {
            @ConfigKeybind(name = "Key")
            public int keyCode = GLFW.GLFW_KEY_UNKNOWN;

            @ConfigOption(name = "Message", order = 1)
            public String message = "";
        }
    }
    public static class hunting {
        @ConfigSubSettings(name = "HideonLeaf Tracker", description = "Hideon Leaf Tracker Settings", order = 1)
        public HideonLeafTracker hideonLeafTracker = new HideonLeafTracker();
        @ConfigSubSettings(name = "Cinderbat Tracker",description = "Cinderbat Tracker Settings",order = 2)
        public CinderBatTracker cinderBatTracker = new CinderBatTracker();
        @ConfigSubSettings(name = "AutoFusion",description = "Automatically refuse the shards untill they run out or you press stop",order = 99)
        public AutoFusion autoFusion = new AutoFusion();
        @ConfigSubSettings(name = "Auto Lasso Puller",description = "Automatically refuse the shards untill they run out or you press stop",order = 3)
        public AutoLassoPuller autoLassoPuller = new AutoLassoPuller();
    }
    public static class  AutoLassoPuller{
        @ConfigOption(name = "Enabled")
        public boolean enabled = false;
        @ConfigOption(name = "Cool Down Between Pulls in Ticks")
        @ConfigRange(min = 10,max = 30)
        public int coolDownBetweenPulls = 30; // 1.5 Seconds
        @ConfigOption(name = "Min Delay in Ticks")
        @ConfigRange(min = 2,max = 9)
        public int minDelay = 5; // 250 ms
        @ConfigRange(min = 10,max = 15)
        @ConfigOption(name = "Max Delay in Ticks")
        public int maxDelay = 10; // 500 ms
    }
    public static class AutoFusion {
        @ConfigOption(name = "Enabled")
        public boolean enabled = false;
        @ConfigOption(name = "Action delay ticks",order = 1)
        @ConfigRange(min = 5, max = 40, step = 1)
        public int actionDelayTicks = 20;
        @ConfigOption(name = "Long delay ticks",order = 2)
        @ConfigRange(min=5,max=60,step = 1)
        public int longDelayTicks = 40;

        @ConfigOption(name = "Sound Alert Enabled", order = 3)
        public boolean soundAlertEnabled = false;

        @ConfigOption(name = "Sound Type", order = 4, type = WidgetType.DROPDOWN)
        @ConfigDependency(field = "soundAlertEnabled")
        public String soundType = "Level Up";

        @ConfigOption(name = "Sound Volume", order = 5)
        @ConfigRange(min = 0.0, max = 1.0)
        @ConfigDependency(field = "soundAlertEnabled")
        public double soundVolume = 1.0;

        @ConfigOption(name = "Sound Pitch", order = 6)
        @ConfigRange(min = 0.5, max = 2.0)
        @ConfigDependency(field = "soundAlertEnabled")
        public double soundPitch = 1.0;
    }
    public static class HideonLeafTracker {
        @ConfigOption(name = "Enabled")
        public boolean enabled = false;
        @ConfigOption(name = "HideonLeafs Glowing", order = 1)
        public boolean hideOnLeafsGlowing = false;
    }
    public static class CinderBatTracker{
        @ConfigOption(name = "Enabled")
        public boolean enabled = false;
        @ConfigOption(name = "Cinderbat Glowing", order = 1)
        public boolean cinderBatGlowing = false;
    }
    public static class Debug {
        @ConfigOption(name = "Debug Mode", description = "Enable debug mode")
        public boolean debugMode = false;
    }
}
