package com.lowdragmc.mbd2.api.recipe.event;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import lombok.Getter;
import net.minecraftforge.eventbus.api.Cancelable;

@Getter
@Cancelable
@LDLRegister(name = "FuelRecipeUIEvent", group = "RecipeTypeEvent")
public class FuelRecipeUIEvent extends RecipeTypeEvent {
    public MBDRecipe recipe;
    public WidgetGroup root;

    public FuelRecipeUIEvent(MBDRecipeType recipeType, MBDRecipe recipe, WidgetGroup root) {
        super(recipeType);
        this.recipe = recipe;
        this.root = root;
    }
}
