package com.lowdragmc.mbd2.api.capability;

import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * What a block offers to a renderer that animates it: where it sits, which way it faces, and which
 * machine state its animation should follow.
 * <p>
 * Exposed as {@link MBDCapabilities#CAPABILITY_ANIMATION_SOURCE} instead of being an {@code instanceof}
 * chain inside the renderer, so any block entity can opt in — a machine, a {@code proxyWhileFormed}
 * port, or one MBD2 does not own at all — without the renderer having to learn about it.
 */
public interface IAnimationSource {

    /** The animation source a block entity exposes, or null if it has none. */
    @Nullable
    static IAnimationSource of(@Nullable BlockEntity blockEntity) {
        if (blockEntity == null || blockEntity.getLevel() == null) {
            return null;
        }
        return MBDCapabilities.CAPABILITY_ANIMATION_SOURCE.getCapability(blockEntity.getLevel(),
                blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, null);
    }

    /** Position the model is drawn at. */
    BlockPos getAnimationPos();

    /** Facing the model is rotated by. */
    Direction getAnimationFacing();

    /**
     * Name of the machine state the animation follows. A block that proxies another one reports the
     * proxied state here, so its animation tracks whatever it stands in for.
     */
    String getAnimationState();

    /**
     * Machine that animation events are fired against, or null if there is none. A port has no machine
     * of its own, so it names the controller it belongs to.
     */
    @Nullable
    default MBDMachine getAnimationEventTarget() {
        return null;
    }

    /**
     * Scratch storage for the animation instance a renderer binds to this block, keyed by renderer.
     * One model can be shared by many blocks while animation progress is per block, so the map has to
     * live on the block and last exactly as long as it does.
     */
    Map<IRenderer, Object> getAnimatableCache();
}
