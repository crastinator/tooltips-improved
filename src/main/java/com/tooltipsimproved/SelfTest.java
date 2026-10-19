package com.tooltipsimproved;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//? if >=1.21.5 {
import net.minecraft.core.component.DataComponentMap;
//?}
//? if >=1.21.2 {
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPatterns;
//?} else {
/*import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.armortrim.TrimPatterns;
*///?}

/**
 * In-game checks for development, run on the title screen when the game is started with
 * {@code -Dtooltipsimproved.selftest=true} (Gradle: {@code runClient -Pselftest}). Logs one line per
 * check and a summary, then quits. Mixin targets are loaded first, so a hook that no longer matches
 * its target fails here rather than in someone's game.
 */
public final class SelfTest {
    public static final boolean ENABLED = Boolean.getBoolean(TooltipsImproved.MOD_ID + ".selftest");
    private static final String PREFIX = "[Tooltips Improved self-test] ";
    private static boolean ran;
    private static int passed, failed;

    public static void runOnce() {
        if (!ENABLED || ran) return;
        ran = true;

        TooltipsImprovedConfig config = TooltipsImprovedConfig.get();
        configBefore = new com.google.gson.Gson().toJson(config);
        check("mixin targets load", SelfTest::loadMixinTargets);
        check("config file written", () -> require(Files.exists(Minecraft.getInstance().gameDirectory.toPath()
            .resolve("config").resolve(TooltipsImproved.MOD_ID + ".json")), "missing"));
        check("keybinds registered", () -> {
            List<KeyMapping> all = Arrays.asList(Minecraft.getInstance().options.keyMappings);
            for (KeyMapping mapping : TooltipsImprovedKeys.ALL) require(all.contains(mapping), mapping.getName());
        });
        check("trim folds to one line", () -> withField(() -> config.foldTrims = true, () -> {
            List<Component> lines = trimTooltip();
            require(lines.size() == 1, "got " + lines.size() + " lines");
            require(lines.get(0).getString().equals("Sentry Armor Trim (Iron)"), "got '" + lines.get(0).getString() + "'");
        }));
        check("trim unfolded when disabled", () -> withField(() -> config.foldTrims = false, () -> {
            List<Component> lines = trimTooltip();
            require(lines.size() == 3, "got " + lines.size() + " lines");
        }));
        check("wrap splits wide lines and keeps style", () -> withField(() -> config.wrap = TooltipsImprovedConfig.WrapMode.SCREEN,
            SelfTest::checkWrap));
        check("wrap off leaves lines alone", () -> withField(() -> config.wrap = TooltipsImprovedConfig.WrapMode.OFF, () -> {
                List<ClientTooltipComponent> lines = List.of(text(Component.literal(longText())));
                require(wrap(lines, true) == lines, "list was replaced");
            }));
        check("placement leaves fitting tooltips alone", () -> {
            ClientTooltipPositioner placed = TooltipPlacement.wrap(DefaultTooltipPositioner.INSTANCE);
            Vector2ic vanilla = DefaultTooltipPositioner.INSTANCE.positionTooltip(400, 300, 100, 100, 60, 30);
            require(placed.positionTooltip(400, 300, 100, 100, 60, 30).equals(vanilla), "moved");
        });
        for (TooltipsImprovedConfig.Placement placement : List.of(TooltipsImprovedConfig.Placement.CENTER_WHEN_CRAMPED,
            TooltipsImprovedConfig.Placement.CORNER_WHEN_CRAMPED, TooltipsImprovedConfig.Placement.CORNER)) {
            check("placement " + placement + " clears the cursor", () -> withField(() -> config.placement = placement, () -> {
                    // 300 wide on a 400 wide screen fits on neither side of a centered cursor
                    Vector2ic at = TooltipPlacement.wrap(DefaultTooltipPositioner.INSTANCE).positionTooltip(400, 300, 200, 150, 300, 60);
                    require(at.x() >= 0 && at.x() + 300 <= 400 && at.y() >= 0 && at.y() + 60 <= 300, "off screen at " + at);
                    boolean coversCursor = at.x() - 4 < 200 && at.x() + 304 > 200 && at.y() - 4 < 150 && at.y() + 64 > 150;
                    require(!coversCursor, "covers the cursor at " + at);
                }));
        }
        check("tall tooltips show their top", () -> require(TooltipPlacement.keepTopInView(new Vector2i(10, -50)).y() > 0, "still above the screen"));
        check("button tooltips go under the button", () -> {
            Vector2ic at = TooltipPlacement.besideButton(new ScreenRectangle(100, 100, 50, 20)).positionTooltip(400, 300, 0, 0, 80, 20);
            require(at.y() >= 120 && at.x() + 40 == 125, "at " + at);
        });
        check("scroll lands exactly and resets", () -> withField(() -> {
            config.scrollWheel = true;
            // The check redraws the tooltip before each scroll, however long the checks take
            config.resetWhenHidden = false;
        }, SelfTest::checkScroll));
        check("wheel leaves fitting tooltips alone", () -> withField(() -> config.scrollWheel = true, () -> {
            TooltipScroll.reset();
            TooltipLines.prepare(List.of(text(Component.literal("Short")), text(Component.literal("Fits on screen"))), new Object());
            require(!TooltipScroll.onScroll(0, -1), "the wheel event was taken");
            require(settle(new Vector2i(50, 50)).equals(new Vector2i(50, 50)), "a fitting tooltip moved");
            TooltipScroll.reset();
        }));
        check("a scrolled tooltip stays partly on screen", () -> withField(() -> config.scrollWheel = true, () -> {
            TooltipScroll.reset();
            List<ClientTooltipComponent> lines = tall("Far");
            int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            Vector2ic origin = new Vector2i(50, 50);
            for (int i = 0; i < 1000; i++) scroll(lines, -1);
            require(settle(origin).y() <= screenHeight - 24, "scrolled off the bottom to " + settle(origin));
            for (int i = 0; i < 2000; i++) scroll(lines, 1);
            int height = 0;
            for (ClientTooltipComponent line : TooltipLines.prepare(lines, new Object())) height += TooltipLines.height(line, Minecraft.getInstance().font);
            require(settle(origin).y() + height >= 24, "scrolled off the top to " + settle(origin));
            TooltipScroll.reset();
        }));
        check("wrapping leaves other mods' text lines alone", () -> withField(() -> config.wrap = TooltipsImprovedConfig.WrapMode.SCREEN, () -> {
            ClientTooltipComponent custom = new ClientTextTooltip(Component.literal(longText()).getVisualOrderText()) {};
            List<ClientTooltipComponent> lines = List.of(custom);
            require(wrap(lines, true) == lines, "a subclass was rebuilt");
        }));
        check("tooltips drawn together keep the offset", () -> withField(() -> config.scrollWheel = true, () -> {
            TooltipScroll.reset();
            List<ClientTooltipComponent> main = tall("Main"), extra = List.of(text(Component.literal("Extra")));
            Vector2ic origin = new Vector2i(50, 50);
            for (int frame = 0; frame < 3; frame++) {
                Object graphics = new Object();
                TooltipLines.prepare(main, graphics);
                TooltipLines.prepare(extra, graphics);
                require(TooltipScroll.onScroll(0, -1), "the tall tooltip didn't scroll");
            }
            require(settle(origin).y() == 50 + 3 * config.wheelStep, "settled at " + settle(origin));
            TooltipScroll.reset();
        }));
        check("prepared lines aren't processed twice", () -> withField(() -> {
            config.scrollWheel = true;
            config.wrap = TooltipsImprovedConfig.WrapMode.SCREEN;
        }, SelfTest::checkPreparedTwice));
        check("broken config file is kept", SelfTest::checkBrokenConfig);
        //? if <26.3 {
        /*check("first line gap removed without changing height", () -> withField(() -> config.removeFirstLineGap = true, () -> {
                var font = Minecraft.getInstance().font;
                List<ClientTooltipComponent> two = List.of(text(Component.literal("Title")), text(Component.literal("Line")));
                List<ClientTooltipComponent> gapless = TooltipLines.removeFirstLineGap(two);
                //? if >=1.21.2 {
                int before = two.get(0).getHeight(font), after = gapless.get(0).getHeight(font);
                //?} else {
                /^int before = two.get(0).getHeight(), after = gapless.get(0).getHeight();
                ^///?}
                require(after == before - 2, "first line height " + before + " -> " + after);
                List<ClientTooltipComponent> one = List.of(text(Component.literal("Only")));
                require(TooltipLines.removeFirstLineGap(one) == one, "single line tooltip changed");
            }));
        *///?}
        check("background fades", () -> withField(() -> config.backgroundOpacity = 0.5, () -> {
            int faded = TooltipBackground.fade(0xF0100010);
            require(faded == 0x78100010, Integer.toHexString(faded));
        }));

        check("right-to-left lines aren't wrapped", () -> withField(() -> config.wrap = TooltipsImprovedConfig.WrapMode.SCREEN, () -> {
            List<ClientTooltipComponent> lines = List.of(text(Component.literal("שלום עולם ".repeat(60))));
            require(wrap(lines, true) == lines, "Hebrew line was re-split");
        }));
        check("cursor wrap only applies beside the cursor", () -> withField(() -> config.wrap = TooltipsImprovedConfig.WrapMode.CURSOR, () -> {
            List<ClientTooltipComponent> lines = List.of(text(Component.literal(longText())));
            int besideButton = wrap(lines, false).size();
            config.wrap = TooltipsImprovedConfig.WrapMode.SCREEN;
            require(besideButton == wrap(lines, false).size(), "a tooltip away from the cursor was wrapped to the cursor's space");
        }));
        check("wrapped title keeps the gap under all of it", () -> withField(() -> config.wrap = TooltipsImprovedConfig.WrapMode.SCREEN, () -> {
            var font = Minecraft.getInstance().font;
            ClientTooltipComponent body = text(Component.literal("Body"));
            int plain = TooltipLines.height(body, font);
            List<ClientTooltipComponent> wrapped = wrap(List.of(text(Component.literal(longText())), body), true);
            int parts = wrapped.size() - 1;
            require(parts > 1, "title wasn't wrapped");
            // Vanilla adds its gap after the first line; it belongs after the last part instead
            require(TooltipLines.height(wrapped.get(0), font) == plain - 2, "first part keeps the gap");
            require(TooltipLines.height(wrapped.get(parts - 1), font) == plain + 2, "last part has no gap");
            require(wrapped.get(parts) == body, "body line was replaced");
            List<ClientTooltipComponent> alone = wrap(List.of(text(Component.literal(longText()))), true);
            require(TooltipLines.height(alone.get(alone.size() - 1), font) == plain, "a title with nothing under it got a gap");
            List<ClientTooltipComponent> gapless = TooltipWrap.wrap(List.of(text(Component.literal(longText())), body), true, true, false);
            require(TooltipLines.height(gapless.get(parts - 1), font) == plain, "gap kept with removeFirstLineGap");
        }));
        check("wrapping the same line again reuses the split", () -> withField(() -> config.wrap = TooltipsImprovedConfig.WrapMode.SCREEN, () -> {
            List<ClientTooltipComponent> lines = List.of(text(Component.literal("Title")), text(Component.literal(longText())));
            var first = ((com.tooltipsimproved.mixin.ClientTextTooltipAccessor) wrap(lines, true).get(1)).tooltipsimproved$getText();
            var again = ((com.tooltipsimproved.mixin.ClientTextTooltipAccessor) wrap(lines, true).get(1)).tooltipsimproved$getText();
            require(first == again, "split again");
        }));
        check("unbound and mouse-bound keys read as released", () -> {
            KeyMapping mouse = new KeyMapping("key.tooltipsimproved.test", com.mojang.blaze3d.platform.InputConstants.Type.MOUSE, 4,
                TooltipsImprovedKeys.UP.getCategory());
            require(!TooltipsImprovedKeys.isHeld(mouse), "a mouse button counted as held");
            require(!TooltipsImprovedKeys.isHeld(TooltipsImprovedKeys.RESET), "unbound key counted as held");
        });
        check("sideways key defaults to Left Alt", () -> require(
            TooltipsImprovedKeys.SIDEWAYS.getDefaultKey().getValue() == com.mojang.blaze3d.platform.InputConstants.KEY_LALT,
            TooltipsImprovedKeys.SIDEWAYS.getDefaultKey().getName()));
        check("a reused list is prepared every frame", () -> withField(() -> config.scrollWheel = true, () -> {
            // Some mods keep the same list between frames; it must still count as a new frame
            TooltipScroll.reset();
            List<ClientTooltipComponent> reused = new ArrayList<>(tall("Reused"));
            TooltipLines.prepare(reused, new Object());
            require(TooltipScroll.onScroll(0, -1), "didn't scroll");
            reused.set(0, text(Component.literal("Something else")));
            TooltipLines.prepare(reused, new Object());
            require(TooltipScroll.offset(new Vector2i(50, 50), frame).equals(new Vector2i(50, 50)), "changed title in a reused list was missed");
            TooltipScroll.reset();
        }));
        check("closing settings without changes doesn't save", () -> {
            java.nio.file.Path file = Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(TooltipsImproved.MOD_ID + ".json");
            var before = Files.getLastModifiedTime(file);
            Thread.sleep(20);
            TooltipsImprovedScreen screen = openScreen();
            screen.removed();
            require(Files.getLastModifiedTime(file).equals(before), "config was rewritten");
        });
        check("settings screen builds", () -> require(!openScreen().children().isEmpty(), "no widgets"));
        //? if neoforge {
        check("mod list opens the settings", () -> {
            var container = net.neoforged.fml.ModList.get().getModContainerById(TooltipsImproved.MOD_ID).orElseThrow();
            var factory = container.getCustomExtension(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class);
            require(factory.isPresent(), "no config screen registered");
            require(factory.get().createScreen(container, null) instanceof TooltipsImprovedScreen, "wrong screen");
        });
        //?} elif forge {
        /*check("mod list opens the settings", () -> {
            //? if >=26.1 {
            var container = net.minecraftforge.fml.ModList.getModContainerById(TooltipsImproved.MOD_ID).orElseThrow();
            //?} else {
            /^var container = net.minecraftforge.fml.ModList.get().getModContainerById(TooltipsImproved.MOD_ID).orElseThrow();
            ^///?}
            var factory = net.minecraftforge.client.ConfigScreenHandler.getScreenFactoryFor(container.getModInfo());
            require(factory.isPresent(), "no config screen registered");
            require(factory.get().apply(Minecraft.getInstance(), null) instanceof TooltipsImprovedScreen, "wrong screen");
        });
        *///?} elif fabric {
        /*if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("modmenu")) {
            check("Mod Menu opens the settings", () -> {
                Object screen = Class.forName("com.terraformersmc.modmenu.ModMenu")
                    .getMethod("getConfigScreen", String.class, net.minecraft.client.gui.screens.Screen.class)
                    .invoke(null, TooltipsImproved.MOD_ID, null);
                require(screen instanceof TooltipsImprovedScreen, "got " + screen);
            });
        }
        *///?}

        TooltipsImproved.LOGGER.info(PREFIX + "{} ({} passed, {} failed)", failed == 0 ? "PASS" : "FAIL", passed, failed);
        Minecraft.getInstance().stop();
    }

