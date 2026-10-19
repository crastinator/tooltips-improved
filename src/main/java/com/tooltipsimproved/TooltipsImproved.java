package com.tooltipsimproved;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//? if >=1.21.2 {
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;
//?} else {
/*import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimPattern;
*///?}

public class TooltipsImproved {
    public static final String MOD_ID = "tooltipsimproved";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final String VERSION = /*$ mod_version*/ "0.1.0";
    public static final String MINECRAFT = /*$ minecraft*/ "26.3";

    public static void init() {
        LOGGER.info("Tooltips Improved {} loaded on Minecraft {}", VERSION, MINECRAFT);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /**
     * Holding Shift shows the vanilla tooltip lines instead of the folded ones. Tooltips built off the
     * render thread, for the creative search, are never expanded, as key state can't be read there.
     */
    public static boolean isExpanded() {
        if (!net.minecraft.client.Minecraft.getInstance().isSameThread()) return false;
        //? if >=1.21.9 {
        return net.minecraft.client.Minecraft.getInstance().hasShiftDown();
        //?} else
        /*return net.minecraft.client.gui.screens.Screen.hasShiftDown();*/
    }

    /** Builds the single trim line, e.g. "Sentry Armor Trim (Iron)". */
    public static Component trimLine(Holder<TrimPattern> pattern, Holder<TrimMaterial> material) {
        Component materialName = material.value().description();
        Style materialStyle = materialName.getStyle();
        // Brackets are added here rather than in lang so every language gets them
        return Component.empty()
            .append(pattern.value().description().copy().withStyle(ChatFormatting.GRAY))
            .append(CommonComponents.SPACE)
            .append(Component.literal("(").withStyle(materialStyle))
            .append(shortMaterialName(materialName).withStyle(materialStyle))
            .append(Component.literal(")").withStyle(materialStyle));
    }

    /**
     * Vanilla names read "Iron Material", which is redundant inside the brackets.
     * Short names come from our own lang keys so the expanded vanilla lines stay untouched;
     * materials without one (i.e. from other mods) keep their full name.
     */
    private static MutableComponent shortMaterialName(Component name) {
        if (name.getContents() instanceof TranslatableContents translatable)
            return Component.translatableWithFallback(MOD_ID + "." + translatable.getKey(), name.getString());
        return name.copy();
    }
}
