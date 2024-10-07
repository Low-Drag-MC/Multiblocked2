package com.lowdragmc.mbd2.integration.mekanism.trait.chemical;

import mekanism.api.Action;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.api.chemical.infuse.IInfusionHandler;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.api.chemical.pigment.IPigmentHandler;
import mekanism.api.chemical.pigment.PigmentStack;
import mekanism.api.chemical.slurry.ISlurryHandler;
import mekanism.api.chemical.slurry.SlurryStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class ChemicalHandlerList<CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>> implements IChemicalHandler<CHEMICAL, STACK> {

    public final STACK emptyStack;
    public final IChemicalHandler<CHEMICAL, STACK>[] handlers;

    public ChemicalHandlerList(STACK emptyStack, IChemicalHandler<CHEMICAL, STACK>[] handlers) {
        this.emptyStack = emptyStack;
        this.handlers = handlers;
    }

    @Override
    public int getTanks() {
        return Arrays.stream(handlers).mapToInt(IChemicalHandler::getTanks).sum();
    }

    @Override
    public STACK getChemicalInTank(int tank) {
        int index = 0;
        for (var handler : handlers) {
            if (tank - index < handler.getTanks()) {
                return handler.getChemicalInTank(tank - index);
            }
            index += handler.getTanks();
        }
        return emptyStack;
    }

    @Override
    public void setChemicalInTank(int tank, STACK stack) {
        int index = 0;
        for (var handler : handlers) {
            if (tank - index < handler.getTanks()) {
                handler.setChemicalInTank(tank - index, stack);
                return;
            }
            index += handler.getTanks();
        }
    }

    @Override
    public long getTankCapacity(int tank) {
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
    public boolean isValid(int tank, STACK stack) {
        int index = 0;
        for (var handler : handlers) {
            if (tank - index < handler.getTanks()) {
                return handler.isValid(tank - index, stack);
            }
            index += handler.getTanks();
        }
        return false;
    }

    @Override
    public STACK insertChemical(int tank, STACK stack, Action action) {
        int index = 0;
        for (var handler : handlers) {
            if (tank - index < handler.getTanks()) {
                return handler.insertChemical(tank - index, stack, action);
            }
            index += handler.getTanks();
        }
        return stack;
    }

    @Override
    public STACK extractChemical(int tank, long amount, Action action) {
        int index = 0;
        for (var handler : handlers) {
            if (tank - index < handler.getTanks()) {
                return handler.extractChemical(tank - index, amount, action);
            }
            index += handler.getTanks();
        }
        return emptyStack;
    }

    @Override
    public @NotNull STACK getEmptyStack() {
        return emptyStack;
    }

    public static class Gas extends ChemicalHandlerList<mekanism.api.chemical.gas.Gas, GasStack> implements IGasHandler {
        public Gas(IChemicalHandler<mekanism.api.chemical.gas.Gas, GasStack>[] handlers) {
            super(GasStack.EMPTY, handlers);
        }
    }

    public static class Infuse extends ChemicalHandlerList<mekanism.api.chemical.infuse.InfuseType, InfusionStack> implements IInfusionHandler {
        public Infuse(IChemicalHandler<mekanism.api.chemical.infuse.InfuseType, InfusionStack>[] handlers) {
            super(InfusionStack.EMPTY, handlers);
        }
    }

    public static class Pigment extends ChemicalHandlerList<mekanism.api.chemical.pigment.Pigment, PigmentStack> implements IPigmentHandler {
        public Pigment(IChemicalHandler<mekanism.api.chemical.pigment.Pigment, PigmentStack>[] handlers) {
            super(PigmentStack.EMPTY, handlers);
        }
    }

    public static class Slurry extends ChemicalHandlerList<mekanism.api.chemical.slurry.Slurry, SlurryStack> implements ISlurryHandler {
        public Slurry(IChemicalHandler<mekanism.api.chemical.slurry.Slurry, SlurryStack>[] handlers) {
            super(SlurryStack.EMPTY, handlers);
        }
    }

}
