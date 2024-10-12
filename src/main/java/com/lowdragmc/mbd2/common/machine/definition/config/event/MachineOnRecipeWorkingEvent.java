package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.parameter.ExposedParameter;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.graphprocessor.GraphParameterGet;
import lombok.Getter;
import net.minecraftforge.eventbus.api.Cancelable;

import java.util.Map;
import java.util.Optional;

@Getter
@Cancelable
@LDLRegister(name = "MachineOnRecipeWorkingEvent", group = "MachineEvent")
public class MachineOnRecipeWorkingEvent extends MachineEvent {
    @GraphParameterGet
    public final MBDRecipe recipe;
    @GraphParameterGet
    public final int progress;

    public MachineOnRecipeWorkingEvent(MBDMachine machine, MBDRecipe recipe, int progress) {
        super(machine);
        this.recipe = recipe;
        this.progress = progress;
    }

    @Override
    public void bindParameters(Map<String, ExposedParameter> exposedParameters) {
        super.bindParameters(exposedParameters);
        Optional.ofNullable(exposedParameters.get("recipe")).ifPresent(p -> p.setValue(recipe));
        Optional.ofNullable(exposedParameters.get("progress")).ifPresent(p -> p.setValue(progress));
    }
}
