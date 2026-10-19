package com.tooltipsimproved;

import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * The settings screen, opened from Mod Menu on Fabric and from the mod list on NeoForge and Forge.
 * Built from vanilla's options widgets so it looks like the rest of the settings menus.
 * Changes apply straight away and are written to the config file when the screen closes.
 */
public class TooltipsImprovedScreen extends OptionsSubScreen {
    private static final String KEY = TooltipsImproved.MOD_ID + ".config.";
    private final TooltipsImprovedConfig config = TooltipsImprovedConfig.get();
    /** Only written back if something changed, so just looking doesn't replace a file that failed to load. */
    private boolean changed;

    public TooltipsImprovedScreen(Screen parent) {
        super(parent, Minecraft.getInstance().options, Component.translatable(KEY + "title"));
    }

    @Override
    protected void addOptions() {
        section("trims");
        list.addBig(toggle("foldTrims", config.foldTrims, value -> config.foldTrims = value));

        section("wrapping");
        list.addSmall(
            choice("wrap", TooltipsImprovedConfig.WrapMode.values(), config.wrap, value -> config.wrap = value),
            slider("wrapWidth", TooltipsImprovedConfig.MIN_WRAP_WIDTH, TooltipsImprovedConfig.MAX_WRAP_WIDTH, config.wrapWidth, value -> config.wrapWidth = value),
            toggle("unwrapButtonTooltips", config.unwrapButtonTooltips, value -> config.unwrapButtonTooltips = value));

        section("placement");
        list.addSmall(
            choice("placement", TooltipsImprovedConfig.Placement.values(), config.placement, value -> config.placement = value),
            toggle("placeAllTooltips", config.placeAllTooltips, value -> config.placeAllTooltips = value),
            toggle("topFirst", config.topFirst, value -> config.topFirst = value),
            toggle("buttonTooltipsBesideButton", config.buttonTooltipsBesideButton, value -> config.buttonTooltipsBesideButton = value));

        section("look");
        list.addSmall(
            percent("backgroundOpacity", 0, config.backgroundOpacity, value -> config.backgroundOpacity = value),
            toggle("removeFirstLineGap", config.removeFirstLineGap, value -> config.removeFirstLineGap = value));

        section("scrolling");
        list.addSmall(
            toggle("scrollWheel", config.scrollWheel, value -> config.scrollWheel = value),
            toggle("movementKeys", config.movementKeys, value -> config.movementKeys = value),
            toggle("resetWhenHidden", config.resetWhenHidden, value -> config.resetWhenHidden = value),
            toggle("invertHorizontal", config.invertHorizontal, value -> config.invertHorizontal = value),
            toggle("invertVertical", config.invertVertical, value -> config.invertVertical = value),
            slider("wheelStep", TooltipsImprovedConfig.MIN_WHEEL_STEP, TooltipsImprovedConfig.MAX_WHEEL_STEP, config.wheelStep, value -> config.wheelStep = value),
            slider("keySpeed", TooltipsImprovedConfig.MIN_KEY_SPEED, TooltipsImprovedConfig.MAX_KEY_SPEED, config.keySpeed, value -> config.keySpeed = value),
            percent("smoothness", (int) Math.round(TooltipsImprovedConfig.MIN_SMOOTHNESS * 100), config.smoothness, value -> config.smoothness = value));
    }

    @Override
    public void removed() {
        super.removed();
        if (changed) config.save();
    }

    /** Wraps an option's change handler so the screen knows to save. */
    private <T> Consumer<T> track(Consumer<T> onChange) {
        return value -> {
            changed = true;
            onChange.accept(value);
        };
    }

    private void section(String name) {
        //? if >=1.21.11 {
        list.addHeader(Component.translatable(KEY + "section." + name));
        //?}
    }

    private OptionInstance<Boolean> toggle(String name, boolean value, Consumer<Boolean> onChange) {
        return OptionInstance.createBoolean(KEY + name, tooltip(name), value, track(onChange)::accept);
    }

    private <E extends Enum<E>> OptionInstance<E> choice(String name, E[] values, E value, Consumer<E> onChange) {
        Codec<E> codec = Codec.STRING.xmap(id -> Enum.valueOf(value.getDeclaringClass(), id), Enum::name);
        return new OptionInstance<>(KEY + name, tooltip(name),
            (caption, choice) -> Options.genericValueLabel(caption,
                Component.translatable(KEY + name + "." + choice.name().toLowerCase(Locale.ROOT))),
            new OptionInstance.Enum<>(Arrays.asList(values), codec), value, track(onChange)::accept);
    }

    private OptionInstance<Integer> slider(String name, int min, int max, int value, Consumer<Integer> onChange) {
        return new OptionInstance<>(KEY + name, tooltip(name), Options::genericValueLabel,
            new OptionInstance.IntRange(min, max), Math.max(min, Math.min(max, value)), track(onChange)::accept);
    }

    /** A 0 to 1 setting shown as a percentage, no lower than {@code minPercent}. */
    private OptionInstance<Integer> percent(String name, int minPercent, double value, Consumer<Double> onChange) {
        Consumer<Double> tracked = track(onChange);
        return new OptionInstance<>(KEY + name, tooltip(name),
            (caption, percent) -> Options.genericValueLabel(caption, Component.literal(percent + "%")),
            new OptionInstance.IntRange(minPercent, 100), (int) Math.round(value * 100), percent -> tracked.accept(percent / 100.0));
    }

    private static <T> OptionInstance.TooltipSupplier<T> tooltip(String name) {
        return OptionInstance.cachedConstantTooltip(Component.translatable(KEY + name + ".tooltip"));
    }
}
