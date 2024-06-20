package com.lowdragmc.mbd2.common.trait.item;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.common.capability.recipe.ItemRecipeCapability;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTraitDefinition;
import com.lowdragmc.mbd2.utils.WidgetUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

@LDLRegister(name = "item_slot", group = "trait", priority = -100)
public class ItemSlotCapabilityTraitDefinition extends SimpleCapabilityTraitDefinition<IItemHandler, Ingredient> {

    @Getter @Setter
    @Configurable(name = "config.definition.trait.item_slot.slot_size", tips = "config.definition.trait.item_slot.slot_size.tooltip")
    @NumberRange(range = {1, Integer.MAX_VALUE})
    private int slotSize = 1;
    @Getter @Setter
    @Configurable(name = "config.definition.trait.item_slot.slot_limit", tips = "config.definition.trait.item_slot.slot_limit.tooltip")
    @NumberRange(range = {1, 64})
    private int slotLimit = 64;
    @Getter
    @Configurable(name = "config.definition.trait.item_slot.filter", subConfigurable = true, tips = "config.definition.trait.item_slot.filter.tooltip")
    private final ItemFilterSettings itemFilterSettings = new ItemFilterSettings();
    @Getter
    @Configurable(name = "config.definition.trait.item_slot.fancy_renderer", subConfigurable = true, tips = "config.definition.trait.item_slot.fancy_renderer.tooltip")
    private final ItemFancyRendererSettings itemRendererSettings = new ItemFancyRendererSettings(this);

    @Override
    public ItemSlotCapabilityTrait createTrait(MBDMachine machine) {
        return new ItemSlotCapabilityTrait(machine, this);
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(Items.CHEST);
    }

    @Override
    public RecipeCapability<Ingredient> getRecipeCapability() {
        return ItemRecipeCapability.CAP;
    }

    @Override
    public Capability<IItemHandler> getCapability() {
        return ForgeCapabilities.ITEM_HANDLER;
    }

    @Override
    public IRenderer getBESRenderer() {
        return itemRendererSettings.createRenderer();
    }

    @Override
    public void createTraitUITemplate(WidgetGroup ui) {
        var row = Math.ceil(Math.sqrt(slotSize));
        var prefix = uiPrefixName();
        for (var i = 0; i < this.slotSize; i++) {
            var slotWidget = new SlotWidget();
            slotWidget.setSelfPosition(new Position(10 + i % (int) row * 18, 10 + i / (int) row * 18));
            slotWidget.initTemplate();
            slotWidget.setId(prefix + "_" + i);
            ui.addWidget(slotWidget);
        }
    }

    @Override
    public void initTraitUI(ITrait trait, WidgetGroup group) {
        if (trait instanceof ItemSlotCapabilityTrait itemSlotTrait) {
            var prefix = uiPrefixName();
            var guiIO = getGuiIO();
            var ingredientIO = guiIO == IO.IN ? IngredientIO.INPUT : guiIO == IO.OUT ? IngredientIO.OUTPUT : guiIO == IO.BOTH ? IngredientIO.BOTH : IngredientIO.RENDER_ONLY;
            var canTakeItems = guiIO == IO.BOTH || guiIO == IO.OUT;
            var canPutItems = guiIO == IO.BOTH || guiIO == IO.IN;
            WidgetUtils.widgetByIdForEach(group, "^%s_[0-9]+$".formatted(prefix), SlotWidget.class, slotWidget -> {
                var index = WidgetUtils.widgetIdIndex(slotWidget);
                if (index >= 0 && index < itemSlotTrait.storage.getSlots()) {
                    slotWidget.setHandlerSlot(itemSlotTrait.storage, index);
                    slotWidget.setIngredientIO(ingredientIO);
                    slotWidget.setCanTakeItems(canTakeItems);
                    slotWidget.setCanPutItems(canPutItems);
                }
            });
        }
    }
}
