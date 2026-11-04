package com.tooltipsimproved;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
//? if >=1.21.11 {
import net.minecraft.client.model.object.banner.BannerFlagModel;
//?} else {
/*import net.minecraft.client.model.geom.ModelPart;
*///?}

/** The drawing the tooltip previews need, so what changes between versions stays in this one class. */
final class PreviewCanvas {
    static final int BANNER_WIDTH = 20, BANNER_HEIGHT = 40;
    private static final int MAP_TEXTURE_SIZE = 128;

    //? if >=26.1 {
    private final GuiGraphicsExtractor graphics;

    PreviewCanvas(GuiGraphicsExtractor graphics) {
        this.graphics = graphics;
    }
    //?} else {
    /*private final GuiGraphics graphics;

    PreviewCanvas(GuiGraphics graphics) {
        this.graphics = graphics;
    }
    *///?}

    // The model is built once per resource reload, which replaces the model set
    private static EntityModelSet flagModels;
    //? if >=1.21.11 {
    private static BannerFlagModel flag;

    private static BannerFlagModel flag() {
        EntityModelSet models = Minecraft.getInstance().getEntityModels();
        if (flag == null || flagModels != models) {
            flagModels = models;
            flag = new BannerFlagModel(models.bakeLayer(ModelLayers.STANDING_BANNER_FLAG));
        }
        return flag;
    }
    //?} else {
    /*private static ModelPart flag;

    private static ModelPart flag() {
        EntityModelSet models = Minecraft.getInstance().getEntityModels();
        if (flag == null || flagModels != models) {
            flagModels = models;
            flag = models.bakeLayer(ModelLayers.BANNER).getChild("flag");
        }
        return flag;
    }
    *///?}

    void fill(int x0, int y0, int x1, int y1, int argb) {
        graphics.fill(x0, y0, x1, y1, argb);
    }

    void text(Font font, FormattedCharSequence text, int x, int y, int argb) {
        //? if >=26.1 {
        graphics.text(font, text, x, y, argb, false);
        //?} else {
        /*graphics.drawString(font, text, x, y, argb, false);
        *///?}
    }

    void text(Font font, Component text, int x, int y, int argb) {
        text(font, text.getVisualOrderText(), x, y, argb);
    }

    void map(MapId id, MapItemSavedData data, int x, int y, int size) {
        //? if >=1.21.11 {
        // Only uploads the texture again if the map has changed
        var texture = Minecraft.getInstance().getMapTextureManager().prepareMapTexture(id, data);
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, size, size,
            MAP_TEXTURE_SIZE, MAP_TEXTURE_SIZE, MAP_TEXTURE_SIZE, MAP_TEXTURE_SIZE);
        //?} else {
        /*var pose = graphics.pose();
        pose.pushPose();
        // Vanilla draws maps just behind their own position, which would put them behind the tooltip's background
        pose.translate(x, y, 1);
        pose.scale(size / (float) MAP_TEXTURE_SIZE, size / (float) MAP_TEXTURE_SIZE, 1);
        Minecraft.getInstance().gameRenderer.getMapRenderer().render(pose, graphics.bufferSource(), id, data, true, net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);
        pose.popPose();
        graphics.flush();
        *///?}
    }

    /** Draws a banner's flag, {@link #BANNER_WIDTH} by {@link #BANNER_HEIGHT}, as the loom's result slot does. */
    void banner(DyeColor base, BannerPatternLayers layers, int x, int y) {
        //? if >=26.1 {
        graphics.bannerPattern(flag(), base, layers, x, y, x + BANNER_WIDTH, y + BANNER_HEIGHT);
        //?} elif >=1.21.11 {
        /*graphics.submitBannerPatternRenderState(flag(), base, layers, x, y, x + BANNER_WIDTH, y + BANNER_HEIGHT);
        *///?} else {
        /*ModelPart flag = flag();
        var pose = graphics.pose();
        pose.pushPose();
        // The flag hangs 32 units from its model's origin, and each unit is one pixel at this scale
        pose.translate(x + BANNER_WIDTH / 2f, y + 32, 1);
        pose.scale(16, 16, -2 / 3f);
        flag.xRot = 0;
        flag.y = -32;
        // Otherwise it's lit from the side, and how dark it looks depends on what was drawn before it
        com.mojang.blaze3d.platform.Lighting.setupForFlatItems();
        net.minecraft.client.renderer.blockentity.BannerRenderer.renderPatterns(pose, graphics.bufferSource(),
            net.minecraft.client.renderer.LightTexture.FULL_BRIGHT, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
            flag, net.minecraft.client.resources.model.ModelBakery.BANNER_BASE, true, base, layers);
        pose.popPose();
        graphics.flush();
        com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
        *///?}
    }
}
