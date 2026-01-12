package com.lowdragmc.mbd2.integration.rei;

import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.rei.IGui2Renderer;
import com.lowdragmc.lowdraglib.rei.ModularDisplay;
import com.lowdragmc.lowdraglib.rei.ModularUIDisplayCategory;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.pattern.PatternPreviewWidget;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.machine.definition.MultiblockMachineDefinition;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public class MultiblockInfoDisplayCategory extends ModularUIDisplayCategory<MultiblockInfoDisplayCategory.MultiblockInfoDisplay> {

    public static class MultiblockInfoDisplay extends ModularDisplay<WidgetGroup> {

        public final MultiblockMachineDefinition definition;

        public MultiblockInfoDisplay(MultiblockMachineDefinition definition) {
            super(() -> PatternPreviewWidget.getPatternWidget(definition), MultiblockInfoDisplayCategory.CATEGORY);
            this.definition = definition;
        }

        @Override
        public Optional<ResourceLocation> getDisplayLocation() {
            return Optional.of(definition.id());
        }

        @Override
        public void clearSlotWidgetHandler(SlotWidget slotW, int slotIndex) {
            slotW.setDrawHoverOverlay(false);
        }
    }

    public static final CategoryIdentifier<MultiblockInfoDisplay> CATEGORY = CategoryIdentifier
            .of(MBD2.id("multiblock_info"));
    private final Renderer icon;

    public MultiblockInfoDisplayCategory() {
        this.icon = IGui2Renderer.toDrawable(new ResourceTexture("mbd2:textures/gui/multiblock_info_page.png"));
    }

    public static void registerDisplays(DisplayRegistry registry) {
        MBDRegistries.MACHINE_DEFINITIONS.values().stream()
                .filter(MultiblockMachineDefinition.class::isInstance)
                .map(MultiblockMachineDefinition.class::cast)
                .map(MultiblockInfoDisplay::new)
                .forEach(registry::add);
    }

    public static void registerWorkStations(CategoryRegistry registry) {
        for (var definition : MBDRegistries.MACHINE_DEFINITIONS.values()) {
            if (definition instanceof MultiblockMachineDefinition multiblockDefinition) {
                registry.addWorkstations(CATEGORY, EntryStacks.of(multiblockDefinition.asStack()));
            }
        }
    }

    @Override
    public int getDisplayHeight() {
        return 160 + 8;
    }

    @Override
    public int getDisplayWidth(MultiblockInfoDisplay display) {
        return 160 + 8;
    }

    @Override
    public CategoryIdentifier<? extends MultiblockInfoDisplay> getCategoryIdentifier() {
        return CATEGORY;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("mbd2.jei.multiblock_info");
    }

    @Override
    public Renderer getIcon() {
        return icon;
    }
}
