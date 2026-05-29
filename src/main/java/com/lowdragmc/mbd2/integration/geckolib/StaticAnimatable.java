package com.lowdragmc.mbd2.integration.geckolib;

import lombok.Getter;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.util.RenderUtil;

public class StaticAnimatable implements GeoAnimatable {
    @Getter
    private final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this, false);
    private RawAnimation previewAnimation;
    private AnimationController<StaticAnimatable> previewController;

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        previewController = new AnimationController<>(this, "preview", state -> {
            if (previewAnimation == null) {
                return PlayState.STOP;
            }
            state.getController().setAnimation(previewAnimation);
            return PlayState.CONTINUE;
        });
        controllers.add(previewController);
    }

    public void playPreview(RawAnimation animation) {
        this.previewAnimation = animation;
        if (previewController != null) {
            previewController.forceAnimationReset();
        }
    }

    public void stopPreview() {
        this.previewAnimation = null;
        if (previewController != null) {
            previewController.stop();
        }
    }

    @Override
    public double getTick(Object object) {
        return RenderUtil.getCurrentTick();
    }
}
