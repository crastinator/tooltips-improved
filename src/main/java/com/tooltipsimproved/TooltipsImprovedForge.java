package com.tooltipsimproved;

//? if forge {
/*import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TooltipsImproved.MOD_ID)
public class TooltipsImprovedForge {
    public TooltipsImprovedForge(FMLJavaModLoadingContext context) {
        TooltipsImproved.init();
        // Adds a Config button for the mod in Forge's mod list
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory(TooltipsImprovedScreen::new));
    }
}
*///?}
