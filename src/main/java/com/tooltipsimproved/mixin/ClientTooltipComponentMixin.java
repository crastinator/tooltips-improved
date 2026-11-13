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
 * Fabric's needing Fabric API. Forge 1.20.1 ships a Mixin that can't hook a static interface
 * method, so there {@code TooltipsImprovedForge} uses Forge's event instead.
 */
@Mixin(ClientTooltipComponent.class)
public interface ClientTooltipComponentMixin {
    //? if !forge || >=1.21 {
    @Inject(method = "create(Lnet/minecraft/world/inventory/tooltip/TooltipComponent;)Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;",
        at = @At("HEAD"), cancellable = true)
    private static void tooltipsimproved$createPreview(TooltipComponent component, CallbackInfoReturnable<ClientTooltipComponent> cir) {
        if (component instanceof TooltipPreviews.Preview preview) cir.setReturnValue(preview.renderer());
    }
    //?}
}
