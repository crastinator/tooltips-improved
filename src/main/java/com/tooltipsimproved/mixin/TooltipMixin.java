package com.tooltipsimproved.mixin;

import com.tooltipsimproved.TooltipsImprovedConfig;
import net.minecraft.client.gui.components.Tooltip;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Tooltip.class)
public abstract class TooltipMixin {
    // Vanilla wraps button tooltips at a fixed 170 pixels
    @ModifyConstant(method = "splitTooltip", constant = @Constant(intValue = 170))
    private static int tooltipsimproved$buttonTooltipWidth(int width) {
        return TooltipsImprovedConfig.get().unwrapButtonTooltips ? Integer.MAX_VALUE : width;
    }
}
