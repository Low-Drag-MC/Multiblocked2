package com.lowdragmc.mbd2.common.recipe;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
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

import javax.annotation.Nonnull;

/**
 * @author KilaBash
 * @date 2022/05/27
 * @implNote WhetherCondition, specific whether
 */
@Getter
@Setter
@NoArgsConstructor
public class PositionYCondition extends RecipeCondition {
    public static final MapCodec<PositionYCondition> CODEC = RecordCodecBuilder
            .mapCodec(instance -> instance.group(
                    Codec.BOOL.optionalFieldOf("reverse", false).forGetter(val -> val.isReverse),
                    Codec.INT.fieldOf("min").forGetter(val -> val.min),
                    Codec.INT.fieldOf("max").forGetter(val -> val.max)
            ).apply(instance, PositionYCondition::new));

    public final static PositionYCondition INSTANCE = new PositionYCondition();
    @Configurable(name = "config.recipe.condition.pos_y.min")
    @NumberRange(range = {Integer.MIN_VALUE, Integer.MAX_VALUE})
    private int min;
    @Configurable(name = "config.recipe.condition.pos_y.max")
    @NumberRange(range = {Integer.MIN_VALUE, Integer.MAX_VALUE})
    private int max;

    public PositionYCondition(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public PositionYCondition(boolean isReverse, int min, int max) {
        super(isReverse);
        this.min = min;
        this.max = max;
    }

    @Override
    public String getType() {
        return "pos_y";
    }

    @Override
    public Component getTooltips() {
        return Component.translatable("recipe.condition.pos_y.tooltip", this.min, this.max);
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        int y = recipeLogic.machine.getPos().getY();
        return y >= this.min && y <= this.max;
    }

    @Override
    public MapCodec<? extends RecipeCondition> codec() {
        return CODEC;
    }

}
