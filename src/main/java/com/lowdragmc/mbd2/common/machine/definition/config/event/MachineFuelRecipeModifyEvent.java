package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.parameter.ExposedParameter;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.graphprocessor.GraphParameterGet;
import com.lowdragmc.mbd2.common.graphprocessor.GraphParameterSet;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import lombok.Setter;
import net.neoforged.bus.api.ICancellableEvent;

import java.util.Map;
import java.util.Optional;

@Getter
@LDLRegister(name = "MachineFuelRecipeModifyEvent", group = "MachineEvent")
public class MachineFuelRecipeModifyEvent extends MachineEvent implements ICancellableEvent {
    @GraphParameterGet(identity = "recipe.in")
    @GraphParameterSet(identity = "recipe.out")
    @Setter
    public MBDRecipe recipe;

    public MachineFuelRecipeModifyEvent(MBDMachine machine, MBDRecipe recipe) {
        super(machine);
        this.recipe = recipe;
    }

    @Override
    public void bindParameters(Map<String, ExposedParameter> exposedParameters) {
        super.bindParameters(exposedParameters);
        Optional.ofNullable(exposedParameters.get("recipe.in")).ifPresent(p -> p.setValue(recipe));
    }

    @Override
    public void gatherParameters(Map<String, ExposedParameter> exposedParameters) {
        super.gatherParameters(exposedParameters);
        this.recipe = Optional.ofNullable(exposedParameters.get("recipe.out"))
                .map(ExposedParameter::getValue)
                .filter(MBDRecipe.class::isInstance)
                .map(MBDRecipe.class::cast)
                .orElse(null);
    }

}
