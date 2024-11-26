package com.lowdragmc.mbd2.common.trait.fluid;

import com.lowdragmc.lowdraglib.misc.FluidStorage;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.ingredient.FluidIngredient;
import com.lowdragmc.mbd2.common.capability.recipe.FluidRecipeCapability;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ICapabilityProviderTrait;
import com.lowdragmc.mbd2.common.trait.RecipeHandlerTrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class FluidTankCapabilityTrait extends SimpleCapabilityTrait {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(FluidTankCapabilityTrait.class);

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

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
    public List<IRecipeHandlerTrait<?>> getRecipeHandlerTraits() {
        return List.of(recipeHandler);
    }

    @Override
    public List<ICapabilityProviderTrait<?>> getCapabilityProviderTraits() {
        return List.of(fluidHandlerCap);
    }

    @Override
    public void serverTick() {
        var timer = getMachine().getOffsetTimer();
        var partSettings = this.getMachine().getDefinition().partSettings();
        if (partSettings == null) return;
        Direction facing = getMachine().getFrontFacing().orElse(Direction.NORTH);

        for (var extraTraitAction : partSettings.extraTraitActions()) {
            if (timer % extraTraitAction.interval() != 0) continue;
            if (extraTraitAction.getFilter().matcher(getDefinition().getName()).find()) {
                for (Direction direction : Direction.values()) {
                    var io = extraTraitAction.capabilityIO().getIO(facing, direction);
                    var partCapIO = getDefinition().getCapabilityIO().getIO(facing, direction);
                    if (io.support(partCapIO) && !io.doAny()) continue;
                    BlockPos targetPos = getMachine().getPos().relative(direction);
                    var level = getMachine().getLevel();
                    if (!level.isLoaded(targetPos)) continue;
                    var blockEntity = level.getBlockEntity(targetPos);
                    if (blockEntity == null) continue;
                    blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER).ifPresent(handler -> {
                        if (io.doInput()) {
                            for (int i = 0; i < handler.getTanks(); i++) {
                                var fluidInTank = handler.getFluidInTank(i);
                                if (fluidInTank.isEmpty()) continue;
                                IFluidHandler inputHandler = fluidHandlerCap.getCapContent(IO.IN);
                                for (int j = 0; j < inputHandler.getTanks(); j++) {
                                    var filled = inputHandler.fill(fluidInTank, FluidAction.SIMULATE);
                                    if (filled > 0) {
                                        var drain = handler.drain(fluidInTank, FluidAction.EXECUTE);
                                        inputHandler.fill(drain, FluidAction.EXECUTE);
                                        break;
                                    }
                                }
                            }
                        }
                        if (io.doOutput()) {
                            IFluidHandler outputHandler = fluidHandlerCap.getCapContent(IO.OUT);
                            for (int i = 0; i < outputHandler.getTanks(); i++) {
                                var fluidInTank = outputHandler.getFluidInTank(i);
                                for (int j = 0; j < handler.getTanks(); j++) {
                                    var filled = handler.fill(fluidInTank, FluidAction.SIMULATE);
                                    if (filled > 0) {
                                        var toDrain = fluidInTank.copy();
                                        toDrain.setAmount(filled);
                                        var drain = outputHandler.drain(toDrain, FluidAction.EXECUTE);
                                        handler.fill(drain, FluidAction.EXECUTE);
                                        break;
                                    }
                                }
                            }
                        }
                    });
                }
            }
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
