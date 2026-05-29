package com.lowdragmc.mbd2.integration.geckolib;

import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import net.neoforged.neoforge.common.NeoForge;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.util.RenderUtil;

public class AnimatableMachine implements GeoAnimatable {
    public static final String DEFAULT_CONTROLLER = "base_controller";

    @Getter
    private final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this, false);
    @Getter
    private final MBDMachine machine;
    @Getter
    private final GeckolibRenderer renderer;

    public AnimatableMachine(MBDMachine machine, GeckolibRenderer renderer) {
        this.machine = machine;
        this.renderer = renderer;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        var controller = new AnimationController<>(this, DEFAULT_CONTROLLER, state -> {
            if (renderer.isScheduleStateAnimation()) {
                var animation = renderer.getRawAnimation(machine.getMachineStateName());
                if (animation != null) {
                    state.getController().setAnimation(animation);
                    return PlayState.CONTINUE;
                }
            }
            return PlayState.STOP;
        });
        controller.setCustomInstructionKeyframeHandler(frame -> NeoForge.EVENT_BUS.post(
                new MachineCustomKeyframeEvent(machine, frame.getKeyframeData().getInstructions(),
                        frame.getController().getName(), frame.getAnimationTick()).postCustomEvent()));
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
