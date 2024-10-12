package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.parameter.ExposedParameter;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.graphprocessor.GraphParameterGet;
import com.lowdragmc.mbd2.common.graphprocessor.GraphParameterSet;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.eventbus.api.Cancelable;

import java.util.Map;
import java.util.Optional;

@Getter
public class MachineRecipeModifyEvent extends MachineEvent {
    @GraphParameterGet
    @GraphParameterSet
    @Setter
    public MBDRecipe recipe;

    public MachineRecipeModifyEvent(MBDMachine machine, MBDRecipe recipe) {
        super(machine);
        this.recipe = recipe;
    }

    @Override
    public void bindParameters(Map<String, ExposedParameter> exposedParameters) {
        super.bindParameters(exposedParameters);
        Optional.ofNullable(exposedParameters.get("recipe")).ifPresent(p -> p.setValue(recipe));
    }

    @Override
    public void gatherParameters(Map<String, ExposedParameter> exposedParameters) {
        super.gatherParameters(exposedParameters);
        this.recipe = Optional.ofNullable(exposedParameters.get("recipe"))
                .map(ExposedParameter::getValue)
                .filter(MBDRecipe.class::isInstance)
                .map(MBDRecipe.class::cast)
                .orElse(this.recipe);
    }

    @LDLRegister(name = "MachineRecipeModifyEvent.Before", group = "MachineEvent")
    @Cancelable
    public static class Before extends MachineRecipeModifyEvent {
        public Before(MBDMachine machine, MBDRecipe recipe) {
            super(machine, recipe);
        }
    }

    @LDLRegister(name = "MachineRecipeModifyEvent.After", group = "MachineEvent")
    public static class After extends MachineRecipeModifyEvent {
        public After(MBDMachine machine, MBDRecipe recipe) {
            super(machine, recipe);
        }
    }

}
