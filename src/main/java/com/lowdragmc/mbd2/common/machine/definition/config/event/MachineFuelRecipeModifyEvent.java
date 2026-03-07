package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import lombok.Setter;
import net.neoforged.bus.api.ICancellableEvent;

@Getter
//@LDLRegister(name = "MachineFuelRecipeModifyEvent", group = "MachineEvent")
public class MachineFuelRecipeModifyEvent extends MachineEvent implements ICancellableEvent {
    @Setter
    public MBDRecipe recipe;

    public MachineFuelRecipeModifyEvent(MBDMachine machine, MBDRecipe recipe) {
        super(machine);
        this.recipe = recipe;
    }

}
