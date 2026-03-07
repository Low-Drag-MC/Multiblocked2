//package com.lowdragmc.mbd2.integration.pneumaticcraft.trait.heat;
//
//import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
//import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
//import com.lowdragmc.lowdraglib2.syncdata.field.ManagedFieldHolder;
//import com.lowdragmc.mbd2.api.capability.recipe.IO;
//import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
//import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
//import com.lowdragmc.mbd2.common.machine.MBDMachine;
//import com.lowdragmc.mbd2.common.trait.ICapabilityProviderTrait;
//import com.lowdragmc.mbd2.common.trait.RecipeCapabilityTrait;
//import com.lowdragmc.mbd2.common.trait.RecipeHandlerTrait;
//import com.lowdragmc.mbd2.integration.pneumaticcraft.PNCHeatRecipeCapability;
//import lombok.Getter;
//import me.desht.pneumaticcraft.api.PNCCapabilities;
//import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Direction;
//import net.minecraft.world.level.block.Block;
//import net.neoforged.common.capabilities.Capability;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.List;
//
//@Getter
//public class PNCHeatExchangerTrait extends RecipeCapabilityTrait {
//    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(PNCHeatExchangerTrait.class);
//    @Override
//    public ManagedFieldHolder getFieldHolder() { return MANAGED_FIELD_HOLDER; }
//
//    @Persisted
//    @DescSynced
//    public final HeatExchanger handler;
//    private final HeatRecipeHandler heatRecipeHandler = new HeatRecipeHandler();
//    private final HeatExchangerCap heatExchangerCap = new HeatExchangerCap();
//    // runtime
//    private boolean isFirstTick = true;
//
//    public PNCHeatExchangerTrait(MBDMachine machine, PNCHeatExchangerTraitDefinition definition) {
//        super(machine, definition);
//        handler = createHandler();
//    }
//
//    @Override
//    public PNCHeatExchangerTraitDefinition getDefinition() {
//        return (PNCHeatExchangerTraitDefinition) super.getDefinition();
//    }
//
//    @Override
//    public void onLoadingTraitInPreview() {
//        handler.setTemperatureWithoutNotify(100);
//    }
//
//    protected HeatExchanger createHandler() {
//        var handler = new HeatExchanger();
//        handler.setThermalCapacity(getDefinition().getThermalCapacity());
//        handler.setThermalResistance(getDefinition().getThermalResistance());
//        return handler;
//    }
//
//    @Override
//    public void serverTick() {
//        super.serverTick();
//        if (isFirstTick) {
//            handler.initializeAsHull(getMachine().getLevel(), getMachine().getPos(), IHeatExchangerLogic.ALL_BLOCKS, Direction.values());
//            isFirstTick = false;
//        }
//        handler.tick();
//    }
//
//    @Override
//    public void onNeighborChanged(Block block, BlockPos fromPos, boolean isMoving) {
//        handler.initializeAsHull(getMachine().getLevel(), getMachine().getPos(), IHeatExchangerLogic.ALL_BLOCKS, Direction.values());
//    }
//
//    public class HeatRecipeHandler extends RecipeHandlerTrait<Double> {
//        protected HeatRecipeHandler() {
//            super(PNCHeatExchangerTrait.this, PNCHeatRecipeCapability.CAP);
//        }
//
//        @Override
//        public List<Double> handleRecipeInner(IO io, MBDRecipe recipe, List<Double> left, @Nullable String slotName, boolean simulate) {
//            if (!compatibleWith(io)) return left;
//            double required = left.stream().reduce(0d, Double::sum);
//            var temp = handler.getTemperature();
//            var cap = handler.getThermalCapacity();
//            // check temp condition first
//            for (var condition : recipe.conditions) {
//                if (condition instanceof PNCTemperatureCondition tempCondition) {
//                    if (tempCondition.getMinTemperature() > (temp - 273) || tempCondition.getMaxTemperature() < (temp - 273)) {
//                        return left;
//                    }
//                }
//            }
//            var requiredTemp = required / cap;
//            if (io == IO.IN) {
//                if (requiredTemp < temp) {
//                    if (!simulate) {
//                        handler.addHeat(-required);
//                    }
//                    return null;
//                }
//            } else if (io == IO.OUT) {
//                if (requiredTemp < 2273 - temp) {
//                    if (!simulate) {
//                        handler.addHeat(required);
//                    }
//                    return null;
//                }
//            }
//            return List.of(required);
//        }
//    }
//
//    @Override
//    public List<IRecipeHandlerTrait<?>> getRecipeHandlerTraits() {
//        return List.of(heatRecipeHandler);
//    }
//
//    @Override
//    public List<ICapabilityProviderTrait<?>> getCapabilityProviderTraits() {
//        return List.of(heatExchangerCap);
//    }
//
//    public class HeatExchangerCap implements ICapabilityProviderTrait<IHeatExchangerLogic>{
//
//        @Override
//        public IO getCapabilityIO(@Nullable Direction side) {
//            return IO.BOTH;
//        }
//
//        @Override
//        public Capability<? super IHeatExchangerLogic> getCapability() {
//            return PNCCapabilities.HEAT_EXCHANGER_CAPABILITY;
//        }
//
//        @Override
//        public IHeatExchangerLogic getCapContent(IO capbilityIO) {
//            return handler;
//        }
//
//        @Override
//        public IHeatExchangerLogic mergeContents(List<IHeatExchangerLogic> contents) {
//            return handler;
//        }
//    }
//
//}
