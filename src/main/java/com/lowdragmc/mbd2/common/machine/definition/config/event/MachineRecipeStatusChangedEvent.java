package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.parameter.ExposedParameter;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.graphprocessor.GraphParameterGet;
import lombok.Getter;

import java.util.Map;
import java.util.Optional;


@Getter
@LDLRegister(name = "MachineRecipeStatusChangedEvent", group = "MachineEvent")
public class MachineRecipeStatusChangedEvent extends MachineEvent {
    @GraphParameterGet(displayName = "old status", type = String.class, tips = "graph_processor.node.mbd2.recipe_logic.status.tips")
    public final RecipeLogic.Status oldStatus;
    @GraphParameterGet(displayName = "new status", type = String.class, tips = "graph_processor.node.mbd2.recipe_logic.status.tips")
    public final RecipeLogic.Status newStatus;

    public MachineRecipeStatusChangedEvent(MBDMachine machine, RecipeLogic.Status oldStatus, RecipeLogic.Status newStatus) {
        super(machine);
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    @Override
    public void bindParameters(Map<String, ExposedParameter> exposedParameters) {
        super.bindParameters(exposedParameters);
        Optional.ofNullable(exposedParameters.get("oldStatus")).ifPresent(p -> p.setValue(oldStatus.toString()));
        Optional.ofNullable(exposedParameters.get("newStatus")).ifPresent(p -> p.setValue(newStatus.toString()));
    }
}
