package com.lowdragmc.mbd2.api.recipe.event;

import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.Nullable;

@Getter
//@LDLRegister(name = "TransferProxyRecipeEvent", group = "RecipeTypeEvent")
public class TransferProxyRecipeEvent extends RecipeTypeEvent implements ICancellableEvent {

    public final ResourceLocation proxyTypeId;
    public final RecipeType<?> proxyType;
    public final ResourceLocation proxyRecipeId;
    public final Recipe<?> proxyRecipe;
    @Nullable
    public MBDRecipe mbdRecipe;

    public TransferProxyRecipeEvent(MBDRecipeType recipeType, ResourceLocation proxyTypeId, RecipeType<?> proxyType, ResourceLocation proxyRecipeId, Recipe<?> proxyRecipe, @Nullable MBDRecipe mbdRecipe) {
        super(recipeType);
        this.proxyTypeId = proxyTypeId;
        this.proxyType = proxyType;
        this.proxyRecipe = proxyRecipe;
        this.proxyRecipeId = proxyRecipeId;
        this.mbdRecipe = mbdRecipe;
    }
}
