package com.lowdragmc.mbd2.api.recipe.event;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeBuilder;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.common.graphprocessor.GraphParameterGet;
import com.lowdragmc.mbd2.common.graphprocessor.GraphParameterSet;
import dev.latvian.mods.rhino.util.HideFromJS;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.Cancelable;
import org.jetbrains.annotations.Nullable;

@Getter
@Cancelable
@LDLRegister(name = "TransferProxyRecipeEvent", group = "RecipeTypeEvent")
public class TransferProxyRecipeEvent extends RecipeTypeEvent {

    @GraphParameterGet
    public final ResourceLocation proxyTypeId;
    @GraphParameterGet
    public final RecipeType<?> proxyType;
    @GraphParameterGet
    public final ResourceLocation proxyRecipeId;
    @GraphParameterGet
    public final Recipe<?> proxyRecipe;
    @Nullable
    @GraphParameterSet(identity = "drops.out")
    public MBDRecipe mbdRecipe;

    public TransferProxyRecipeEvent(MBDRecipeType recipeType, ResourceLocation proxyTypeId, RecipeType<?> proxyType, ResourceLocation proxyRecipeId, Recipe<?> proxyRecipe, @Nullable MBDRecipe mbdRecipe) {
        super(recipeType);
        this.proxyTypeId = proxyTypeId;
        this.proxyType = proxyType;
        this.proxyRecipe = proxyRecipe;
        this.proxyRecipeId = proxyRecipeId;
        this.mbdRecipe = mbdRecipe;
    }

    @HideFromJS
    public MBDRecipeBuilder recipeBuilder() {
        return recipeType.getRecipeBuilder();
    }
}
