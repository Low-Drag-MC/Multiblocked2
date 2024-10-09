package com.lowdragmc.mbd2.integration.create.machine;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeCapabilityHolder;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.IRecipeCapabilityTrait;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.TraitDefinition;
import com.lowdragmc.mbd2.integration.create.CreateStressRecipeCapability;
import lombok.Getter;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CreateStressTrait implements IRecipeCapabilityTrait<Float> {
    protected List<Runnable> listeners = new ArrayList<>();

    public final static TraitDefinition DEFINITION = new TraitDefinition() {
        @Override
        public ITrait createTrait(MBDMachine machine) {
            return new CreateStressTrait(machine);
        }

        @Override
        public IGuiTexture getIcon() {
            return IGuiTexture.EMPTY;
        }

        @Override
        public String name() {
            return "!create_stress";
        }

        @Override
        public String group() {
            return "trait";
        }

        @Override
        public boolean allowMultiple() {
            return false;
        }
    };

    @Getter
    private final MBDMachine machine;
    @Getter
    private final boolean isGenerator;
    @Getter
    private float impact;
    private float available, lastSpeed = -1;

    public CreateStressTrait(MBDMachine machine) {
        this.machine = machine;
        this.isGenerator = machine.getDefinition() instanceof CreateKineticMachineDefinition definition && definition.kineticMachineSettings().isGenerator();
        this.impact = machine.getDefinition() instanceof CreateKineticMachineDefinition definition ? definition.kineticMachineSettings().getImpact() : 0;
    }

    @Override
    public ISubscription addChangedListener(Runnable listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public void notifyListeners() {
        listeners.forEach(Runnable::run);
    }

    @Override
    public void serverTick() {
        if (machine.getHolder() instanceof MBDKineticMachineBlockEntity kineticMachine) {
            var speed = kineticMachine.getSpeed();
            if (speed != lastSpeed) {
                lastSpeed = speed;
                notifyListeners();
            }
        }
    }

    @Override
    public List<Float> handleRecipeInner(IO io, MBDRecipe recipe, List<Float> left, @Nullable String slotName, boolean simulate) {
        if (io != getHandlerIO()) return left;
        if (machine.getHolder() instanceof MBDKineticMachineBlockEntity holder) {
            float sum = left.stream().reduce(0f, Float::sum);
            if (io == IO.IN && !isGenerator) {
                float capacity = Mth.abs(holder.getSpeed()) * impact;
                if (capacity > 0) {
                    sum = sum - capacity;
                }
            } else if (io == IO.OUT && isGenerator) {
                if (simulate) {
                    available = holder.scheduleWorking(sum, true);
                }
                sum = sum - available;
            }
            return sum <= 0 ? null : Collections.singletonList(sum);
        }
        return left;
    }

    @Override
    public void preWorking(IRecipeCapabilityHolder holder, IO io, MBDRecipe recipe) {
        if (machine.getHolder() instanceof MBDKineticMachineBlockEntity blockEntity) {
            if (available > 0 && isGenerator && io == IO.OUT) {
                blockEntity.scheduleWorking(available, false);
            }
        }
    }

    @Override
    public void postWorking(IRecipeCapabilityHolder holder, IO io, MBDRecipe recipe) {
        if (machine.getHolder() instanceof MBDKineticMachineBlockEntity blockEntity) {
            if (isGenerator && io == IO.OUT) {
                blockEntity.stopWorking();
            }
        }
    }

    @Override
    public RecipeCapability<Float> getRecipeCapability() {
        return CreateStressRecipeCapability.CAP;
    }

    @Override
    public IO getHandlerIO() {
        return (getMachine().getDefinition() instanceof CreateKineticMachineDefinition definition && definition.kineticMachineSettings.isGenerator) ? IO.OUT : IO.IN;
    }

    @Override
    public TraitDefinition getDefinition() {
        return DEFINITION;
    }
}
