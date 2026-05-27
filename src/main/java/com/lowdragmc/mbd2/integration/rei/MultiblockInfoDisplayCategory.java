package com.lowdragmc.mbd2.integration.rei;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.integration.xei.rei.ModularUIDisplay;
import com.lowdragmc.lowdraglib2.integration.xei.rei.ModularUIDisplayCategory;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.pattern.PatternPreview;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.machine.definition.MultiblockMachineDefinition;
import lombok.Getter;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MultiblockInfoDisplayCategory
        extends ModularUIDisplayCategory<MultiblockInfoDisplayCategory.MultiblockInfoDisplay> {

    public static final CategoryIdentifier<MultiblockInfoDisplay> CATEGORY =
            CategoryIdentifier.of(MBD2.id("multiblock_info"));

    public static final int WIDTH = PatternPreview.WIDTH;
    public static final int HEIGHT = PatternPreview.HEIGHT;

    @Getter
    private final Renderer icon;

    public MultiblockInfoDisplayCategory() {
        super(display -> ModularUI.of(UI.of(PatternPreview.create(display.definition), StylesheetManager.ORE_MERGED)));
        this.icon = EntryStacks.of(Items.STRUCTURE_BLOCK);
    }

    @Override
    public CategoryIdentifier<MultiblockInfoDisplay> getCategoryIdentifier() {
        return CATEGORY;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("multiblocked.multiblock_info");
    }

    @Override
    public int getDisplayWidth(MultiblockInfoDisplay display) {
        return WIDTH;
    }

    @Override
    public int getDisplayHeight() {
        return HEIGHT;
    }

    public static void registerDisplays(DisplayRegistry registry) {
        for (var definition : MBDRegistries.MACHINE_DEFINITIONS) {
            if (definition instanceof MultiblockMachineDefinition mb) {
                registry.add(new MultiblockInfoDisplay(mb));
            }
        }
    }

    public static void registerWorkstations(CategoryRegistry registry) {
        for (var definition : MBDRegistries.MACHINE_DEFINITIONS) {
            if (definition instanceof MultiblockMachineDefinition mb && mb.item() != null) {
                registry.addWorkstations(CATEGORY, EntryStacks.of(mb.item()));
            }
        }
    }

    public static class MultiblockInfoDisplay implements ModularUIDisplay {
        public final MultiblockMachineDefinition definition;

        public MultiblockInfoDisplay(MultiblockMachineDefinition definition) {
            this.definition = definition;
        }

        @Override
        public CategoryIdentifier<?> getCategoryIdentifier() {
            return CATEGORY;
        }
    }
}
