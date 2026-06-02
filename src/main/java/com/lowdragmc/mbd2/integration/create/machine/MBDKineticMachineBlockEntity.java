package com.lowdragmc.mbd2.integration.create.machine;

import com.lowdragmc.lowdraglib2.syncdata.storage.MultiManagedStorage;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public class MBDKineticMachineBlockEntity extends KineticBlockEntity implements IMachineBlockEntity {
    @Getter
    public final MultiManagedStorage rootStorage = new MultiManagedStorage();
    @Getter
    private final long offset = MBD2.RND.nextLong();
    @Getter
    private IMachine metaMachine;
    public float workingSpeed;
    public boolean reActivateSource;
    public final CreateKineticMachineDefinition definition;
    /**
     * When non-null, overrides the static {@code kineticMachineSettings.torque} for stress
     * calculations until cleared. Set by {@link CreateRotationTrait} when a recipe with a
     * {@code torqueOverride} starts; cleared when the recipe ends.
     */
    @org.jetbrains.annotations.Nullable
    protected Float dynamicTorqueOverride;

    public MBDKineticMachineBlockEntity(CreateKineticMachineDefinition definition,
                                        BlockEntityType<?> type, BlockPos pos, BlockState blockState,
                                        Function<IMachineBlockEntity, IMachine> machineFactory) {
        super(type, pos, blockState);
        this.definition = definition;
        setMachine(machineFactory.apply(this));
    }

    public void setMachine(IMachine newMachine) {
        if (metaMachine == newMachine) return;
        if (metaMachine != null && level != null && !level.isClientSide) {
            metaMachine.onUnload();
        }
        if (metaMachine instanceof MBDMachine machine) {
            machine.detach();
        }
        metaMachine = newMachine;
        // CreateRotationTrait is now auto-included by CreateKineticMachineDefinition.loadFactory()
        // and attached to rootStorage by MBDMachine.loadAdditionalTraits() — no manual wiring here.
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (metaMachine != null) metaMachine.onUnload();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (metaMachine != null) metaMachine.onLoad();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (metaMachine != null) metaMachine.onChunkUnloaded();
    }

    @Override
    public AABB getRenderBoundingBox() {
        if (metaMachine instanceof MBDMachine machine) {
            var value = machine.getRenderBoundingBox();
            if (value != null) return value;
        }
        return super.getRenderBoundingBox();
    }

    // -------- Create kinetic API --------

    /**
     * Generator-side: schedule a target stress to provide. Returns the actually available stress
     * (which may be less than requested due to maxRPM clamp).
     */
    public float scheduleWorking(float stress, boolean simulate) {
        if (definition.kineticMachineSettings().isGenerator) {
            var capacity = definition.kineticMachineSettings().getCapacity();
            float speed = Math.min(definition.kineticMachineSettings().maxRPM, stress / capacity);
            if (!simulate) {
                workingSpeed = speed;
                updateGeneratedRotation();
            }
            return speed * capacity;
        }
        return 0;
    }

    public float scheduleWorkingRPM(float rpm, boolean simulate) {
        var stress = rpm * definition.kineticMachineSettings().getCapacity();
        return scheduleWorking(stress, simulate);
    }

    public void stopWorking() {
        if (definition.kineticMachineSettings().isGenerator && getGeneratedSpeed() != 0) {
            workingSpeed = 0;
            updateGeneratedRotation();
        }
    }

    @Override
    public float getGeneratedSpeed() {
        return workingSpeed;
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        if (level != null && !level.isClientSide && !definition.kineticMachineSettings().isGenerator) {
            if (Math.abs(speed) > definition.kineticMachineSettings().maxRPM) {
                level.destroyBlock(worldPosition, true);
            }
        }
    }

    @Override
    public float calculateStressApplied() {
        float impact = dynamicTorqueOverride != null && !definition.kineticMachineSettings().isGenerator
                ? dynamicTorqueOverride
                : definition.kineticMachineSettings().getImpact();
        this.lastStressApplied = impact;
        return impact;
    }

    @Override
    public float calculateAddedStressCapacity() {
        float capacity;
        if (dynamicTorqueOverride != null && definition.kineticMachineSettings().isGenerator) {
            capacity = Math.max(dynamicTorqueOverride, Float.MIN_VALUE);
        } else {
            capacity = definition.kineticMachineSettings().getCapacity();
        }
        this.lastCapacityProvided = capacity;
        return capacity;
    }

    /**
     * Set or clear the per-BE torque override. When non-null, supersedes
     * {@code kineticMachineSettings.torque} in stress calculations until cleared.
     * Triggers a network stress refresh when attached to a kinetic network.
     */
    public void setDynamicTorqueOverride(@Nullable Float torque) {
        if (java.util.Objects.equals(this.dynamicTorqueOverride, torque)) return;
        this.dynamicTorqueOverride = torque;
        if (level != null && !level.isClientSide && hasNetwork()) {
            var network = getOrCreateNetwork();
            notifyStressCapacityChange(calculateAddedStressCapacity());
            notifyStressChange(calculateStressApplied());
            network.updateStress();
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        float stressBase = calculateAddedStressCapacity();
        if (stressBase != 0.0F && IRotate.StressImpact.isEnabled()) {
            CreateLang.translate("gui.goggles.generator_stats").forGoggles(tooltip);
            CreateLang.translate("tooltip.capacityProvided").style(ChatFormatting.GRAY).forGoggles(tooltip);
            float speedAbs = getTheoreticalSpeed();
            if (speedAbs != getGeneratedSpeed() && speedAbs != 0.0F) {
                stressBase *= getGeneratedSpeed() / speedAbs;
            }
            speedAbs = Math.abs(speedAbs);
            float stressTotal = stressBase * speedAbs;
            CreateLang.number(stressTotal).translate("generic.unit.stress").style(ChatFormatting.AQUA).space()
                    .add(CreateLang.translate("gui.goggles.at_current_speed").style(ChatFormatting.DARK_GRAY))
                    .forGoggles(tooltip, 1);
            added = true;
        }
        return added;
    }

    protected void notifyStressCapacityChange(float capacity) {
        getOrCreateNetwork().updateCapacityFor(this, capacity);
    }

    protected void notifyStressChange(float stress) {
        getOrCreateNetwork().updateStressFor(this, stress);
    }

    @Override
    public void removeSource() {
        if (definition.kineticMachineSettings().isGenerator && hasSource() && isSource()) {
            reActivateSource = true;
        }
        super.removeSource();
    }

    @Override
    public void setSource(BlockPos source) {
        super.setSource(source);
        if (!definition.kineticMachineSettings().isGenerator) return;
        if (level != null && level.getBlockEntity(source) instanceof KineticBlockEntity sourceTe) {
            if (reActivateSource && Math.abs(sourceTe.getSpeed()) >= Math.abs(getGeneratedSpeed())) {
                reActivateSource = false;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (definition.kineticMachineSettings().isGenerator && reActivateSource) {
            updateGeneratedRotation();
            reActivateSource = false;
        }
    }

    public void updateGeneratedRotation() {
        if (!definition.kineticMachineSettings().isGenerator) return;
        if (level == null || level.isClientSide) return;
        float targetSpeed = getGeneratedSpeed();
        float prevSpeed = this.speed;
        if (prevSpeed != targetSpeed) {
            if (!hasSource()) {
                IRotate.SpeedLevel levelBefore = IRotate.SpeedLevel.of(this.speed);
                IRotate.SpeedLevel levelAfter = IRotate.SpeedLevel.of(targetSpeed);
                if (levelBefore != levelAfter) {
                    this.effects.queueRotationIndicators();
                }
            }
            applyNewSpeed(prevSpeed, targetSpeed);
        }
        if (hasNetwork() && targetSpeed != 0.0F) {
            KineticNetwork network = getOrCreateNetwork();
            notifyStressCapacityChange(calculateAddedStressCapacity());
            notifyStressChange(calculateStressApplied());
            network.updateStress();
        }
        onSpeedChanged(prevSpeed);
        sendData();
    }

    public void applyNewSpeed(float prevSpeed, float newSpeed) {
        if (isChunkUnloaded()) return;
        if (newSpeed == 0.0F) {
            if (hasSource()) {
                notifyStressCapacityChange(0.0F);
                notifyStressChange(calculateStressApplied());
            } else {
                detachKinetics();
                setSpeed(0.0F);
                setNetwork(null);
            }
        } else if (prevSpeed == 0.0F) {
            setSpeed(newSpeed);
            setNetwork(createNetworkId());
            attachKinetics();
        } else if (hasSource()) {
            if (Math.abs(prevSpeed) >= Math.abs(newSpeed)) {
                if (Math.signum(prevSpeed) != Math.signum(newSpeed) && level != null) {
                    level.destroyBlock(worldPosition, true);
                }
            } else {
                detachKinetics();
                setSpeed(newSpeed);
                this.source = null;
                setNetwork(createNetworkId());
                attachKinetics();
            }
        } else {
            detachKinetics();
            setSpeed(newSpeed);
            attachKinetics();
        }
    }

    public Long createNetworkId() {
        return worldPosition.asLong();
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putFloat("workingSpeed", workingSpeed);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        workingSpeed = compound.contains("workingSpeed") ? compound.getFloat("workingSpeed") : 0f;
    }
}
