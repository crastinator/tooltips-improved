package com.tooltipsimproved;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;

import java.util.List;

//? if >=1.21.5 {
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.TooltipDisplay;
//?}

public final class TooltipDurability {
    private static final String KEY = TooltipsImproved.MOD_ID + ".durability.";
    /** Vanilla's own line, shown for damaged items with advanced tooltips (F3+H) on. */
    private static final String VANILLA_KEY = "item.durability";

    /**
     * @return the line to add, or {@code null} for none
     */
    public static Component line(ItemStack stack, List<Component> lines, boolean shift) {
        TooltipsImprovedConfig.Durability config = TooltipsImprovedConfig.get().durability;
        // Also false for items with the Unbreakable component
        if (!config.enabled || !stack.isDamageableItem()) return null;
        boolean wanted = switch (config.showWhen) {
            case ALWAYS -> true;
            case WHEN_DAMAGED -> stack.isDamaged();
            case SHIFT_HELD -> shift;
        };
        if (!wanted || !shown(stack) || hasVanillaLine(lines)) return null;

        int max = stack.getMaxDamage();
        int remaining = Math.max(0, max - stack.getDamageValue());
        int percent = percent(remaining, max);
        Object[] args = switch (config.format) {
            case FRACTION -> new Object[] {number(remaining, stack, config), number(max, stack, config)};
            case PERCENT -> new Object[] {number(percent, stack, config)};
            case BOTH -> new Object[] {number(remaining, stack, config), number(max, stack, config), number(percent, stack, config)};
        };
        return Component.translatable(KEY + config.format.name().toLowerCase(java.util.Locale.ROOT), args).withStyle(ChatFormatting.GRAY);
    }

    /** The share left, rounded but never to 0 or 100 unless that's exactly true, so the last use doesn't read as none. */
    static int percent(int remaining, int max) {
        if (remaining <= 0) return 0;
        if (remaining >= max) return 100;
        return Math.max(1, Math.min(99, Math.round(remaining * 100f / max)));
    }

    private static MutableComponent number(int value, ItemStack stack, TooltipsImprovedConfig.Durability config) {
        MutableComponent number = Component.literal(Integer.toString(value));
        return config.colorByRemaining ? number.withColor(stack.getBarColor()) : number;
    }

    /** Whether the item's tooltip_display component leaves its damage visible; older versions can't hide it. */
    private static boolean shown(ItemStack stack) {
        //? if >=1.21.5 {
        return stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT).shows(DataComponents.DAMAGE);
        //?} else {
        /*return true;
        *///?}
    }

    private static boolean hasVanillaLine(List<Component> lines) {
        for (Component line : lines) {
            if (line.getContents() instanceof TranslatableContents translatable && VANILLA_KEY.equals(translatable.getKey())) return true;
        }
        return false;
    }
}
