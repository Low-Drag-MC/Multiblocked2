package com.lowdragmc.mbd2.integration.jei;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.integration.xei.jei.ModularUIRecipeCategory;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.pattern.PatternPreview;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.machine.definition.MultiblockMachineDefinition;
import lombok.Getter;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MultiblockInfoCategory extends ModularUIRecipeCategory<MultiblockMachineDefinition> {

    public static final RecipeType<MultiblockMachineDefinition> RECIPE_TYPE =
            new RecipeType<>(MBD2.id("multiblock_info"), MultiblockMachineDefinition.class);

    public static final int WIDTH = PatternPreview.WIDTH;
    public static final int HEIGHT = PatternPreview.HEIGHT;

    @Getter
    private final IDrawable icon;

    public MultiblockInfoCategory(IJeiHelpers helpers) {
        super(def -> ModularUI.of(UI.of(PatternPreview.create(def), StylesheetManager.ORE_MERGED)));
        this.icon = helpers.getGuiHelper().createDrawableItemLike(Items.STRUCTURE_BLOCK);
    }

    @Override
    public RecipeType<MultiblockMachineDefinition> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("multiblocked.multiblock_info");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        var defs = new ArrayList<MultiblockMachineDefinition>();
        for (var definition : MBDRegistries.MACHINE_DEFINITIONS) {
            if (definition instanceof MultiblockMachineDefinition mb) {
                defs.add(mb);
            }
        }
        registration.addRecipes(RECIPE_TYPE, defs);
    }

    public static void registerCatalysts(IRecipeCatalystRegistration registration) {
        for (var definition : MBDRegistries.MACHINE_DEFINITIONS) {
            if (definition instanceof MultiblockMachineDefinition mb && mb.item() != null) {
                registration.addRecipeCatalyst(new ItemStack(mb.item()), RECIPE_TYPE);
            }
        }
    }
}
