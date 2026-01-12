package com.lowdragmc.mbd2.integration.emi;

import com.lowdragmc.lowdraglib.emi.ModularEmiRecipe;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.pattern.PatternPreviewWidget;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.machine.definition.MultiblockMachineDefinition;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class MultiblockInfoEmiCategory extends EmiRecipeCategory {

    public static class MultiblockInfoEmiRecipe extends ModularEmiRecipe<WidgetGroup> {

        public final MultiblockMachineDefinition definition;

        public MultiblockInfoEmiRecipe(MultiblockMachineDefinition definition) {
            super(() -> PatternPreviewWidget.getPatternWidget(definition));
            this.definition = definition;
        }

        @Override
        public EmiRecipeCategory getCategory() {
            return MultiblockInfoEmiCategory.CATEGORY;
        }

        @Override
        public @Nullable ResourceLocation getId() {
            return definition.id();
        }

        @Override
        public void clearSlotWidgetHandler(SlotWidget slotW, int slotIndex) {
            slotW.setDrawHoverOverlay(false);
        }
    }

    public static final MultiblockInfoEmiCategory CATEGORY = new MultiblockInfoEmiCategory();

    private MultiblockInfoEmiCategory() {
        super(MBD2.id("multiblock_info"), new EmiTexture(MBD2.id("textures/gui/multiblock_info_page.png"), 0, 0, 16, 16, 16, 16, 16, 16));
    }

    public static void registerDisplays(EmiRegistry registry) {
        MBDRegistries.MACHINE_DEFINITIONS.values().stream()
                .filter(MultiblockMachineDefinition.class::isInstance)
                .map(MultiblockMachineDefinition.class::cast)
                .map(MultiblockInfoEmiRecipe::new)
                .forEach(registry::addRecipe);
    }

    public static void registerWorkStations(EmiRegistry registry) {
        for (var definition : MBDRegistries.MACHINE_DEFINITIONS.values()) {
            if (definition instanceof MultiblockMachineDefinition multiblockDefinition) {
                registry.addWorkstation(CATEGORY, EmiStack.of(multiblockDefinition.asStack()));
            }
        }
    }

    @Override
    public Component getName() {
        return Component.translatable("mbd2.jei.multiblock_info");
    }
}
