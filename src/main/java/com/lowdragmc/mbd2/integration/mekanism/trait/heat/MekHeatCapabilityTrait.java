package com.lowdragmc.mbd2.integration.mekanism.trait.heat;

import com.gregtechceu.gtceu.api.misc.EnergyContainerList;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import mekanism.api.heat.IHeatHandler;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MekHeatCapabilityTrait extends SimpleCapabilityTrait<IHeatHandler, Double> {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(MekHeatCapabilityTrait.class);
    @Override
    public ManagedFieldHolder getFieldHolder() { return MANAGED_FIELD_HOLDER; }

    @Persisted
    @DescSynced
    public final CopiableHeatContainer container;

    public MekHeatCapabilityTrait(MBDMachine machine, MekHeatCapabilityTraitDefinition definition) {
        super(machine, definition);
        container = createStorages();
    }

    @Override
    public MekHeatCapabilityTraitDefinition getDefinition() {
        return (MekHeatCapabilityTraitDefinition) super.getDefinition();
    }

    @Override
    public void onLoadingTraitInPreview() {
        container.handleHeat(getDefinition().getCapacity() / 2);
    }

    protected CopiableHeatContainer createStorages() {
        return new CopiableHeatContainer(getDefinition().getCapacity(),getDefinition().getInverseConduction());
    }

    @Override
    public List<Double> handleRecipeInner(IO io, MBDRecipe recipe, List<Double> left, @Nullable String slotName, boolean simulate) {
        if (io != getHandlerIO()) return left;
        double required = left.stream().reduce(0d, Double::sum);
        var capability = simulate ? container.copy() : container;
        if (io == IO.IN) {
            capability.handleHeat(-required);
        } else if (io == IO.OUT) {
            capability.handleHeat(required);
        }
        return null;
    }

    @Override
    public IHeatHandler getCapContent(@Nullable Direction side) {
        return new HeatContainerWrapper(this.container, getCapabilityIO(side));
    }

    @Override
    public IHeatHandler mergeContents(List<IHeatHandler> contents) {
        return new HeatContainerList(contents.toArray(new IHeatHandler[0]));
    }
}
