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
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
        check("durability line formats and colors", () -> withField(() -> {
            config.durability.enabled = true;
            config.durability.showWhen = TooltipsImprovedConfig.DurabilityShowWhen.WHEN_DAMAGED;
            config.durability.format = TooltipsImprovedConfig.DurabilityFormat.BOTH;
            config.durability.colorByRemaining = true;
        }, SelfTest::checkDurabilityLine));
        check("durability line is skipped when it should be", () -> withField(() -> {
            config.durability.enabled = true;
            config.durability.showWhen = TooltipsImprovedConfig.DurabilityShowWhen.ALWAYS;
        }, SelfTest::checkDurabilitySkipped));
        check("durability percent stays honest at the ends", () -> {
            require(TooltipDurability.percent(1, 1561) == 1, "one use left read as 0%");
            require(TooltipDurability.percent(1560, 1561) == 99, "a damaged item read as 100%");
            require(TooltipDurability.percent(312, 1561) == 20, "312 / 1561");
        });
        check("comparison deltas use vanilla's basis", SelfTest::checkComparisonDeltas);
        check("comparison picks the right slot", SelfTest::checkComparisonSlot);
        check("comparison lines read and color correctly", () -> withField(() -> config.comparison.hideIdentical = true, SelfTest::checkComparisonLines));
        check("previews appear for what they should", () -> withField(() -> {
            config.previews = new TooltipsImprovedConfig.Previews();
            config.wrap = TooltipsImprovedConfig.WrapMode.SCREEN;
        }, SelfTest::checkPreviews));
        check("previews respect their settings", () -> withField(() -> config.previews = new TooltipsImprovedConfig.Previews(), SelfTest::checkPreviewSettings));
        check("previews report their size and survive wrapping", () -> withField(() -> config.previews = new TooltipsImprovedConfig.Previews(), SelfTest::checkPreviewSizes));
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
        for (Class<?> target : List.of(ArmorTrim.class, ClientTextTooltip.class, ClientTooltipComponent.class, KeyMapping.class, ItemStack.class,
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

    private static HolderLookup.Provider registries() {
        //? if >=26.3 {
        return VanillaRegistries.createWorldLookup();
        //?} else {
        /*return VanillaRegistries.createLookup();
        *///?}
    }

    /**
     * A stack the title screen can build. Since 26.1 items only get their components when a world
     * loads, and NeoForge rejects the ones built from the dev registry lookup, so each item used
     * here gets a minimal set instead; checks then set what they need on the stack on every version.
     */
    private static ItemStack stack(Item item) {
        //? if >=26.1 {
        var holder = item.builtInRegistryHolder();
        if (!holder.areComponentsBound()) {
            holder.bindComponents(DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 1).build());
        }
        //?}
        return new ItemStack(item);
    }

    private static ItemStack damageable(Item item, int maxDamage) {
        ItemStack stack = stack(item);
        stack.set(DataComponents.MAX_DAMAGE, maxDamage);
        stack.set(DataComponents.DAMAGE, 0);
        return stack;
    }

    private static List<Component> trimTooltip() {
        HolderLookup.Provider registries = registries();
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

    private static void checkDurabilityLine() {
        ItemStack stack = damageable(Items.DIAMOND_PICKAXE, 1561);
        require(TooltipDurability.line(stack, List.of(), false) == null, "shown on an undamaged item");
        stack.setDamageValue(stack.getMaxDamage() - 312);
        Component line = TooltipDurability.line(stack, List.of(), false);
        require(line != null, "not shown on a damaged item");
        require(line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents, "not translated");
        String expected = "312 / " + stack.getMaxDamage() + " (20";
        require(line.getString().contains(expected), "got '" + line.getString() + "'");
        int[] colors = {0};
        line.visit((style, text) -> {
            if (text.contains("312") && style.getColor() != null) colors[0] = style.getColor().getValue();
            return java.util.Optional.empty();
        }, Style.EMPTY);
        require(colors[0] == stack.getBarColor(), "color " + Integer.toHexString(colors[0]));
    }

    private static void checkDurabilitySkipped() {
        TooltipsImprovedConfig config = TooltipsImprovedConfig.get();
        ItemStack damaged = damageable(Items.IRON_SWORD, 250);
        damaged.setDamageValue(10);
        require(TooltipDurability.line(damaged, List.of(), false) != null, "not shown");
        Component vanilla = Component.translatable("item.durability", 1, 2);
        require(TooltipDurability.line(damaged, List.of(vanilla), false) == null, "doubled vanilla's line");
        require(TooltipDurability.line(stack(Items.DIRT), List.of(), false) == null, "shown on dirt");
        ItemStack unbreakable = damageable(Items.IRON_SWORD, 250);
        //? if >=1.21.5 {
        unbreakable.set(DataComponents.UNBREAKABLE, net.minecraft.util.Unit.INSTANCE);
        //?} else {
        /*unbreakable.set(DataComponents.UNBREAKABLE, new net.minecraft.world.item.component.Unbreakable(true));
        *///?}
        require(TooltipDurability.line(unbreakable, List.of(), false) == null, "shown on an unbreakable item");
        config.durability.showWhen = TooltipsImprovedConfig.DurabilityShowWhen.SHIFT_HELD;
        require(TooltipDurability.line(damaged, List.of(), false) == null, "shown without Shift");
        require(TooltipDurability.line(damaged, List.of(), true) != null, "not shown with Shift");
        config.durability.enabled = false;
        require(TooltipDurability.line(damaged, List.of(), true) == null, "shown while disabled");

        // The hook on the real method, including the immutable list vanilla returns for hidden tooltips
        config.durability.enabled = true;
        config.durability.showWhen = TooltipsImprovedConfig.DurabilityShowWhen.ALWAYS;
        config.durability.format = TooltipsImprovedConfig.DurabilityFormat.FRACTION;
        String expected = (damaged.getMaxDamage() - 10) + " / " + damaged.getMaxDamage();
        List<Component> lines = damaged.getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.NORMAL);
        require(lines.get(lines.size() - 1).getString().contains(expected), "hook added no line: " + lines);
        List<Component> advanced = damaged.getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.ADVANCED);
        require(advanced.stream().filter(line -> line.getString().contains(expected)).count() == 1, "doubled F3+H's line");
        ItemStack hidden = damaged.copy();
        //? if >=1.21.5 {
        hidden.set(DataComponents.TOOLTIP_DISPLAY, new net.minecraft.world.item.component.TooltipDisplay(true, new java.util.LinkedHashSet<>()));
        //?} else {
        /*hidden.set(DataComponents.HIDE_TOOLTIP, net.minecraft.util.Unit.INSTANCE);
        *///?}
        require(hidden.getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.NORMAL).isEmpty(), "added to a hidden tooltip");
    }

    private static ItemAttributeModifiers weapon(double damage, double speed) {
        return ItemAttributeModifiers.builder()
            .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, damage, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, speed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .build();
    }

    private static ItemAttributeModifiers armor(net.minecraft.core.Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation, EquipmentSlotGroup group) {
        return ItemAttributeModifiers.builder().add(attribute, new AttributeModifier(TooltipsImproved.id("test"), amount, operation), group).build();
    }

    private static void requireDelta(List<TooltipComparison.AttributeDelta> deltas, net.minecraft.core.Holder<Attribute> attribute, double shown) {
        for (TooltipComparison.AttributeDelta delta : deltas) {
            if (delta.attribute() == attribute) {
                require(Math.abs(delta.displayAmount() - shown) < 1e-6, attribute.getRegisteredName() + " was " + delta.displayAmount() + ", not " + shown);
                return;
            }
        }
        throw new AssertionError(attribute.getRegisteredName() + " missing from " + deltas);
    }

    private static void checkComparisonDeltas() {
        ItemAttributeModifiers sword = weapon(5, -2.4), axe = weapon(8, -3.2);
        AttributeModifier.Operation add = AttributeModifier.Operation.ADD_VALUE;

        List<TooltipComparison.AttributeDelta> swordVsAxe = TooltipComparison.deltas(sword, axe, EquipmentSlot.MAINHAND);
        require(swordVsAxe.size() == 2, "got " + swordVsAxe);
        requireDelta(swordVsAxe, Attributes.ATTACK_DAMAGE, -3);
        requireDelta(swordVsAxe, Attributes.ATTACK_SPEED, 0.8);

        // Nothing equipped: the real change from bare hands, not the 6 and 1.6 vanilla displays
        List<TooltipComparison.AttributeDelta> swordVsEmpty = TooltipComparison.deltas(sword, ItemAttributeModifiers.EMPTY, EquipmentSlot.MAINHAND);
        requireDelta(swordVsEmpty, Attributes.ATTACK_DAMAGE, 5);
        requireDelta(swordVsEmpty, Attributes.ATTACK_SPEED, -2.4);

        ItemAttributeModifiers lucky = armor(Attributes.LUCK, 1, add, EquipmentSlotGroup.MAINHAND);
        requireDelta(TooltipComparison.deltas(lucky, sword, EquipmentSlot.MAINHAND), Attributes.LUCK, 1);
        requireDelta(TooltipComparison.deltas(sword, lucky, EquipmentSlot.MAINHAND), Attributes.LUCK, -1);

        ItemAttributeModifiers knockback = armor(Attributes.KNOCKBACK_RESISTANCE, 0.1, add, EquipmentSlotGroup.CHEST);
        requireDelta(TooltipComparison.deltas(knockback, ItemAttributeModifiers.EMPTY, EquipmentSlot.CHEST), Attributes.KNOCKBACK_RESISTANCE, 1);
        ItemAttributeModifiers flat = armor(Attributes.ARMOR, 2, add, EquipmentSlotGroup.CHEST);
        ItemAttributeModifiers percent = armor(Attributes.ARMOR, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, EquipmentSlotGroup.CHEST);
        List<TooltipComparison.AttributeDelta> mixed = TooltipComparison.deltas(flat, percent, EquipmentSlot.CHEST);
        require(mixed.size() == 2, "operations were merged: " + mixed);

        require(TooltipComparison.deltas(sword, sword, EquipmentSlot.MAINHAND).isEmpty(), "identical items differ");
        require(TooltipComparison.deltas(armor(Attributes.ARMOR, 0.1 + 0.2, add, EquipmentSlotGroup.HEAD), armor(Attributes.ARMOR, 0.3, add, EquipmentSlotGroup.HEAD),
            EquipmentSlot.HEAD).isEmpty(), "floating point noise was shown");
        require(TooltipComparison.deltas(flat, ItemAttributeModifiers.EMPTY, EquipmentSlot.HEAD).isEmpty(), "counted another slot's modifier");

        //? if >=1.21.5 {
        var modifier = new AttributeModifier(TooltipsImproved.id("test"), 5, add);
        for (var display : List.of(ItemAttributeModifiers.Display.hidden(), ItemAttributeModifiers.Display.override(Component.literal("Secret")))) {
            var concealed = new ItemAttributeModifiers(List.of(new ItemAttributeModifiers.Entry(Attributes.ARMOR, modifier, EquipmentSlotGroup.CHEST, display)));
            require(TooltipComparison.deltas(concealed, ItemAttributeModifiers.EMPTY, EquipmentSlot.CHEST).isEmpty(), "showed a modifier displayed as " + display);
        }
        //?}
    }

    private static void checkComparisonSlot() {
        ItemAttributeModifiers none = ItemAttributeModifiers.EMPTY;
        AttributeModifier.Operation add = AttributeModifier.Operation.ADD_VALUE;
        require(TooltipComparison.slotFor(EquipmentSlot.HEAD, armor(Attributes.ARMOR, 2, add, EquipmentSlotGroup.HEAD)) == EquipmentSlot.HEAD, "armor");
        require(TooltipComparison.slotFor(EquipmentSlot.CHEST, none) == EquipmentSlot.CHEST, "elytra-like item");
        require(TooltipComparison.slotFor(EquipmentSlot.MAINHAND, weapon(5, -2.4)) == EquipmentSlot.MAINHAND, "weapon");
        require(TooltipComparison.slotFor(EquipmentSlot.MAINHAND, none) == null, "a plain item was compared");
        require(TooltipComparison.slotFor(EquipmentSlot.OFFHAND, none) == null, "a shield was compared");
        require(TooltipComparison.slotFor(EquipmentSlot.MAINHAND, armor(Attributes.ARMOR, 3, add, EquipmentSlotGroup.BODY)) == null, "animal armor was compared");
    }

    private static void checkComparisonLines() {
        TooltipsImprovedConfig config = TooltipsImprovedConfig.get();
        AttributeModifier.Operation add = AttributeModifier.Operation.ADD_VALUE;
        ItemStack hovered = stack(Items.IRON_CHESTPLATE), equipped = stack(Items.DIAMOND_CHESTPLATE);
        hovered.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
            .add(Attributes.ARMOR, new AttributeModifier(TooltipsImproved.id("armor"), 6, add), EquipmentSlotGroup.CHEST)
            .add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(TooltipsImproved.id("toughness"), 2, add), EquipmentSlotGroup.CHEST).build());
        equipped.set(DataComponents.ATTRIBUTE_MODIFIERS, armor(Attributes.ARMOR, 8, add, EquipmentSlotGroup.CHEST));

        List<Component> lines = TooltipComparison.lines(hovered, equipped, EquipmentSlot.CHEST);
        require(lines.size() == 4, "got " + lines);
        require(lines.get(1).getString().startsWith("Compared to equipped: "), "header " + lines.get(1).getString());
        require(lines.get(2).getString().equals("-2 Armor"), "got '" + lines.get(2).getString() + "'");
        require(lines.get(3).getString().equals("+2 Armor Toughness"), "got '" + lines.get(3).getString() + "'");
        require(TextColor.fromLegacyFormat(ChatFormatting.RED).equals(lines.get(2).getStyle().getColor()), "worse isn't red");
        require(TextColor.fromLegacyFormat(ChatFormatting.GREEN).equals(lines.get(3).getStyle().getColor()), "better isn't green");

        List<Component> empty = TooltipComparison.lines(hovered, ItemStack.EMPTY, EquipmentSlot.CHEST);
        require(empty.get(1).getString().equals("Compared to equipped: Nothing"), empty.get(1).getString());
        require(empty.stream().skip(2).allMatch(line -> line.getString().startsWith("+")), "not all plus: " + empty);

        require(TooltipComparison.lines(hovered, hovered, EquipmentSlot.CHEST).isEmpty(), "compared the equipped stack to itself");
        ItemStack same = hovered.copy();
        require(TooltipComparison.lines(hovered, same, EquipmentSlot.CHEST).isEmpty(), "identical stats weren't hidden");
        config.comparison.hideIdentical = false;
        List<Component> shown = TooltipComparison.lines(hovered, same, EquipmentSlot.CHEST);
        require(shown.size() == 3 && shown.get(2).getString().equals("No difference in stats"), "got " + shown);
        require(TooltipComparison.lines(hovered, (net.minecraft.world.entity.player.Player) null, false).isEmpty(), "needs a player");

        ItemStack secret = equipped.copy();
        //? if >=1.21.5 {
        var hiddenStats = new java.util.LinkedHashSet<net.minecraft.core.component.DataComponentType<?>>();
        hiddenStats.add(DataComponents.ATTRIBUTE_MODIFIERS);
        secret.set(DataComponents.TOOLTIP_DISPLAY, new net.minecraft.world.item.component.TooltipDisplay(false, hiddenStats));
        //?} else {
        /*secret.set(DataComponents.ATTRIBUTE_MODIFIERS, armor(Attributes.ARMOR, 8, add, EquipmentSlotGroup.CHEST).withTooltip(false));
        *///?}
        require(TooltipComparison.lines(hovered, secret, EquipmentSlot.CHEST).isEmpty(), "gave away the equipped item's hidden stats");
        require(TooltipComparison.lines(secret, hovered, EquipmentSlot.CHEST).isEmpty(), "gave away the hovered item's hidden stats");
    }

    private static ItemStack book(String... pages) {
        List<net.minecraft.server.network.Filterable<Component>> filterable = new ArrayList<>();
        for (String page : pages) filterable.add(net.minecraft.server.network.Filterable.passThrough(Component.literal(page)));
        ItemStack book = stack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(net.minecraft.server.network.Filterable.passThrough("Title"), "Author", 0, filterable, true));
        return book;
    }

    private static ItemStack map() {
        ItemStack map = stack(Items.FILLED_MAP);
        map.set(DataComponents.MAP_ID, new net.minecraft.world.level.saveddata.maps.MapId(12345));
        return map;
    }

    private static BannerPatternLayers patterns() {
        var pattern = registries().lookupOrThrow(Registries.BANNER_PATTERN).getOrThrow(BannerPatterns.STRIPE_TOP);
        return new BannerPatternLayers.Builder().add(pattern, DyeColor.RED).build();
    }

    private static ItemStack plainBanner() {
        //? if >=26.2 {
        return stack(Items.BANNER.pick(DyeColor.WHITE));
        //?} else {
        /*return stack(Items.WHITE_BANNER);
        *///?}
    }

    private static ItemStack banner() {
        ItemStack banner = plainBanner();
        banner.set(DataComponents.BANNER_PATTERNS, patterns());
        return banner;
    }

    private static FireworkExplosion explosion() {
        return new FireworkExplosion(FireworkExplosion.Shape.STAR, it.unimi.dsi.fastutil.ints.IntList.of(0xFF0000, 0x00FF00), it.unimi.dsi.fastutil.ints.IntList.of(0x0000FF), false, false);
    }

    private static ItemStack star() {
        ItemStack star = stack(Items.FIREWORK_STAR);
        star.set(DataComponents.FIREWORK_EXPLOSION, explosion());
        return star;
    }

    private static ClientTooltipComponent renderer(ItemStack stack) {
        var image = stack.getTooltipImage();
        require(image.isPresent(), "no preview for " + stack);
        // Through vanilla's method, as a tooltip would be, which fails if the hook on it no longer matches
        return ClientTooltipComponent.create(image.get());
    }

    private static void checkPreviews() {
        var font = Minecraft.getInstance().font;
        require(stack(Items.DIRT).getTooltipImage().isEmpty(), "preview on dirt");

        require(TooltipPreviews.forStack(book("First page", "Second page"), false).orElseThrow() instanceof TooltipPreviews.BookPreview preview
            && preview.lines().size() == 1, "not just the first page");
        require(book().getTooltipImage().isEmpty(), "preview of a book with no pages");

        // No world here, so this is also what an unsynced map gets
        ClientTooltipComponent map = renderer(map());
        require(map.getWidth(font) == font.width(Component.translatable(TooltipsImproved.MOD_ID + ".preview.map_not_loaded")), "not the not loaded line");
        require(stack(Items.FILLED_MAP).getTooltipImage().isEmpty(), "preview of a map with no id");

        require(renderer(banner()).getWidth(font) == PreviewCanvas.BANNER_WIDTH, "banner width");
        require(plainBanner().getTooltipImage().isEmpty(), "preview of a plain banner");
        ItemStack shield = stack(Items.SHIELD);
        shield.set(DataComponents.BANNER_PATTERNS, patterns());
        require(TooltipPreviews.forStack(shield, false).orElseThrow() instanceof TooltipPreviews.BannerPreview preview && preview.base() == DyeColor.WHITE, "shield without a base color");
        shield.set(DataComponents.BASE_COLOR, DyeColor.BLUE);
        require(TooltipPreviews.forStack(shield, false).orElseThrow() instanceof TooltipPreviews.BannerPreview preview && preview.base() == DyeColor.BLUE, "shield's base color");

        require(renderer(star()).getWidth(font) > 0, "star");
        ItemStack rocket = stack(Items.FIREWORK_ROCKET);
        rocket.set(DataComponents.FIREWORKS, new Fireworks(1, List.of()));
        require(rocket.getTooltipImage().isEmpty(), "preview of a rocket with no explosions");
        rocket.set(DataComponents.FIREWORKS, new Fireworks(1, List.of(explosion(), explosion())));
        require(height(renderer(rocket)) > height(renderer(star())), "a second explosion took no room");
    }

    private static void checkPreviewSettings() {
        TooltipsImprovedConfig config = TooltipsImprovedConfig.get();
        config.previews.bookLines = 2;
        String longPage = "Word ".repeat(200);
        ItemStack long1 = book(longPage);
        require(TooltipPreviews.forStack(long1, false).orElseThrow() instanceof TooltipPreviews.BookPreview preview && preview.lines().size() == 2, "bookLines ignored");
        config.previews.bookLines = 4;
        require(TooltipPreviews.forStack(long1, false).orElseThrow() instanceof TooltipPreviews.BookPreview preview && preview.lines().size() == 4, "kept the old split");

        config.previews.books = false;
        require(TooltipPreviews.forStack(long1, false).isEmpty(), "books toggle");
        config.previews.maps = false;
        require(TooltipPreviews.forStack(map(), false).isEmpty(), "maps toggle");
        config.previews.banners = false;
        require(TooltipPreviews.forStack(banner(), false).isEmpty(), "banners toggle");
        config.previews.fireworks = false;
        require(TooltipPreviews.forStack(star(), false).isEmpty(), "fireworks toggle");

        config.previews = new TooltipsImprovedConfig.Previews();
        config.previews.showWhen = TooltipsImprovedConfig.ShowWhen.SHIFT_HELD;
        require(TooltipPreviews.forStack(star(), false).isEmpty(), "shown without Shift");
        require(TooltipPreviews.forStack(star(), true).isPresent(), "not shown with Shift");
        config.previews.showWhen = TooltipsImprovedConfig.ShowWhen.ALWAYS;

        ItemStack hidden = star();
        //? if >=1.21.5 {
        hidden.set(DataComponents.TOOLTIP_DISPLAY, new net.minecraft.world.item.component.TooltipDisplay(true, new java.util.LinkedHashSet<>()));
        //?} else {
        /*hidden.set(DataComponents.HIDE_TOOLTIP, net.minecraft.util.Unit.INSTANCE);
        *///?}
        require(TooltipPreviews.forStack(hidden, false).isEmpty(), "shown on a hidden tooltip");

        ItemStack star = star();
        boolean[] offThread = {false};
        Thread other = new Thread(() -> offThread[0] = TooltipPreviews.forStack(star, false).isPresent());
        other.start();
        try {
            other.join();
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
        require(!offThread[0], "built a preview off the render thread");

        var unicode = Minecraft.getInstance().options.forceUnicodeFont();
        boolean wasUnicode = unicode.get();
        try {
            var cached = bookLines(long1);
            require(bookLines(long1) == cached, "split the same page again");
            unicode.set(!wasUnicode);
            require(bookLines(long1) != cached, "kept lines split with the old font");
        } finally {
            unicode.set(wasUnicode);
        }

        String key = TooltipsImproved.MOD_ID + ".config.durability.format.";
        require(Component.translatable(key + "percent").getString().equals("20%"), "percent label");
        require(Component.translatable(key + "both").getString().equals("312 / 1561 (20%)"), "both label");

        java.nio.file.Path file;
        try {
            file = Files.createTempFile(TooltipsImproved.MOD_ID, ".json");
            Files.writeString(file, "{ \"previews\": { \"mapSize\": 99999, \"bookLines\": -4 }, \"durability\": null, \"comparison\": { \"showWhen\": \"NOPE\" } }");
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
        TooltipsImprovedConfig loaded = TooltipsImprovedConfig.load(file);
        require(loaded.previews.mapSize == TooltipsImprovedConfig.MAX_MAP_SIZE && loaded.previews.bookLines == TooltipsImprovedConfig.MIN_BOOK_LINES, "sizes weren't limited");
        require(loaded.durability != null && loaded.comparison.showWhen == TooltipsImprovedConfig.ShowWhen.ALWAYS, "a bad section wasn't fixed");
    }

    private static List<net.minecraft.util.FormattedCharSequence> bookLines(ItemStack book) {
        return ((TooltipPreviews.BookPreview) TooltipPreviews.forStack(book, false).orElseThrow()).lines();
    }

    private static int height(ClientTooltipComponent line) {
        return TooltipLines.height(line, Minecraft.getInstance().font);
    }

    private static void checkPreviewSizes() {
        TooltipsImprovedConfig config = TooltipsImprovedConfig.get();
        var font = Minecraft.getInstance().font;
        MapItemSavedData data = MapItemSavedData.createFresh(0, 0, (byte) 0, false, false, Level.OVERWORLD);
        for (int size : List.of(TooltipsImprovedConfig.MIN_MAP_SIZE, 64, TooltipsImprovedConfig.MAX_MAP_SIZE)) {
            ClientTooltipComponent map = new TooltipPreviews.MapPreview(new net.minecraft.world.level.saveddata.maps.MapId(1), data, size).renderer();
            require(map.getWidth(font) == size && height(map) == size + 2, "map at " + size);
        }
        config.previews.bookLines = 3;
        ClientTooltipComponent page = renderer(book("Word ".repeat(200)));
        require(height(page) == 3 * font.lineHeight + 2 * 4 + 2, "page height " + height(page));

        config.wrap = TooltipsImprovedConfig.WrapMode.SCREEN;
        config.scrollWheel = true;
        TooltipScroll.reset();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int tallSize = screenHeight + 200;
        ClientTooltipComponent image = new TooltipPreviews.MapPreview(new net.minecraft.world.level.saveddata.maps.MapId(1), data, tallSize).renderer();
        List<ClientTooltipComponent> lines = List.of(text(Component.literal("Title")), image, text(Component.literal(longText())));
        List<ClientTooltipComponent> prepared = TooltipLines.prepare(lines, new Object());
        require(prepared.size() > lines.size() && prepared.get(1) == image, "wrapping changed the image");
        require(TooltipScroll.onScroll(0, -1), "a tooltip with a tall image didn't scroll");
        TooltipScroll.reset();
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

        scroll(first, -1);
        require(settle(origin).y() == 50 + step, "wheel down settled at " + settle(origin));
        scroll(first, 1);
        require(settle(origin).equals(origin), "scrolling back settled at " + settle(origin));
        scroll(first, 1);
        require(settle(origin).y() == 50 - step, "wheel up settled at " + settle(origin));

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

    private static List<ClientTooltipComponent> tall(String title) {
        List<ClientTooltipComponent> lines = new ArrayList<>(List.of(text(Component.literal(title))));
        for (int i = 0; i < 400; i++) lines.add(text(Component.literal("Line " + i)));
        return lines;
    }

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

    private static void withField(Runnable set, Check check) throws Exception {
        set.run();
        check.run();
    }

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
