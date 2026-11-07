package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import lombok.Setter;
import net.neoforged.bus.api.ICancellableEvent;

@Getter
public class MachineRecipeModifyEvent extends MachineEvent implements RecipeLogicEvent {
    public final RecipeLogic recipeLogic;
    @Setter
    public MBDRecipe recipe;

    public MachineRecipeModifyEvent(MBDMachine machine, MBDRecipe recipe) {
        this(machine, machine.getContextRecipeLogic(), recipe);
    }

    public MachineRecipeModifyEvent(MBDMachine machine, RecipeLogic recipeLogic, MBDRecipe recipe) {
        super(machine);
        this.recipeLogic = recipeLogic;
        this.recipe = recipe;
    }


//    @LDLRegister(name = "MachineRecipeModifyEvent.Before", group = "MachineEvent")
    public static class Before extends MachineRecipeModifyEvent implements ICancellableEvent {
        public Before(MBDMachine machine, MBDRecipe recipe) {
            super(machine, recipe);
        }

        public Before(MBDMachine machine, RecipeLogic recipeLogic, MBDRecipe recipe) {
            super(machine, recipeLogic, recipe);
        }
    }

//    @LDLRegister(name = "MachineRecipeModifyEvent.After", group = "MachineEvent")
    public static class After extends MachineRecipeModifyEvent {
        public After(MBDMachine machine, MBDRecipe recipe) {
            super(machine, recipe);
        }

        public After(MBDMachine machine, RecipeLogic recipeLogic, MBDRecipe recipe) {
            super(machine, recipeLogic, recipe);
        }
    }

}
