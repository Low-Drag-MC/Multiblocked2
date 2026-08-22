package com.lowdragmc.mbd2.integration.geckolib;

import com.lowdragmc.mbd2.api.capability.IAnimationSource;
import lombok.Getter;
import net.neoforged.neoforge.common.NeoForge;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.util.RenderUtil;

/**
 * The animation instance bound to one block in the world. Everything it needs comes from the block's
 * {@link IAnimationSource}, so a machine, a {@code proxyWhileFormed} port and any block entity that
 * exposes the capability all animate through this one class.
 * <p>
 * One per (block, renderer): a model may be shared by many blocks, but animation progress is not.
 */
public class AnimatableBlock implements GeoAnimatable {
    public static final String DEFAULT_CONTROLLER = "base_controller";

    @Getter
    private final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this, false);
    @Getter
    private final IAnimationSource source;
    @Getter
    private final GeckolibRenderer renderer;

    public AnimatableBlock(IAnimationSource source, GeckolibRenderer renderer) {
        this.source = source;
        this.renderer = renderer;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        var controller = new AnimationController<>(this, DEFAULT_CONTROLLER, state -> {
            if (renderer.isScheduleStateAnimation()) {
                var animation = renderer.getRawAnimation(source.getAnimationState());
                if (animation != null) {
                    state.getController().setAnimation(animation);
                    return PlayState.CONTINUE;
                }
            }
            return PlayState.STOP;
        });
        controller.setCustomInstructionKeyframeHandler(frame -> {
            // A block with no machine to speak for — a port whose controller is gone — just skips the event.
            var target = source.getAnimationEventTarget();
            if (target != null) {
                NeoForge.EVENT_BUS.post(new MachineCustomKeyframeEvent(target,
                        frame.getKeyframeData().getInstructions(),
                        frame.getController().getName(), frame.getAnimationTick()).postCustomEvent());
            }
        });
        for (var animation : renderer.getAnimations()) {
            var rawAnimation = renderer.getRawAnimation(animation.getName());
            if (rawAnimation != null) {
                controller.triggerableAnim(animation.getName(), rawAnimation);
            }
        }
        controllers.add(controller);
    }

    @Override
    public double getTick(Object object) {
        return RenderUtil.getCurrentTick();
    }
}
