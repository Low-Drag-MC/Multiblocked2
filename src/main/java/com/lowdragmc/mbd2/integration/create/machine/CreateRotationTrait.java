package com.lowdragmc.mbd2.integration.create.machine;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeCapabilityHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.IUIProviderTrait;
import com.lowdragmc.mbd2.common.trait.TraitDefinition;
import com.lowdragmc.mbd2.integration.create.*;
import com.lowdragmc.mbd2.utils.WidgetUtils;
import lombok.Getter;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CreateRotationTrait implements ITrait {
    protected List<Runnable> listeners = new ArrayList<>();

    public static class CreateRotationTraitDefinition extends TraitDefinition implements IUIProviderTrait {
        @Override
        public ITrait createTrait(MBDMachine machine) {
            return new CreateRotationTrait(machine);
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

        @Override
        public void createTraitUITemplate(WidgetGroup ui) {
            var prefix = uiPrefixName();
            var group = new WidgetGroup(0, 0, 100, 32);
            var rpm = new CreateRPMWidget();
            rpm.setId(prefix + "_rpm");
            var stress = new CreateStressWidget();
            stress.setId(prefix + "_stress");
            stress.setSelfPositionY(16);
            group.addWidget(rpm);
            group.addWidget(stress);
            ui.addWidget(group);
        }

        @Override
        public void initTraitUI(ITrait trait, WidgetGroup group) {
            if (trait instanceof CreateRotationTrait rotationTrait) {
                var prefix = uiPrefixName();
                WidgetUtils.widgetByIdForEach(group, "^%s_rpm$".formatted(prefix), CreateRPMWidget.class, rpmWidget -> {
                    rpmWidget.setRpmSupplier(() -> Mth.abs(rotationTrait.lastSpeed));
                });
                WidgetUtils.widgetByIdForEach(group, "^%s_stress$".formatted(prefix), CreateStressWidget.class, stressWidget -> {
                    stressWidget.setStressSupplier(() -> rotationTrait.getTorque() * Mth.abs(rotationTrait.lastSpeed));
                });
            }
        }
    }

    public final static TraitDefinition DEFINITION = new CreateRotationTraitDefinition();

    @Getter
    private final MBDMachine machine;
    @Getter
    private final boolean isGenerator;
    @Getter
    private final float torque;
    @Getter
    private float lastSpeed;
    private final Map<MBDRecipe, Float> availableCache = new ConcurrentHashMap<>();
    private final StressRecipeHandler stressRecipeHandler = new StressRecipeHandler();
    private final RPMRecipeHandler rpmRecipeHandler = new RPMRecipeHandler();

    public CreateRotationTrait(MBDMachine machine) {
        this.machine = machine;
        this.isGenerator = machine.getDefinition() instanceof CreateKineticMachineDefinition definition && definition.kineticMachineSettings().isGenerator();
        this.torque = machine.getDefinition() instanceof CreateKineticMachineDefinition definition ? definition.kineticMachineSettings().torque() : 0;
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
    public TraitDefinition getDefinition() {
        return DEFINITION;
    }

    public void preWorking(IO io, MBDRecipe recipe) {
        if (machine.getHolder() instanceof MBDKineticMachineBlockEntity blockEntity) {
            if (isGenerator && io == IO.OUT) {
                var available = availableCache.get(recipe);
                if (available == null) {

                } else if (available > 0) {
                    blockEntity.scheduleWorking(available, false);
                }
            }
        }
    }

    public void postWorking(IO io, MBDRecipe recipe) {
        if (machine.getHolder() instanceof MBDKineticMachineBlockEntity blockEntity) {
            if (isGenerator && io == IO.OUT) {
                blockEntity.stopWorking();
            }
        }
    }

    public IO getHandlerIO() {
        return (getMachine().getDefinition() instanceof CreateKineticMachineDefinition definition && definition.kineticMachineSettings.isGenerator) ? IO.OUT : IO.IN;
    }

    @Override
    public List<IRecipeHandlerTrait<?>> getRecipeHandlerTraits() {
        return List.of(stressRecipeHandler, rpmRecipeHandler);
    }

    public class StressRecipeHandler implements IRecipeHandlerTrait<Float> {

        public CreateRotationTrait getTrait() {
            return CreateRotationTrait.this;
        }

        @Override
        public ISubscription addChangedListener(Runnable listener) {
            listeners.add(listener);
            return () -> listeners.remove(listener);
        }

        @Override
        public List<Float> handleRecipeInner(IO io, MBDRecipe recipe, List<Float> left, @Nullable String slotName, boolean simulate) {
            if (!compatibleWith(io)) return left;
            if (machine.getHolder() instanceof MBDKineticMachineBlockEntity holder) {
                float sum = left.stream().reduce(0f, Float::sum);
                if (io == IO.IN && !isGenerator) {
                    float capacity = Mth.abs(holder.getSpeed()) * torque;
                    if (capacity > 0) {
                        sum = sum - capacity;
                    }
                } else if (io == IO.OUT && isGenerator) {
                    if (simulate) {
                        var available = holder.scheduleWorking(sum, true);
                        availableCache.put(recipe, available);
                        sum = sum - available;
                    } else {
                        var available = availableCache.remove(recipe);
                        if (available != null) {
                            sum = sum - available;
                        }
                    }
                }
                return sum <= 0 ? null : Collections.singletonList(sum);
            }
            return left;
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
        public RecipeCapability<Float> getRecipeCapability() {
            return CreateStressRecipeCapability.CAP;
        }

    }

    public class RPMRecipeHandler implements IRecipeHandlerTrait<Float> {

        public CreateRotationTrait getTrait() {
            return CreateRotationTrait.this;
        }

        @Override
        public ISubscription addChangedListener(Runnable listener) {
            listeners.add(listener);
            return () -> listeners.remove(listener);
        }

        @Override
        public List<Float> handleRecipeInner(IO io, MBDRecipe recipe, List<Float> left, @Nullable String slotName, boolean simulate) {
            if (!compatibleWith(io)) return left;
            if (machine.getHolder() instanceof MBDKineticMachineBlockEntity holder) {
                float sum = left.stream().reduce(0f, Float::max);
                if (io == IO.IN && !isGenerator) {
                    // check rotation condition first
                    var rpm = Mth.abs(holder.getSpeed());
                    var stress = rpm * torque;
                    for (var condition : recipe.conditions) {
                        if (condition instanceof CreateRotationCondition rotationCondition) {
                            if (rpm < rotationCondition.getMinRPM() || rpm > rotationCondition.getMaxRPM() ||
                                    stress < rotationCondition.getMinStress() || stress > rotationCondition.getMaxStress()) {
                                return left;
                            }
                        }
                    }
                    if (rpm >= sum) {
                        return null;
                    }
                } else if (io == IO.OUT && isGenerator) {
                    if (simulate) {
                        var available = holder.scheduleWorkingRPM(sum, true);
                        availableCache.put(recipe, available);
                    } else {
                        availableCache.remove(recipe);
                    }
                    return null;
                }
            }
            return left;
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
        public RecipeCapability<Float> getRecipeCapability() {
            return CreateRPMRecipeCapability.CAP;
        }
    }
}
