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

import javax.annotation.Nonnull;

/**
 * @author KilaBash
 * @date 2022/05/27
 * @implNote WhetherCondition, specific whether
 */
@Getter
@Setter
@NoArgsConstructor
@LDLRegister(name = "pos_y", registry = "mbd2:recipe_condition")
public class PositionYCondition extends RecipeCondition {
    @Configurable(name = "config.recipe.condition.pos_y")
    @ConfigNumber(range = {Integer.MIN_VALUE, Integer.MAX_VALUE}, type = ConfigNumber.Type.INTEGER)
    private Range y = Range.of(-64, 255);

    public PositionYCondition(int min, int max) {
        this(false, min, max);
    }

    public PositionYCondition(boolean isReverse, int min, int max) {
        super(isReverse);
        this.y = Range.of(min, max);
    }
    @Override
    public Component getTooltips() {
        return Component.translatable("recipe.condition.pos_y.tooltip", this.y.getMin().intValue(), this.y.getMax().intValue());
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        int y = recipeLogic.machine.getPos().getY();
        return y >= this.y.getMin().intValue() && y <= this.y.getMax().intValue();
    }
}
