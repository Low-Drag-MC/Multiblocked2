package com.lowdragmc.mbd2.integration.mekanism.trait.heat;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.AutoIO;
import com.lowdragmc.mbd2.common.trait.IAutoIOTrait;
import com.lowdragmc.mbd2.common.trait.RecipeHandlerTrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import com.lowdragmc.mbd2.integration.mekanism.MekanismHeatRecipeCapability;
import lombok.Getter;
import mekanism.api.heat.HeatAPI;
import mekanism.api.heat.IHeatHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class MekHeatCapabilityTrait extends SimpleCapabilityTrait<IHeatHandler, @Nullable Direction> implements IAutoIOTrait {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(MekHeatCapabilityTrait.class);

    @Override
    public ManagedFieldHolder getFieldHolder() { return MANAGED_FIELD_HOLDER; }

    @Persisted
    public final CopiableHeatContainer storage;

    private final HeatRecipeHandler recipeHandler = new HeatRecipeHandler();
    private final Map<BlockPos, EnumMap<Direction, BlockCapabilityCache<IHeatHandler, @Nullable Direction>>> nearbyCache = new HashMap<>();

    public MekHeatCapabilityTrait(MBDMachine machine, MekHeatCapabilityTraitDefinition definition) {
        super(machine, definition);
        storage = new CopiableHeatContainer(definition.getHeatCapacity(), definition.getInverseConductionCoefficient());
        storage.setOnContentsChanged(this::notifyListeners);
    }

    @Override
    public MekHeatCapabilityTraitDefinition getDefinition() {
        return (MekHeatCapabilityTraitDefinition) super.getDefinition();
    }

    @Override
    public IHeatHandler getCapContent(IO capabilityIO) {
        return new HeatContainerWrapper(storage, capabilityIO);
    }

    @Override
    public List<IRecipeHandlerTrait<?>> getRecipeHandlerTraits() {
        return List.of(recipeHandler);
    }

    @Override
    public @Nullable AutoIO getAutoIO() {
        return getDefinition().getAutoIO().isEnable() ? getDefinition().getAutoIO() : null;
    }

    @Override
    public void serverTick() {
        IAutoIOTrait.super.serverTick();
        simulateEnvironment();
    }

    /**
     * Bleed heat to (or absorb from) the surrounding biome to model passive cooling.
     */
    private void simulateEnvironment() {
        var level = getMachine().getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;
        var pos = getMachine().getPos();
        double ambient = HeatAPI.getAmbientTemp(serverLevel, pos);
        double temperature = storage.getTemperature(0);
        double delta = temperature - ambient;
        if (Math.abs(delta) < HeatAPI.EPSILON) return;
        double conduction = storage.getInverseConduction(0) + HeatAPI.AIR_INVERSE_COEFFICIENT;
        double transfer = -(delta * storage.getHeatCapacity(0)) / conduction;
        storage.handleHeat(0, transfer);
    }

    @NotNull
    public BlockCapabilityCache<IHeatHandler, @Nullable Direction> getNearbyCache(ServerLevel serverLevel, BlockPos pos, Direction side) {
        return nearbyCache.computeIfAbsent(pos, p -> new EnumMap<>(Direction.class))
                .computeIfAbsent(side, direction -> BlockCapabilityCache.create(
                        mekanism.common.capabilities.Capabilities.HEAT, serverLevel, pos, direction));
    }

    @Override
    public void handleAutoIO(BlockPos port, @NotNull Direction side, IO io) {
        if (!(getMachine().getLevel() instanceof ServerLevel serverLevel)) return;
        var neighbor = getNearbyCache(serverLevel, port, side).getCapability();
        if (neighbor == null) return;
        if (io.support(IO.OUT)) {
            transfer(storage, neighbor);
        }
        if (io.support(IO.IN)) {
            transfer(neighbor, storage);
        }
    }

    private static void transfer(IHeatHandler source, IHeatHandler target) {
        double srcTemp = source.getTemperature(0);
        double tgtTemp = target.getTemperature(0);
        double delta = srcTemp - tgtTemp;
        if (delta <= HeatAPI.EPSILON) return;
        double conduction = source.getInverseConduction(0) + target.getInverseConduction(0);
        double srcCap = source.getHeatCapacity(0);
        double transfer = (delta * srcCap) / conduction;
        if (transfer < HeatAPI.EPSILON) return;
        source.handleHeat(0, -transfer);
        target.handleHeat(0, transfer);
    }

    public class HeatRecipeHandler extends RecipeHandlerTrait<Double> {
        protected HeatRecipeHandler() {
            super(MekHeatCapabilityTrait.this, MekanismHeatRecipeCapability.CAP);
        }

        @Override
        public List<Double> handleRecipeInner(IO io, MBDRecipe recipe, List<Double> left, @Nullable String slotName, boolean simulate) {
            if (!compatibleWith(io)) return left;
            double required = left.stream().mapToDouble(Double::doubleValue).sum();
            var target = simulate ? storage.copy() : storage;
            if (io == IO.IN) {
                // recipe consumes heat from the machine
                double available = Math.max(0, target.getTemperature(0)) * target.getHeatCapacity(0);
                double consumed = Math.min(available, required);
                if (consumed > 0) target.handleHeat(0, -consumed);
                required -= consumed;
            } else {
                // recipe produces heat into the machine
                target.handleHeat(0, required);
                required = 0;
            }
            return required > 0 ? List.of(required) : null;
        }

        public double getTemperature() {
            return storage.getTemperature(0);
        }
    }
}
