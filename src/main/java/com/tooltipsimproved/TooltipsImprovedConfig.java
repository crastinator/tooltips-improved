package com.tooltipsimproved;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Settings stored in {@code config/tooltipsimproved.json}. Loaded on first use, because the
 * game directory isn't known yet when loaders construct the mod.
 */
public final class TooltipsImprovedConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile TooltipsImprovedConfig instance;

    // Limits shared with the settings screen's sliders
    public static final int MIN_WRAP_WIDTH = 50, MAX_WRAP_WIDTH = 400;
    public static final int MIN_WHEEL_STEP = 1, MAX_WHEEL_STEP = 50;
    public static final int MIN_KEY_SPEED = 10, MAX_KEY_SPEED = 1000;
    public static final double MIN_SMOOTHNESS = 0.05;

    /** Show armor trims on one line. */
    public boolean foldTrims = true;

    /** How to break text lines too wide for the space they get. */
    public WrapMode wrap = WrapMode.CURSOR;
    /** Maximum text width in pixels for {@link WrapMode#FIXED}. */
    public int wrapWidth = 200;
    /** Skip vanilla's 170 pixel wrap on button tooltips, leaving them to {@link #wrap}. */
    public boolean unwrapButtonTooltips = false;

    /** Where to put a hover tooltip that doesn't fit beside the cursor. */
    public Placement placement = Placement.CENTER_WHEN_CRAMPED;
    /** Apply {@link #placement} to every tooltip, not just ones that follow the cursor. */
    public boolean placeAllTooltips = false;
    /** Keep the top of a tooltip taller than the screen in view, rather than the bottom. */
    public boolean topFirst = true;
    /** Show button tooltips centered above or below the button instead of at the cursor. */
    public boolean buttonTooltipsBesideButton = false;

    /** Opacity of the tooltip background and border, from 0 to 1. Text stays opaque. */
    public double backgroundOpacity = 1.0;
    /** Drop the 2 pixel gap vanilla puts under a tooltip's first line. */
    public boolean removeFirstLineGap = false;

    /** Move tooltips with the mouse wheel. */
    public boolean scrollWheel = true;
    /** Move tooltips with the player's movement keys (WASD by default) as well as the keybinds. */
    public boolean movementKeys = false;
    /** Put the tooltip back in place once it has been hidden. */
    public boolean resetWhenHidden = true;
    public boolean invertHorizontal = false;
    public boolean invertVertical = false;
    /** Pixels moved per mouse wheel notch. */
    public int wheelStep = 10;
    /** Pixels moved per second while a key is held. */
    public int keySpeed = 300;
    /** Share of the way the tooltip glides toward its target per frame at 60 FPS, from 0.05 (gliding) to 1 (instant). */
    public double smoothness = 0.25;

    public enum WrapMode {
        /** Never wrap. */
        OFF,
        /** Wrap only what wouldn't fit on the screen at all. */
        SCREEN,
        /** Wrap to the wider of the spaces left and right of the cursor, so the tooltip fits beside it. */
        CURSOR,
        HALF_SCREEN,
        /** Wrap at {@link #wrapWidth}. */
        FIXED
    }

    public enum Placement {
        /** Leave it where vanilla puts it, even if that covers the cursor. */
        VANILLA,
        /** Center it horizontally on the cursor, above it if there's room, otherwise below. */
        CENTER_WHEN_CRAMPED,
        /** Move it into the screen corner nearest the cursor that doesn't cover it. */
        CORNER_WHEN_CRAMPED,
        /** Always use a corner, like a side panel. */
        CORNER
    }

    /** Synchronized because vanilla also builds tooltips off the render thread, for the creative search. */
    public static TooltipsImprovedConfig get() {
        TooltipsImprovedConfig config = instance;
        if (config != null) return config;
        synchronized (TooltipsImprovedConfig.class) {
            if (instance == null) instance = load(path());
            return instance;
        }
    }

    private static Path path() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(TooltipsImproved.MOD_ID + ".json");
    }

    /**
     * Reads the config at {@code path}. A file that can't be read is copied to {@code <name>.broken}
     * and left as it is, so a typo in a hand edit doesn't cost the rest of the settings.
     */
    static TooltipsImprovedConfig load(Path path) {
        TooltipsImprovedConfig config = null;
        boolean readable = true;
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                config = GSON.fromJson(reader, TooltipsImprovedConfig.class);
            } catch (IOException | JsonParseException e) {
                readable = false;
                Path backup = path.resolveSibling(path.getFileName() + ".broken");
                TooltipsImproved.LOGGER.error("Couldn't read {}, using defaults until it's fixed; saved a copy as {}", path, backup.getFileName(), e);
                try {
                    Files.copy(path, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException copyError) {
                    TooltipsImproved.LOGGER.error("Couldn't copy {}", path, copyError);
                }
            }
        }
        if (config == null) config = new TooltipsImprovedConfig();

        TooltipsImprovedConfig defaults = new TooltipsImprovedConfig();
        // Unknown enum names in the file come back as null
        if (config.wrap == null) config.wrap = defaults.wrap;
        if (config.placement == null) config.placement = defaults.placement;
        config.wrapWidth = Mth.clamp(config.wrapWidth, MIN_WRAP_WIDTH, MAX_WRAP_WIDTH);
        config.backgroundOpacity = clamp(config.backgroundOpacity, 0.0, 1.0, defaults.backgroundOpacity);
        config.wheelStep = Mth.clamp(config.wheelStep, MIN_WHEEL_STEP, MAX_WHEEL_STEP);
        config.keySpeed = Mth.clamp(config.keySpeed, MIN_KEY_SPEED, MAX_KEY_SPEED);
        config.smoothness = clamp(config.smoothness, MIN_SMOOTHNESS, 1.0, defaults.smoothness);
        // Written back so newly added options show up in the file, unless that would replace one we couldn't read
        if (readable) config.save(path);
        return config;
    }

    /** Gson reads NaN from a hand-edited file, and {@link Mth#clamp} would let it through. */
    private static double clamp(double value, double min, double max, double fallback) {
        return Double.isNaN(value) ? fallback : Mth.clamp(value, min, max);
    }

    public void save() {
        save(path());
    }

    void save(Path path) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            TooltipsImproved.LOGGER.error("Couldn't write {}", path, e);
        }
    }
}
