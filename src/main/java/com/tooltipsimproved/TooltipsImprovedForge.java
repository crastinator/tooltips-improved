package com.tooltipsimproved;

//? if forge {
/*import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.common.Mod;
//? if >=1.21 {
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//?} else {
/^import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
^///?}

@Mod(TooltipsImproved.MOD_ID)
public class TooltipsImprovedForge {
    //? if >=1.21 {
    public TooltipsImprovedForge(FMLJavaModLoadingContext context) {
    //?} else {
    /^// Forge 1.20.1 only passes the context to constructors since 47.3.10, and later builds mark get() for
    // removal even though 1.20.1 won't change any more; get() works on every 1.20.1 build
    @SuppressWarnings("removal")
    public TooltipsImprovedForge() {
        ModLoadingContext context = ModLoadingContext.get();
    ^///?}
        TooltipsImproved.init();
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory(TooltipsImprovedScreen::new));
    }

    //? if <1.21 {
    /^/^¹* Draws the previews, which mixin/ClientTooltipComponentMixin does on every other target. ¹^/
    @Mod.EventBusSubscriber(modid = TooltipsImproved.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Previews {
        @SubscribeEvent
        public static void register(RegisterClientTooltipComponentFactoriesEvent event) {
            for (Class<?> type : TooltipPreviews.Preview.class.getPermittedSubclasses())
                event.register(type.asSubclass(TooltipPreviews.Preview.class), TooltipPreviews.Preview::renderer);
        }
    }
    ^///?}
}
*///?}