    // Loading a class applies its mixins, which throw here if a target no longer matches
    private static void loadMixinTargets() throws ClassNotFoundException {
        for (Class<?> target : List.of(ArmorTrim.class, ClientTextTooltip.class, KeyMapping.class,
            net.minecraft.client.MouseHandler.class, net.minecraft.client.Options.class,
            net.minecraft.client.gui.screens.Screen.class, net.minecraft.client.gui.screens.TitleScreen.class,
            //? if >=26.1 {
            net.minecraft.client.gui.GuiGraphicsExtractor.class,
            //?} else {
            /*net.minecraft.client.gui.GuiGraphics.class,
            *///?}
            net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil.class,
            net.minecraft.client.gui.components.Tooltip.class, net.minecraft.client.gui.components.WidgetTooltipHolder.class)) {
            Class.forName(target.getName(), true, SelfTest.class.getClassLoader());
        }
    }

    /** The settings screen, laid out at the window's size as if it had been opened. */
    private static TooltipsImprovedScreen openScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        TooltipsImprovedScreen screen = new TooltipsImprovedScreen(null);
        int width = minecraft.getWindow().getGuiScaledWidth(), height = minecraft.getWindow().getGuiScaledHeight();
        //? if >=1.21.9 {
        screen.init(width, height);
        //?} else {
        /*screen.init(minecraft, width, height);
        *///?}
        return screen;
    }

    private static List<Component> trimTooltip() {
        //? if >=26.3 {
        HolderLookup.Provider registries = VanillaRegistries.createWorldLookup();
        //?} else {
        /*HolderLookup.Provider registries = VanillaRegistries.createLookup();
        *///?}
        ArmorTrim trim = new ArmorTrim(
            registries.lookupOrThrow(Registries.TRIM_MATERIAL).getOrThrow(TrimMaterials.IRON),
            registries.lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(TrimPatterns.SENTRY));
        List<Component> lines = new ArrayList<>();
        //? if >=1.21.5 {
        trim.addToTooltip(Item.TooltipContext.of(registries), lines::add, TooltipFlag.NORMAL, DataComponentMap.EMPTY);
        //?} else {
        /*trim.addToTooltip(Item.TooltipContext.of(registries), lines::add, TooltipFlag.NORMAL);
        *///?}
        return lines;
    }

    private static void checkWrap() {
        Minecraft minecraft = Minecraft.getInstance();
        int limit = minecraft.getWindow().getGuiScaledWidth() - 2 * TooltipPlacement.EDGE - 2 * TooltipPlacement.FRAME;
        Component title = Component.literal("Title");
        Component body = Component.literal(longText()).withStyle(ChatFormatting.GOLD);
        ClientTooltipComponent titleLine = text(title);
        List<ClientTooltipComponent> wrapped = wrap(List.of(titleLine, text(body)), true);

        require(wrapped.size() > 2, "not split, " + wrapped.size() + " lines");
        require(wrapped.get(0) == titleLine, "fitting line was replaced");
        StringBuilder joined = new StringBuilder();
        for (ClientTooltipComponent line : wrapped.subList(1, wrapped.size())) {
            require(line.getWidth(minecraft.font) <= limit, "line wider than " + limit);
            ((com.tooltipsimproved.mixin.ClientTextTooltipAccessor) line).tooltipsimproved$getText().accept((index, style, codePoint) -> {
                require(TextColor.fromLegacyFormat(ChatFormatting.GOLD).equals(style.getColor()), "lost its colour");
                joined.appendCodePoint(codePoint);
                return true;
            });
        }
        require(joined.toString().replace(" ", "").equals(longText().replace(" ", "")), "text changed");
    }

    private static void checkScroll() {
        TooltipsImprovedConfig config = TooltipsImprovedConfig.get();
        int step = config.wheelStep;
        Vector2ic origin = new Vector2i(50, 50);
        TooltipScroll.reset();
        List<ClientTooltipComponent> first = tall("First");

        // Simulated 60 FPS frames: the glide has to land exactly, in both directions
        scroll(first, -1);
        require(settle(origin).y() == 50 + step, "wheel down settled at " + settle(origin));
        scroll(first, 1);
        require(settle(origin).equals(origin), "scrolling back settled at " + settle(origin));
        scroll(first, 1);
        require(settle(origin).y() == 50 - step, "wheel up settled at " + settle(origin));

        // Trackpads send fractions of a notch
        scroll(first, -0.1);
        require(settle(origin).y() == 50 - step + Math.round(step * 0.1f), "a tenth of a notch settled at " + settle(origin));

        TooltipLines.prepare(tall("Second"), new Object());
        require(TooltipScroll.offset(origin, frame).equals(origin), "a different tooltip kept the offset");
        TooltipScroll.reset();
    }

    /** Draws the tooltip again as a new frame, then scrolls it, so the check doesn't depend on how fast it runs. */
    private static void scroll(List<ClientTooltipComponent> lines, double notches) {
        TooltipLines.prepare(lines, new Object());
        require(TooltipScroll.onScroll(0, notches), "the wheel didn't move the tooltip");
    }

    private static long frame;

    private static Vector2ic settle(Vector2ic origin) {
        Vector2ic at = origin;
        for (int i = 0; i < 300; i++) at = TooltipScroll.offset(origin, frame += 16_666_667L);
        return at;
    }

    private static void checkPreparedTwice() {
        // NeoForge passes the prepared lines on to its own overload, which mustn't reset or re-wrap them
        TooltipScroll.reset();
        List<ClientTooltipComponent> lines = tall(longText());
        Object graphics = new Object();
        List<ClientTooltipComponent> prepared = TooltipLines.prepare(lines, graphics);
        require(prepared.size() > lines.size(), "long title wasn't wrapped");
        require(TooltipScroll.onScroll(0, -1), "didn't scroll");
        require(TooltipLines.prepare(prepared, graphics) == prepared, "prepared lines were processed again");
        Vector2ic origin = new Vector2i(50, 50);
        require(!settle(origin).equals(origin), "the overload reset the scroll");
        TooltipScroll.reset();
    }

    private static void checkBrokenConfig() throws Exception {
        java.nio.file.Path dir = Files.createTempDirectory(TooltipsImproved.MOD_ID);
        java.nio.file.Path file = dir.resolve("config.json");
        String broken = "{ \"foldTrims\": false, \"wheelStep\": 25, }}";
        Files.writeString(file, broken);
        TooltipsImprovedConfig loaded = TooltipsImprovedConfig.load(file);
        require(loaded.foldTrims, "didn't fall back to defaults");
        require(Files.readString(file).equals(broken), "the unreadable file was overwritten");
        require(Files.readString(dir.resolve("config.json.broken")).equals(broken), "no backup copy");

        Files.writeString(file, "{ \"wheelStep\": 25 }");
        require(TooltipsImprovedConfig.load(file).wheelStep == 25, "a valid file wasn't read");
        require(Files.readString(file).contains("\"foldTrims\""), "missing options weren't written back");

        Files.writeString(file, "{ \"smoothness\": NaN, \"backgroundOpacity\": NaN, \"keySpeed\": 1 }");
        TooltipsImprovedConfig odd = TooltipsImprovedConfig.load(file);
        TooltipsImprovedConfig defaults = new TooltipsImprovedConfig();
        require(odd.smoothness == defaults.smoothness && odd.backgroundOpacity == defaults.backgroundOpacity, "NaN was kept");
        require(odd.keySpeed == TooltipsImprovedConfig.MIN_KEY_SPEED, "keySpeed " + odd.keySpeed);
        require(!Files.readString(file).contains("NaN"), "NaN was written back");
    }

    /** A tooltip taller than any screen, so the wheel moves it. */
    private static List<ClientTooltipComponent> tall(String title) {
        List<ClientTooltipComponent> lines = new ArrayList<>(List.of(text(Component.literal(title))));
        for (int i = 0; i < 400; i++) lines.add(text(Component.literal("Line " + i)));
        return lines;
    }

    /** Wraps as for a tooltip with vanilla's gap under the title. */
    private static List<ClientTooltipComponent> wrap(List<ClientTooltipComponent> lines, boolean followsCursor) {
        return TooltipWrap.wrap(lines, followsCursor, true, true);
    }

    private static String longText() {
        return "This is a deliberately long tooltip line that is far wider than any screen, ".repeat(6).trim();
    }

    private static ClientTooltipComponent text(Component component) {
        return new ClientTextTooltip(component.getVisualOrderText());
    }

    private interface Check {
        void run() throws Exception;
    }

    private static void check(String name, Check check) {
        try {
            try {
                check.run();
            } finally {
                restoreConfig();
            }
            passed++;
            TooltipsImproved.LOGGER.info(PREFIX + "ok   {}", name);
        } catch (Throwable e) {
            failed++;
            TooltipsImproved.LOGGER.error(PREFIX + "FAIL {}: {}", name, e.toString(), e);
        }
    }

    /** Runs a check with a config field changed; {@link #check} puts the config back afterwards. */
    private static void withField(Runnable set, Check check) throws Exception {
        set.run();
        check.run();
    }

    /** The config as it was before the checks, restored after each one so they all see the developer's settings. */
    private static String configBefore;

    private static void restoreConfig() throws IllegalAccessException {
        TooltipsImprovedConfig config = TooltipsImprovedConfig.get();
        TooltipsImprovedConfig before = new com.google.gson.Gson().fromJson(configBefore, TooltipsImprovedConfig.class);
        for (var field : TooltipsImprovedConfig.class.getFields()) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) field.set(config, field.get(before));
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
