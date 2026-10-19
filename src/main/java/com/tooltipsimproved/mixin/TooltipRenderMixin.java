package com.tooltipsimproved.mixin;

import com.tooltipsimproved.TooltipsImprovedConfig;
import com.tooltipsimproved.TooltipPlacement;
import com.tooltipsimproved.TooltipLines;
import com.tooltipsimproved.TooltipScroll;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

//? if <1.21.2 {
/*import com.tooltipsimproved.TooltipBackground;
*///?}

/**
 * Hooks the method that draws every tooltip: wraps its lines, swaps in the configured placement,
 * then moves the placed tooltip by the scroll offset.
 */
//? if >=26.1 {
@Mixin(net.minecraft.client.gui.GuiGraphicsExtractor.class)
//?} else {
/*@Mixin(net.minecraft.client.gui.GuiGraphics.class)
*///?}
public abstract class TooltipRenderMixin {
    //? if >=26.1 {
    private static final String NAME = "tooltip";
    //?} elif >=1.21.6 {
    /*private static final String NAME = "renderTooltip";
    *///?} else {
    /*private static final String NAME = "renderTooltipInternal";
    *///?}
    // NeoForge and Forge move the body into an overload taking an ItemStack, so match every overload there
    private static final String METHOD = NAME /*? if !fabric {*/+ "*"/*?}*/;

    // HEAD injectors run in the order they're declared: the positioner and gap are noted before the lines are wrapped
    @ModifyVariable(method = METHOD, at = @At("HEAD"), argsOnly = true)
    private ClientTooltipPositioner tooltipsimproved$placeTooltip(ClientTooltipPositioner positioner) {
        TooltipLines.positioner(positioner);
        return TooltipPlacement.wrap(positioner);
    }

    //? if >=26.3 {
    // 26.3 made the gap under the first line a parameter; older versions are handled in TooltipLines
    @ModifyVariable(method = METHOD, at = @At("HEAD"), argsOnly = true)
    private boolean tooltipsimproved$firstLineGap(boolean gap) {
        boolean kept = gap && !TooltipsImprovedConfig.get().removeFirstLineGap;
        TooltipLines.firstLineGap(kept);
        return kept;
    }
    //?}

    @ModifyVariable(method = METHOD, at = @At("HEAD"), argsOnly = true)
    private List<ClientTooltipComponent> tooltipsimproved$prepareLines(List<ClientTooltipComponent> lines) {
        // The graphics object is new every frame, which tells tooltips drawn together apart from new ones
        return TooltipLines.prepare(lines, this);
    }

    @ModifyVariable(method = METHOD, at = @At("STORE"))
    private Vector2ic tooltipsimproved$offsetTooltip(Vector2ic position) {
        return TooltipScroll.offset(TooltipPlacement.keepTopInView(position));
    }

    //? if <1.21.2 {
    /*// 1.21.1 draws the background from plain fills; fade those while a tooltip background is drawn
    @ModifyVariable(method = "fill(IIIIII)V", at = @At("HEAD"), argsOnly = true, ordinal = 5)
    private int tooltipsimproved$fadeFill(int color) {
        return TooltipBackground.isDrawing() ? TooltipBackground.fade(color) : color;
    }

    @ModifyVariable(method = "fillGradient(IIIIIII)V", at = @At("HEAD"), argsOnly = true, ordinal = 5)
    private int tooltipsimproved$fadeGradientFrom(int color) {
        return TooltipBackground.isDrawing() ? TooltipBackground.fade(color) : color;
    }

    @ModifyVariable(method = "fillGradient(IIIIIII)V", at = @At("HEAD"), argsOnly = true, ordinal = 6)
    private int tooltipsimproved$fadeGradientTo(int color) {
        return TooltipBackground.isDrawing() ? TooltipBackground.fade(color) : color;
    }
    *///?}
}
