package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import lombok.Setter;
import net.neoforged.bus.api.ICancellableEvent;

@Getter
public class MachineRecipeModifyEvent extends MachineEvent {
    @Setter
    public MBDRecipe recipe;

    public MachineRecipeModifyEvent(MBDMachine machine, MBDRecipe recipe) {
        super(machine);
        this.recipe = recipe;
    }


//    @LDLRegister(name = "MachineRecipeModifyEvent.Before", group = "MachineEvent")
    public static class Before extends MachineRecipeModifyEvent implements ICancellableEvent {
        public Before(MBDMachine machine, MBDRecipe recipe) {
            super(machine, recipe);
        }
    }

//    @LDLRegister(name = "MachineRecipeModifyEvent.After", group = "MachineEvent")
    public static class After extends MachineRecipeModifyEvent {
        public After(MBDMachine machine, MBDRecipe recipe) {
            super(machine, recipe);
        }
    }

}
