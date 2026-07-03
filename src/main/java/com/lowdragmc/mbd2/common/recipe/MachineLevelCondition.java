package com.lowdragmc.mbd2.common.recipe;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
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

@Getter
@Setter
@NoArgsConstructor
@LDLRegister(name = "machine_level", registry = "mbd2:recipe_condition")
public class MachineLevelCondition extends RecipeCondition {
    @Configurable(name = "config.recipe.condition.machine_level.level", tips="config.recipe.condition.machine_level.level.tips")
    @ConfigNumber(range = {0, Integer.MAX_VALUE}, type = ConfigNumber.Type.INTEGER)
    private Range level = Range.of(0f, 1f);

    public MachineLevelCondition(int level) {
        this(false, level, level);
    }

    public MachineLevelCondition(boolean isReverse, int min, int max) {
        super(isReverse);
        this.level = Range.of(min, max);
    }

    @Override
    public Component getTooltips() {
        return Component.translatable("recipe.condition.machine_level.tooltip", this.level);
    }

    @Override
    public IGuiTexture getIcon() {
        return new TextTexture("LV");
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        var level = recipeLogic.machine.getMachineLevel();
        return level >= this.level.getMin().intValue() && level <= this.level.getMax().intValue();
    }

}
