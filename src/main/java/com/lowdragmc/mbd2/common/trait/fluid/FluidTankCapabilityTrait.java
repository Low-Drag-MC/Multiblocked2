package com.lowdragmc.mbd2.common.trait.fluid;

import com.google.common.base.Predicates;
import com.lowdragmc.lowdraglib2.misc.FluidStorage;
import com.lowdragmc.lowdraglib2.misc.FluidTransferList;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.capability.recipe.FluidRecipeCapability;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Predicate;

public class FluidTankCapabilityTrait extends SimpleCapabilityTrait<IFluidHandler, @Nullable Direction> implements IAutoIOTrait {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(FluidTankCapabilityTrait.class);
    @Override
    public ManagedFieldHolder getFieldHolder() { return MANAGED_FIELD_HOLDER; }

    @Persisted
    @DescSynced
    public final FluidStorage[] storages;
    private final FluidRecipeHandler recipeHandler = new FluidRecipeHandler();

    // runtime
    private final Random random = new Random();
    private Boolean isEmpty;
    private final Map<BlockPos, EnumMap<Direction, BlockCapabilityCache<IFluidHandler, @Nullable Direction>>> nearbyCache = new HashMap<>();

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
            storages[0].setFluid(new FluidStack(Fluids.WATER, Math.max(getDefinition().getCapacity() / 2, 1)));
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
                            var toFilled = new FluidStack(liquidBlock.fluid.getSource(), 1000);
                            for (FluidStorage storage : storages) {
                                if (storage.fill(toFilled, IFluidHandler.FluidAction.SIMULATE) == 1000) {
                                    storage.fill(toFilled, IFluidHandler.FluidAction.EXECUTE);
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
                            var drained = storage.drain(1000, IFluidHandler.FluidAction.SIMULATE);
                            if (drained.getAmount() == 1000 && drained.getFluid().getFluidType().canBePlacedInLevel(level, pos, drained)) {
                                if (!(state.getFluidState().isSource()) && state.canBeReplaced(drained.getFluid())) {
                                    if (!level.isClientSide) {
                                        level.destroyBlock(pos, true);
                                    }
                                    level.setBlockAndUpdate(pos, drained.getFluid().defaultFluidState().createLegacyBlock());
                                    leftBlocks--;
                                    storage.drain(1000, IFluidHandler.FluidAction.EXECUTE);
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
    public IFluidHandler getCapContent(IO capbilityIO) {
        return new FluidHandlerWrapper(storages, capbilityIO, getDefinition().isAllowSameFluids());
    }

    @Override
    public @Nullable AutoIO getAutoIO() {
        return getDefinition().getAutoIO().isEnable() ? getDefinition().getAutoIO() : null;
    }

    @Nonnull
    public BlockCapabilityCache<IFluidHandler, @Nullable Direction> getNearbyCache(ServerLevel serverLevel,
                                                                                  BlockPos pos,
                                                                                  @Nonnull Direction side) {
        return nearbyCache.computeIfAbsent(pos, blockPos -> new EnumMap<>(Direction.class))
                .computeIfAbsent(side, direction -> BlockCapabilityCache.create(Capabilities.FluidHandler.BLOCK,
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

                Predicate<FluidStack> filter = getDefinition().getFluidFilterSettings().isEnable() ?
                        getDefinition().getFluidFilterSettings() : Predicates.alwaysTrue();

                var storage = new FluidTransferList(storages);
                var maxAmount = Integer.MAX_VALUE;

                for (int srcIndex = 0; srcIndex < source.getTanks(); srcIndex++) {
                    var currentFluid = source.getFluidInTank(srcIndex);
                    if (currentFluid.isEmpty() || !filter.test(currentFluid)) {
                        continue;
                    }

                    var toDrain = currentFluid.copy();
                    toDrain.setAmount(maxAmount);

                    var filled = storage.fill(source.drain(toDrain, IFluidHandler.FluidAction.SIMULATE), IFluidHandler.FluidAction.SIMULATE);
                    if (filled > 0) {
                        maxAmount -= filled;
                        toDrain = currentFluid.copy();
                        toDrain.setAmount(filled);
                        storage.fill(source.drain(toDrain, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                    }
                    if (maxAmount <= 0) return;
                }
            }
            if (io.support(IO.OUT) && !isEmpty()){
                var target = getNearbyCache(serverLevel, port, side).getCapability();
                if (target == null) return;

                var source = new FluidTransferList(storages);
                int maxAmount = Integer.MAX_VALUE;

                for (int srcIndex = 0; srcIndex < source.getTanks(); srcIndex++) {
                    var currentFluid = source.getFluidInTank(srcIndex);
                    if (currentFluid.isEmpty()) {
                        continue;
                    }

                    var toDrain = currentFluid.copy();
                    toDrain.setAmount(maxAmount);

                    var filled = target.fill(source.drain(toDrain, IFluidHandler.FluidAction.SIMULATE), IFluidHandler.FluidAction.SIMULATE);
                    if (filled > 0) {
                        maxAmount -= filled;
                        toDrain = currentFluid.copy();
                        toDrain.setAmount(filled);
                        target.fill(source.drain(toDrain, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                    }
                    if (maxAmount <= 0) return;
                }
            }
        }
    }

    public class FluidRecipeHandler extends RecipeHandlerTrait<SizedFluidIngredient> {
        protected FluidRecipeHandler() {
            super(FluidTankCapabilityTrait.this, FluidRecipeCapability.CAP);
        }

        @Override
        public List<SizedFluidIngredient> handleRecipeInner(IO io, MBDRecipe recipe, List<SizedFluidIngredient> left, @Nullable String slotName, boolean simulate) {
            if (!compatibleWith(io)) return left;
            var containers = simulate ? Arrays.stream(storages).map(FluidStorage::copy).toArray(FluidStorage[]::new) : storages;
            var result = new ArrayList<SizedFluidIngredient>();
            var iterator = left.iterator();
            if (io == IO.IN) {
                while (iterator.hasNext()) {
                    var sizedIngredient = iterator.next();
                    var need = sizedIngredient.amount();
                    for (FluidStorage container : containers) {
                        var fluidStack = container.getFluid();
                        if (sizedIngredient.ingredient().test(fluidStack)) {
                            var extracted = container.drain(need, IFluidHandler.FluidAction.EXECUTE);
                            need -= extracted.getAmount();
                            if (need <= 0) {
                                break;
                            }
                        }
                    }
                    if (need > 0) {
                        if (need == sizedIngredient.amount()) {
                            result.add(sizedIngredient);
                        } else {
                            result.add(new SizedFluidIngredient(sizedIngredient.ingredient(), need));
                        }
                    }
                }
            } else if (io == IO.OUT) {
                while (iterator.hasNext()) {
                    var sizedIngredient = iterator.next();
                    var fluids = sizedIngredient.getFluids();
                    if (fluids.length == 0) {
                        continue;
                    }
                    if (fluids.length == 1) {
                        var output = fluids[0];
                        var leftCount = sizedIngredient.amount();
                        if (leftCount > 0) {
                            for (FluidStorage container : containers) {
                                var filled = container.fill(output.copyWithAmount(leftCount), IFluidHandler.FluidAction.EXECUTE);
                                leftCount -= filled;
                                if (leftCount == 0) break;
                            }
                        }
                        if (leftCount > 0) {
                            result.add(leftCount == sizedIngredient.amount() ?
                                    sizedIngredient :
                                    new SizedFluidIngredient(sizedIngredient.ingredient(), leftCount));
                        }
                    } else { // random output
                        var shuffledItems = Arrays.asList(Arrays.copyOf(fluids, fluids.length));
                        random.setSeed(getMachine().getOffsetTimer());
                        Collections.shuffle(shuffledItems, random);

                        var probe = Arrays.stream(containers).map(FluidStorage::copy).toArray(FluidStorage[]::new);

                        // find index
                        var index = -1;
                        for (int i = 0; i < shuffledItems.size(); i++) {
                            var output = shuffledItems.get(i).copy();
                            var leftCount = sizedIngredient.amount();
                            if (leftCount > 0) {
                                for (FluidStorage fluidStorage : probe) {
                                    var filled = fluidStorage.fill(output.copyWithAmount(leftCount), IFluidHandler.FluidAction.SIMULATE);
                                    leftCount -= filled;
                                    if (leftCount == 0) break;
                                }
                            }
                            if (leftCount == 0) {
                                index = i;
                                break;
                            }
                        }
                        if (index != -1) {
                            if (!simulate) {
                                var output = shuffledItems.get(index);
                                var leftCount = sizedIngredient.amount();
                                for (FluidStorage container : containers) {
                                    var filled = container.fill(output.copyWithAmount(leftCount), IFluidHandler.FluidAction.EXECUTE);
                                    leftCount -= filled;
                                    if (leftCount == 0) break;
                                }
                            }
                        } else {
                            result.add(sizedIngredient);
                        }
                    }
                }
            }
            return result.isEmpty() ? null : result;
        }
    }
}
