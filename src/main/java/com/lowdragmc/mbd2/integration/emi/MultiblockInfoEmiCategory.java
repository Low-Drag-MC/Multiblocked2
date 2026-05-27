package com.lowdragmc.mbd2.integration.emi;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.integration.xei.emi.ModularUIEMIRecipe;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.pattern.PatternPreview;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.machine.definition.MultiblockMachineDefinition;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MultiblockInfoEmiCategory extends EmiRecipeCategory {

    public static final MultiblockInfoEmiCategory CATEGORY = new MultiblockInfoEmiCategory();

    public static final int WIDTH = PatternPreview.WIDTH;
    public static final int HEIGHT = PatternPreview.HEIGHT;

    public MultiblockInfoEmiCategory() {
        super(MBD2.id("multiblock_info"), EmiStack.of(Items.STRUCTURE_BLOCK));
    }

    public static class MultiblockInfoEmiRecipe extends ModularUIEMIRecipe {
        private final MultiblockMachineDefinition definition;

        public MultiblockInfoEmiRecipe(MultiblockMachineDefinition definition) {
            super(self -> ModularUI.of(UI.of(PatternPreview.create(
                    ((MultiblockInfoEmiRecipe) self).definition), StylesheetManager.ORE_MERGED)));
            this.definition = definition;
        }

        @Override
        public EmiRecipeCategory getCategory() {
            return CATEGORY;
        }

        @Override
        public @Nullable ResourceLocation getId() {
            return MBD2.id("multiblock_info/" + definition.id().getNamespace() + "/" + definition.id().getPath());
        }

        @Override
        public int getDisplayWidth() {
            return WIDTH;
        }

        @Override
        public int getDisplayHeight() {
            return HEIGHT;
        }
    }

    public static void registerDisplays(EmiRegistry registry) {
        for (var definition : MBDRegistries.MACHINE_DEFINITIONS) {
            if (definition instanceof MultiblockMachineDefinition mb) {
                registry.addRecipe(new MultiblockInfoEmiRecipe(mb));
            }
        }
    }

    public static void registerWorkstations(EmiRegistry registry) {
        for (var definition : MBDRegistries.MACHINE_DEFINITIONS) {
            if (definition instanceof MultiblockMachineDefinition mb && mb.item() != null) {
                registry.addWorkstation(CATEGORY, EmiStack.of(mb.item()));
            }
        }
    }
}
