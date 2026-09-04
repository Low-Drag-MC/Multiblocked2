package com.lowdragmc.mbd2.common.trait.forgeenergy;

import com.lowdragmc.lowdraglib2.syncdata.annotation.ConditionalSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.capability.recipe.ForgeEnergyRecipeCapability;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.runtime.RuntimeAutoIO;
import com.lowdragmc.mbd2.common.runtime.RuntimeValue;
import com.lowdragmc.mbd2.common.trait.*;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;

@Getter
public class ForgeEnergyCapabilityTrait extends SimpleCapabilityTrait<IEnergyStorage, @Nullable Direction> implements IAutoIOTrait {
    @Persisted
    @DescSynced
    @ConditionalSynced(methodName = "shouldSyncStorage")
    public final CopiableEnergyStorage storage;
    private final ForgeEnergyRecipeHandler recipeHandler = new ForgeEnergyRecipeHandler();

    // per-machine overrides of the values authored on the definition
    // @Getter(NONE): the class carries a Lombok @Getter, which would otherwise republish getAutoIO()
    // returning RuntimeAutoIO — the name of the old @Nullable AutoIO method, with a different type and
    // without its "null when disabled" contract. getRuntimeAutoIO() is the only door.
    @Getter(lombok.AccessLevel.NONE)
    public final RuntimeAutoIO autoIO =
            new RuntimeAutoIO(runtimeValues, "auto_io", () -> getDefinition().getAutoIO());
    public final RuntimeValue<Integer> maxReceive =
            runtimeValues.ofInt("max_receive", () -> getDefinition().getMaxReceive())
            .onChanged(() -> {
                // the value is baked into the wrapper handed out at capability-resolution time, and a
                // neighbour's BlockCapabilityCache keeps that wrapper until the position is invalidated
                getMachine().invalidateCapabilities();
                getMachine().notifyBlockUpdate();
            });
    public final RuntimeValue<Integer> maxExtract =
            runtimeValues.ofInt("max_extract", () -> getDefinition().getMaxExtract())
            .onChanged(() -> {
                // the value is baked into the wrapper handed out at capability-resolution time, and a
                // neighbour's BlockCapabilityCache keeps that wrapper until the position is invalidated
                getMachine().invalidateCapabilities();
                getMachine().notifyBlockUpdate();
            });
    /**
     * Buffer size, for a machine whose tier should hold more than its definition says.
     * <p>
     * Unlike its two siblings the value is not read live: {@link CopiableEnergyStorage} owns it, and the
     * stored energy has to be clamped when it shrinks. The hook resizes the storage instead, which is
     * also why {@link #createStorages} still reads the definition — at construction time there is no
     * override to read yet, and the hook applies one the moment NBT restores it.
     */
    public final RuntimeValue<Integer> capacity =
            runtimeValues.ofInt("capacity", () -> getDefinition().getCapacity())
            // getStorage() rather than the field, and `this.capacity` rather than a bare name: inside a
            // field initialiser the blank final `storage` is not definitely assigned yet and `capacity` is
            // an illegal forward reference to the field being declared. Neither restriction is real here —
            // the hook runs long after the constructor — but both are compile errors as written.
            .onChanged(() -> {
                if (getStorage().setCapacity(this.capacity.get())) {
                    notifyListeners();
                    getMachine().invalidateCapabilities();
                    getMachine().notifyBlockUpdate();
                }
            });
    private final Map<BlockPos, EnumMap<Direction, BlockCapabilityCache<IEnergyStorage, @Nullable Direction>>> nearbyCache = new HashMap<>();

    public ForgeEnergyCapabilityTrait(MBDMachine machine, ForgeEnergyCapabilityTraitDefinition definition) {
        super(machine, definition);
        storage = createStorages();
        storage.setOnContentsChanged(this::notifyListeners);
    }

    @Override
    public ForgeEnergyCapabilityTraitDefinition getDefinition() {
        return (ForgeEnergyCapabilityTraitDefinition) super.getDefinition();
    }

    @Override
    public IEnergyStorage getCapContent(IO capbilityIO) {
        return new EnergyStorageWrapper(storage, capbilityIO, maxReceive.get(), maxExtract.get());
    }

    @Override
    public void onLoadingTraitInPreview() {
        storage.receiveEnergy(getDefinition().getCapacity() / 2, false);
    }

    protected CopiableEnergyStorage createStorages() {
        return new CopiableEnergyStorage(getDefinition().getCapacity());
    }

    @Override
    public List<IRecipeHandlerTrait<?>> getRecipeHandlerTraits() {
        return List.of(recipeHandler);
    }

    @Override
    public RuntimeAutoIO getRuntimeAutoIO() {
        return autoIO;
    }

    @Nonnull
    public BlockCapabilityCache<IEnergyStorage, @Nullable Direction> getNearbyCache(ServerLevel serverLevel,
                                                                                   BlockPos pos,
                                                                                   @Nonnull Direction side) {
        return nearbyCache.computeIfAbsent(pos, blockPos -> new EnumMap<>(Direction.class))
                .computeIfAbsent(side, direction -> BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK,
                        serverLevel,
                        pos, direction
                ));
    }

    @Override
    public void handleAutoIO(BlockPos port, @NotNull Direction side, IO io) {
        if (getMachine().getLevel() instanceof ServerLevel serverLevel) {
            if (io.support(IO.IN)) {
                var source = getNearbyCache(serverLevel, port, side).getCapability();
                if (source == null) return;

                source.extractEnergy(
                        storage.receiveEnergy(source.extractEnergy(maxReceive.get(), true),
                                false),
                        false);
            }
            if (io.support(IO.OUT)) {
                var target = getNearbyCache(serverLevel, port, side).getCapability();
                if (target == null) return;

                target.receiveEnergy(
                        storage.extractEnergy(target.receiveEnergy(maxExtract.get(), true),
                                false),
                        false);
            }
        }
    }

    public boolean shouldSyncStorage(CopiableEnergyStorage value) {
        return getDefinition().getFancyRendererSettings().isEnable();
    }

    public class ForgeEnergyRecipeHandler extends RecipeHandlerTrait<Integer> {
        protected ForgeEnergyRecipeHandler() {
            super(ForgeEnergyCapabilityTrait.this, ForgeEnergyRecipeCapability.CAP);
        }

        @Override
        public List<Integer> handleRecipeInner(IO io, MBDRecipe recipe, List<Integer> left, @Nullable String slotName, boolean simulate) {
            if (!compatibleWith(io)) return left;
            int required = left.stream().reduce(0, Integer::sum);
            var capability = simulate ? storage.copy() : storage;
            if (io == IO.IN) {
                var extracted = capability.extractEnergy(required, simulate);
                required -= extracted;
            } else {
                var received = capability.receiveEnergy(required, simulate);
                required -= received;
            }
            return required > 0 ? List.of(required) : null;
        }
    }
}
