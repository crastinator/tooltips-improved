package com.tooltipsimproved.mixin;

import com.tooltipsimproved.TooltipExtras;
import com.tooltipsimproved.TooltipPreviews;
import com.tooltipsimproved.TooltipsImproved;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

/**
 * Adds this mod's lines and previews to item tooltips. A mixin rather than each loader's tooltip event, as
 * Fabric's lives in Fabric API, which this mod doesn't need. It runs after those events, so
 * lines from other mods come first.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true)
    private void tooltipsimproved$addLines(Item.TooltipContext context, Player player, TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir) {
        cir.setReturnValue(TooltipExtras.addLines((ItemStack) (Object) this, cir.getReturnValue(), player, flag));
    }

    // An item's own image, like a bundle's contents, comes first
    @Inject(method = "getTooltipImage", at = @At("RETURN"), cancellable = true)
    private void tooltipsimproved$addPreview(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        if (cir.getReturnValue().isEmpty()) cir.setReturnValue(TooltipPreviews.forStack((ItemStack) (Object) this, TooltipsImproved.isExpanded()));
    }
}
