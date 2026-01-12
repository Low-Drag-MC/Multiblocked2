package com.lowdragmc.mbd2.integration.kubejs.events;

import com.lowdragmc.mbd2.api.recipe.event.FuelRecipeUIEvent;
import com.lowdragmc.mbd2.api.recipe.event.RecipeTypeEvent;
import com.lowdragmc.mbd2.api.recipe.event.RecipeUIEvent;
import com.lowdragmc.mbd2.api.recipe.event.TransferProxyRecipeEvent;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventJS;
import lombok.Getter;

public class MBDRecipeTypeEvents {
    public static EventGroup MBD_RECIPE_TYPE_EVENTS = EventGroup.of("MBDRecipeTypeEvents");

    @Getter
    public static class RecipeTypeEventJS<E extends RecipeTypeEvent> extends EventJS {
        public final E event;

        public RecipeTypeEventJS(E event) {
            this.event = event;
        }
    }

    public static class TransferProxyRecipeEventJS extends RecipeTypeEventJS<TransferProxyRecipeEvent> {
        public TransferProxyRecipeEventJS(TransferProxyRecipeEvent event) {
            super(event);
        }
    }

    public static class RecipeUIEventJS extends RecipeTypeEventJS<RecipeUIEvent> {
        public RecipeUIEventJS(RecipeUIEvent event) {
            super(event);
        }
    }

    public static class FuelRecipeUIEventJS extends RecipeTypeEventJS<FuelRecipeUIEvent> {
        public FuelRecipeUIEventJS(FuelRecipeUIEvent event) {
            super(event);
        }
    }
}
