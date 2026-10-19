package com.tooltipsimproved.mixin;

import com.tooltipsimproved.TooltipScroll;
import com.tooltipsimproved.TooltipWrap;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "onClose", at = @At("HEAD"))
    private void tooltipsimproved$forgetTooltips(CallbackInfo ci) {
        TooltipScroll.reset();
        TooltipWrap.clearCache();
    }
}
