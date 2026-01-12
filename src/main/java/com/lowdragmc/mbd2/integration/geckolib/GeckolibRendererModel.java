package com.lowdragmc.mbd2.integration.geckolib;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

public class GeckolibRendererModel extends GeoModel<GeoAnimatable> {
    private final GeckolibRenderer renderer;

    public GeckolibRendererModel(GeckolibRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public ResourceLocation getModelResource(GeoAnimatable animatable) {
        return renderer.getModelPath();
    }

    @Override
    public ResourceLocation getTextureResource(GeoAnimatable animatable) {
        return renderer.getTexturePath();
    }

    @Override
    public ResourceLocation getAnimationResource(GeoAnimatable animatable) {
        return renderer.getAnimationPath();
    }

    @Override
    public RenderType getRenderType(GeoAnimatable animatable, ResourceLocation texture) {
        return renderer.useTranslucent ?  RenderType.entityTranslucentCull(texture) : RenderType.entityCutoutNoCull(texture);
    }
}
