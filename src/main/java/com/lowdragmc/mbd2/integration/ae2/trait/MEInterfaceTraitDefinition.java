package com.lowdragmc.mbd2.integration.ae2.trait;

import appeng.core.definitions.AEBlocks;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTraitDefinition;
import com.lowdragmc.mbd2.utils.WidgetUtils;
import lombok.Getter;
import lombok.Setter;

@LDLRegister(name = "ae2_me_interface", group = "trait", modID = "ae2")
public class MEInterfaceTraitDefinition extends SimpleCapabilityTraitDefinition {
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.ae2_me_interface.slot_size")
    @NumberRange(range = {1, Integer.MAX_VALUE})
    private int slotSize = 9;

    @Override
    public SimpleCapabilityTrait createTrait(MBDMachine machine) {
        return new MEInterfaceTrait(machine, this);
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(AEBlocks.INTERFACE.asItem());
    }

    @Override
    public boolean allowMultiple() {
        return false;
    }

    @Override
    public void createTraitUITemplate(WidgetGroup ui) {
        var row = slotSize <= 9 ? slotSize : Math.ceil(Math.sqrt(slotSize));
        var prefix = uiPrefixName();
        for (var i = 0; i < this.slotSize; i++) {
            var slotWidget = new AEInterfaceSlotWidget();
            slotWidget.setSelfPosition(new Position(10 + i % (int) row * 18, 10 + i / (int) row * 72));
            slotWidget.initTemplate();
            slotWidget.setId(prefix + "_" + i);
            ui.addWidget(slotWidget);
        }
    }

    @Override
    public void initTraitUI(ITrait trait, WidgetGroup group) {
        if (trait instanceof MEInterfaceTrait interfaceTrait) {
            var prefix = uiPrefixName();
            var guiIO = getGuiIO();
            var ingredientIO = guiIO == IO.IN ? IngredientIO.INPUT : guiIO == IO.OUT ? IngredientIO.OUTPUT : guiIO == IO.BOTH ? IngredientIO.BOTH : IngredientIO.RENDER_ONLY;
            WidgetUtils.widgetByIdForEach(group, "^%s_[0-9]+$".formatted(prefix), AEInterfaceSlotWidget.class, slotWidget -> {
                var index = WidgetUtils.widgetIdIndex(slotWidget);
                if (index >= 0 && index < slotSize) {
                    slotWidget.setItemInterfaceLogic(interfaceTrait.getInterfaceLogic(), index);
                    slotWidget.setIngredientIO(ingredientIO);
                    slotWidget.setCanTakeItems(guiIO.support(IO.OUT));
                    slotWidget.setCanPutItems(guiIO.support(IO.IN));
                }
            });
        }
    }

}
