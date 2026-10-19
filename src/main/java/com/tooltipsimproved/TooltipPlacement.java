package com.tooltipsimproved;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.util.Mth;
import org.joml.Vector2i;
import org.joml.Vector2ic;

/**
 * Decides where tooltips go when vanilla's spot is a poor one. Vanilla puts a hover tooltip
 * right of the cursor, or left of it if that doesn't fit; when neither fits it ends up over
 * the cursor, covering whatever is being hovered.
 */
public final class TooltipPlacement {
    /** Space vanilla keeps between the cursor and its tooltip. */
    public static final int CURSOR_GAP = 12;
    /** Space kept free along the screen edges. */
    public static final int EDGE = 4;
    /** Background padding plus border drawn around a tooltip's contents, per side. */
    public static final int FRAME = 4;
    /** Half the size of the area around the cursor a tooltip shouldn't cover. */
    private static final int CURSOR_RADIUS = 8;
    /** Space between a button and its tooltip. */
    private static final int BUTTON_GAP = 4;

    /** Wraps the positioner a tooltip was drawn with in one that applies the configured placement. */
    public static ClientTooltipPositioner wrap(ClientTooltipPositioner positioner) {
        // NeoForge and Forge hand the positioner on to an overload that is hooked too
        if (positioner instanceof Placed || positioner instanceof BesideButton) return positioner;

        TooltipsImprovedConfig config = TooltipsImprovedConfig.get();
        if (config.placement == TooltipsImprovedConfig.Placement.VANILLA) return positioner;
        if (!config.placeAllTooltips && !(positioner instanceof DefaultTooltipPositioner)) return positioner;
        return new Placed(positioner);
    }

    /** Whether a tooltip with this positioner is drawn next to the cursor. */
    public static boolean followsCursor(ClientTooltipPositioner positioner) {
        if (positioner instanceof Placed placed) positioner = placed.vanilla();
        return positioner instanceof DefaultTooltipPositioner;
    }

    /** Applied to every tooltip after it's placed, before it's scrolled. */
    public static Vector2ic keepTopInView(Vector2ic position) {
        // Vanilla keeps the bottom on screen, so a tooltip taller than the screen goes past the top
        if (!TooltipsImprovedConfig.get().topFirst || position.y() >= EDGE + FRAME) return position;
        return new Vector2i(position.x(), EDGE + FRAME);
    }

    /** Positioner for button tooltips: centered under the button, or above it if it doesn't fit below. */
    public static ClientTooltipPositioner besideButton(ScreenRectangle button) {
        return new BesideButton(button);
    }

    private record Placed(ClientTooltipPositioner vanilla) implements ClientTooltipPositioner {
        @Override
        public Vector2ic positionTooltip(int screenWidth, int screenHeight, int x, int y, int width, int height) {
            Vector2ic position = vanilla.positionTooltip(screenWidth, screenHeight, x, y, width, height);
            TooltipsImprovedConfig.Placement placement = TooltipsImprovedConfig.get().placement;

            if (placement == TooltipsImprovedConfig.Placement.CORNER)
                return corner(screenWidth, screenHeight, x, y, width, height);
            if (fitsBesideCursor(screenWidth, x, width)) return position;
            return placement == TooltipsImprovedConfig.Placement.CENTER_WHEN_CRAMPED
                ? centered(position, screenWidth, screenHeight, x, y, width, height)
                : corner(screenWidth, screenHeight, x, y, width, height);
        }
    }

    private record BesideButton(ScreenRectangle button) implements ClientTooltipPositioner {
        @Override
        public Vector2ic positionTooltip(int screenWidth, int screenHeight, int x, int y, int width, int height) {
            int left = clampX(button.left() + button.width() / 2 - width / 2, screenWidth, width);
            int below = button.bottom() + BUTTON_GAP + FRAME;
            int above = button.top() - BUTTON_GAP - FRAME - height;
            return new Vector2i(left, pickSide(below, above, screenHeight, height));
        }
    }

