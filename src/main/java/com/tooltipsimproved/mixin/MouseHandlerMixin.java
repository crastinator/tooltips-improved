package com.tooltipsimproved.mixin;

import com.tooltipsimproved.TooltipScroll;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    // Only cancelled when the tooltip moved, so scrollable screens such as the creative inventory keep scrolling otherwise
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void tooltipsimproved$scrollTooltip(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (TooltipScroll.onScroll(horizontal, vertical)) ci.cancel();
    }
}
