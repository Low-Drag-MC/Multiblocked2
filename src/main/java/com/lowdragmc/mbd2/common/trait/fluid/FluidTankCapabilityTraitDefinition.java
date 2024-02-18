package com.lowdragmc.mbd2.common.trait.fluid;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.ingredient.FluidIngredient;
import com.lowdragmc.mbd2.common.capability.recipe.FluidRecipeCapability;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTraitDefinition;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;

@LDLRegister(name = "fluid_tank", group = "trait")
public class FluidTankCapabilityTraitDefinition extends SimpleCapabilityTraitDefinition<IFluidHandler, FluidIngredient> {

    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.fluid_tank.tank_size", tips = "config.definition.trait.fluid_tank.tank_size.tooltip")
    @NumberRange(range = {1, Integer.MAX_VALUE})
    private int tankSize = 1;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.fluid_tank.capacity")
    @NumberRange(range = {1, Integer.MAX_VALUE})
    private int capacity = 1000;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.fluid_tank.allow_same_fluids", tips = "config.definition.trait.fluid_tank.allow_same_fluids.tooltip")
    private boolean allowSameFluids = true;
    @Configurable(name = "config.definition.trait.fluid_tank.fancy_renderer", subConfigurable = true, tips = "config.definition.trait.fluid_tank.fancy_renderer.tooltip")
    private final FluidFancyRendererSettings fancyRendererSettings = new FluidFancyRendererSettings(this);

    @Override
    public SimpleCapabilityTrait<IFluidHandler, FluidIngredient> createTrait(MBDMachine machine) {
        return new FluidTankCapabilityTrait(machine, this);
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(Items.WATER_BUCKET);
    }

    @Override
    public RecipeCapability<FluidIngredient> getRecipeCapability() {
        return FluidRecipeCapability.CAP;
    }

    @Override
    public Capability<IFluidHandler> getCapability() {
        return ForgeCapabilities.FLUID_HANDLER;
    }

    @Override
    public IRenderer getBESRenderer() {
        return fancyRendererSettings.createRenderer();
    }
}
