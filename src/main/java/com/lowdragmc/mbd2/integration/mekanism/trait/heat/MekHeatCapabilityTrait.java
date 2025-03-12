package com.lowdragmc.mbd2.integration.mekanism.trait.heat;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ICapabilityProviderTrait;
import com.lowdragmc.mbd2.common.trait.RecipeHandlerTrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import com.lowdragmc.mbd2.integration.mekanism.MEKTemperatureCondition;
import com.lowdragmc.mbd2.integration.mekanism.MekanismHeatRecipeCapability;
import lombok.Getter;
import mekanism.api.heat.HeatAPI;
import mekanism.api.heat.IHeatHandler;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.heat.CachedAmbientTemperature;
import mekanism.common.util.CapabilityUtils;
import mekanism.common.util.EnumUtils;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.Direction;
import net.neoforged.common.capabilities.Capability;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
public class MekHeatCapabilityTrait extends SimpleCapabilityTrait {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(MekHeatCapabilityTrait.class);
    @Override
    public ManagedFieldHolder getFieldHolder() { return MANAGED_FIELD_HOLDER; }

    @Persisted
    @DescSynced
    public final CopiableHeatContainer container;
    private final HeatRecipeHandler recipeHandler = new HeatRecipeHandler();
    private final HeatHandlerCap heatHandlerCap = new HeatHandlerCap();
    private final CachedAmbientTemperature ambientTemperature = new CachedAmbientTemperature(() -> getMachine().getLevel(), () -> getMachine().getPos());


    public MekHeatCapabilityTrait(MBDMachine machine, MekHeatCapabilityTraitDefinition definition) {
        super(machine, definition);
        container = createStorages();
        container.setOnContentsChanged(this::notifyListeners);
    }

    @Override
    public MekHeatCapabilityTraitDefinition getDefinition() {
        return (MekHeatCapabilityTraitDefinition) super.getDefinition();
    }

    @Override
    public void onLoadingTraitInPreview() {
        container.handleHeat(container.capacity * HeatAPI.AMBIENT_TEMP);
    }

    protected CopiableHeatContainer createStorages() {
        return new CopiableHeatContainer(getDefinition().getCapacity(),getDefinition().getInverseConduction());
    }

    protected double getTotalInverseConductionCoefficient() {
        var heatCapacitorCount = container.getHeatCapacitorCount();
        if (heatCapacitorCount == 0) {
            return HeatAPI.DEFAULT_INVERSE_CONDUCTION;
        } else if (heatCapacitorCount == 1) {
            return container.getInverseConduction(0);
        }
        var sum = 0d;
        var totalCapacity = container.getTotalHeatCapacity();
        for (var capacitor = 0; capacitor < heatCapacitorCount; capacitor++) {
            sum += container.getInverseConduction(capacitor) * (container.getHeatCapacity(capacitor) / totalCapacity);
        }
        return sum;
    }

    @Override
    public void serverTick() {
        super.serverTick();
        if (getDefinition().isSimulateEnvironment()) {
            var heatCapacity = container.getTotalHeatCapacity();
            //transfer to air otherwise
            var invConduction = HeatAPI.AIR_INVERSE_COEFFICIENT + HeatAPI.DEFAULT_INVERSE_INSULATION + getTotalInverseConductionCoefficient();
            //transfer heat difference based on environment temperature (ambient)
            var tempToTransfer = (container.getTotalTemperature() - HeatAPI.AMBIENT_TEMP) / invConduction;
            container.handleHeat(-tempToTransfer * heatCapacity);
        }
        var autoIO = getDefinition().getAutoIO();
        if (autoIO.isEnable() && getMachine().getOffsetTimer() % autoIO.getInterval() == 0) {
            var front = getMachine().getFrontFacing().orElse(Direction.NORTH);
            for (var side : EnumUtils.DIRECTIONS) {
                var io = autoIO.getIO(front, side);
                if (io.support(IO.OUT)) {
                    var adj = WorldUtils.getTileEntity(getMachine().getLevel(), getMachine().getPos().relative(side));
                    var sink = CapabilityUtils.getCapability(adj, Capabilities.HEAT_HANDLER, side.getOpposite()).resolve().orElse(null);
                    if (sink != null) {
                        double heatCapacity = container.getTotalHeatCapacity();
                        double invConduction = sink.getTotalInverseConduction() + getTotalInverseConductionCoefficient();
                        double tempToTransfer = (container.getTotalTemperature() - ambientTemperature.getTemperature(side)) / invConduction;
                        double heatToTransfer = tempToTransfer * heatCapacity;
                        container.handleHeat(-heatToTransfer);
                        sink.handleHeat(heatToTransfer);
                    }
                }
            }
        }

    }

    @Override
    public List<IRecipeHandlerTrait<?>> getRecipeHandlerTraits() {
        return List.of(recipeHandler);
    }

    @Override
    public List<ICapabilityProviderTrait<?>> getCapabilityProviderTraits() {
        return List.of(heatHandlerCap);
    }

    public class HeatRecipeHandler extends RecipeHandlerTrait<Double> {
        protected HeatRecipeHandler() {
            super(MekHeatCapabilityTrait.this, MekanismHeatRecipeCapability.CAP);
        }

        @Override
        public List<Double> handleRecipeInner(IO io, MBDRecipe recipe, List<Double> left, @Nullable String slotName, boolean simulate) {
            if (!compatibleWith(io)) return left;
            double required = left.stream().reduce(0d, Double::sum);
            var capability = simulate ? container.copy() : container;
            // check heat condition first
            for (RecipeCondition condition : recipe.conditions) {
                if (condition instanceof MEKTemperatureCondition heatCondition) {
                    var temp = capability.getTemperature(0);
                    if (heatCondition.getMinTemperature() > temp || heatCondition.getMaxTemperature() < temp) {
                        return left;
                    }
                }
            }
            if (io == IO.IN) {
                capability.handleHeat(-required);
            } else if (io == IO.OUT) {
                capability.handleHeat(required);
            }
            return null;
        }
    }

    public class HeatHandlerCap implements ICapabilityProviderTrait<IHeatHandler> {
        @Override
        public IO getCapabilityIO(@Nullable Direction side) {
            return MekHeatCapabilityTrait.this.getCapabilityIO(side);
        }

        @Override
        public Capability<IHeatHandler> getCapability() {
            return Capabilities.HEAT_HANDLER;
        }

        @Override
        public IHeatHandler getCapContent(IO capbilityIO) {
            return new HeatContainerWrapper(container, capbilityIO);
        }

        @Override
        public IHeatHandler mergeContents(List<IHeatHandler> contents) {
            return new HeatContainerList(contents.toArray(new IHeatHandler[0]));
        }
    }
}
