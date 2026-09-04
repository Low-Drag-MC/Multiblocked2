package com.lowdragmc.mbd2.common.trait.fluid;

import com.google.common.base.Predicates;
import com.lowdragmc.lowdraglib2.misc.FluidStorage;
import com.lowdragmc.lowdraglib2.misc.FluidTransferList;
import com.lowdragmc.lowdraglib2.syncdata.annotation.ConditionalSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.capability.recipe.FluidRecipeCapability;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.runtime.RuntimeAutoIO;
import com.lowdragmc.mbd2.common.runtime.RuntimeAutoWorldIO;
import com.lowdragmc.mbd2.common.runtime.RuntimeValue;
import com.lowdragmc.mbd2.common.trait.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Predicate;

public class FluidTankCapabilityTrait extends SimpleCapabilityTrait<IFluidHandler, @Nullable Direction>
        implements IAutoIOTrait, IAutoWorldIOTrait {
    @Persisted
    @DescSynced
    @ConditionalSynced(methodName = "shouldSyncStorage")
    public final FluidStorage[] storages;
    private final FluidRecipeHandler recipeHandler = new FluidRecipeHandler();

    // per-machine overrides of the values authored on the definition
    public final RuntimeAutoIO autoIO =
            new RuntimeAutoIO(runtimeValues, "auto_io", () -> getDefinition().getAutoIO());
    public final RuntimeAutoWorldIO autoWorldInput =
            new RuntimeAutoWorldIO(runtimeValues, "auto_world_input", () -> getDefinition().getAutoInput());
    public final RuntimeAutoWorldIO autoWorldOutput =
            new RuntimeAutoWorldIO(runtimeValues, "auto_world_output", () -> getDefinition().getAutoOutput());
    public final RuntimeValue<Boolean> allowSameFluids =
            runtimeValues.ofBool("allow_same_fluids", () -> getDefinition().isAllowSameFluids())
            .onChanged(() -> {
                // the value is baked into the wrapper handed out at capability-resolution time, and a
                // neighbour's BlockCapabilityCache keeps that wrapper until the position is invalidated
                getMachine().invalidateCapabilities();
                getMachine().notifyBlockUpdate();
            });
    /** Whether the definition's fluid filter applies to this machine. @see ItemSlotCapabilityTrait#filterEnabled */
    public final RuntimeValue<Boolean> filterEnabled =
            runtimeValues.ofBool("filter.enable", () -> getDefinition().getFluidFilterSettings().isEnable());
    /**
     * Per-tank capacity, for a machine that should hold more than its definition says.
     * <p>
     * {@link FluidTank#fill} reads the {@code capacity} field rather than {@code getCapacity()}, so this
     * has to resize the storages rather than be read live — see {@link #applyCapacity}.
     */
    public final RuntimeValue<Integer> capacity =
            runtimeValues.ofInt("capacity", () -> getDefinition().getCapacity())
            .onChanged(() -> {
                if (applyCapacity()) {
                    onContentsChanged();
                    getMachine().invalidateCapabilities();
                    getMachine().notifyBlockUpdate();
                }
            });

    // runtime
    private final Random random = new Random();
    private Boolean isEmpty;
    private final Map<BlockPos, EnumMap<Direction, BlockCapabilityCache<IFluidHandler, @Nullable Direction>>> nearbyCache = new HashMap<>();

    public FluidTankCapabilityTrait(MBDMachine machine, FluidTankCapabilityTraitDefinition definition) {
        super(machine, definition);
        storages = createStorages();
    }

    public boolean shouldSyncStorage(FluidStorage[] value) {
        return getDefinition().getFancyRendererSettings().isEnable();
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
            storages[i] = new FluidStorage(getDefinition().getCapacity()) {
                /**
                 * {@link FluidStorage} saves its capacity and restores it verbatim, which was already
                 * stale whenever the definition was edited after the machine was placed — the tank kept
                 * whatever size it had when it was last saved. Now that capacity is a runtime value there
                 * is a single authority for it, so re-derive rather than trust the tag.
                 */
                @Override
                public void deserializeNBT(@NotNull HolderLookup.Provider provider, CompoundTag nbt) {
                    super.deserializeNBT(provider, nbt);
                    applyCapacity(this);
                }
            };
            storages[i].setOnContentsChanged(this::onContentsChanged);
            // live-read, and matches() rather than test(), both for the reasons spelled out in
            // ItemSlotCapabilityTrait#createStorage
            storages[i].setValidator(stack -> !filterEnabled.get()
                    || getDefinition().getFluidFilterSettings().matches(stack));
        }
        return storages;
    }

    /**
     * Resize every tank to the effective capacity, spilling anything that no longer fits.
     *
     * <p>Over-full is not a state to leave a tank in: {@code FluidTank.fill} computes its headroom as
     * {@code capacity - amount}, so a negative headroom silently rejects every fill, and every
     * percentage-based reader draws past full.</p>
     *
     * @return whether anything actually changed
     */
    private boolean applyCapacity() {
        var changed = false;
        for (var storage : storages) {
            changed |= applyCapacity(storage);
        }
        return changed;
    }

    private boolean applyCapacity(FluidStorage storage) {
        var target = Math.max(0, capacity.get());
        var fluid = storage.getFluid();
        var overflowing = fluid.getAmount() > target;
        if (storage.getCapacity() == target && !overflowing) return false;
        storage.setCapacity(target);
        if (overflowing) {
            storage.setFluidInTank(target == 0 ? FluidStack.EMPTY : fluid.copyWithAmount(target), false);
        }
        return true;
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
        var autoInput = autoWorldInput;
        var autoOutput = autoWorldOutput;
        var level = getMachine().getLevel();
        if (autoInput.enable.get() && timer % autoInput.intervalTicks() == 0) {
            var leftBlocks = autoInput.speed.get();
            var range = autoInput.getRotatedRange(getMachine().getFrontFacing().orElse(Direction.NORTH)).move(getMachine().getPos());
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
        if (autoOutput.enable.get() && timer % autoOutput.intervalTicks() == 0) {
            var leftBlocks = autoOutput.speed.get();
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
        return new FluidHandlerWrapper(storages, capbilityIO, allowSameFluids.get());
    }

    @Override
    public RuntimeAutoIO getRuntimeAutoIO() {
        return autoIO;
    }

    @Override
    public RuntimeAutoWorldIO getRuntimeAutoWorldInput() {
        return autoWorldInput;
    }

    @Override
    public RuntimeAutoWorldIO getRuntimeAutoWorldOutput() {
        return autoWorldOutput;
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
            var nearby = getNearbyCache(serverLevel, port.relative(side), side.getOpposite()).getCapability();
            if (nearby == null) return;
            if (io.support(IO.IN)) {
                var source = nearby;

                Predicate<FluidStack> filter = filterEnabled.get()
                        ? getDefinition().getFluidFilterSettings()::matches : Predicates.alwaysTrue();

                // fill through the wrapper, not the raw storages — it is what enforces
                // "allow same fluids", so pulling in bypasses the setting otherwise.
                var storage = new FluidHandlerWrapper(storages, IO.IN, allowSameFluids.get());
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
                var target = nearby;

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
