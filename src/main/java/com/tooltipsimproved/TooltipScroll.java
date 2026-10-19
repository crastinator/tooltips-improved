package com.tooltipsimproved;

import com.tooltipsimproved.mixin.ClientTextTooltipAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import java.util.ArrayList;
import java.util.List;

/**
 * Moves the tooltip being drawn by an offset the player controls with the mouse wheel and keys,
 * so tooltips taller or wider than the screen can be read. The offset is applied after vanilla
 * positions the tooltip, so it can go past the screen edges.
 */
public final class TooltipScroll {
    /** A tooltip drawn within this many milliseconds counts as visible, so input moves it. */
    private static final long VISIBLE_FOR_MS = 100;
    /** Frame gaps longer than this mean the tooltip was away, not a slow frame, so nothing moves. */
    private static final long MAX_FRAME_NANOS = 250_000_000L;
    /** {@link TooltipsImprovedConfig#smoothness} is the share of the distance covered per frame at this rate. */
    private static final double REFERENCE_FPS = 60;
    /** How much of a moved tooltip always stays on screen, so it can't be lost off the edge. */
    private static final int MIN_VISIBLE = 24;
    /** More tooltips than this in one frame means frames aren't being told apart, so the list starts over. */
    private static final int MAX_FRAME_TITLES = 16;

    // The drawn offset glides toward the target one
    private static double x, y;
    private static double targetX, targetY;
    private static long lastDrawn;
    private static long lastFrameNanos;
    // Whether a tooltip too tall or too wide for the screen was drawn this frame or the one before;
    // the wheel only moves tooltips that need it
    private static boolean tall, wide, wasTall, wasWide;
    /** Size of the contents of the tooltip being drawn. */
    private static int width, height;

    // First lines of the tooltips drawn in the current frame and the one before, so several
    // tooltips drawn together don't count as new ones and reset each other every frame
    private static Object frame;
    private static List<String> frameTitles = new ArrayList<>();
    private static List<String> lastFrameTitles = new ArrayList<>();

    /**
     * Called before a tooltip is drawn, with its lines as they came in (before wrapping, which
     * can change as the cursor moves) and the graphics object it's drawn with, which is new every
     * frame. A tooltip whose first line wasn't drawn last frame starts back in place; only the first
     * line is compared so tooltips with live text further down keep their offset.
     */
    public static void beginTooltip(List<ClientTooltipComponent> lines, Object graphics) {
        stillDrawn();
        if (graphics != frame) {
            frame = graphics;
            List<String> previous = lastFrameTitles;
            lastFrameTitles = frameTitles;
            frameTitles = previous;
            frameTitles.clear();
            wasTall = tall;
            wasWide = wide;
            tall = wide = false;
        }
        if (frameTitles.size() >= MAX_FRAME_TITLES) frameTitles.clear();

        ClientTooltipComponent first = lines.isEmpty() ? null : lines.get(0);
        for (String title : lastFrameTitles) {
            if (reads(first, title)) {
                frameTitles.add(title);
                return;
            }
        }
        resetOffset();
        frameTitles.add(read(first));
    }

    /** Keeps the current tooltip counted as visible without comparing it again. */
    public static void stillDrawn() {
        if (!isVisible() && TooltipsImprovedConfig.get().resetWhenHidden) resetOffset();
        lastDrawn = System.currentTimeMillis();
    }

    /** Called with the size of the tooltip's contents once it's wrapped. */
    public static void measured(int width, int height) {
        TooltipScroll.width = width;
        TooltipScroll.height = height;
        var window = Minecraft.getInstance().getWindow();
        int border = 2 * (TooltipPlacement.FRAME + TooltipPlacement.EDGE);
        if (height + border > window.getGuiScaledHeight()) tall = true;
        if (width + border > window.getGuiScaledWidth()) wide = true;
    }

    /** Called when a tooltip is drawn with its placed position, returns where to draw it. */
    public static Vector2ic offset(Vector2ic position) {
        return offset(position, System.nanoTime());
    }

    /** {@link #offset(Vector2ic)} at a given time, so the self-test can simulate frames. */
    static Vector2ic offset(Vector2ic position, long nanos) {
        TooltipsImprovedConfig config = TooltipsImprovedConfig.get();
        long elapsed = lastFrameNanos == 0 ? 0 : nanos - lastFrameNanos;
        lastFrameNanos = nanos;
        // Several tooltips in one frame get almost no time each, so they don't speed anything up
        double seconds = elapsed <= 0 || elapsed > MAX_FRAME_NANOS ? 0 : elapsed / 1e9;

        readKeys(config, seconds);
        keepOnScreen(position);
        double share = config.smoothness >= 1 ? 1 : 1 - Math.pow(1 - config.smoothness, seconds * REFERENCE_FPS);
        x = approach(x, targetX, share);
        y = approach(y, targetY, share);

        int dx = (int) Math.round(x), dy = (int) Math.round(y);
        if (dx == 0 && dy == 0) return position;
        return new Vector2i(position.x() + dx, position.y() + dy);
    }

    /** Limits the target offset so at least {@link #MIN_VISIBLE} pixels of the tooltip stay on screen. */
    private static void keepOnScreen(Vector2ic position) {
        var window = Minecraft.getInstance().getWindow();
        targetX = clamp(targetX, MIN_VISIBLE - position.x() - width, window.getGuiScaledWidth() - MIN_VISIBLE - position.x());
        targetY = clamp(targetY, MIN_VISIBLE - position.y() - height, window.getGuiScaledHeight() - MIN_VISIBLE - position.y());
    }

