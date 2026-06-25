package com.lowdragmc.mbd2.integration.mekanism.trait.chemical;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.FillDirection;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.common.gui.MBDSprites;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTraitDefinition;
import com.lowdragmc.mbd2.common.trait.ToggleAutoIO;
import com.lowdragmc.mbd2.common.trait.TraitDefinitionType;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.integration.mekanism.ChemicalSlot;
import lombok.Getter;
import lombok.Setter;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.common.registries.MekanismBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ChemicalTankCapabilityTraitDefinition extends SimpleCapabilityTraitDefinition<IChemicalHandler, @Nullable Direction> {
    @LDLRegister(name = "chemical_tank", registry = "mbd2:trait_definition_type", group = "trait", priority = -100, modID = "mekanism")
    public static final SimpleCapabilityTraitDefinition.Type<IChemicalHandler, @Nullable Direction, ChemicalTankCapabilityTraitDefinition> TYPE =
            new SimpleCapabilityTraitDefinition.Type<>("chemical_tank", "trait") {
                @Override
                public ChemicalTankCapabilityTraitDefinition createDefinition() {
                    return new ChemicalTankCapabilityTraitDefinition();
                }

                @Override
                protected BlockCapability<IChemicalHandler, @Nullable Direction> getCapability() {
                    return mekanism.common.capabilities.Capabilities.CHEMICAL.block();
                }

                @Override
                protected IChemicalHandler merge(List<IChemicalHandler> contents) {
                    return new ChemicalHandlerList(contents.toArray(IChemicalHandler[]::new));
                }
            };

    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.chemical_tank.tank_size")
    @ConfigNumber(range = {1, Integer.MAX_VALUE})
    private int tankSize = 1;

    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.chemical_tank.capacity")
    @ConfigNumber(range = {1, Long.MAX_VALUE})
    private long capacity = 16000;

    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.chemical_tank.allow_same_chemicals", tips = "config.definition.trait.chemical_tank.allow_same_chemicals.tooltip")
    private boolean allowSameChemicals = true;

    @Getter
    @Configurable(name = "config.definition.trait.chemical_tank.filter", subConfigurable = true, tips = "config.definition.trait.chemical_tank.filter.tooltip")
    private final ChemicalFilterSettings filterSettings = new ChemicalFilterSettings();

    @Getter
    @Configurable(name = "config.definition.trait.auto_io", subConfigurable = true, tips = "config.definition.trait.chemical_tank.auto_io.tooltip")
    private final ToggleAutoIO autoIO = new ToggleAutoIO();

    @Getter
    @Configurable(name = "config.definition.trait.chemical_tank.fancy_renderer", subConfigurable = true,
            tips = "config.definition.trait.chemical_tank.fancy_renderer.tooltip")
    private final ChemicalFancyRendererSettings fancyRendererSettings = new ChemicalFancyRendererSettings(this);

    @Override
    public ChemicalTankCapabilityTrait createTrait(MBDMachine machine) {
        return new ChemicalTankCapabilityTrait(machine, this);
    }

    @Override
    public TraitDefinitionType<?> type() {
        return TYPE;
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(new ItemStack(MekanismBlocks.BASIC_CHEMICAL_TANK));
    }

    @Override
    public IRenderer getBESRenderer(IMachine machine) {
        return fancyRendererSettings.getFancyRenderer(machine);
    }

    @Override
    public void createTraitUITemplate(UIElement container) {
        var prefix = uiId();
        for (int i = 0; i < tankSize; i++) {
            var slot = new ChemicalSlot();
            slot.getLayout().width(20).height(58);
            slot.getStyle().overlay(MBDSprites.FLUID_SLOT_OVERLAY);
            slot.setFillDirection(FillDirection.DOWN_TO_UP);
            slot.amountLabel.setDisplay(false);
            slot.setId(prefix + "_" + i);
            container.addChild(slot);
        }
    }

    @Override
    public void initTraitUI(ITrait trait, UI ui) {
        if (trait instanceof ChemicalTankCapabilityTrait chemicalTrait) {
            var prefix = uiId();
            ui.selectRegex("^%s_[0-9]+$".formatted(prefix), ChemicalSlot.class).forEach(slot -> {
                var idStr = slot.getId();
                var lastUnderscore = idStr.lastIndexOf('_');
                if (lastUnderscore < 0) return;
                int index;
                try {
                    index = Integer.parseInt(idStr.substring(lastUnderscore + 1));
                } catch (NumberFormatException e) {
                    return;
                }
                if (index >= 0 && index < chemicalTrait.storages.length) {
                    slot.setCapacity(chemicalTrait.storages[index].getCapacity());
                    slot.bind(chemicalTrait.getCapContent(getGuiIO()), index);
                }
            });
        }
    }
}
