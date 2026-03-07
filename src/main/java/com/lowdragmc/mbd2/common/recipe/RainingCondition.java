package com.lowdragmc.mbd2.common.recipe;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.math.Range;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

/**
 * @author KilaBash
 * @date 2022/05/27
 * @implNote WhetherCondition, specific whether
 */
@Getter
@Setter
@NoArgsConstructor
@LDLRegister(name = "rain", registry = "mbd2:recipe_condition")
public class RainingCondition extends RecipeCondition {
    @Configurable(name = "config.recipe.condition.rain")
    @ConfigNumber(range = {0f, 1f}, type = ConfigNumber.Type.FLOAT)
    private Range rain = Range.of(0f, 1f);

    public RainingCondition(float minLevel, float maxLevel) {
        this(false, minLevel, maxLevel);
    }

    public RainingCondition(boolean isReverse, float minLevel, float maxLevel) {
        super(isReverse);
        this.rain = Range.of(minLevel, maxLevel);
    }

    @Override
    public Component getTooltips() {
        return Component.translatable("recipe.condition.rain.tooltip",
                rain.getMin().floatValue(), rain.getMax().floatValue());
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        Level level = recipeLogic.machine.getLevel();
        return level != null &&
                level.getRainLevel(1) >= this.rain.getMin().floatValue() &&
                level.getRainLevel(1) <= this.rain.getMax().floatValue();
    }

}
