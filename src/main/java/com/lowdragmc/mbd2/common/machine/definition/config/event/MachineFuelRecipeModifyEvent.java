package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import lombok.Setter;
import net.neoforged.bus.api.ICancellableEvent;

@Getter
//@LDLRegister(name = "MachineFuelRecipeModifyEvent", group = "MachineEvent")
public class MachineFuelRecipeModifyEvent extends MachineEvent implements ICancellableEvent, RecipeLogicEvent {
    public final RecipeLogic recipeLogic;
    @Setter
    public MBDRecipe recipe;

    public MachineFuelRecipeModifyEvent(MBDMachine machine, MBDRecipe recipe) {
        this(machine, machine.getContextRecipeLogic(), recipe);
    }

    public MachineFuelRecipeModifyEvent(MBDMachine machine, RecipeLogic recipeLogic, MBDRecipe recipe) {
        super(machine);
        this.recipeLogic = recipeLogic;
        this.recipe = recipe;
    }

}
