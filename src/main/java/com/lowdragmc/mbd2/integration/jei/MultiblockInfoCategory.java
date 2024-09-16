package com.lowdragmc.mbd2.integration.jei;

import com.lowdragmc.lowdraglib.jei.ModularUIRecipeCategory;
import com.lowdragmc.lowdraglib.jei.ModularWrapper;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.pattern.PatternPreviewWidget;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.machine.definition.MultiblockMachineDefinition;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class MultiblockInfoCategory extends ModularUIRecipeCategory<MultiblockInfoCategory.MultiblockInfoWrapper> {

    public static class MultiblockInfoWrapper extends ModularWrapper<PatternPreviewWidget> {

        public final MultiblockMachineDefinition definition;

        public MultiblockInfoWrapper(MultiblockMachineDefinition definition) {
            super(PatternPreviewWidget.getPatternWidget(definition));
            this.definition = definition;
        }
    }

    public final static RecipeType<MultiblockInfoWrapper> RECIPE_TYPE = new RecipeType<>(MBD2.id("multiblock_info"),
            MultiblockInfoWrapper.class);
    private final IDrawable background;
    private final IDrawable icon;

    public MultiblockInfoCategory(IJeiHelpers helpers) {
        IGuiHelper guiHelper = helpers.getGuiHelper();
        this.background = guiHelper.createBlankDrawable(160, 160);
        this.icon = helpers.getGuiHelper().drawableBuilder(MBD2.id("textures/gui/multiblock_info_page.png"), 0, 0, 16, 16).setTextureSize(16, 16).build();
    }

    public static void registerRecipes(IRecipeRegistration registry) {
        registry.addRecipes(RECIPE_TYPE, MBDRegistries.MACHINE_DEFINITIONS.values().stream()
                .filter(MultiblockMachineDefinition.class::isInstance)
                .map(MultiblockMachineDefinition.class::cast)
                .map(MultiblockInfoWrapper::new)
                .toList());
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (var definition : MBDRegistries.MACHINE_DEFINITIONS.values()) {
            if (definition instanceof MultiblockMachineDefinition multiblockDefinition) {
                registration.addRecipeCatalyst(multiblockDefinition.asStack(), RECIPE_TYPE);
            }
        }
    }

    @Override
    @NotNull
    public RecipeType<MultiblockInfoWrapper> getRecipeType() {
        return RECIPE_TYPE;
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Component.translatable("mbd2.jei.multiblock_info");
    }

    @NotNull
    @Override
    public IDrawable getBackground() {
        return background;
    }

    @NotNull
    @Override
    public IDrawable getIcon() {
        return icon;
    }
}
