package com.tooltipsimproved;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TooltipComparison {
    private static final String KEY = TooltipsImproved.MOD_ID + ".comparison.";
    /** Vanilla shows amounts to two decimals, so anything smaller would read as a change of 0. */
    private static final double SMALLEST_SHOWN = 0.005;

    /** How much one kind of modifier on one attribute differs; vanilla lists each operation on its own line. */
    public record AttributeDelta(Holder<Attribute> attribute, AttributeModifier.Operation operation, double amount) {
        /** The amount as vanilla's tooltips scale it: multipliers as percentages, knockback resistance times ten. */
        public double displayAmount() {
            if (operation != AttributeModifier.Operation.ADD_VALUE) return amount * 100;
            return attribute.is(Attributes.KNOCKBACK_RESISTANCE) ? amount * 10 : amount;
        }
    }

    private record Stat(Holder<Attribute> attribute, AttributeModifier.Operation operation) {}

    /**
     * Compares two items' modifiers in a slot, leaving out changes too small to show.
     * Amounts are the real change, not differences of the totals vanilla displays, which add the
     * player's base value only for items that have the stat: a sword's displayed 1.6 Attack Speed
     * is really 2.4 slower than an empty hand's 4.
     *
     * @return one entry per changed attribute and operation, the hovered item's stats first
     */
    public static List<AttributeDelta> deltas(ItemAttributeModifiers hovered, ItemAttributeModifiers equipped, EquipmentSlot slot) {
        Map<Stat, Double> totals = new LinkedHashMap<>();
        addTotals(totals, hovered, slot, 1);
        addTotals(totals, equipped, slot, -1);

        List<AttributeDelta> deltas = new ArrayList<>();
        totals.forEach((stat, total) -> {
            AttributeDelta delta = new AttributeDelta(stat.attribute(), stat.operation(), total);
            if (Math.abs(delta.displayAmount()) >= SMALLEST_SHOWN) deltas.add(delta);
        });
        return deltas;
    }

    private static void addTotals(Map<Stat, Double> totals, ItemAttributeModifiers modifiers, EquipmentSlot slot, int sign) {
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (!entry.slot().test(slot) || !showsAmount(entry)) continue;
            AttributeModifier modifier = entry.modifier();
            totals.merge(new Stat(entry.attribute(), modifier.operation()), sign * modifier.amount(), Double::sum);
        }
    }

    /** A modifier set to show nothing or custom text is one its item doesn't want shown as a number. */
    private static boolean showsAmount(ItemAttributeModifiers.Entry entry) {
        //? if >=1.21.5 {
        return entry.display().equals(ItemAttributeModifiers.Display.attributeModifiers());
        //?} else {
        /*return true;
        *///?}
    }

    /**
     * The slot to compare a hovered item in: where it's worn if it's armor, otherwise the main hand
     * if it has stats there. Items with neither, and ones for other slots like a wolf's armor, aren't compared.
     *
     * @param equipSlot where a player would equip the item, which is the main hand for anything not equippable
     * @return the slot, or {@code null} for none
     */
    static EquipmentSlot slotFor(EquipmentSlot equipSlot, ItemAttributeModifiers modifiers) {
        if (equipSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) return equipSlot;
        boolean[] inHand = {false};
        modifiers.forEach(EquipmentSlot.MAINHAND, (attribute, modifier) -> inHand[0] = true);
        return inHand[0] ? EquipmentSlot.MAINHAND : null;
    }

    public static List<Component> lines(ItemStack hovered, Player player, boolean shift) {
        TooltipsImprovedConfig.Comparison config = TooltipsImprovedConfig.get().comparison;
        if (!config.enabled || player == null) return List.of();
        if (config.showWhen == TooltipsImprovedConfig.ShowWhen.SHIFT_HELD && !shift) return List.of();
        EquipmentSlot slot = slotFor(player.getEquipmentSlotForItem(hovered), modifiers(hovered));
        if (slot == null) return List.of();
        return lines(hovered, player.getItemBySlot(slot), slot);
    }

    /** {@link #lines(ItemStack, Player, boolean)} without a player, as the self-test runs before there is one. */
    static List<Component> lines(ItemStack hovered, ItemStack equipped, EquipmentSlot slot) {
        // The very stack being worn or held, not just an identical one
        if (hovered == equipped) return List.of();
        // Deltas would give away the stats of either item that hides them
        if (!TooltipExtras.shows(hovered, DataComponents.ATTRIBUTE_MODIFIERS) || !TooltipExtras.shows(equipped, DataComponents.ATTRIBUTE_MODIFIERS)) return List.of();

        // TODO: compare enchantments too. Their modifiers aren't in these components, and only
        //  ItemStack#forEachModifier and EnchantmentHelper report them.
        List<AttributeDelta> deltas = deltas(modifiers(hovered), modifiers(equipped), slot);
        if (deltas.isEmpty() && TooltipsImprovedConfig.get().comparison.hideIdentical) return List.of();

        List<Component> lines = new ArrayList<>(deltas.size() + 2);
        lines.add(CommonComponents.EMPTY);
        Component name = equipped.isEmpty() ? Component.translatable(KEY + "empty_slot") : equipped.getHoverName();
        lines.add(Component.translatable(KEY + "header", name).withStyle(ChatFormatting.GRAY));
        for (AttributeDelta delta : deltas) lines.add(line(delta));
        if (deltas.isEmpty()) lines.add(Component.translatable(KEY + "identical").withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    private static Component line(AttributeDelta delta) {
        double shown = delta.displayAmount();
        boolean increase = shown > 0;
        return Component.translatable("attribute.modifier." + (increase ? "plus." : "take.") + delta.operation().id(),
                ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(Math.abs(shown)),
                Component.translatable(delta.attribute().value().getDescriptionId()))
            .withStyle(color(delta.attribute().value(), increase));
    }

    private static ChatFormatting color(Attribute attribute, boolean increase) {
        ChatFormatting vanilla = attribute.getStyle(increase);
        if (vanilla == ChatFormatting.RED || vanilla == ChatFormatting.GRAY) return vanilla;
        return ChatFormatting.GREEN;
    }

    private static ItemAttributeModifiers modifiers(ItemStack stack) {
        //? if neoforge {
        // Includes what other mods add through ItemAttributeModifierEvent, as NeoForge's own tooltip does
        return stack.getAttributeModifiers();
        //?} elif >=1.21.5 {
        /*return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        *///?} else {
        /*ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        return modifiers.modifiers().isEmpty() ? stack.getItem().getDefaultAttributeModifiers() : modifiers;
        *///?}
    }
}
