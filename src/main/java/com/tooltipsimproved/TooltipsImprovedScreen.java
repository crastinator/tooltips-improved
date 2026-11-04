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

        section("durability");
        TooltipsImprovedConfig.Durability durability = config.durability;
        list.addSmall(
            toggle("durability.enabled", durability.enabled, value -> durability.enabled = value),
            choice("durability.showWhen", TooltipsImprovedConfig.DurabilityShowWhen.values(), durability.showWhen, value -> durability.showWhen = value),
            choice("durability.format", TooltipsImprovedConfig.DurabilityFormat.values(), durability.format, value -> durability.format = value),
            toggle("durability.colorByRemaining", durability.colorByRemaining, value -> durability.colorByRemaining = value));

        section("comparison");
        TooltipsImprovedConfig.Comparison comparison = config.comparison;
        list.addSmall(
            toggle("comparison.enabled", comparison.enabled, value -> comparison.enabled = value),
            choice("comparison.showWhen", TooltipsImprovedConfig.ShowWhen.values(), comparison.showWhen, value -> comparison.showWhen = value),
            toggle("comparison.hideIdentical", comparison.hideIdentical, value -> comparison.hideIdentical = value));

        section("previews");
        TooltipsImprovedConfig.Previews previews = config.previews;
        list.addSmall(
            choice("previews.showWhen", TooltipsImprovedConfig.ShowWhen.values(), previews.showWhen, value -> previews.showWhen = value),
            toggle("previews.books", previews.books, value -> previews.books = value),
            slider("previews.bookLines", TooltipsImprovedConfig.MIN_BOOK_LINES, TooltipsImprovedConfig.MAX_BOOK_LINES, previews.bookLines, value -> previews.bookLines = value),
            toggle("previews.maps", previews.maps, value -> previews.maps = value),
            slider("previews.mapSize", TooltipsImprovedConfig.MIN_MAP_SIZE, TooltipsImprovedConfig.MAX_MAP_SIZE, previews.mapSize, value -> previews.mapSize = value),
            toggle("previews.banners", previews.banners, value -> previews.banners = value),
            toggle("previews.fireworks", previews.fireworks, value -> previews.fireworks = value));

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
