package com.tooltipsimproved.mixin;

import com.tooltipsimproved.TooltipsImproved;
import com.tooltipsimproved.TooltipsImprovedConfig;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

//? if >=1.21.5 {
import net.minecraft.core.component.DataComponentGetter;
//?}
//? if >=1.21.2 {
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;
//?} else {
/*import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimPattern;
*///?}

/**
 * Replaces vanilla's three trim lines ("Upgrade:", pattern, material) with a single one.
 */
@Mixin(ArmorTrim.class)
public abstract class ArmorTrimMixin {
    @Shadow @Final private Holder<TrimMaterial> material;
    @Shadow @Final private Holder<TrimPattern> pattern;
    //? if <1.21.5 {
    /*@Shadow @Final private boolean showInTooltip;
    *///?}

    @Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true)
    //? if >=1.21.5 {
    private void tooltipsimproved$foldTrim(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components, CallbackInfo ci) {
    //?} else
    /*private void tooltipsimproved$foldTrim(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, CallbackInfo ci) {*/
        if (!TooltipsImprovedConfig.get().foldTrims || TooltipsImproved.isExpanded()) return;
        //? if <1.21.5 {
        /*if (!showInTooltip) return;
        *///?}

        consumer.accept(TooltipsImproved.trimLine(pattern, material));
        ci.cancel();
    }
}
