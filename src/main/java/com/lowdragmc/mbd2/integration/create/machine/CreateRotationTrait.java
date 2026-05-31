package com.lowdragmc.mbd2.integration.create.machine;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeCapabilityHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.IUIProviderTrait;
import com.lowdragmc.mbd2.common.trait.TraitDefinition;
import com.lowdragmc.mbd2.common.trait.TraitDefinitionType;
import com.lowdragmc.mbd2.integration.create.CreateRotation;
import com.lowdragmc.mbd2.integration.create.CreateRotationElement;
import com.lowdragmc.mbd2.integration.create.CreateRotationRecipeCapability;
import lombok.Getter;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CreateRotationTrait implements ITrait {
    public static final CreateRotationTraitDefinition DEFINITION = new CreateRotationTraitDefinition();

    public static class CreateRotationTraitDefinition extends TraitDefinition implements IUIProviderTrait {
        @LDLRegister(name = "!create_rotation", registry = "mbd2:trait_definition_type", group = "trait", modID = "create")
        public static final TraitDefinitionType<CreateRotationTraitDefinition> TYPE =
                new TraitDefinitionType<>("!create_rotation", "trait") {
                    @Override
                    public CreateRotationTraitDefinition createDefinition() {
                        return DEFINITION;
                    }
                };

        @Override
        public ITrait createTrait(MBDMachine machine) {
            return new CreateRotationTrait(machine);
        }

        @Override
        public TraitDefinitionType<?> type() {
            return TYPE;
        }

        @Override
        public IGuiTexture getIcon() {
            return IGuiTexture.EMPTY;
        }

        @Override
        public boolean allowMultiple() {
            return false;
        }

        @Override
        public boolean isMandatory() {
            return true;
        }

        @Override
        public TraitUILayoutType getTraitUILayoutType() {
            return TraitUILayoutType.BAR;
        }

        @Override
        public void createTraitUITemplate(UIElement container) {
            var prefix = uiId();
            var rotation = new CreateRotationElement();
            rotation.setId(prefix + "_rotation");
            rotation.getLayout().flex(1).height(16);
            container.addChild(rotation);
        }

        @Override
        public void initTraitUI(ITrait trait, UI ui) {
            if (trait instanceof CreateRotationTrait rotationTrait) {
                var prefix = uiId();
                ui.selectId(prefix + "_rotation", CreateRotationElement.class).forEach(rotationElement -> {
                    rotationElement.getRpmValue().bind(DataBindingBuilder.floatValS2C(
                            () -> Mth.abs(rotationTrait.getLastSpeed())).build());
                    rotationElement.getStressValue().bind(DataBindingBuilder.floatValS2C(
                            () -> Mth.abs(rotationTrait.getLastSpeed()) * rotationTrait.getTorque()).build());
                    rotationElement.getTorqueValue().bind(DataBindingBuilder.floatValS2C(
                            rotationTrait::getTorque).build());
                });
            }
        }
    }

    protected final List<Runnable> listeners = new ArrayList<>();
    @Getter
    private final MBDMachine machine;
    @Getter
    private final boolean isGenerator;
    private final float baseTorque;
    /**
     * Effective torque used by the recipe handler for stress<->RPM math. Falls back to
     * {@link #baseTorque} when no recipe override is active.
     */
    @Getter
    private float torque;
    @Getter
    private float lastSpeed;
    private final Map<MBDRecipe, Float> availableCache = new ConcurrentHashMap<>();
    private final RotationRecipeHandler rotationRecipeHandler = new RotationRecipeHandler();

    public CreateRotationTrait(MBDMachine machine) {
        this.machine = machine;
        this.isGenerator = machine.getDefinition() instanceof CreateKineticMachineDefinition definition && definition.kineticMachineSettings().isGenerator;
        this.baseTorque = machine.getDefinition() instanceof CreateKineticMachineDefinition definition ? definition.kineticMachineSettings().torque : 0;
        this.torque = this.baseTorque;
    }

    @Override
    public TraitDefinition getDefinition() {
        return DEFINITION;
    }

    public void notifyListeners() {
        listeners.forEach(Runnable::run);
    }

    @Override
    public void serverTick() {
        if (machine.getHolder() instanceof MBDKineticMachineBlockEntity kineticBE) {
            var speed = kineticBE.getSpeed();
            if (speed != lastSpeed) {
                lastSpeed = speed;
                notifyListeners();
            }
        }
    }

    public IO getHandlerIO() {
        return isGenerator ? IO.OUT : IO.IN;
    }

    public void preWorking(IO io, MBDRecipe recipe) {
        applyTorqueOverride(recipe);
        if (machine.getHolder() instanceof MBDKineticMachineBlockEntity blockEntity && isGenerator && io == IO.OUT) {
            var available = availableCache.get(recipe);
            if (available != null && available > 0) {
                blockEntity.scheduleWorking(available, false);
            }
        }
    }

    public void postWorking(IO io, MBDRecipe recipe) {
        if (machine.getHolder() instanceof MBDKineticMachineBlockEntity blockEntity && isGenerator && io == IO.OUT) {
            blockEntity.stopWorking();
        }
        clearTorqueOverride();
    }

    private void applyTorqueOverride(MBDRecipe recipe) {
        if (!(machine.getHolder() instanceof MBDKineticMachineBlockEntity blockEntity)) return;
        Float override = findTorqueOverride(recipe);
        if (override != null) {
            this.torque = override;
            blockEntity.setDynamicTorqueOverride(override);
        } else {
            this.torque = this.baseTorque;
            blockEntity.setDynamicTorqueOverride(null);
        }
    }

    private void clearTorqueOverride() {
        this.torque = this.baseTorque;
        if (machine.getHolder() instanceof MBDKineticMachineBlockEntity blockEntity) {
            blockEntity.setDynamicTorqueOverride(null);
        }
    }

    /**
     * Walk both inputs and outputs of the recipe for any {@link CreateRotation} content whose
     * {@code torqueOverride} toggle is enabled, and return its float value. First match wins.
     */
    private static @Nullable Float findTorqueOverride(MBDRecipe recipe) {
        Float fromInputs = findInMap(recipe.inputs);
        if (fromInputs != null) return fromInputs;
        return findInMap(recipe.outputs);
    }

    private static @Nullable Float findInMap(Map<RecipeCapability<?>, List<com.lowdragmc.mbd2.api.recipe.content.Content>> map) {
        if (map == null) return null;
        var list = map.get(CreateRotationRecipeCapability.CAP);
        if (list == null) return null;
        for (var content : list) {
            if (content.content instanceof CreateRotation cr
                    && cr.torqueOverride != null && cr.torqueOverride.isEnable()) {
                return cr.torqueOverride.getValue();
            }
        }
        return null;
    }

    @Override
    public List<IRecipeHandlerTrait<?>> getRecipeHandlerTraits() {
        return List.of(rotationRecipeHandler);
    }

    /**
     * Single handler for {@link CreateRotationRecipeCapability}. Splits the incoming content
     * list by {@link CreateRotation.Mode} and applies the appropriate stress / RPM math —
     * preserving the semantics the old {@code StressRecipeHandler} / {@code RPMRecipeHandler}
     * had.
     */
    public class RotationRecipeHandler implements IRecipeHandlerTrait<CreateRotation> {

        public CreateRotationTrait getTrait() {
            return CreateRotationTrait.this;
        }

        @Override
        public ISubscription addChangedListener(Runnable listener) {
            listeners.add(listener);
            return () -> listeners.remove(listener);
        }

        @Override
        public List<CreateRotation> handleRecipeInner(IO io, MBDRecipe recipe, List<CreateRotation> left, @Nullable String slotName, boolean simulate) {
            if (!compatibleWith(io)) return left;
            if (!(machine.getHolder() instanceof MBDKineticMachineBlockEntity holder)) return left;

            // Sum stress entries, max RPM entries — same reduction logic the old handlers used.
            float stressSum = 0f;
            float rpmMax = 0f;
            boolean hasStress = false, hasRpm = false;
            for (var c : left) {
                if (c == null) continue;
                if (c.mode == CreateRotation.Mode.STRESS) { stressSum += c.value; hasStress = true; }
                else { rpmMax = Math.max(rpmMax, c.value); hasRpm = true; }
            }

            float stressRemaining = stressSum;
            float rpmRemaining = rpmMax;

            if (io == IO.IN && !isGenerator) {
                float speedAbs = Mth.abs(holder.getSpeed());
                if (hasStress) {
                    float capacity = speedAbs * torque;
                    if (capacity > 0) stressRemaining = stressSum - capacity;
                }
                if (hasRpm) {
                    var stress = speedAbs * torque;
                    for (var condition : recipe.conditions) {
                        if (condition instanceof com.lowdragmc.mbd2.integration.create.CreateRotationCondition rc) {
                            if (speedAbs < rc.getMinRPM() || speedAbs > rc.getMaxRPM()
                                    || stress < rc.getMinStress() || stress > rc.getMaxStress()) {
                                return left;
                            }
                        }
                    }
                    if (speedAbs >= rpmMax) rpmRemaining = 0f;
                }
            } else if (io == IO.OUT && isGenerator) {
                if (hasStress) {
                    if (simulate) {
                        var available = holder.scheduleWorking(stressSum, true);
                        availableCache.put(recipe, available);
                        stressRemaining = stressSum - available;
                    } else {
                        var available = availableCache.remove(recipe);
                        if (available != null) stressRemaining = stressSum - available;
                    }
                }
                if (hasRpm) {
                    if (simulate) {
                        var available = holder.scheduleWorkingRPM(rpmMax, true);
                        availableCache.put(recipe, available);
                    } else {
                        availableCache.remove(recipe);
                    }
                    rpmRemaining = 0f;
                }
            }

            // Rebuild the leftover list preserving content mode + any torque overrides on entries
            // we didn't fully consume.
            List<CreateRotation> result = new ArrayList<>();
            if (hasStress && stressRemaining > 0f) {
                result.add(new CreateRotation(stressRemaining, CreateRotation.Mode.STRESS,
                        findOverride(left, CreateRotation.Mode.STRESS)));
            }
            if (hasRpm && rpmRemaining > 0f) {
                result.add(new CreateRotation(rpmRemaining, CreateRotation.Mode.RPM,
                        findOverride(left, CreateRotation.Mode.RPM)));
            }
            return result.isEmpty() ? null : result;
        }

        private com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleFloat findOverride(
                List<CreateRotation> list, CreateRotation.Mode mode) {
            for (var c : list) {
                if (c != null && c.mode == mode && c.torqueOverride != null && c.torqueOverride.isEnable()) {
                    return com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleFloat.of(
                            true, c.torqueOverride.getValue());
                }
            }
            return com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleFloat.ofDisabled();
        }

        @Override
        public void preWorking(IRecipeCapabilityHolder holder, IO io, MBDRecipe recipe) {
            CreateRotationTrait.this.preWorking(io, recipe);
        }

        @Override
        public void postWorking(IRecipeCapabilityHolder holder, IO io, MBDRecipe recipe) {
            CreateRotationTrait.this.postWorking(io, recipe);
        }

        @Override
        public IO getHandlerIO() {
            return CreateRotationTrait.this.getHandlerIO();
        }

        @Override
        public RecipeCapability<CreateRotation> getRecipeCapability() {
            return CreateRotationRecipeCapability.CAP;
        }
    }
}
