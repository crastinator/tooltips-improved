package com.tooltipsimproved;

/** Applies {@link TooltipsImprovedConfig#backgroundOpacity} to tooltip background colours. */
public final class TooltipBackground {
    /**
     * How many tooltip background draws are in progress. On 1.21.1 the background is drawn
     * with plain rectangle fills, which are only faded while this is above zero.
     */
    private static int drawing;

    public static void begin() {
        drawing++;
    }

    public static void end() {
        drawing = Math.max(0, drawing - 1);
    }

    public static boolean isDrawing() {
        return drawing > 0;
    }

    public static float opacity() {
        return (float) TooltipsImprovedConfig.get().backgroundOpacity;
    }

    /** Scales the alpha of an ARGB colour by the configured opacity. */
    public static int fade(int argb) {
        int alpha = Math.round((argb >>> 24) * opacity());
        return (alpha << 24) | (argb & 0xFFFFFF);
    }
}
