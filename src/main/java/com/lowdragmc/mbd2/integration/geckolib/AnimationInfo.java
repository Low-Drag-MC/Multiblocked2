package com.lowdragmc.mbd2.integration.geckolib;

import software.bernie.geckolib.animation.Animation;

public record AnimationInfo(String name, double length, String loopType, boolean loop) {

    public static AnimationInfo of(Animation animation) {
        var loopType = animation.loopType();
        String loopTypeId;
        try {
            loopTypeId = loopType.getId();
        } catch (IllegalStateException ignored) {
            loopTypeId = "custom";
        }
        return new AnimationInfo(animation.name(), animation.length(), loopTypeId, loopType == Animation.LoopType.LOOP);
    }

    public Animation.LoopType geckoLoopType() {
        return Animation.LoopType.fromString(loopType);
    }
}
