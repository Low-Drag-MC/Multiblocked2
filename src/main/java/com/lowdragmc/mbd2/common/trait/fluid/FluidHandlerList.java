package com.lowdragmc.mbd2.common.trait.fluid;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public record FluidHandlerList(IFluidHandler[] handlers) implements IFluidHandler {
    @Override
    public int getTanks() {
        return Arrays.stream(handlers).mapToInt(IFluidHandler::getTanks).sum();
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        int index = 0;
        for (var handler : handlers) {
            if (tank - index < handler.getTanks()) {
                return handler.getFluidInTank(tank - index);
            }
            index += handler.getTanks();
        }
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        int index = 0;
        for (var handler : handlers) {
            if (tank - index < handler.getTanks()) {
                return handler.getTankCapacity(tank - index);
            }
            index += handler.getTanks();
        }
        return 0;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        int index = 0;
        for (var handler : handlers) {
            if (tank - index < handler.getTanks()) {
                return handler.isFluidValid(tank - index, stack);
            }
            index += handler.getTanks();
        }
        return false;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        var copied = resource.copy();
        for (var handler : handlers) {
            var candidate = copied.copy();
            copied.shrink(handler.fill(candidate, action));
            if (copied.isEmpty()) break;
        }
        return resource.getAmount() - copied.getAmount();
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        var copied = resource.copy();
        for (var handler : handlers) {
            var candidate = copied.copy();
            copied.shrink(handler.drain(candidate, action).getAmount());
            if (copied.isEmpty()) break;
        }
        copied.setAmount(resource.getAmount() - copied.getAmount());
        return copied;
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain == 0) {
            return FluidStack.EMPTY;
        }
        FluidStack totalDrained = null;
        for (var storage : handlers) {
            if (totalDrained == null || totalDrained.isEmpty()) {
                totalDrained = storage.drain(maxDrain, action);
                if (totalDrained.isEmpty()) {
                    totalDrained = null;
                } else {
                    maxDrain -= totalDrained.getAmount();
                }
            } else {
                FluidStack copy = totalDrained.copy();
                copy.setAmount(maxDrain);
                FluidStack drain = storage.drain(copy, action);
                totalDrained.grow(drain.getAmount());
                maxDrain -= drain.getAmount();
            }
            if (maxDrain <= 0) break;
        }
        return totalDrained == null ? FluidStack.EMPTY : totalDrained;
    }
}
