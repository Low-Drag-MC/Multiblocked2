package com.lowdragmc.mbd2.integration.mekanism.trait.chemical;

import com.lowdragmc.mbd2.api.capability.recipe.IO;
import mekanism.api.Action;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.api.chemical.infuse.IInfusionHandler;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.api.chemical.pigment.IPigmentHandler;
import mekanism.api.chemical.pigment.PigmentStack;
import mekanism.api.chemical.slurry.ISlurryHandler;
import mekanism.api.chemical.slurry.SlurryStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class ChemicalStorageWrapper<CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>> implements IChemicalHandler<CHEMICAL, STACK> {
    private final STACK emptyStack;
    private final ChemicalStorage<CHEMICAL, STACK>[] storages;
    private final IO io;

    public ChemicalStorageWrapper(STACK emptyStack, ChemicalStorage<CHEMICAL, STACK>[] storages, IO io) {
        this.emptyStack = emptyStack;
        this.storages = storages;
        this.io = io;
    }

    @Override
    public int getTanks() {
        return storages.length;
    }

    @Override
    public STACK getChemicalInTank(int tank) {
        return storages[tank].getStack();
    }

    @Override
    public void setChemicalInTank(int tank, STACK stack) {
        storages[tank].setStack(stack);
    }

    @Override
    public long getTankCapacity(int tank) {
        return storages[tank].getCapacity();
    }

    @Override
    public boolean isValid(int tank, STACK stack) {
        return storages[tank].isValid(stack);
    }

    @Override
    public STACK insertChemical(int tank, STACK stack, Action action) {
        if (io == IO.IN || io == IO.BOTH) {
            return storages[tank].insertChemical(stack, action);
        }
        return stack;
    }

    @Override
    public STACK extractChemical(int tank, long amount, Action action) {
        if (io == IO.OUT || io == IO.BOTH) {
            return storages[tank].extractChemical(amount, action);
        }
        return emptyStack;
    }

    @Override
    public @NotNull STACK getEmptyStack() {
        return emptyStack;
    }

    public static class Gas extends ChemicalStorageWrapper<mekanism.api.chemical.gas.Gas, GasStack> implements IGasHandler {
        public Gas(ChemicalStorage<mekanism.api.chemical.gas.Gas, GasStack>[] storages, IO io) {
            super(GasStack.EMPTY, storages, io);
        }
    }

    public static class Infuse extends ChemicalStorageWrapper<InfuseType, InfusionStack> implements IInfusionHandler {
        public Infuse(ChemicalStorage<InfuseType, InfusionStack>[] storages, IO io) {
            super(InfusionStack.EMPTY, storages, io);
        }
    }

    public static class Pigment extends ChemicalStorageWrapper<mekanism.api.chemical.pigment.Pigment, mekanism.api.chemical.pigment.PigmentStack> implements IPigmentHandler {
        public Pigment(ChemicalStorage<mekanism.api.chemical.pigment.Pigment, mekanism.api.chemical.pigment.PigmentStack>[] storages, IO io) {
            super(PigmentStack.EMPTY, storages, io);
        }
    }

    public static class Slurry extends ChemicalStorageWrapper<mekanism.api.chemical.slurry.Slurry, mekanism.api.chemical.slurry.SlurryStack> implements ISlurryHandler {
        public Slurry(ChemicalStorage<mekanism.api.chemical.slurry.Slurry, mekanism.api.chemical.slurry.SlurryStack>[] storages, IO io) {
            super(SlurryStack.EMPTY, storages, io);
        }
    }

}
