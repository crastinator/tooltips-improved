package com.tooltipsimproved;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;

//? if >=1.21.5 {
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.TooltipDisplay;
//?} else {
/*import net.minecraft.core.component.DataComponents;
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
}
