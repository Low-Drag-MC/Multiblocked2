package com.lowdragmc.mbd2.common.trait.fluid;

import com.google.common.base.Predicates;
import com.lowdragmc.lowdraglib.misc.FluidStorage;
import com.lowdragmc.lowdraglib.misc.FluidTransferList;
import com.lowdragmc.lowdraglib.side.fluid.*;
import com.lowdragmc.lowdraglib.side.fluid.forge.FluidHelperImpl;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.ingredient.FluidIngredient;
import com.lowdragmc.mbd2.common.capability.recipe.FluidRecipeCapability;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.*;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.common.capabilities.Capability;
import net.neoforged.common.capabilities.ForgeCapabilities;
import net.neoforged.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class FluidTankCapabilityTrait extends SimpleCapabilityTrait implements IAutoIOTrait {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(FluidTankCapabilityTrait.class);
    @Override
    public ManagedFieldHolder getFieldHolder() { return MANAGED_FIELD_HOLDER; }

    @Persisted
    @DescSynced
    public final FluidStorage[] storages;
    @Setter
    protected boolean allowSameFluids; // Can different tanks be filled with the same fluid. It should be determined while creating tanks.
    private Boolean isEmpty;
    private final FluidRecipeHandler recipeHandler = new FluidRecipeHandler();
    private final FluidHandlerCap fluidHandlerCap = new FluidHandlerCap();

    public FluidTankCapabilityTrait(MBDMachine machine, FluidTankCapabilityTraitDefinition definition) {
        super(machine, definition);
        storages = createStorages();
    }

    @Override
    public FluidTankCapabilityTraitDefinition getDefinition() {
        return (FluidTankCapabilityTraitDefinition) super.getDefinition();
    }

    @Override
    public void onLoadingTraitInPreview() {
        if (storages.length > 0) {
            storages[0].setFluid(FluidStack.create(Fluids.WATER, Math.max(getDefinition().getCapacity() / 2, 1)));
        }
    }

    protected FluidStorage[] createStorages() {
        var storages = new FluidStorage[getDefinition().getTankSize()];
        for (int i = 0; i < storages.length; i++) {
            storages[i] = new FluidStorage(getDefinition().getCapacity());
            storages[i].setOnContentsChanged(this::onContentsChanged);
            if (getDefinition().getFluidFilterSettings().isEnable()) {
                storages[i].setValidator(getDefinition().getFluidFilterSettings());
            }
        }
        return storages;
    }

    public void onContentsChanged() {
        isEmpty = null;
        notifyListeners();
    }

    public boolean isEmpty() {
        if (isEmpty == null) {
            isEmpty = true;
            for (FluidStorage storage : storages) {
                if (!storage.getFluid().isEmpty()) {
                    isEmpty = false;
                    break;
                }
            }
        }
        return isEmpty;
    }

    @Override
    public void serverTick() {
        IAutoIOTrait.super.serverTick();
        var timer = getMachine().getOffsetTimer();
        var autoInput = getDefinition().getAutoInput();
        var autoOutput = getDefinition().getAutoOutput();
        var level = getMachine().getLevel();
        if (autoInput.isEnable() && timer % autoInput.getInterval() == 0) {
            var leftBlocks = autoInput.getSpeed();
            var range = autoOutput.getRotatedRange(getMachine().getFrontFacing().orElse(Direction.NORTH)).move(getMachine().getPos());
            for (int x = (int) Math.round(range.minX); x < (int) Math.round(range.maxX); x++) {
                if (leftBlocks <= 0) break;
                for (int y = (int) Math.round(range.minY); y < (int) Math.round(range.maxY); y++) {
                    if (leftBlocks <= 0) break;
                    for (int z = (int) Math.round(range.minZ); z < (int) Math.round(range.maxZ); z++) {
                        if (leftBlocks <= 0) break;
                        var pos = new BlockPos(x, y, z);
                        var state = level.getBlockState(pos);
                        var block = state.getBlock();
                        if (block instanceof LiquidBlock liquidBlock && state.getFluidState().isSource()) {
                            var toFilled = FluidStack.create(liquidBlock.getFluid().getSource(), 1000);
                            for (FluidStorage storage : storages) {
                                if (storage.fill(toFilled, true) == 1000) {
                                    storage.fill(toFilled, false);
                                    leftBlocks--;
                                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        if (autoOutput.isEnable() && timer % autoOutput.getInterval() == 0) {
            var leftBlocks = autoOutput.getSpeed();
            var range = autoOutput.getRotatedRange(getMachine().getFrontFacing().orElse(Direction.NORTH)).move(getMachine().getPos());

            for (int x = (int) Math.round(range.minX); x < (int) Math.round(range.maxX); x++) {
                if (leftBlocks <= 0 || isEmpty()) break;
                for (int y = (int) Math.round(range.minY); y < (int) Math.round(range.maxY); y++) {
                    if (leftBlocks <= 0 || isEmpty()) break;
                    for (int z = (int) Math.round(range.minZ); z < (int) Math.round(range.maxZ) ; z++) {
                        if (leftBlocks <= 0 || isEmpty()) break;
                        var pos = new BlockPos(x, y, z);
                        var state = level.getBlockState(pos);
                        for (FluidStorage storage : storages) {
                            var drained = storage.drain(1000, true);
                            if (drained.getAmount() == 1000 && drained.getFluid().getFluidType().canBePlacedInLevel(level, pos, FluidHelperImpl.toFluidStack(drained))) {
                                if (!(state.getFluidState().isSource()) && state.canBeReplaced(drained.getFluid())) {
                                    if (!level.isClientSide) {
                                        level.destroyBlock(pos, true);
                                    }
                                    level.setBlockAndUpdate(pos, drained.getFluid().defaultFluidState().createLegacyBlock());
                                    leftBlocks--;
                                    storage.drain(1000, false);
                                    break;
                                }
                            }
                        }
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
        return List.of(fluidHandlerCap);
    }

    @Override
    public @Nullable AutoIO getAutoIO() {
        return getDefinition().getAutoIO().isEnable() ? getDefinition().getAutoIO() : null;
    }

    @Override
    public void handleAutoIO(BlockPos port, Direction side, IO io) {
        if (io.support(IO.IN)) {
            FluidTransferHelper.importToTarget(new FluidTransferList(storages), Integer.MAX_VALUE,
                    getDefinition().getFluidFilterSettings().isEnable() ? getDefinition().getFluidFilterSettings() : Predicates.alwaysTrue(),
                    getMachine().getLevel(), port.relative(side), side.getOpposite());
        }
        if (io.support(IO.OUT)){
            FluidTransferHelper.exportToTarget(new FluidTransferList(storages), Integer.MAX_VALUE, Predicates.alwaysTrue(),
                    getMachine().getLevel(), port.relative(side), side.getOpposite());
        }
    }

    public class FluidRecipeHandler extends RecipeHandlerTrait<FluidIngredient> {
        protected FluidRecipeHandler() {
            super(FluidTankCapabilityTrait.this, FluidRecipeCapability.CAP);
        }

        @Override
        public List<FluidIngredient> handleRecipeInner(IO io, MBDRecipe recipe, List<FluidIngredient> left, @Nullable String slotName, boolean simulate) {
            if (!compatibleWith(io)) return left;
            var capabilities = simulate ? Arrays.stream(storages).map(FluidStorage::copy).toArray(FluidStorage[]::new) : storages;
            for (FluidStorage capability : capabilities) {
                Iterator<FluidIngredient> iterator = left.iterator();
                if (io == IO.IN) {
                    while (iterator.hasNext()) {
                        FluidIngredient fluidStack = iterator.next();
                        if (fluidStack.isEmpty()) {
                            iterator.remove();
                            continue;
                        }
                        boolean found = false;
                        FluidStack foundStack = null;
                        for (int i = 0; i < capability.getTanks(); i++) {
                            FluidStack stored = capability.getFluidInTank(i);
                            if (!fluidStack.test(stored)) {
                                continue;
                            }
                            found = true;
                            foundStack = stored;
                        }
                        if (!found) continue;
                        FluidStack drained = capability.drain(foundStack.copy(fluidStack.getAmount()), false);

                        fluidStack.setAmount(fluidStack.getAmount() - drained.getAmount());
                        if (fluidStack.getAmount() <= 0) {
                            iterator.remove();
                        }
                    }
                } else if (io == IO.OUT) {
                    while (iterator.hasNext()) {
                        FluidIngredient fluidStack = iterator.next();
                        if (fluidStack.isEmpty()) {
                            iterator.remove();
                            continue;
                        }
                        var fluids = fluidStack.getStacks();
                        if (fluids.length == 0) {
                            iterator.remove();
                            continue;
                        }
                        FluidStack output = fluids[0];
                        long filled = capability.fill(output.copy(), false);
                        if (!fluidStack.isEmpty()) {
                            fluidStack.setAmount(fluidStack.getAmount() - filled);
                        }
                        if (fluidStack.getAmount() <= 0) {
                            iterator.remove();
                        }
                    }
                }
                if (left.isEmpty()) break;
            }
            return left.isEmpty() ? null : left;
        }
    }

    public class FluidHandlerCap implements ICapabilityProviderTrait<IFluidHandler> {
        @Override
        public IO getCapabilityIO(@Nullable Direction side) {
            return FluidTankCapabilityTrait.this.getCapabilityIO(side);
        }

        @Override
        public Capability<IFluidHandler> getCapability() {
            return ForgeCapabilities.FLUID_HANDLER;
        }

        @Override
        public IFluidHandler getCapContent(IO capbilityIO) {
            return new FluidHandlerWrapper(storages, capbilityIO, getDefinition().isAllowSameFluids());
        }

        @Override
        public IFluidHandler mergeContents(List<IFluidHandler> contents) {
            return new FluidHandlerList(contents.toArray(new IFluidHandler[0]));
        }
    }
}
