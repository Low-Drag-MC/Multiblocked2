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
@LDLRegister(name = "thunder", registry = "mbd2:recipe_condition")
public class ThunderCondition extends RecipeCondition {
    @Configurable(name = "config.recipe.condition.thunder")
    @ConfigNumber(range = {0f, 1f}, type = ConfigNumber.Type.FLOAT)
    private Range thunder = Range.of(0f, 1f);

    public ThunderCondition(float minLevel, float maxLevel) {
        this(false, minLevel, maxLevel);
    }

    public ThunderCondition(boolean isReverse, float minLevel, float maxLevel) {
        super(isReverse);
        this.thunder = Range.of(minLevel, maxLevel);
    }

    @Override
    public String getType() {
        return "thunder";
    }

    @Override
    public Component getTooltips() {
        return Component.translatable("recipe.condition.thunder.tooltip",
                thunder.getMin().floatValue(),
                thunder.getMax().floatValue());
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        Level level = recipeLogic.machine.getLevel();
        return level != null &&
                level.getThunderLevel(1) >= this.thunder.getMin().floatValue() &&
                level.getThunderLevel(1) <= this.thunder.getMax().floatValue();
    }

}
