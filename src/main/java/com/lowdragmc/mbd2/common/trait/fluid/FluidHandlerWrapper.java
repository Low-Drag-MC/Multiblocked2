package com.lowdragmc.mbd2.common.trait.fluid;

import com.lowdragmc.lowdraglib.misc.FluidStorage;
import com.lowdragmc.lowdraglib.side.fluid.forge.FluidHelperImpl;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

public class FluidHandlerWrapper implements IFluidHandler {

    private final FluidStorage[] storages;
    private final IO io;
    private final boolean allowSameFluids;

    public FluidHandlerWrapper(FluidStorage[] storages, IO io, boolean allowSameFluids) {
        this.storages = storages;
        this.io = io;
        this.allowSameFluids = allowSameFluids;
    }

    private boolean canCapInput() {
        return io == IO.IN || io == IO.BOTH;
    }

    private boolean canCapOutput() {
        return io == IO.OUT || io == IO.BOTH;
    }

    @Override
    public int getTanks() {
        return storages.length;
    }

    @NotNull
    @Override
    public FluidStack getFluidInTank(int tank) {
        return FluidHelperImpl.toFluidStack(storages[tank].getFluid());
    }

    public void setFluidInTank(int tank, @NotNull FluidStack fluidStack) {
        storages[tank].setFluid(FluidHelperImpl.toFluidStack(fluidStack));
    }

    @Override
    public int getTankCapacity(int tank) {
        return (int) storages[tank].getCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return storages[tank].isFluidValid(FluidHelperImpl.toFluidStack(stack));
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (canCapInput()) {
            return (int) fillInternal(FluidHelperImpl.toFluidStack(resource), action.simulate());
        }
        return 0;
    }

    public long fillInternal(com.lowdragmc.lowdraglib.side.fluid.FluidStack resource, boolean simulate) {
        if (resource.isEmpty()) return 0;
        var copied = resource.copy();
        FluidStorage existingStorage = null;
        if (!allowSameFluids) {
            for (var storage : storages) {
                if (!storage.getFluid().isEmpty() && storage.getFluid().isFluidEqual(resource)) {
                    existingStorage = storage;
                    break;
                }
            }
        }
        if (existingStorage == null) {
            for (var storage : storages) {
                var filled = storage.fill(copied.copy(), simulate);
                if (filled > 0) {
                    copied.shrink(filled);
                    if (!allowSameFluids) {
                        break;
                    }
                }
                if (copied.isEmpty()) break;
            }
        } else {
            copied.shrink(existingStorage.fill(copied.copy(), simulate));
        }
        return resource.getAmount() - copied.getAmount();
    }



    @NotNull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (canCapOutput()) {
            return FluidHelperImpl.toFluidStack(drainInternal(FluidHelperImpl.toFluidStack(resource), action.simulate()));
        }
        return FluidStack.EMPTY;
    }

    public com.lowdragmc.lowdraglib.side.fluid.FluidStack drainInternal(com.lowdragmc.lowdraglib.side.fluid.FluidStack resource, boolean simulate) {
        if (!resource.isEmpty()) {
            var copied = resource.copy();
            for (var transfer : storages) {
                var candidate = copied.copy();
                copied.shrink(transfer.drain(candidate, simulate).getAmount());
                if (copied.isEmpty()) break;
            }
            copied.setAmount(resource.getAmount() - copied.getAmount());
            return copied;
        }
        return com.lowdragmc.lowdraglib.side.fluid.FluidStack.empty();
    }

    @NotNull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (canCapOutput()) {
            return FluidHelperImpl.toFluidStack(drainInternal(maxDrain, action.simulate()));
        }
        return FluidStack.EMPTY;
    }

    public com.lowdragmc.lowdraglib.side.fluid.FluidStack drainInternal(long maxDrain, boolean simulate) {
        if (maxDrain == 0) {
            return com.lowdragmc.lowdraglib.side.fluid.FluidStack.empty();
        }
        com.lowdragmc.lowdraglib.side.fluid.FluidStack totalDrained = null;
        for (var storage : storages) {
            if (totalDrained == null || totalDrained.isEmpty()) {
                totalDrained = storage.drain(maxDrain, simulate);
                if (totalDrained.isEmpty()) {
                    totalDrained = null;
                } else {
                    maxDrain -= totalDrained.getAmount();
                }
            } else {
                com.lowdragmc.lowdraglib.side.fluid.FluidStack copy = totalDrained.copy();
                copy.setAmount(maxDrain);
                com.lowdragmc.lowdraglib.side.fluid.FluidStack drain = storage.drain(copy, simulate);
                totalDrained.grow(drain.getAmount());
                maxDrain -= drain.getAmount();
            }
            if (maxDrain <= 0) break;
        }
        return totalDrained == null ? com.lowdragmc.lowdraglib.side.fluid.FluidStack.empty() : totalDrained;
    }
}
