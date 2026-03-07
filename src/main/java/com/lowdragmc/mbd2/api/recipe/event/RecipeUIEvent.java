package com.lowdragmc.mbd2.api.recipe.event;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import lombok.Getter;
import net.neoforged.bus.api.ICancellableEvent;

@Getter
//@LDLRegister(name = "RecipeUIEvent", group = "RecipeTypeEvent")
public class RecipeUIEvent extends RecipeTypeEvent implements ICancellableEvent {
    public MBDRecipe recipe;
    public UI ui;

    public RecipeUIEvent(MBDRecipeType recipeType, MBDRecipe recipe, UI ui) {
        super(recipeType);
        this.recipe = recipe;
        this.ui = ui;
    }
}
