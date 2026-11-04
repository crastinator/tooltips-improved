package com.tooltipsimproved.mixin;

import com.tooltipsimproved.TooltipPreviews;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tells vanilla how to draw this mod's tooltip images, which it would otherwise reject. Every
 * loader's tooltip code ends up here, so this replaces their separate registration events, and
 * Fabric's needing Fabric API.
 */
@Mixin(ClientTooltipComponent.class)
public interface ClientTooltipComponentMixin {
    @Inject(method = "create(Lnet/minecraft/world/inventory/tooltip/TooltipComponent;)Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;",
        at = @At("HEAD"), cancellable = true)
    private static void tooltipsimproved$createPreview(TooltipComponent component, CallbackInfoReturnable<ClientTooltipComponent> cir) {
        if (component instanceof TooltipPreviews.Preview preview) cir.setReturnValue(preview.renderer());
    }
}
