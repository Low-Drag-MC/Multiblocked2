package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;


@Getter
//@LDLRegister(name = "MachineRecipeStatusChangedEvent", group = "MachineEvent")
public class MachineRecipeStatusChangedEvent extends MachineEvent implements RecipeLogicEvent {
    public final RecipeLogic recipeLogic;
    public final RecipeLogic.Status oldStatus;
    public final RecipeLogic.Status newStatus;

    public MachineRecipeStatusChangedEvent(MBDMachine machine, RecipeLogic.Status oldStatus, RecipeLogic.Status newStatus) {
        this(machine, machine.getContextRecipeLogic(), oldStatus, newStatus);
    }

    public MachineRecipeStatusChangedEvent(MBDMachine machine, RecipeLogic recipeLogic, RecipeLogic.Status oldStatus, RecipeLogic.Status newStatus) {
        super(machine);
        this.recipeLogic = recipeLogic;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

}
