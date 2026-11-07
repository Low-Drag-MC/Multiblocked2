package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.mbd2.api.recipe.RecipeLogic;

/**
 * A {@link MachineEvent} posted by one recipe logic of the machine. A machine may have several, see
 * {@link com.lowdragmc.mbd2.api.machine.IMachine#getRecipeLogics()}; this is the one the event is about.
 */
public interface RecipeLogicEvent {
    RecipeLogic getRecipeLogic();
}
