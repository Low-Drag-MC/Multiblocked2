package com.lowdragmc.mbd2.integration.mekanism.trait.chemical;

import com.lowdragmc.lowdraglib.syncdata.IContentChangeAware;
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;
import lombok.Getter;
import lombok.Setter;
import mekanism.api.NBTConstants;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.attribute.ChemicalAttributeValidator;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.api.chemical.pigment.PigmentStack;
import mekanism.api.chemical.slurry.SlurryStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Predicate;

public abstract class ChemicalStorage<CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>> extends BasicChemicalTank<CHEMICAL, STACK> implements IContentChangeAware, ITagSerializable<CompoundTag> {
    @Getter
    @Setter
    private Runnable onContentsChanged = () -> {};
    public final Predicate<@NotNull CHEMICAL> validator;
    @Nullable
    public final ChemicalAttributeValidator attributeValidator;


    protected ChemicalStorage(long capacity,
                              Predicate<@NotNull CHEMICAL> validator,
                              @Nullable ChemicalAttributeValidator attributeValidator) {
        super(capacity, (a, b) -> true, (a, b) -> true, validator, attributeValidator, null);
        this.validator = validator;
        this.attributeValidator = attributeValidator;
    }

    public abstract STACK readFromNBT(CompoundTag nbt);

    public abstract ChemicalStorage<CHEMICAL, STACK> copy();

    public void onContentsChanged() {
        if (onContentsChanged != null) {
            onContentsChanged.run();
        }
    }

    @Override
    @Nonnull
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        if (!isEmpty()) {
            nbt.put(NBTConstants.STORED, stored.write(new CompoundTag()));
        }
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains(NBTConstants.STORED, Tag.TAG_COMPOUND)) {
            setStackUnchecked(readFromNBT(nbt.getCompound(NBTConstants.STORED)));
        }
    }

    @MethodsReturnNonnullByDefault
    @ParametersAreNonnullByDefault
    public static class Gas extends ChemicalStorage<mekanism.api.chemical.gas.Gas, GasStack> {
        public Gas(long capacity,
                   Predicate<mekanism.api.chemical.gas.@NotNull Gas> validator,
                   @Nullable ChemicalAttributeValidator attributeValidator) {
            super(capacity, validator, attributeValidator);
        }

        @Override
        public Gas copy() {
            var copied= new Gas(getCapacity(), validator, attributeValidator);
            copied.setStack(getStack());
            return copied;
        }

        @Override
        public GasStack readFromNBT(CompoundTag nbt) {
            return GasStack.readFromNBT(nbt);
        }

        @Override
        public GasStack createStack(GasStack stored, long size) {
            return new GasStack(stored, size);
        }

        @NotNull
        @Override
        public GasStack getEmptyStack() {
            return GasStack.EMPTY;
        }
    }

    @MethodsReturnNonnullByDefault
    @ParametersAreNonnullByDefault
    public static class Infuse extends ChemicalStorage<InfuseType, InfusionStack> {
        public Infuse(long capacity,
                      Predicate<InfuseType> validator,
                      @Nullable ChemicalAttributeValidator attributeValidator) {
            super(capacity, validator, attributeValidator);
        }

        @Override
        public Infuse copy() {
            var copied= new Infuse(getCapacity(), validator, attributeValidator);
            copied.setStack(getStack());
            return copied;
        }

        @Override
        public InfusionStack readFromNBT(CompoundTag nbt) {
            return InfusionStack.readFromNBT(nbt);
        }

        @Override
        public InfusionStack createStack(InfusionStack stored, long size) {
            return new InfusionStack(stored, size);
        }

        @NotNull
        @Override
        public InfusionStack getEmptyStack() {
            return InfusionStack.EMPTY;
        }
    }

    @MethodsReturnNonnullByDefault
    @ParametersAreNonnullByDefault
    public static class Pigment extends ChemicalStorage<mekanism.api.chemical.pigment.Pigment, PigmentStack> {
        public Pigment(long capacity,
                       Predicate<mekanism.api.chemical.pigment.Pigment> validator,
                       @Nullable ChemicalAttributeValidator attributeValidator) {
            super(capacity, validator, attributeValidator);
        }

        @Override
        public Pigment copy() {
            var copied= new Pigment(getCapacity(), validator, attributeValidator);
            copied.setStack(getStack());
            return copied;
        }

        @Override
        public PigmentStack readFromNBT(CompoundTag nbt) {
            return PigmentStack.readFromNBT(nbt);
        }

        @Override
        public PigmentStack createStack(PigmentStack stored, long size) {
            return new PigmentStack(stored, size);
        }

        @NotNull
        @Override
        public PigmentStack getEmptyStack() {
            return PigmentStack.EMPTY;
        }
    }

    @MethodsReturnNonnullByDefault
    @ParametersAreNonnullByDefault
    public static class Slurry extends ChemicalStorage<mekanism.api.chemical.slurry.Slurry, SlurryStack> {
        public Slurry(long capacity,
                      Predicate<mekanism.api.chemical.slurry.Slurry> validator,
                      @Nullable ChemicalAttributeValidator attributeValidator) {
            super(capacity, validator, attributeValidator);
        }

        @Override
        public Slurry copy() {
            var copied= new Slurry(getCapacity(), validator, attributeValidator);
            copied.setStack(getStack());
            return copied;
        }

        @Override
        public SlurryStack readFromNBT(CompoundTag nbt) {
            return SlurryStack.readFromNBT(nbt);
        }

        @Override
        public SlurryStack createStack(SlurryStack stored, long size) {
            return new SlurryStack(stored, size);
        }

        @NotNull
        @Override
        public SlurryStack getEmptyStack() {
            return SlurryStack.EMPTY;
        }
    }
}
