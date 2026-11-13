package com.tooltipsimproved.mixin;

import com.tooltipsimproved.SelfTest;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if <1.21 {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
*///?}

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
    //? if >=1.21 {
    @Inject(method = "<init>*", at = @At("RETURN"))
    private void tooltipsimproved$selfTest(CallbackInfo ci) {
        if (SelfTest.ENABLED) SelfTest.runOnce();
    }
    //?} else {
    /*// 1.20.1 opens the title screen while resources are still loading, before translations and fonts exist
    @Inject(method = "render", at = @At("HEAD"))
    private void tooltipsimproved$selfTest(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (SelfTest.ENABLED && Minecraft.getInstance().getOverlay() == null) SelfTest.runOnce();
    }
    *///?}
}
