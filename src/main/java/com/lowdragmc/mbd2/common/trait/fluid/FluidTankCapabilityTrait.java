package com.lowdragmc.mbd2.common.trait.fluid;

import com.lowdragmc.lowdraglib.misc.FluidStorage;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.ingredient.FluidIngredient;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import lombok.Setter;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class FluidTankCapabilityTrait extends SimpleCapabilityTrait<IFluidHandler, FluidIngredient> {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(FluidTankCapabilityTrait.class);
    @Override
    public ManagedFieldHolder getFieldHolder() { return MANAGED_FIELD_HOLDER; }

    @Persisted
    @DescSynced
    public final FluidStorage[] storages;
    @Setter
    protected boolean allowSameFluids; // Can different tanks be filled with the same fluid. It should be determined while creating tanks.
    private Boolean isEmpty;

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
        }
        return storages;
    }

    public void onContentsChanged() {
        isEmpty = null;
        notifyListeners();
    }

    @Override
    public List<FluidIngredient> handleRecipeInner(IO io, MBDRecipe recipe, List<FluidIngredient> left, @Nullable String slotName, boolean simulate) {
        if (io != getHandlerIO()) return left;
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
    public IFluidHandler getCapContent(@Nullable Direction side) {
        return new FluidHandlerWrapper(this.storages, getCapabilityIO(side), getDefinition().isAllowSameFluids());
    }

    @Override
    public IFluidHandler mergeContents(List<IFluidHandler> contents) {
        return new FluidHandlerList(contents.toArray(new IFluidHandler[0]));
    }
}
