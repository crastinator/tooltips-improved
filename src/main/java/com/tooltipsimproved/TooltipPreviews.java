package com.tooltipsimproved;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.List;
import java.util.Optional;

//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}

/**
 * Pictures of what an item holds, shown in its tooltip: a book's first page, a map, a banner's
 * pattern, a firework's explosions. They go through vanilla's tooltip images, which report their
 * size, so wrapping, placement and scrolling treat them like any other part of a tooltip.
 */
public final class TooltipPreviews {
    private static final int BOTTOM_GAP = 2;

    /** A preview's data; {@code mixin/ClientTooltipComponentMixin} turns it into what's drawn. */
    public interface Preview extends TooltipComponent {
        ClientTooltipComponent renderer();
    }

    public static Optional<TooltipComponent> forStack(ItemStack stack, boolean shift) {
        // Other mods may ask off the render thread, where neither the font nor the page cache is safe to use
        if (!Minecraft.getInstance().isSameThread()) return Optional.empty();
        TooltipsImprovedConfig.Previews config = TooltipsImprovedConfig.get().previews;
        if (config.showWhen == TooltipsImprovedConfig.ShowWhen.SHIFT_HELD && !shift) return Optional.empty();

        Preview preview = null;
        if (config.books) preview = book(stack, config.bookLines);
        if (preview == null && config.maps) preview = map(stack, config.mapSize);
        if (preview == null && config.banners) preview = banner(stack);
        if (preview == null && config.fireworks) preview = fireworks(stack);
        return Optional.ofNullable(preview);
    }

    private abstract static class Renderer implements ClientTooltipComponent {
        abstract int width(Font font);

        abstract int height(Font font);

        abstract void draw(PreviewCanvas canvas, Font font, int x, int y);

        @Override
        public int getWidth(Font font) {
            return width(font);
        }

        //? if >=1.21.2 {
        @Override
        public int getHeight(Font font) {
            return height(font);
        }
        //?} else {
        /*@Override
        public int getHeight() {
            return height(Minecraft.getInstance().font);
        }
        *///?}

