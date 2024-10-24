package com.lowdragmc.mbd2.integration.embers.trait;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import com.rekindled.embers.api.power.IEmberCapability;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EmbersEmberCapabilityTrait extends SimpleCapabilityTrait<IEmberCapability, Double> {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(EmbersEmberCapabilityTrait.class);
    @Override
    public ManagedFieldHolder getFieldHolder() { return MANAGED_FIELD_HOLDER; }

    @Persisted
    @DescSynced
    public final CopiableEmberCapability storage;

    public EmbersEmberCapabilityTrait(MBDMachine machine, EmbersEmberCapabilityTraitDefinition definition) {
        super(machine, definition);
        storage = createStorages();
        storage.setOnContentsChanged(this::notifyListeners);
    }

    @Override
    public EmbersEmberCapabilityTraitDefinition getDefinition() {
        return (EmbersEmberCapabilityTraitDefinition) super.getDefinition();
    }

    @Override
    public void onLoadingTraitInPreview() {
        storage.setEmber(storage.getEmberCapacity() / 2);
    }

    protected CopiableEmberCapability createStorages() {
        return new CopiableEmberCapability(getDefinition().getCapacity());
    }

    @Override
    public List<Double> handleRecipeInner(IO io, MBDRecipe recipe, List<Double> left, @Nullable String slotName, boolean simulate) {
        if (io != getHandlerIO()) return left;
        var required = left.stream().mapToDouble(Double::doubleValue).reduce(0, Double::sum);
        var capability = simulate ? storage.copy() : storage;
        if (io == IO.IN) {
            var extracted = capability.removeAmount(required, !simulate);
            required -= extracted;
        } else {
            var received = capability.addAmount(required, !simulate);
            required -= received;
        }
        return required > 0 ? List.of(required) : null;
    }

    @Override
    public IEmberCapability getCapContent(IO capbilityIO) {
        return new EmberCapabilityWrapper(this.storage, capbilityIO);
    }

    @Override
    public IEmberCapability mergeContents(List<IEmberCapability> contents) {
        return contents.get(0);
    }
}
