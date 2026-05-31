package com.lowdragmc.mbd2.integration.pneumaticcraft.trait.pressure;

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
@LDLRegister(name = "pneumatic_pressure", registry = "mbd2:recipe_condition", modID = "pneumaticcraft")
public class PNCPressureCondition extends RecipeCondition {

    @Configurable(name = "config.recipe.condition.pneumatic_pressure.is_air",
            tips = "recipe.capability.pneumatic_pressure_air.is_air.tooltip")
    private boolean isAir;

    @Configurable(name = "config.recipe.condition.pneumatic_pressure.min")
    @ConfigNumber(range = {-Float.MAX_VALUE, Float.MAX_VALUE})
    private float minValue = 1f;

    @Configurable(name = "config.recipe.condition.pneumatic_pressure.max")
    @ConfigNumber(range = {-Float.MAX_VALUE, Float.MAX_VALUE})
    private float maxValue = 10f;

    public PNCPressureCondition(boolean isAir, float minValue, float maxValue) {
        this.isAir = isAir;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    @Override
    public String getType() {
        return "pneumatic_pressure";
    }

    @Override
    public Component getTooltips() {
        return isAir
                ? Component.translatable("recipe.condition.pneumatic_pressure.air.tooltip", minValue, maxValue)
                : Component.translatable("recipe.condition.pneumatic_pressure.pressure.tooltip", minValue, maxValue);
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(new ItemStack(ModItems.PRESSURE_GAUGE.get()));
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        if (!(recipeLogic.machine instanceof MBDMachine machine)) return false;
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof PNCPressureAirHandlerTrait pressureTrait) {
                var handler = pressureTrait.getHandler();
                if (isAir) {
                    int air = handler.getAir();
                    return air >= minValue && air <= maxValue;
                } else {
                    float pressure = handler.getPressure();
                    return pressure >= minValue && pressure <= maxValue;
                }
            }
        }
        return false;
    }
}
