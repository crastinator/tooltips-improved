package com.tooltipsimproved.mixin;

import com.tooltipsimproved.TooltipBackground;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

//? if >=1.21.6 {
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.resources.Identifier;
//?} elif forge {
/*import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///?} else {
/*import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
*///?}
//? if >=26.3 {
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
//?} elif >=1.21.6 {
/*import com.mojang.blaze3d.pipeline.RenderPipeline;
*///?}
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}

/** Fades the tooltip background and border by the configured opacity. */
@Mixin(TooltipRenderUtil.class)
public abstract class TooltipBackgroundMixin {
    //? if >=26.1 {
    private static final String METHOD = "extractTooltipBackground";
    //?} else {
    /*private static final String METHOD = "renderTooltipBackground";
    *///?}

    //? if >=26.3 {
    private static final String BLIT = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V";
    //?} elif >=26.1 {
    /*private static final String BLIT = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V";
    *///?} elif >=1.21.6 {
    /*private static final String BLIT = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V";
    *///?}

    // Background and frame are sprites; when faded they're drawn with the variant that takes an alpha.
    // A wrap rather than a redirect, so other tooltip mods can hook the same call
    //? if >=26.1 {
    @WrapOperation(method = METHOD, at = @At(value = "INVOKE", target = BLIT))
    private static void tooltipsimproved$fadeSprite(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, Operation<Void> original) {
        float opacity = TooltipBackground.opacity();
        if (opacity >= 1) original.call(graphics, pipeline, sprite, x, y, width, height);
        else graphics.blitSprite(pipeline, sprite, x, y, width, height, opacity);
    }
    //?} elif >=1.21.6 {
    /*@WrapOperation(method = METHOD, at = @At(value = "INVOKE", target = BLIT))
    private static void tooltipsimproved$fadeSprite(GuiGraphics graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, Operation<Void> original) {
        float opacity = TooltipBackground.opacity();
        if (opacity >= 1) original.call(graphics, pipeline, sprite, x, y, width, height);
        else graphics.blitSprite(pipeline, sprite, x, y, width, height, opacity);
    }
    *///?} else {
    /*//? if forge {
    /^// Marks the fills that make up the background, see TooltipRenderMixin. Forge 1.21.1 doesn't ship
    // MixinExtras, so the mark is set at the start and cleared on return; drawing a background only
    // throws when the game is about to crash anyway. Forge adds an overload taking the colours, hooked too
    @Inject(method = {"renderTooltipBackground(Lnet/minecraft/client/gui/GuiGraphics;IIIII)V",
        "renderTooltipBackground(Lnet/minecraft/client/gui/GuiGraphics;IIIIIIIII)V"}, at = @At("HEAD"))
    private static void tooltipsimproved$beginBackground(CallbackInfo ci) {
        TooltipBackground.begin();
    }

    @Inject(method = {"renderTooltipBackground(Lnet/minecraft/client/gui/GuiGraphics;IIIII)V",
        "renderTooltipBackground(Lnet/minecraft/client/gui/GuiGraphics;IIIIIIIII)V"}, at = @At("RETURN"))
    private static void tooltipsimproved$endBackground(CallbackInfo ci) {
        TooltipBackground.end();
    }
    ^///?} else {
    // Marks the fills that make up the background, see TooltipRenderMixin. Wrapped rather than
    // hooked at start and return, so the mark is cleared even if drawing throws; otherwise every
    // fill in the game would stay faded
    @WrapMethod(method = "renderTooltipBackground(Lnet/minecraft/client/gui/GuiGraphics;IIIII)V")
    private static void tooltipsimproved$markBackground(GuiGraphics graphics, int x, int y, int width, int height, int z, Operation<Void> original) {
        TooltipBackground.begin();
        try {
            original.call(graphics, x, y, width, height, z);
        } finally {
            TooltipBackground.end();
        }
    }
    //? if !fabric {
    // NeoForge and Forge add an overload taking the colours, which they also call directly
    @WrapMethod(method = "renderTooltipBackground(Lnet/minecraft/client/gui/GuiGraphics;IIIIIIIII)V")
    private static void tooltipsimproved$markColoredBackground(GuiGraphics graphics, int x, int y, int width, int height, int z,
                                                              int backgroundTop, int backgroundBottom, int borderTop, int borderBottom, Operation<Void> original) {
        TooltipBackground.begin();
        try {
            original.call(graphics, x, y, width, height, z, backgroundTop, backgroundBottom, borderTop, borderBottom);
        } finally {
            TooltipBackground.end();
        }
    }
    //?}
    //?}
    *///?}
}
