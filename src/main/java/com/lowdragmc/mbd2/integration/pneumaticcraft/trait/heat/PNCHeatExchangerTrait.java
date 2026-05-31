package com.lowdragmc.mbd2.integration.pneumaticcraft.trait.heat;

import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
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
import com.lowdragmc.mbd2.integration.pneumaticcraft.PNCHeatRecipeCapability;
import lombok.Getter;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class PNCHeatExchangerTrait extends SimpleCapabilityTrait<IHeatExchangerLogic, @Nullable Direction> implements IAutoIOTrait {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(PNCHeatExchangerTrait.class);

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Persisted
    @DescSynced
    public final HeatExchanger handler;
    private final HeatRecipeHandler recipeHandler = new HeatRecipeHandler();
    private final Map<BlockPos, EnumMap<Direction, BlockCapabilityCache<IHeatExchangerLogic, @Nullable Direction>>> nearbyCache = new HashMap<>();
    private boolean isFirstTick = true;

    public PNCHeatExchangerTrait(MBDMachine machine, PNCHeatExchangerTraitDefinition definition) {
        super(machine, definition);
        handler = new HeatExchanger();
        handler.setThermalCapacity(definition.getThermalCapacity());
        handler.setThermalResistance(definition.getThermalResistance());
        handler.setOnContentsChanged(this::notifyListeners);
    }

    @Override
    public PNCHeatExchangerTraitDefinition getDefinition() {
        return (PNCHeatExchangerTraitDefinition) super.getDefinition();
    }

    @Override
    public void onLoadingTraitInPreview() {
        handler.setTemperatureWithoutNotify(373); // 100°C
    }

    @Override
    public IHeatExchangerLogic getCapContent(IO capabilityIO) {
        return handler;
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
        var level = getMachine().getLevel();
        if (level == null) return;
        if (isFirstTick) {
            handler.initializeAsHull(level, getMachine().getPos(), IHeatExchangerLogic.ALL_BLOCKS, Direction.values());
            isFirstTick = false;
        }
        handler.tick();
    }

    @Override
    public void onNeighborChanged(Block block, BlockPos fromPos, boolean isMoving) {
        var level = getMachine().getLevel();
        if (level != null) {
            handler.initializeAsHull(level, getMachine().getPos(), IHeatExchangerLogic.ALL_BLOCKS, Direction.values());
        }
    }

    @NotNull
    public BlockCapabilityCache<IHeatExchangerLogic, @Nullable Direction> getNearbyCache(ServerLevel serverLevel, BlockPos pos, Direction side) {
        return nearbyCache.computeIfAbsent(pos, p -> new EnumMap<>(Direction.class))
                .computeIfAbsent(side, direction -> BlockCapabilityCache.create(
                        PNCCapabilities.HEAT_EXCHANGER_BLOCK, serverLevel, pos, direction));
    }

    @Override
    public void handleAutoIO(BlockPos port, @NotNull Direction side, IO io) {
        if (!(getMachine().getLevel() instanceof ServerLevel serverLevel)) return;
        var neighbor = getNearbyCache(serverLevel, port, side).getCapability();
        if (neighbor == null) return;
        if (io.support(IO.OUT)) {
            transferHeat(handler, neighbor);
        }
        if (io.support(IO.IN)) {
            transferHeat(neighbor, handler);
        }
    }

    private static void transferHeat(IHeatExchangerLogic source, IHeatExchangerLogic target) {
        double srcTemp = source.getTemperature();
        double tgtTemp = target.getTemperature();
        double delta = srcTemp - tgtTemp;
        if (delta <= 0.01) return;
        double resistance = source.getThermalResistance() + target.getThermalResistance();
        double srcCap = source.getThermalCapacity();
        double transfer = (delta * srcCap) / Math.max(1.0, resistance);
        if (transfer < 0.01) return;
        source.addHeat(-transfer);
        target.addHeat(transfer);
    }

    public class HeatRecipeHandler extends RecipeHandlerTrait<Double> {
        protected HeatRecipeHandler() {
            super(PNCHeatExchangerTrait.this, PNCHeatRecipeCapability.CAP);
        }

        @Override
        public List<Double> handleRecipeInner(IO io, MBDRecipe recipe, List<Double> left, @Nullable String slotName, boolean simulate) {
            if (!compatibleWith(io)) return left;
            double required = left.stream().mapToDouble(Double::doubleValue).sum();
            var target = simulate ? handler.copy() : handler;
            double cap = target.getThermalCapacity();
            double temp = target.getTemperature();
            double requiredTemp = required / Math.max(1.0, cap);
            if (io == IO.IN) {
                if (requiredTemp < temp) {
                    target.addHeat(-required);
                    return null;
                }
            } else if (io == IO.OUT) {
                if (requiredTemp < 2273 - temp) {
                    target.addHeat(required);
                    return null;
                }
            }
            return List.of(required);
        }
    }
}
