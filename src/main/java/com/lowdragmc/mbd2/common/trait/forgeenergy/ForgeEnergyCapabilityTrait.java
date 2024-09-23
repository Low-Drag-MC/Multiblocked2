package com.lowdragmc.mbd2.common.trait.forgeenergy;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import net.minecraft.core.Direction;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ForgeEnergyCapabilityTrait extends SimpleCapabilityTrait<IEnergyStorage, Integer> {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(ForgeEnergyCapabilityTrait.class);
    @Override
    public ManagedFieldHolder getFieldHolder() { return MANAGED_FIELD_HOLDER; }

    @Persisted
    @DescSynced
    public final CopiableEnergyStorage storage;

    public ForgeEnergyCapabilityTrait(MBDMachine machine, ForgeEnergyCapabilityTraitDefinition definition) {
        super(machine, definition);
        storage = createStorages();
    }

    @Override
    public ForgeEnergyCapabilityTraitDefinition getDefinition() {
        return (ForgeEnergyCapabilityTraitDefinition) super.getDefinition();
    }

    @Override
    public void onLoadingTraitInPreview() {
        storage.receiveEnergy(getDefinition().getCapacity() / 2, false);
    }

    protected CopiableEnergyStorage createStorages() {
        return new CopiableEnergyStorage(getDefinition().getCapacity(), getDefinition().getMaxReceive(), getDefinition().getMaxExtract());
    }

    @Override
    public List<Integer> handleRecipeInner(IO io, MBDRecipe recipe, List<Integer> left, @Nullable String slotName, boolean simulate) {
        if (io != getHandlerIO()) return left;
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

    @Override
    public IEnergyStorage getCapContent(@Nullable Direction side) {
        return new EnergyStorageWrapper(this.storage, getCapabilityIO(side));
    }

    @Override
    public IEnergyStorage mergeContents(List<IEnergyStorage> contents) {
        return new EnergyStorageList(contents.toArray(new IEnergyStorage[0]));
    }
}
