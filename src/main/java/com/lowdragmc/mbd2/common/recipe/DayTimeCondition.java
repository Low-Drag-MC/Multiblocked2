package com.lowdragmc.mbd2.common.recipe;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import javax.annotation.Nonnull;

@Getter
@Setter
@NoArgsConstructor
@LDLRegister(name = "day_time", registry = "mbd2:recipe_condition")
public class DayTimeCondition extends RecipeCondition {
    @Configurable(name = "config.recipe.condition.day_time.is_day")
    private boolean isDay;

    public DayTimeCondition(boolean isDay) {
        this.isDay = isDay;
    }

    public DayTimeCondition(boolean isReverse, boolean isDay) {
        super(isReverse);
        this.isDay = isDay;
    }

    @Override
    public Component getTooltips() {
        return isDay ? Component.translatable("recipe.condition.day_time.tooltip.true") : Component.translatable("recipe.condition.day_time.tooltip.false");
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(Items.CLOCK);
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        var level = recipeLogic.machine.getLevel();
        return level != null && level.isDay() == isDay;
    }

}
