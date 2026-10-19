package com.tooltipsimproved;

import com.tooltipsimproved.mixin.ClientTextTooltipAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Breaks tooltip text lines that are wider than {@link TooltipsImprovedConfig#wrap} allows.
 * Lines that already fit, and non-text parts such as bundle contents, are left alone.
 * So are lines with right-to-left text: tooltips store text already reordered for display, and
 * splitting it again would reorder it a second time.
 */
public final class TooltipWrap {
    /** Vanilla's extra space under a tooltip's first line. */
    private static final int TITLE_GAP = 2;
    private static final int CACHE_SIZE = 64;
    /** The cursor's space is rounded down to a multiple of this, so moving the mouse doesn't split lines anew every pixel. */
    private static final int CURSOR_STEP = 8;

    /**
     * Recently split lines. Tooltips are rebuilt every frame with the same text, so this saves
     * splitting it again each time. Cleared when a screen closes, which is also when resource packs change fonts.
     */
    private static final Map<Key, List<FormattedCharSequence>> CACHE = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Key, List<FormattedCharSequence>> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    private record Run(String text, Style style) {}

    /** Includes the font options that change glyph widths without closing a screen. */
    private record Key(List<Run> runs, int limit, boolean unicodeFont, boolean japaneseGlyphs) {}

    /**
     * @param followsCursor whether the tooltip is drawn next to the cursor, which {@link TooltipsImprovedConfig.WrapMode#CURSOR} measures from
     * @param vanillaGap whether vanilla puts its gap under the first line
     * @param keepGap whether the tooltip should have that gap under its title
     */
    public static List<ClientTooltipComponent> wrap(List<ClientTooltipComponent> lines, boolean followsCursor, boolean vanillaGap, boolean keepGap) {
        TooltipsImprovedConfig config = TooltipsImprovedConfig.get();
        if (config.wrap == TooltipsImprovedConfig.WrapMode.OFF) return lines;

        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int limit = limit(config, minecraft, followsCursor);

        boolean unicodeFont = minecraft.options.forceUnicodeFont().get();
        boolean japaneseGlyphs = minecraft.options.japaneseGlyphVariants().get();

        // Only copied once something actually needs wrapping
        List<ClientTooltipComponent> wrapped = null;
        for (int i = 0; i < lines.size(); i++) {
            ClientTooltipComponent line = lines.get(i);
            // Only plain text lines: other mods' subclasses may draw more than their text, which a rebuilt line would lose
            FormattedCharSequence text = line.getClass() == ClientTextTooltip.class
                ? ((ClientTextTooltipAccessor) line).tooltipsimproved$getText() : null;
            List<Run> runs = text != null && line.getWidth(font) > limit ? runs(text) : null;
            if (runs != null) {
                if (wrapped == null) wrapped = new ArrayList<>(lines.subList(0, i));
                List<FormattedCharSequence> parts = CACHE.computeIfAbsent(new Key(runs, limit, unicodeFont, japaneseGlyphs), key -> split(font, key));
                if (i == 0) addTitle(wrapped, parts, vanillaGap, keepGap && lines.size() > 1);
                else for (FormattedCharSequence part : parts) wrapped.add(new ClientTextTooltip(part));
            } else if (wrapped != null) {
                wrapped.add(line);
            }
        }
        return wrapped == null ? lines : wrapped;
    }

    /**
     * Adds the parts of a wrapped first line. Vanilla puts its gap after the first part, which would
     * split the title, so the first part absorbs it and the last part adds it back under the whole title.
     */
    private static void addTitle(List<ClientTooltipComponent> wrapped, List<FormattedCharSequence> parts, boolean vanillaGap, boolean keepGap) {
        for (int i = 0; i < parts.size(); i++) {
            int extra = 0;
            if (i == 0 && vanillaGap) extra -= TITLE_GAP;
            if (i == parts.size() - 1 && keepGap) extra += TITLE_GAP;
            wrapped.add(extra == 0 ? new ClientTextTooltip(parts.get(i)) : new TooltipLines.TitleLine(parts.get(i), extra));
        }
    }

    /** Widest a text line may be, in GUI pixels. */
    private static int limit(TooltipsImprovedConfig config, Minecraft minecraft, boolean followsCursor) {
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenLimit = screenWidth - 2 * TooltipPlacement.EDGE - 2 * TooltipPlacement.FRAME;

        int limit = switch (config.wrap) {
            // Tooltips placed elsewhere, like under a button, have nothing to fit beside
            case OFF, SCREEN -> screenLimit;
            case CURSOR -> followsCursor ? cursorLimit(minecraft, screenWidth) / CURSOR_STEP * CURSOR_STEP : screenLimit;
            case HALF_SCREEN -> screenWidth / 2 - 2 * TooltipPlacement.FRAME;
            case FIXED -> config.wrapWidth;
        };
        return Math.max(TooltipsImprovedConfig.MIN_WRAP_WIDTH, Math.min(limit, screenLimit));
    }

    /** The wider of the spaces left and right of the cursor. */
    private static int cursorLimit(Minecraft minecraft, int screenWidth) {
        int mouseX = (int) (minecraft.mouseHandler.xpos() * screenWidth / minecraft.getWindow().getScreenWidth());
        return TooltipPlacement.spaceBesideCursor(screenWidth, mouseX);
    }

    private static boolean isRightToLeft(int codePoint) {
        byte direction = Character.getDirectionality(codePoint);
        return direction == Character.DIRECTIONALITY_RIGHT_TO_LEFT || direction == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;
    }

    /**
     * The flattened text tooltips store, as runs of one style each, so it can be split again;
     * {@code null} for right-to-left text, which isn't wrapped.
     */
    private static List<Run> runs(FormattedCharSequence sequence) {
        List<Run> runs = new ArrayList<>();
        StringBuilder run = new StringBuilder();
        Style[] runStyle = {Style.EMPTY};

        boolean leftToRight = sequence.accept((index, style, codePoint) -> {
            if (isRightToLeft(codePoint)) return false;
            if (!style.equals(runStyle[0]) && !run.isEmpty()) {
                runs.add(new Run(run.toString(), runStyle[0]));
                run.setLength(0);
            }
            runStyle[0] = style;
            run.appendCodePoint(codePoint);
            return true;
        });
        if (!leftToRight) return null;
        if (!run.isEmpty()) runs.add(new Run(run.toString(), runStyle[0]));
        return runs;
    }

    private static List<FormattedCharSequence> split(Font font, Key key) {
        List<FormattedText> parts = new ArrayList<>(key.runs().size());
        for (Run run : key.runs()) parts.add(FormattedText.of(run.text(), run.style()));
        return List.copyOf(font.split(FormattedText.composite(parts), key.limit()));
    }

    /** Forgets split lines, e.g. when a screen closes, as the font may change before the next one. */
    public static void clearCache() {
        CACHE.clear();
    }
}
