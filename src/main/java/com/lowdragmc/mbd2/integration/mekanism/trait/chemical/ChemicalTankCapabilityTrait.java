package com.lowdragmc.mbd2.integration.mekanism.trait.chemical;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import lombok.Setter;
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
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public abstract class ChemicalTankCapabilityTrait<CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>, HANDLER extends IChemicalHandler<CHEMICAL, STACK>> extends SimpleCapabilityTrait<HANDLER, STACK> {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(ChemicalTankCapabilityTrait.class);
    @Override
    public ManagedFieldHolder getFieldHolder() { return MANAGED_FIELD_HOLDER; }

    @Persisted
    @DescSynced
    public final ChemicalStorage<CHEMICAL, STACK>[] storages;
    @Setter
    protected boolean allowSameFluids; // Can different tanks be filled with the same fluid. It should be determined while creating tanks.
    private Boolean isEmpty;

    public ChemicalTankCapabilityTrait(MBDMachine machine, ChemicalTankCapabilityTraitDefinition<CHEMICAL, STACK, HANDLER> definition) {
        super(machine, definition);
        storages = createStorages();
    }

    @Override
    public ChemicalTankCapabilityTraitDefinition<CHEMICAL, STACK, HANDLER> getDefinition() {
        return (ChemicalTankCapabilityTraitDefinition<CHEMICAL, STACK, HANDLER>) super.getDefinition();
    }

    @Override
    public void onLoadingTraitInPreview() {
        if (storages.length > 0) {
            var stack = getDefinition().recipeCapability.createDefaultContent();
            stack.setAmount(getDefinition().getCapacity() / 2);
            storages[0].setStack(stack);
        }
    }


    protected ChemicalStorage<CHEMICAL, STACK>[] createStorages() {
        var storages = new ChemicalStorage[getDefinition().getTankSize()];
        for (int i = 0; i < storages.length; i++) {
            storages[i] = createStorage();
            storages[i].setOnContentsChanged(this::onContentsChanged);
        }
        return storages;
    }

    public void onContentsChanged() {
        isEmpty = null;
        notifyListeners();
    }

    @Override
    public List<STACK> handleRecipeInner(IO io, MBDRecipe recipe, List<STACK> left, @Nullable String slotName, boolean simulate) {
        if (io != getHandlerIO()) return left;
        var capabilities = simulate ? Arrays.stream(storages).map(ChemicalStorage::copy).toArray(ChemicalStorage[]::new) : storages;
        for (var capability : capabilities) {
            Iterator<STACK> iterator = left.iterator();
            if (io == IO.IN) {
                while (iterator.hasNext()) {
                    var chemicalStack = iterator.next();
                    if (chemicalStack.isEmpty()) {
                        iterator.remove();
                        continue;
                    }
                    boolean found = false;
                    STACK foundStack = null;
                    for (int i = 0; i < capability.getTanks(); i++) {
                        var stored = capability.getChemicalInTank(i);
                        if (!chemicalStack.isTypeEqual(stored)) {
                            continue;
                        }
                        found = true;
                        foundStack = (STACK) stored;
                    }
                    if (!found) continue;
                    var copied = foundStack.copy();
                    copied.setAmount(chemicalStack.getAmount());
                    var drained = capability.extractChemical(copied, Action.EXECUTE);

                    chemicalStack.setAmount(chemicalStack.getAmount() - drained.getAmount());
                    if (chemicalStack.getAmount() <= 0) {
                        iterator.remove();
                    }
                }
            } else if (io == IO.OUT) {
                while (iterator.hasNext()) {
                    var chemicalStack = iterator.next();
                    if (chemicalStack.isEmpty()) {
                        iterator.remove();
                        continue;
                    }
                    var remaining = capability.insertChemical(chemicalStack.copy(), Action.EXECUTE);
                    if (!chemicalStack.isEmpty()) {
                        chemicalStack.setAmount(remaining.getAmount());
                    }
                    if (chemicalStack.isEmpty()) {
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
            for (ChemicalStorage<CHEMICAL, STACK> storage : storages) {
                if (!storage.getStack().isEmpty()) {
                    isEmpty = false;
                    break;
                }
            }
        }
        return isEmpty;
    }

    public abstract ChemicalStorage<CHEMICAL,STACK> createStorage();

    public static class Gas extends ChemicalTankCapabilityTrait<mekanism.api.chemical.gas.Gas, GasStack, IGasHandler> {
        public Gas(MBDMachine machine, ChemicalTankCapabilityTraitDefinition<mekanism.api.chemical.gas.Gas, GasStack, IGasHandler> definition) {
            super(machine, definition);
        }

        @Override
        public ChemicalStorage<mekanism.api.chemical.gas.Gas, GasStack> createStorage() {
            return new ChemicalStorage.Gas(getDefinition().getCapacity(), chemical -> getDefinition().getChemicalFilterSettings().isEnable() || getDefinition().getChemicalFilterSettings().test(chemical), null);
        }

        @Override
        public IGasHandler getCapContent(IO capbilityIO) {
            return new ChemicalStorageWrapper.Gas(storages, capbilityIO);
        }

        @Override
        public IGasHandler mergeContents(List<IGasHandler> contents) {
            return new ChemicalHandlerList.Gas(contents.toArray(new IGasHandler[0]));
        }
    }

    public static class Infuse extends ChemicalTankCapabilityTrait<InfuseType, InfusionStack, IInfusionHandler> {
        public Infuse(MBDMachine machine, ChemicalTankCapabilityTraitDefinition<InfuseType, InfusionStack, IInfusionHandler> definition) {
            super(machine, definition);
        }

        @Override
        public ChemicalStorage<InfuseType, InfusionStack> createStorage() {
            return new ChemicalStorage.Infuse(getDefinition().getCapacity(), chemical -> getDefinition().getChemicalFilterSettings().isEnable() || getDefinition().getChemicalFilterSettings().test(chemical), null);
        }

        @Override
        public IInfusionHandler getCapContent(IO capbilityIO) {
            return new ChemicalStorageWrapper.Infuse(storages, capbilityIO);
        }

        @Override
        public IInfusionHandler mergeContents(List<IInfusionHandler> contents) {
            return new ChemicalHandlerList.Infuse(contents.toArray(new IInfusionHandler[0]));
        }
    }

    public static class Pigment extends ChemicalTankCapabilityTrait<mekanism.api.chemical.pigment.Pigment, PigmentStack, IPigmentHandler> {
        public Pigment(MBDMachine machine, ChemicalTankCapabilityTraitDefinition<mekanism.api.chemical.pigment.Pigment, PigmentStack, IPigmentHandler> definition) {
            super(machine, definition);
        }

        @Override
        public ChemicalStorage<mekanism.api.chemical.pigment.Pigment, PigmentStack> createStorage() {
            return new ChemicalStorage.Pigment(getDefinition().getCapacity(), chemical -> getDefinition().getChemicalFilterSettings().isEnable() || getDefinition().getChemicalFilterSettings().test(chemical), null);
        }

        @Override
        public IPigmentHandler getCapContent(IO capbilityIO) {
            return new ChemicalStorageWrapper.Pigment(storages, capbilityIO);
        }

        @Override
        public IPigmentHandler mergeContents(List<IPigmentHandler> contents) {
            return new ChemicalHandlerList.Pigment(contents.toArray(new IPigmentHandler[0]));
        }
    }

    public static class Slurry extends ChemicalTankCapabilityTrait<mekanism.api.chemical.slurry.Slurry, SlurryStack, ISlurryHandler> {
        public Slurry(MBDMachine machine, ChemicalTankCapabilityTraitDefinition<mekanism.api.chemical.slurry.Slurry, SlurryStack, ISlurryHandler> definition) {
            super(machine, definition);
        }

        @Override
        public ChemicalStorage<mekanism.api.chemical.slurry.Slurry, SlurryStack> createStorage() {
            return new ChemicalStorage.Slurry(getDefinition().getCapacity(), chemical -> getDefinition().getChemicalFilterSettings().isEnable() || getDefinition().getChemicalFilterSettings().test(chemical), null);
        }

        @Override
        public ISlurryHandler getCapContent(IO capbilityIO) {
            return new ChemicalStorageWrapper.Slurry(storages, capbilityIO);
        }

        @Override
        public ISlurryHandler mergeContents(List<mekanism.api.chemical.slurry.ISlurryHandler> contents) {
            return new ChemicalHandlerList.Slurry(contents.toArray(new ISlurryHandler[0]));
        }
    }

}
