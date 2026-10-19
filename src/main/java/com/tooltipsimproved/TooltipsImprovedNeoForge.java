package com.tooltipsimproved;

//? if neoforge {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = TooltipsImproved.MOD_ID, dist = Dist.CLIENT)
public class TooltipsImprovedNeoForge {
    public TooltipsImprovedNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        TooltipsImproved.init();
        // Adds a Config button for the mod in NeoForge's mod list
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, (container, parent) -> new TooltipsImprovedScreen(parent));
    }
}
//?}
