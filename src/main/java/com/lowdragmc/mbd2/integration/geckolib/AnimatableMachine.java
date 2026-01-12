package com.lowdragmc.mbd2.integration.geckolib;

import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineCustomKeyframeEvent;
import lombok.Getter;
import net.minecraftforge.common.MinecraftForge;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.util.RenderUtils;

public class AnimatableMachine implements GeoAnimatable {
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
        var controller = new AnimationController<>(this, state -> {
            if (renderer.scheduleStateAnimation) {
                var stateName = machine.getMachineState().name();
                var animation = renderer.getRawAnimation(stateName);
                if (animation != null) {
                    return state.setAndContinue(animation);
                }
            }
            return PlayState.STOP;
        });
        controller.setCustomInstructionKeyframeHandler(frame -> MinecraftForge.EVENT_BUS.post(new MachineCustomKeyframeEvent(machine, frame).postCustomEvent()));
        for (var animation : renderer.animations) {
            var rawAnimation = renderer.getRawAnimation(animation.getName());
            if (rawAnimation != null) {
                controller.triggerableAnim(animation.getName(), rawAnimation);
            }
        }
        controllers.add(controller);
    }

    @Override
    public double getTick(Object object) {
        return RenderUtils.getCurrentTick();
    }
}
