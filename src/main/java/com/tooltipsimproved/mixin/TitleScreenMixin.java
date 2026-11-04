package com.tooltipsimproved.mixin;

import com.tooltipsimproved.SelfTest;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
    @Inject(method = "<init>*", at = @At("RETURN"))
    private void tooltipsimproved$selfTest(CallbackInfo ci) {
        if (SelfTest.ENABLED) SelfTest.runOnce();
    }
}
