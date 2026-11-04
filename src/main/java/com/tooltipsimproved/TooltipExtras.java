package com.tooltipsimproved;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;

//? if >=1.21.5 {
import net.minecraft.world.item.component.TooltipDisplay;
//?} else {
/*import net.minecraft.world.item.component.ItemAttributeModifiers;
*///?}

public final class TooltipExtras {
    /** @return the lines to show; vanilla's own list can be immutable, so it's only added to through a copy */
    public static List<Component> addLines(ItemStack stack, List<Component> lines, Player player, TooltipFlag flag) {
        // Vanilla also builds tooltips off the render thread for the creative search, which shouldn't index these
        if (!Minecraft.getInstance().isSameThread() || (!flag.isCreative() && hidesTooltip(stack))) return lines;

        List<Component> added = new ArrayList<>();
        boolean shift = TooltipsImproved.isExpanded();
        Component durability = TooltipDurability.line(stack, lines, shift);
        if (durability != null) added.add(durability);
        added.addAll(TooltipComparison.lines(stack, player, shift));
        if (added.isEmpty()) return lines;

        List<Component> result = new ArrayList<>(lines.size() + added.size());
        result.addAll(lines);
        result.addAll(added);
        return result;
    }

    static boolean hidesTooltip(ItemStack stack) {
        //? if >=1.21.5 {
        return stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT).hideTooltip();
        //?} else {
        /*return stack.has(DataComponents.HIDE_TOOLTIP);
        *///?}
    }

    /** Whether the item leaves a component's part of the tooltip visible. */
    static boolean shows(ItemStack stack, DataComponentType<?> type) {
        //? if >=1.21.5 {
        return stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT).shows(type);
        //?} else {
        /*// Before tooltip_display, stats had their own setting, damage couldn't be hidden, and the rest shared one
        if (hidesTooltip(stack)) return false;
        if (type == DataComponents.ATTRIBUTE_MODIFIERS) return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).showInTooltip();
        return type == DataComponents.DAMAGE || !stack.has(DataComponents.HIDE_ADDITIONAL_TOOLTIP);
        *///?}
    }
}