    private static double clamp(double value, double min, double max) {
        // A screen too small for the margin on both sides leaves the offset alone
        return min > max ? value : Math.max(min, Math.min(max, value));
    }

    /** Moves {@code from} part of the way to {@code to}, landing on it once within half a pixel. */
    private static double approach(double from, double to, double share) {
        double next = from + (to - from) * share;
        return Math.abs(to - next) < 0.5 ? to : next;
    }

    /**
     * Mouse wheel movement; amounts are wheel notches, fractional for trackpads and smooth wheels.
     * Only moves a tooltip along a direction it doesn't fit in, so the wheel keeps working for the
     * screen and other mods otherwise.
     *
     * @return whether the tooltip moved, so the screen behind shouldn't scroll as well
     */
    public static boolean onScroll(double horizontal, double vertical) {
        TooltipsImprovedConfig config = TooltipsImprovedConfig.get();
        boolean tall = TooltipScroll.tall || wasTall, wide = TooltipScroll.wide || wasWide;
        if (!config.scrollWheel || !isVisible() || !(tall || wide)) return false;

        double step = config.wheelStep;
        double sideways = config.invertHorizontal ? -1 : 1;
        double dx = 0, dy = 0;
        // Wheel up moves the tooltip up, showing more of its bottom
        if (TooltipsImprovedKeys.isHeld(TooltipsImprovedKeys.SIDEWAYS)) {
            dx -= vertical * step * sideways;
        } else {
            dy -= vertical * step * (config.invertVertical ? -1 : 1);
        }
        // Mice with a side wheel, and sideways trackpad swipes
        dx -= horizontal * step * sideways;

        if (!wide) dx = 0;
        if (!tall) dy = 0;
        if (dx == 0 && dy == 0) return false;
        move(dx, dy);
        return true;
    }

    private static void readKeys(TooltipsImprovedConfig config, double seconds) {
        var options = Minecraft.getInstance().options;
        boolean reset = TooltipsImprovedKeys.isHeld(TooltipsImprovedKeys.RESET);
        boolean up = TooltipsImprovedKeys.isHeld(TooltipsImprovedKeys.UP);
        boolean down = TooltipsImprovedKeys.isHeld(TooltipsImprovedKeys.DOWN);
        // The player's own movement keys, so rebinding and other keyboard layouts work
        boolean forward = config.movementKeys && TooltipsImprovedKeys.isHeld(options.keyUp);
        boolean back = config.movementKeys && TooltipsImprovedKeys.isHeld(options.keyDown);
        boolean left = config.movementKeys && TooltipsImprovedKeys.isHeld(options.keyLeft);
        boolean right = config.movementKeys && TooltipsImprovedKeys.isHeld(options.keyRight);
        if (!(reset || up || down || forward || back || left || right)) return;

        // Typing in a search box or anvil shouldn't move the tooltip. Checked last, as it searches the whole screen
        if (TooltipsImprovedKeys.isTyping()) return;

        if (reset) {
            resetOffset();
            return;
        }
        if (seconds == 0) return;

        double step = config.keySpeed * seconds;
        boolean sideways = TooltipsImprovedKeys.isHeld(TooltipsImprovedKeys.SIDEWAYS);
        if (up) {
            if (sideways) move(-step, 0); else move(0, -step);
        } else if (down) {
            if (sideways) move(step, 0); else move(0, step);
        }

        if (forward) move(0, -step);
        else if (back) move(0, step);
        if (left) move(-step, 0);
        else if (right) move(step, 0);
    }

    private static void move(double dx, double dy) {
        targetX += dx;
        targetY += dy;
    }

    private static boolean isVisible() {
        return System.currentTimeMillis() - lastDrawn <= VISIBLE_FOR_MS;
    }

    private static void resetOffset() {
        x = y = targetX = targetY = 0;
    }

    /** Forgets the current tooltip, e.g. when its screen closes. */
    public static void reset() {
        resetOffset();
        frame = null;
        frameTitles.clear();
        lastFrameTitles.clear();
        lastFrameNanos = 0;
        tall = wide = wasTall = wasWide = false;
    }

    private static String read(ClientTooltipComponent line) {
        if (!(line instanceof ClientTextTooltip)) return line == null ? "" : line.getClass().getName();
        FormattedCharSequence text = ((ClientTextTooltipAccessor) line).tooltipsimproved$getText();
        StringBuilder builder = new StringBuilder();
        text.accept((index, style, codePoint) -> {
            builder.appendCodePoint(codePoint);
            return true;
        });
        return builder.toString();
    }

    /** Whether {@link #read} would return {@code title}, without building a string every frame. */
    private static boolean reads(ClientTooltipComponent line, String title) {
        if (!(line instanceof ClientTextTooltip)) return read(line).equals(title);
        FormattedCharSequence text = ((ClientTextTooltipAccessor) line).tooltipsimproved$getText();
        int[] at = {0};
        boolean matched = text.accept((index, style, codePoint) -> {
            if (at[0] >= title.length() || title.codePointAt(at[0]) != codePoint) return false;
            at[0] += Character.charCount(codePoint);
            return true;
        });
        return matched && at[0] == title.length();
    }
}
