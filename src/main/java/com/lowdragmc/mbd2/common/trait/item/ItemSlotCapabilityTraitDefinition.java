package com.lowdragmc.mbd2.common.trait.item;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.common.capability.recipe.ItemRecipeCapability;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTraitDefinition;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

@LDLRegister(name = "item_slot", group = "trait")
public class ItemSlotCapabilityTraitDefinition extends SimpleCapabilityTraitDefinition<IItemHandler, Ingredient> {

    @Getter @Setter
    @Configurable(name = "config.definition.trait.item_slot.slot_size", tips = "config.definition.trait.item_slot.slot_size.tooltip")
    @NumberRange(range = {1, Integer.MAX_VALUE})
    private int slotSize = 1;

    public ItemSlotCapabilityTraitDefinition() {
        this.setName("item trait");
    }

    @Override
    public SimpleCapabilityTrait<IItemHandler, Ingredient> createTrait(MBDMachine machine) {
        return new ItemSlotCapabilityTrait(machine, this);
    }

    @Override
    public IGuiTexture getIcon() {
        return new GuiTextureGroup(
                new ResourceTexture("ldlib:textures/gui/slot.png"),
                new ItemStackTexture(Items.IRON_INGOT).scale(16f / 18)
        );
    }

    @Override
    public RecipeCapability<Ingredient> getRecipeCapability() {
        return ItemRecipeCapability.CAP;
    }

    @Override
    public Capability<IItemHandler> getCapability() {
        return ForgeCapabilities.ITEM_HANDLER;
    }

}
