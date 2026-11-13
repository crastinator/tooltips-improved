package com.tooltipsimproved.mixin;

import com.tooltipsimproved.TooltipsImprovedConfig;
import com.tooltipsimproved.TooltipPlacement;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if >=1.21 {
import net.minecraft.client.gui.components.WidgetTooltipHolder;
import net.minecraft.client.gui.navigation.ScreenRectangle;
//?} else {
/*import net.minecraft.client.gui.components.AbstractWidget;
*///?}

//? if >=1.21 {
@Mixin(WidgetTooltipHolder.class)
public abstract class WidgetTooltipHolderMixin {
    @Inject(method = "createTooltipPositioner", at = @At("RETURN"), cancellable = true)
    private void tooltipsimproved$besideButton(ScreenRectangle button, boolean hovered, boolean focused, CallbackInfoReturnable<ClientTooltipPositioner> cir) {
        if (TooltipsImprovedConfig.get().buttonTooltipsBesideButton)
            cir.setReturnValue(TooltipPlacement.besideButton(button));
    }
}
//?} else {
/*// 1.20.1 has no WidgetTooltipHolder; widgets place their own tooltips
@Mixin(AbstractWidget.class)
public abstract class WidgetTooltipHolderMixin {
    @Inject(method = "createTooltipPositioner", at = @At("RETURN"), cancellable = true)
    private void tooltipsimproved$besideButton(CallbackInfoReturnable<ClientTooltipPositioner> cir) {
        if (TooltipsImprovedConfig.get().buttonTooltipsBesideButton)
            cir.setReturnValue(TooltipPlacement.besideButton(((AbstractWidget) (Object) this).getRectangle()));
    }
}
*///?}
