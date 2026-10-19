package com.tooltipsimproved;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

//? if <26.3 {
/*import com.tooltipsimproved.mixin.ClientTextTooltipAccessor;
*///?}

/** Prepares a tooltip's lines before vanilla draws them: tracks it for scrolling, wraps it, trims the title gap. */
public final class TooltipLines {
    /** Whether the tooltip being drawn follows the cursor; set from its positioner just before {@link #prepare}. */
    private static boolean followsCursor = true;
    /** Whether vanilla leaves a gap under the first line of the tooltip being drawn; 26.3 can turn it off per tooltip. */
    private static boolean firstLineGap = true;

    /**
     * Marks lines this class returned. NeoForge hands them straight on to its own overload of
     * the tooltip method, which is hooked too, and they shouldn't be wrapped or tracked twice.
     */
    private static final class Prepared extends ArrayList<ClientTooltipComponent> {
        Prepared(List<ClientTooltipComponent> lines) {
            super(lines);
        }
    }

    public static void positioner(ClientTooltipPositioner positioner) {
        followsCursor = TooltipPlacement.followsCursor(positioner);
    }

    //? if >=26.3 {
    /** Set from the tooltip method's parameter, after {@link TooltipsImprovedConfig#removeFirstLineGap} is applied to it. */
    public static void firstLineGap(boolean gap) {
        firstLineGap = gap;
    }
    //?}

    /** @param graphics what the tooltip is drawn with, which is new every frame */
    public static List<ClientTooltipComponent> prepare(List<ClientTooltipComponent> lines, Object graphics) {
        if (lines instanceof Prepared) {
            TooltipScroll.stillDrawn();
            return lines;
        }
        // Tracked before wrapping, which changes as the cursor moves when wrapping to fit beside it
        TooltipScroll.beginTooltip(lines, graphics);
        //? if >=26.3 {
        boolean keepGap = firstLineGap;
        //?} else {
        /*boolean keepGap = !TooltipsImprovedConfig.get().removeFirstLineGap;
        *///?}
        List<ClientTooltipComponent> prepared = removeFirstLineGap(TooltipWrap.wrap(lines, followsCursor, firstLineGap, keepGap));
        measure(prepared);
        return new Prepared(prepared);
    }

    /** Tells {@link TooltipScroll} how big the tooltip is, as the wheel only moves ones that don't fit. */
    private static void measure(List<ClientTooltipComponent> lines) {
        Font font = Minecraft.getInstance().font;
        int width = 0, height = 0;
        for (ClientTooltipComponent line : lines) {
            width = Math.max(width, line.getWidth(font));
            height += height(line, font);
        }
        TooltipScroll.measured(width, height);
    }

    static int height(ClientTooltipComponent line, Font font) {
        //? if >=1.21.2 {
        return line.getHeight(font);
        //?} else {
        /*return line.getHeight();
        *///?}
    }

    //? if >=26.3 {
    // 26.3 has a parameter for the gap, set in TooltipRenderMixin
    private static List<ClientTooltipComponent> removeFirstLineGap(List<ClientTooltipComponent> lines) {
        return lines;
    }
    //?} else {
    /*/^*
     * Vanilla adds 2 pixels after the first line when there's more than one. Reporting the first
     * line as 2 pixels shorter cancels that out, and keeps the background the right height.
     * A wrapped first line already has its gap placed by TooltipWrap.
     ^/
    static List<ClientTooltipComponent> removeFirstLineGap(List<ClientTooltipComponent> lines) {
        if (!TooltipsImprovedConfig.get().removeFirstLineGap || lines.size() < 2
            || lines.get(0).getClass() != ClientTextTooltip.class) return lines;
        ClientTextTooltip first = (ClientTextTooltip) lines.get(0);
        List<ClientTooltipComponent> result = new ArrayList<>(lines);
        result.set(0, new TitleLine(((ClientTextTooltipAccessor) first).tooltipsimproved$getText(), -2));
        return result;
    }
    *///?}

    /** A line of the tooltip's title drawn taller or shorter, to move or cancel vanilla's gap under the first line. */
    static final class TitleLine extends ClientTextTooltip {
        private final int extraHeight;

        TitleLine(FormattedCharSequence text, int extraHeight) {
            super(text);
            this.extraHeight = extraHeight;
        }

        //? if >=1.21.2 {
        @Override
        public int getHeight(Font font) {
            return super.getHeight(font) + extraHeight;
        }
        //?} else {
        /*@Override
        public int getHeight() {
            return super.getHeight() + extraHeight;
        }
        *///?}
    }
}
