package com.lowdragmc.mbd2.common.recipe;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import javax.annotation.Nonnull;

@Getter
@Setter
@NoArgsConstructor
public class DayLightCondition extends RecipeCondition {

    public static final MapCodec<DayLightCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("reverse", false).forGetter(val -> val.isReverse),
            Codec.BOOL.optionalFieldOf("isDay", true).forGetter(val -> val.isDay)
    ).apply(instance, DayLightCondition::new));

    public final static DayLightCondition INSTANCE = new DayLightCondition();
    @Configurable(name = "config.recipe.condition.day_light.is_day")
    @NumberRange(range = {0f, 1f})
    private boolean isDay;

    public DayLightCondition(boolean isDay) {
        this.isDay = isDay;
    }

    public DayLightCondition(boolean isReverse, boolean isDay) {
        super(isReverse);
        this.isDay = isDay;
    }

    @Override
    public String getType() {
        return "day_light";
    }

    @Override
    public Component getTooltips() {
        return isDay ? Component.translatable("recipe.condition.day_light.tooltip.true") : Component.translatable("recipe.condition.day_light.tooltip.false");
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(Items.DAYLIGHT_DETECTOR);
    }

    @Override
    public MapCodec<? extends RecipeCondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        var level = recipeLogic.machine.getLevel();
        return level != null && level.isDay() == isDay;
    }

}
