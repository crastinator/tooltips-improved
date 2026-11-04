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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

//? if >=1.21.5 {
import net.minecraft.world.item.component.TooltipDisplay;
//?}

/** Shows how a hovered item's stats differ from what the player has in the matching slot. */
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
     * Amounts are on the basis vanilla's tooltips use: an item's own attack damage and speed count
     * the player's base value, so "6 Attack Damage" against "4 Attack Damage" is a difference of 2,
     * and an empty slot counts as 0.
     *
     * @param baseValue the player's base value of an attribute
     * @return one entry per changed attribute and operation, the hovered item's stats first
     */
    public static List<AttributeDelta> deltas(ItemAttributeModifiers hovered, ItemAttributeModifiers equipped, EquipmentSlot slot, ToDoubleFunction<Holder<Attribute>> baseValue) {
        Map<Stat, Double> totals = new LinkedHashMap<>();
        hovered.forEach(slot, (attribute, modifier) ->
            totals.merge(new Stat(attribute, modifier.operation()), shownAmount(modifier, baseValue), Double::sum));
        equipped.forEach(slot, (attribute, modifier) ->
            totals.merge(new Stat(attribute, modifier.operation()), -shownAmount(modifier, baseValue), Double::sum));

        List<AttributeDelta> deltas = new ArrayList<>();
        totals.forEach((stat, total) -> {
            AttributeDelta delta = new AttributeDelta(stat.attribute(), stat.operation(), total);
            if (Math.abs(delta.displayAmount()) >= SMALLEST_SHOWN) deltas.add(delta);
        });
        return deltas;
    }

    private static double shownAmount(AttributeModifier modifier, ToDoubleFunction<Holder<Attribute>> baseValue) {
        if (modifier.is(Item.BASE_ATTACK_DAMAGE_ID)) return modifier.amount() + baseValue.applyAsDouble(Attributes.ATTACK_DAMAGE);
        if (modifier.is(Item.BASE_ATTACK_SPEED_ID)) return modifier.amount() + baseValue.applyAsDouble(Attributes.ATTACK_SPEED);
        return modifier.amount();
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

    /** The lines to add for a player hovering {@code hovered}; empty for none. */
    public static List<Component> lines(ItemStack hovered, Player player, boolean shift) {
        TooltipsImprovedConfig.Comparison config = TooltipsImprovedConfig.get().comparison;
        if (!config.enabled || player == null || !attributesShown(hovered)) return List.of();
        if (config.showWhen == TooltipsImprovedConfig.ShowWhen.SHIFT_HELD && !shift) return List.of();
        EquipmentSlot slot = slotFor(player.getEquipmentSlotForItem(hovered), modifiers(hovered));
        if (slot == null) return List.of();
        return lines(hovered, player.getItemBySlot(slot), slot, player::getAttributeBaseValue);
    }

    /** {@link #lines(ItemStack, Player, boolean)} without a player, as the self-test runs before there is one. */
    static List<Component> lines(ItemStack hovered, ItemStack equipped, EquipmentSlot slot, ToDoubleFunction<Holder<Attribute>> baseValue) {
        // The very stack being worn or held, not just an identical one
        if (hovered == equipped) return List.of();

        // TODO: compare enchantments too. Their modifiers aren't in these components, and only
        //  ItemStack#forEachModifier and EnchantmentHelper report them.
        List<AttributeDelta> deltas = deltas(modifiers(hovered), modifiers(equipped), slot, baseValue);
        if (deltas.isEmpty() && TooltipsImprovedConfig.get().comparison.hideIdentical) return List.of();

        List<Component> lines = new ArrayList<>(deltas.size() + 2);
        lines.add(CommonComponents.EMPTY);
        Component name = equipped.isEmpty() ? Component.translatable(KEY + "empty_slot") : equipped.getHoverName();
        lines.add(Component.translatable(KEY + "header", name).withStyle(ChatFormatting.GRAY));
        for (AttributeDelta delta : deltas) lines.add(line(delta));
        if (deltas.isEmpty()) lines.add(Component.translatable(KEY + "identical").withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    /** One line like "+2 Armor", using vanilla's own translations so it reads like the lines above it. */
    private static Component line(AttributeDelta delta) {
        double shown = delta.displayAmount();
        boolean increase = shown > 0;
        return Component.translatable("attribute.modifier." + (increase ? "plus." : "take.") + delta.operation().id(),
                ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(Math.abs(shown)),
                Component.translatable(delta.attribute().value().getDescriptionId()))
            .withStyle(color(delta.attribute().value(), increase));
    }

    /** Green if the change is better, red if worse, and gray for attributes that are neither, going by how vanilla rates them. */
    private static ChatFormatting color(Attribute attribute, boolean increase) {
        ChatFormatting vanilla = attribute.getStyle(increase);
        if (vanilla == ChatFormatting.RED || vanilla == ChatFormatting.GRAY) return vanilla;
        return ChatFormatting.GREEN;
    }

    private static ItemAttributeModifiers modifiers(ItemStack stack) {
        ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        //? if <1.21.5 {
        /*// Before components held them by default, an item's built-in stats were a fallback for an empty component
        if (modifiers.modifiers().isEmpty()) return stack.getItem().getDefaultAttributeModifiers();
        *///?}
        return modifiers;
    }

    /** Whether the item leaves its stats visible; a server hiding them shouldn't have them shown here. */
    private static boolean attributesShown(ItemStack stack) {
        //? if >=1.21.5 {
        return stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT).shows(DataComponents.ATTRIBUTE_MODIFIERS);
        //?} else {
        /*return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).showInTooltip();
        *///?}
    }
}
