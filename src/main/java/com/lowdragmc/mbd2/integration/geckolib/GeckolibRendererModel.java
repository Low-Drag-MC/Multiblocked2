package com.lowdragmc.mbd2.integration.geckolib;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public class GeckolibRendererModel extends GeoModel<GeoAnimatable> {
    private final GeckolibRenderer renderer;

    public GeckolibRendererModel(GeckolibRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public ResourceLocation getModelResource(GeoAnimatable animatable, @Nullable GeoRenderer<GeoAnimatable> renderer) {
        return this.renderer.getModelPath();
    }

    @Override
    @Deprecated
    public ResourceLocation getModelResource(GeoAnimatable animatable) {
        return renderer.getModelPath();
    }

    @Override
    public ResourceLocation getTextureResource(GeoAnimatable animatable, @Nullable GeoRenderer<GeoAnimatable> renderer) {
        return this.renderer.getTexturePath();
    }

    @Override
    @Deprecated
    public ResourceLocation getTextureResource(GeoAnimatable animatable) {
        return renderer.getTexturePath();
    }

    @Override
    public ResourceLocation getAnimationResource(GeoAnimatable animatable) {
        return renderer.getAnimationPath();
    }

    @Override
    public RenderType getRenderType(GeoAnimatable animatable, ResourceLocation texture) {
        return renderer.isUseTranslucent() ? RenderType.entityTranslucentCull(texture) : RenderType.entityCutoutNoCull(texture);
    }
}
