package com.lowdragmc.mbd2.common.trait.fluid;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.TankWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.api.recipe.ingredient.FluidIngredient;
import com.lowdragmc.mbd2.common.capability.recipe.FluidRecipeCapability;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTraitDefinition;
import com.lowdragmc.mbd2.utils.WidgetUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;

@LDLRegister(name = "fluid_tank", group = "trait", priority = -100)
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
    @Getter
    @Configurable(name = "config.definition.trait.fluid_tank.filter", subConfigurable = true, tips = "config.definition.trait.fluid_tank.filter.tooltip")
    private final FluidFilterSettings fluidFilterSettings = new FluidFilterSettings();
    @Configurable(name = "config.definition.trait.fluid_tank.fancy_renderer", subConfigurable = true,
            tips = {"config.definition.trait.fluid_tank.fancy_renderer.tooltip.0", "config.definition.trait.fluid_tank.fancy_renderer.tooltip.1"})
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
    public IRenderer getBESRenderer(IMachine machine) {
        return fancyRendererSettings.getFancyRenderer(machine);
    }

    @Override
    public void createTraitUITemplate(WidgetGroup ui) {
        var prefix = uiPrefixName();
        for (var i = 0; i < this.tankSize; i++) {
            var tankWidget = new TankWidget();
            tankWidget.initTemplate();
            tankWidget.setSelfPosition(new Position(10 + i * 20, 10));
            tankWidget.setSize(new Size(20, 58));
            tankWidget.setOverlay(new ResourceTexture("mbd2:textures/gui/fluid_tank_overlay.png"));
            tankWidget.setId(prefix + "_" + i);
            tankWidget.setShowAmount(false);
            ui.addWidget(tankWidget);
        }
    }

    @Override
    public void initTraitUI(ITrait trait, WidgetGroup group) {
        if (trait instanceof FluidTankCapabilityTrait fluidTankTrait) {
            var prefix = uiPrefixName();
            var guiIO = getGuiIO();
            var ingredientIO = guiIO == IO.IN ? IngredientIO.INPUT : guiIO == IO.OUT ? IngredientIO.OUTPUT : guiIO == IO.BOTH ? IngredientIO.BOTH : IngredientIO.RENDER_ONLY;
            var allowClickDrained = guiIO == IO.BOTH || guiIO == IO.OUT;
            var allowClickFilled = guiIO == IO.BOTH || guiIO == IO.IN;
            WidgetUtils.widgetByIdForEach(group, "^%s_[0-9]+$".formatted(prefix), TankWidget.class, tankWidget -> {
                var index = WidgetUtils.widgetIdIndex(tankWidget);
                if (index >= 0 && index < fluidTankTrait.storages.length) {
                    tankWidget.setFluidTank(fluidTankTrait.storages[index]);
                    tankWidget.setIngredientIO(ingredientIO);
                    tankWidget.setAllowClickDrained(allowClickDrained);
                    tankWidget.setAllowClickFilled(allowClickFilled);
                }
            });
        }
    }
}
