package com.lowdragmc.mbd2.integration.photon;

import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.definition.config.fx.MachineFXConfig;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXEffectExecutor;
import com.lowdragmc.photon.client.gameobject.IFXObject;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * One effect anchored to one machine, under one identifier.
 *
 * <p>Photon's own {@link com.lowdragmc.photon.client.fx.BlockEffectExecutor} is the same idea keyed
 * by {@link net.minecraft.core.BlockPos}, and this is a port of it: same start/anchor-check/retire
 * protocol, with the static position cache swapped for {@link PhotonMachineFXManager}'s identifier
 * map. A machine needs the identifier because two effects can sit on the same block, and it needs
 * its own anchor check because "the block is still there" is not the same question as "this is still
 * my machine" — a multiblock re-form or a definition reload replaces the {@link MBDMachine}
 * instance while the block state never changes.</p>
 *
 * <p>All of this runs on the client tick/render thread, like everything in Photon.</p>
 */
@OnlyIn(Dist.CLIENT)
public class MachineFXExecutor extends FXEffectExecutor {

    private final String identifier;
    private final MBDMachine machine;
    private final PhotonMachineFXManager owner;

    public MachineFXExecutor(FX fx, MBDMachine machine, PhotonMachineFXManager owner,
                             String identifier, MachineFXConfig config) {
        super(fx, machine.getLevel());
        this.machine = machine;
        this.owner = owner;
        this.identifier = identifier;
        setDelay(config.getDelay());
        setForcedDeath(config.isForcedDeath());
        setScale(new Vector3f(config.getScale()));

        // A machine's front facing is authored-against-north, exactly like its renderer and its
        // shape (see MachineState.getShape / ShapeUtils.rotate). Rotating the offset as well as the
        // orientation is what makes "a flame in front of the machine" stay in front of it.
        var facing = config.isFollowFacing()
                ? machine.getFrontFacing().orElse(Direction.NORTH)
                : Direction.NORTH;
        var facingRotation = facingRotation(facing);
        setOffset(facingRotation.transform(new Vector3f(config.getOffset())));
        // setRotation(x, y, z) is Photon's own degrees->quaternion conversion; compose the facing
        // onto its result rather than restating the euler order here.
        var rot = config.getRotation();
        setRotation(rot.x, rot.y, rot.z);
        setRotation(facingRotation.mul(rotation, new Quaternionf()));
    }

    /** The rotation that takes a north-authored effect to {@code facing}. */
    private static Quaternionf facingRotation(Direction facing) {
        return switch (facing) {
            case NORTH -> new Quaternionf();
            case SOUTH -> new Quaternionf().rotationY((float) Math.PI);
            case WEST -> new Quaternionf().rotationY((float) (Math.PI / 2));
            case EAST -> new Quaternionf().rotationY((float) (-Math.PI / 2));
            case UP -> new Quaternionf().rotationX((float) (-Math.PI / 2));
            case DOWN -> new Quaternionf().rotationX((float) (Math.PI / 2));
        };
    }

    /** Stop this effect. {@code force} drops visible remnants instead of letting them drain. */
    public void kill(boolean force) {
        if (runtime != null) {
            runtime.destroy(force);
        }
    }

    /**
     * Stop this effect the way it was configured to stop. {@code forcedDeath} is {@code protected} on
     * Photon's executor, so the owning manager cannot read it off another instance.
     */
    public void kill() {
        kill(forcedDeath);
    }

    /**
     * The editor's deterministic source when the preview set one, the world's otherwise.
     *
     * <p>This is the single hook the whole seed feature hangs on: {@code FXObject} seeds each emitted
     * object from {@code effectExecutor.getRandomSource().nextLong()}, so overriding it here makes
     * every particle downstream reproducible — the same mechanism Photon's own editor uses through
     * {@code FXProjectEffectExecutor}.</p>
     */
    @Override
    public net.minecraft.util.RandomSource getRandomSource() {
        var preview = owner.previewRandomSource();
        return preview != null ? preview : super.getRandomSource();
    }

    public boolean isAlive() {
        return runtime != null && !runtimeEnded();
    }

    /**
     * Emit the runtime. The caller owns the identifier slot — {@code PhotonMachineFXManager.play}
     * has already decided whether this effect may replace whatever was there and retires the loser.
     */
    @Override
    public void start() {
        resetFinishedNotification();
        this.runtime = fx.createRuntime();
        var pos = machine.getPos();
        var root = this.runtime.getRoot();
        root.updatePos(new Vector3f(pos.getX(), pos.getY(), pos.getZ())
                .add(offset.x + 0.5f, offset.y + 0.5f, offset.z + 0.5f));
        root.updateRotation(rotation);
        root.updateScale(scale);
        this.runtime.emit(this, delay);
    }

    @Override
    public void updateFXObjectTick(IFXObject fxObject) {
        if (runtime == null || fxObject != runtime.getRoot()) {
            return;
        }
        if (!isAnchorValid()) {
            runtime.destroy(forcedDeath);
            owner.unregister(identifier, this);
        } else if (runtimeEnded()) {
            // Self-evict a finished effect rather than leaving a dead entry that makes isPlaying lie
            // until something else happens to start the same identifier.
            notifyFinished();
            owner.unregister(identifier, this);
        }
    }

    /**
     * Whether the machine this effect is bound to is still the machine at that position.
     *
     * <p>The identity check is the important one: a multiblock re-forming or an edited definition
     * reloading swaps the block entity's machine without the block state changing, and an effect
     * left running against the orphaned instance would follow a machine nothing can see.</p>
     *
     * <p>Allocation-free on purpose — this runs once per live effect per tick. Reading the block
     * entity directly answers "chunk loaded", "block entity still there" and "still my machine" in
     * one lookup, where {@code isLoaded} + {@code IMachine.ofMachine(...).filter(...)} was three,
     * two of which allocated (an {@code Optional} and a capturing lambda).</p>
     */
    private boolean isAnchorValid() {
        return owner.peek(identifier) == this
                && level.getBlockEntity(machine.getPos()) instanceof IMachineBlockEntity be
                && be.getMetaMachine() == machine;
    }
}
