package com.lowdragmc.mbd2.common.recipe;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
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
public class RainingCondition extends RecipeCondition {
    public static final MapCodec<RainingCondition> CODEC = RecordCodecBuilder
            .mapCodec(instance -> instance.group(
                    Codec.BOOL.optionalFieldOf("reverse", false).forGetter(val -> val.isReverse),
                    Codec.FLOAT.validate(v -> DataResult.success(Mth.clamp(v, 0f, 1f))).fieldOf("minLevel").forGetter(val -> val.minLevel),
                    Codec.FLOAT.validate(v -> DataResult.success(Mth.clamp(v, 0f, 1f))).fieldOf("maxLevel").forGetter(val -> val.maxLevel)
            ).apply(instance, RainingCondition::new));

    public final static RainingCondition INSTANCE = new RainingCondition();
    @Configurable(name = "config.recipe.condition.weather.min")
    @NumberRange(range = {0f, 1f})
    private float minLevel;
    @Configurable(name = "config.recipe.condition.weather.max")
    @NumberRange(range = {0f, 1f})
    private float maxLevel;

    public RainingCondition(float minLevel, float maxLevel) {
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    public RainingCondition(boolean isReverse, float minLevel, float maxLevel) {
        super(isReverse);
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    @Override
    public String getType() {
        return "rain";
    }

    @Override
    public Component getTooltips() {
        return Component.translatable("recipe.condition.rain.tooltip", minLevel, maxLevel);
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        Level level = recipeLogic.machine.getLevel();
        return level != null && level.getRainLevel(1) >= this.minLevel && level.getRainLevel(1) <= this.maxLevel;
    }

    @Override
    public MapCodec<? extends RecipeCondition> codec() {
        return CODEC;
    }


}
