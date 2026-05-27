package com.lowdragmc.mbd2.test.framework;

import com.lowdragmc.mbd2.api.recipe.MBDRecipeBuilder;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fluent builder for test-only MBD recipe types. Wraps {@link MBDRecipeType} with a
 * chained {@code .recipe(id, body).recipe(...)} flow for declaring built-in recipes.
 */
public class TestRecipeTypeBuilder {

    private final MBDRecipeType recipeType;
    private final List<PendingRecipe> pending = new ArrayList<>();

    private record PendingRecipe(ResourceLocation id, Consumer<MBDRecipeBuilder> body) {}

    private TestRecipeTypeBuilder(ResourceLocation id) {
        this.recipeType = new MBDRecipeType(id);
    }

    public static TestRecipeTypeBuilder of(ResourceLocation id) {
        return new TestRecipeTypeBuilder(id);
    }

    /**
     * Configure the underlying {@link MBDRecipeType} (e.g. set duration defaults).
     */
    public TestRecipeTypeBuilder configure(Consumer<MBDRecipeType> tweak) {
        tweak.accept(recipeType);
        return this;
    }

    /**
     * Append a built-in recipe. The {@code body} receives a fresh {@link MBDRecipeBuilder}
     * scoped to {@code id}; call {@code inputItems(...)}, {@code outputItems(...)},
     * {@code duration(int)} etc. on it.
     */
    public TestRecipeTypeBuilder recipe(String id, Consumer<MBDRecipeBuilder> body) {
        return recipe(ResourceLocation.fromNamespaceAndPath(recipeType.getRegistryName().getNamespace(), id), body);
    }

    public TestRecipeTypeBuilder recipe(ResourceLocation id, Consumer<MBDRecipeBuilder> body) {
        pending.add(new PendingRecipe(id, body));
        return this;
    }

    /** Register the recipe type AND its built-in recipes. */
    public MBDRecipeType register(MBDRegistryEvent.MBDRecipeType event) {
        event.register(recipeType);
        for (PendingRecipe p : pending) {
            MBDRecipeBuilder builder = recipeType.recipeBuilder(p.id);
            p.body.accept(builder);
            recipeType.getBuiltinRecipes().put(p.id, builder.buildRawRecipe());
        }
        return recipeType;
    }

    public MBDRecipeType getRecipeType() {
        return recipeType;
    }
}