        //? if >=26.1 {
        @Override
        public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
            draw(new PreviewCanvas(graphics), font, x, y);
        }
        //?} elif >=1.21.2 {
        /*@Override
        public void renderImage(Font font, int x, int y, int w, int h, GuiGraphics graphics) {
            draw(new PreviewCanvas(graphics), font, x, y);
        }
        *///?} else {
        /*@Override
        public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
            draw(new PreviewCanvas(graphics), font, x, y);
        }
        *///?}
    }

    private static final int PAGE_TEXT_WIDTH = 114, PAGE_PADDING = 4;
    private static final int PAGE_BACKGROUND = 0xFFF3E9CF, PAGE_BORDER = 0xFF8A7350, PAGE_TEXT = 0xFF000000;

    // Splitting a page into lines is too slow for every frame and the hovered book rarely changes.
    // Compared by identity as a stack keeps the same component, and comparing pages by value is slow too.
    private static Component splitPage;
    private static SplitSettings splitSettings;
    private static List<FormattedCharSequence> splitLines = List.of();

    /** Includes what changes how a page splits without closing a screen, which is when the cache is cleared. */
    private record SplitSettings(int maxLines, boolean unicodeFont, boolean japaneseGlyphs, String language) {}

    record BookPreview(List<FormattedCharSequence> lines) implements Preview {
        @Override
        public ClientTooltipComponent renderer() {
            return new Renderer() {
                @Override
                int width(Font font) {
                    return PAGE_TEXT_WIDTH + 2 * PAGE_PADDING;
                }

                @Override
                int height(Font font) {
                    return lines.size() * font.lineHeight + 2 * PAGE_PADDING + BOTTOM_GAP;
                }

                @Override
                void draw(PreviewCanvas canvas, Font font, int x, int y) {
                    int bottom = y + height(font) - BOTTOM_GAP, right = x + width(font);
                    canvas.fill(x, y, right, bottom, PAGE_BORDER);
                    canvas.fill(x + 1, y + 1, right - 1, bottom - 1, PAGE_BACKGROUND);
                    for (int i = 0; i < lines.size(); i++) {
                        canvas.text(font, lines.get(i), x + PAGE_PADDING, y + PAGE_PADDING + i * font.lineHeight, PAGE_TEXT);
                    }
                }
            };
        }
    }

    private static Preview book(ItemStack stack, int maxLines) {
        WrittenBookContent content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (content == null || !TooltipExtras.shows(stack, DataComponents.WRITTEN_BOOK_CONTENT)) return null;
        Minecraft minecraft = Minecraft.getInstance();
        List<Component> pages = content.getPages(minecraft.isTextFilteringEnabled());
        if (pages.isEmpty()) return null;

        Component page = pages.get(0);
        SplitSettings settings = new SplitSettings(maxLines, minecraft.options.forceUnicodeFont().get(),
            minecraft.options.japaneseGlyphVariants().get(), minecraft.getLanguageManager().getSelected());
        if (page != splitPage || !settings.equals(splitSettings)) {
            List<FormattedCharSequence> lines = minecraft.font.split(page, PAGE_TEXT_WIDTH);
            splitLines = List.copyOf(lines.subList(0, Math.min(lines.size(), maxLines)));
            splitPage = page;
            splitSettings = settings;
        }
        return splitLines.isEmpty() ? null : new BookPreview(splitLines);
    }

    /** Forgets split pages, e.g. when a screen closes, as the font may change before the next one. */
    public static void clearCache() {
        splitPage = null;
        splitLines = List.of();
    }

    /** @param data the map's contents, {@code null} when the server hasn't sent them */
    record MapPreview(MapId id, MapItemSavedData data, int size) implements Preview {
        private static final int NOT_LOADED_COLOR = 0xFFAAAAAA;
        private static final Component NOT_LOADED = Component.translatable(TooltipsImproved.MOD_ID + ".preview.map_not_loaded");

        @Override
        public ClientTooltipComponent renderer() {
            return new Renderer() {
                @Override
                int width(Font font) {
                    return data == null ? font.width(NOT_LOADED) : size;
                }

                @Override
                int height(Font font) {
                    return (data == null ? font.lineHeight : size) + BOTTOM_GAP;
                }

                @Override
                void draw(PreviewCanvas canvas, Font font, int x, int y) {
                    if (data == null) canvas.text(font, NOT_LOADED, x, y, NOT_LOADED_COLOR);
                    else canvas.map(id, data, x, y, size);
                }
            };
        }
    }

    private static Preview map(ItemStack stack, int size) {
        MapId id = stack.get(DataComponents.MAP_ID);
        if (id == null || !(stack.getItem() instanceof MapItem) || !TooltipExtras.shows(stack, DataComponents.MAP_ID)) return null;
        // The client only knows maps it's been sent: held, in a frame nearby, or recently viewed
        Level level = Minecraft.getInstance().level;
        return new MapPreview(id, level == null ? null : MapItem.getSavedData(id, level), size);
    }

    record BannerPreview(DyeColor base, BannerPatternLayers layers) implements Preview {
        @Override
        public ClientTooltipComponent renderer() {
            return new Renderer() {
                @Override
                int width(Font font) {
                    return PreviewCanvas.BANNER_WIDTH;
                }

                @Override
                int height(Font font) {
                    return PreviewCanvas.BANNER_HEIGHT + BOTTOM_GAP;
                }

                @Override
                void draw(PreviewCanvas canvas, Font font, int x, int y) {
                    canvas.banner(base, layers, x, y);
                }
            };
        }
    }

    private static Preview banner(ItemStack stack) {
        BannerPatternLayers layers = stack.get(DataComponents.BANNER_PATTERNS);
        // A plain banner shows nothing its icon doesn't
        if (layers == null || layers.layers().isEmpty() || !TooltipExtras.shows(stack, DataComponents.BANNER_PATTERNS)) return null;
        DyeColor base = stack.getItem() instanceof BannerItem banner ? banner.getColor() : stack.getOrDefault(DataComponents.BASE_COLOR, DyeColor.WHITE);
        return new BannerPreview(base, layers);
    }

    private static final int ICON_SIZE = 7, SWATCH_SIZE = 5, SWATCH_GAP = 1, FADE_GAP = 4, ROW_GAP = 1;
    // Commands can make these as long as they like; survival tops out well below both
    private static final int MAX_ROWS = 16, MAX_SWATCHES = 16;
    private static final int DEFAULT_ICON_COLOR = 0xFFFFFFFF, FADE_MARK_COLOR = 0xFF808080;

    record FireworkPreview(List<FireworkExplosion> explosions) implements Preview {
        @Override
        public ClientTooltipComponent renderer() {
            return new Renderer() {
                @Override
                int width(Font font) {
                    int width = 0;
                    for (FireworkExplosion explosion : explosions) width = Math.max(width, rowWidth(explosion));
                    return width;
                }

                @Override
                int height(Font font) {
                    return explosions.size() * (ICON_SIZE + ROW_GAP) - ROW_GAP + BOTTOM_GAP;
                }

                @Override
                void draw(PreviewCanvas canvas, Font font, int x, int y) {
                    for (FireworkExplosion explosion : explosions) {
                        drawRow(canvas, explosion, x, y);
                        y += ICON_SIZE + ROW_GAP;
                    }
                }
            };
        }

        private static int rowWidth(FireworkExplosion explosion) {
            int width = ICON_SIZE + SWATCH_GAP + swatchesWidth(explosion.colors());
            return explosion.fadeColors().isEmpty() ? width : width + FADE_GAP + swatchesWidth(explosion.fadeColors());
        }

        private static int swatchesWidth(IntList colors) {
            return Math.min(colors.size(), MAX_SWATCHES) * (SWATCH_SIZE + SWATCH_GAP);
        }

        private static void drawRow(PreviewCanvas canvas, FireworkExplosion explosion, int x, int y) {
            IntList colors = explosion.colors();
            drawIcon(canvas, explosion.shape(), colors.isEmpty() ? DEFAULT_ICON_COLOR : opaque(colors.getInt(0)), x, y);
            int at = drawSwatches(canvas, colors, x + ICON_SIZE + SWATCH_GAP, y);
            if (explosion.fadeColors().isEmpty()) return;
            canvas.fill(at, y + ICON_SIZE / 2, at + FADE_GAP - SWATCH_GAP, y + ICON_SIZE / 2 + 1, FADE_MARK_COLOR);
            drawSwatches(canvas, explosion.fadeColors(), at + FADE_GAP, y);
        }

        /** @return where the row continues */
        private static int drawSwatches(PreviewCanvas canvas, IntList colors, int x, int y) {
            int top = y + (ICON_SIZE - SWATCH_SIZE) / 2;
            for (int i = 0; i < Math.min(colors.size(), MAX_SWATCHES); i++) {
                canvas.fill(x, top, x + SWATCH_SIZE, top + SWATCH_SIZE, opaque(colors.getInt(i)));
                x += SWATCH_SIZE + SWATCH_GAP;
            }
            return x;
        }

        private static void drawIcon(PreviewCanvas canvas, FireworkExplosion.Shape shape, int color, int x, int y) {
            String[] icon = icon(shape);
            for (int row = 0; row < ICON_SIZE; row++) {
                for (int column = 0; column < ICON_SIZE; column++) {
                    if (icon[row].charAt(column) == '#') canvas.fill(x + column, y + row, x + column + 1, y + row + 1, color);
                }
            }
        }

        private static int opaque(int rgb) {
            return 0xFF000000 | rgb;
        }

        private static String[] icon(FireworkExplosion.Shape shape) {
            return switch (shape) {
                case SMALL_BALL -> SMALL_BALL_ICON;
                case LARGE_BALL -> LARGE_BALL_ICON;
                case STAR -> STAR_ICON;
                case CREEPER -> CREEPER_ICON;
                case BURST -> BURST_ICON;
                // NeoForge lets mods add shapes
                default -> LARGE_BALL_ICON;
            };
        }
    }

    private static final String[] SMALL_BALL_ICON = {
        ".......",
        ".......",
        "..###..",
        "..###..",
        "..###..",
        ".......",
        ".......",
    };
    private static final String[] LARGE_BALL_ICON = {
        "..###..",
        ".#####.",
        "#######",
        "#######",
        "#######",
        ".#####.",
        "..###..",
    };
    private static final String[] STAR_ICON = {
        "...#...",
        "...#...",
        "#######",
        ".#####.",
        "..###..",
        ".##.##.",
        ".#...#.",
    };
    private static final String[] CREEPER_ICON = {
        ".......",
        ".##.##.",
        ".##.##.",
        "...#...",
        "..###..",
        "..###..",
        "..#.#..",
    };
    private static final String[] BURST_ICON = {
        "#..#..#",
        ".#.#.#.",
        "..###..",
        "#######",
        "..###..",
        ".#.#.#.",
        "#..#..#",
    };

    private static Preview fireworks(ItemStack stack) {
        FireworkExplosion star = stack.get(DataComponents.FIREWORK_EXPLOSION);
        if (star != null && TooltipExtras.shows(stack, DataComponents.FIREWORK_EXPLOSION)) return new FireworkPreview(List.of(star));
        Fireworks rocket = stack.get(DataComponents.FIREWORKS);
        if (rocket == null || rocket.explosions().isEmpty() || !TooltipExtras.shows(stack, DataComponents.FIREWORKS)) return null;
        List<FireworkExplosion> explosions = rocket.explosions();
        return new FireworkPreview(explosions.subList(0, Math.min(explosions.size(), MAX_ROWS)));
    }
}
