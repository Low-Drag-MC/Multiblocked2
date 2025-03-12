package com.lowdragmc.mbd2.common.recipe;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
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
import net.minecraft.util.ExtraCodecs;

import javax.annotation.Nonnull;

@Getter
@Setter
@NoArgsConstructor
public class MachineLevelCondition extends RecipeCondition {
    public static final MapCodec<MachineLevelCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("reverse", false).forGetter(val -> val.isReverse),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("level").forGetter(val -> val.level)
    ).apply(instance, MachineLevelCondition::new));

    public final static MachineLevelCondition INSTANCE = new MachineLevelCondition();
    @Configurable(name = "config.recipe.condition.machine_level.level", tips="config.recipe.condition.machine_level.level.tips")
    @NumberRange(range = {0, Integer.MAX_VALUE})
    private int level;

    public MachineLevelCondition(int level) {
        this.level = level;
    }

    public MachineLevelCondition(boolean isReverse, int level) {
        super(isReverse);
        this.level = level;
    }

    @Override
    public String getType() {
        return "machine_level";
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
    public MapCodec<? extends RecipeCondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        return recipeLogic.machine.getMachineLevel() >= this.level;
    }


}