    private static boolean fitsBesideCursor(int screenWidth, int mouseX, int width) {
        return width <= spaceBesideCursor(screenWidth, mouseX);
    }

    /** Widest a tooltip's contents can be and still fit on the roomier side of the cursor. */
    public static int spaceBesideCursor(int screenWidth, int mouseX) {
        return Math.max(screenWidth - mouseX, mouseX) - (CURSOR_GAP + FRAME + EDGE);
    }

    /** Horizontally centered on the cursor, above or below it. */
    private static Vector2ic centered(Vector2ic vanilla, int screenWidth, int screenHeight, int mouseX, int mouseY, int width, int height) {
        int left = clampX(mouseX - width / 2, screenWidth, width);
        // Too tall to go above or below anything; keep vanilla's height so topFirst and scrolling apply
        if (height + 2 * (EDGE + FRAME) > screenHeight) return new Vector2i(left, vanilla.y());

        int above = mouseY - CURSOR_GAP - FRAME - height;
        int below = mouseY + CURSOR_GAP + FRAME;
        return new Vector2i(left, pickSide(above, below, screenHeight, height));
    }

    /**
     * The screen corner that covers the least of the area around the cursor. Ties, usually several
     * corners covering nothing, go to the corner closest to the cursor so the tooltip stays near
     * what it describes.
     */
    private static Vector2ic corner(int screenWidth, int screenHeight, int mouseX, int mouseY, int width, int height) {
        int left = EDGE + FRAME, right = screenWidth - EDGE - FRAME - width;
        int top = EDGE + FRAME, bottom = screenHeight - EDGE - FRAME - height;

        Vector2ic best = null;
        long bestCover = Long.MAX_VALUE, bestDistance = Long.MAX_VALUE;
        for (Vector2ic corner : new Vector2ic[]{
            new Vector2i(left, top), new Vector2i(right, top), new Vector2i(left, bottom), new Vector2i(right, bottom)
        }) {
            long cover = coveredCursorArea(corner, width, height, mouseX, mouseY);
            long dx = corner.x() + width / 2 - mouseX, dy = corner.y() + height / 2 - mouseY;
            long distance = dx * dx + dy * dy;
            if (cover < bestCover || (cover == bestCover && distance < bestDistance)) {
                best = corner;
                bestCover = cover;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static long coveredCursorArea(Vector2ic position, int width, int height, int mouseX, int mouseY) {
        int overlapX = Math.min(position.x() + width + FRAME, mouseX + CURSOR_RADIUS) - Math.max(position.x() - FRAME, mouseX - CURSOR_RADIUS);
        int overlapY = Math.min(position.y() + height + FRAME, mouseY + CURSOR_RADIUS) - Math.max(position.y() - FRAME, mouseY - CURSOR_RADIUS);
        return overlapX > 0 && overlapY > 0 ? (long) overlapX * overlapY : 0;
    }

    private static int clampX(int x, int screenWidth, int width) {
        return Mth.clamp(x, EDGE + FRAME, Math.max(EDGE + FRAME, screenWidth - EDGE - FRAME - width));
    }

    /** Picks between two heights for a tooltip: {@code preferred} if it fits, else {@code other} if that fits, else whichever spills less. */
    private static int pickSide(int preferred, int other, int screenHeight, int height) {
        int spillPreferred = spill(preferred, screenHeight, height);
        int spillOther = spill(other, screenHeight, height);
        if (spillPreferred <= 0) return preferred;
        if (spillOther <= 0) return other;
        return spillPreferred <= spillOther ? preferred : other;
    }

    /** How far a tooltip at {@code y} would go past the top or bottom margin, or 0 or less if it fits. */
    private static int spill(int y, int screenHeight, int height) {
        return Math.max(EDGE + FRAME - y, y + height + FRAME + EDGE - screenHeight);
    }
}
