package com.lowdragmc.mbd2.integration.pneumaticcraft.trait.heat;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.desht.pneumaticcraft.common.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

@Getter
@Setter
@NoArgsConstructor
@LDLRegister(name = "pneumatic_temperature", registry = "mbd2:recipe_condition", modID = "pneumaticcraft")
public class PNCTemperatureCondition extends RecipeCondition {

    @Configurable(name = "config.recipe.condition.pneumatic_temperature.min")
    @ConfigNumber(range = {-Float.MAX_VALUE, Float.MAX_VALUE})
    private double minTemperature;

    @Configurable(name = "config.recipe.condition.pneumatic_temperature.max")
    @ConfigNumber(range = {-Float.MAX_VALUE, Float.MAX_VALUE})
    private double maxTemperature = Float.MAX_VALUE;

    public PNCTemperatureCondition(double minTemperature, double maxTemperature) {
        this.minTemperature = minTemperature;
        this.maxTemperature = maxTemperature;
    }

    @Override
    public String getType() {
        return "pneumatic_temperature";
    }

    @Override
    public Component getTooltips() {
        return Component.translatable("recipe.condition.pneumatic_temperature.tooltip", minTemperature, maxTemperature);
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(new ItemStack(ModItems.HEAT_FRAME.get()));
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        if (!(recipeLogic.machine instanceof MBDMachine machine)) return false;
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof PNCHeatExchangerTrait heatTrait) {
                double tempC = heatTrait.getHandler().getTemperature() - 273;
                if (tempC >= minTemperature && tempC <= maxTemperature) {
                    return true;
                }
            }
        }
        return false;
    }
}
