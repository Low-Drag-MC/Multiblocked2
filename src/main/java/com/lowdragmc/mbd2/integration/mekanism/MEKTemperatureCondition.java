package com.lowdragmc.mbd2.integration.mekanism;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.integration.mekanism.trait.heat.MekHeatCapabilityTrait;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;

@Getter
@Setter
@NoArgsConstructor
@LDLRegister(name = "mekanism_heat", registry = "mbd2:recipe_condition", modID = "mekanism")
public class MEKTemperatureCondition extends RecipeCondition {

    @Configurable(name = "config.recipe.condition.temperature.min")
    @ConfigNumber(range = {-Float.MAX_VALUE, Float.MAX_VALUE})
    private double minTemperature;

    @Configurable(name = "config.recipe.condition.temperature.max")
    @ConfigNumber(range = {-Float.MAX_VALUE, Float.MAX_VALUE})
    private double maxTemperature = Float.MAX_VALUE;

    public MEKTemperatureCondition(double minTemperature, double maxTemperature) {
        this.minTemperature = minTemperature;
        this.maxTemperature = maxTemperature;
    }

    @Override
    public String getType() {
        return "mekanism_heat";
    }

    @Override
    public Component getTooltips() {
        return Component.translatable("recipe.condition.mekanism_heat.tooltip", minTemperature, maxTemperature);
    }

    @Override
    public IGuiTexture getIcon() {
        return SpriteTexture.of("mbd2:textures/gui/thermometer.png");
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        if (!(recipeLogic.machine instanceof com.lowdragmc.mbd2.common.machine.MBDMachine machine)) return false;
        for (var trait : machine.getRecipeLogicTraits()) {
            if (trait instanceof MekHeatCapabilityTrait heatTrait) {
                double temp = heatTrait.getStorage().getTemperature(0);
                if (temp >= minTemperature && temp <= maxTemperature) {
                    return true;
                }
            }
        }
        return false;
    }
}
