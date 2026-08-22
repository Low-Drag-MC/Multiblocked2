package com.lowdragmc.mbd2.integration.geckolib;

import com.lowdragmc.mbd2.api.capability.IAnimationSource;
import com.lowdragmc.mbd2.common.machine.MBDMachine;

/**
 * @deprecated since 21.0.12, use {@link AnimatableBlock}. Animation is no longer machine-only: a block
 * describes itself through {@link IAnimationSource}, so ports and third-party block entities animate
 * through the same class. {@link GeckolibRenderer} therefore builds plain {@link AnimatableBlock}s and
 * never hands out this type — code that reached for the machine should use {@link #getSource()} and
 * check for {@link MBDMachine} instead.
 */
@Deprecated(since = "21.0.12", forRemoval = true)
public class AnimatableMachine extends AnimatableBlock {
    public static final String DEFAULT_CONTROLLER = AnimatableBlock.DEFAULT_CONTROLLER;

    public AnimatableMachine(MBDMachine machine, GeckolibRenderer renderer) {
        super(machine, renderer);
    }

    /** The machine this was built for; always the {@link IAnimationSource} passed to the constructor. */
    public MBDMachine getMachine() {
        return (MBDMachine) getSource();
    }
}
