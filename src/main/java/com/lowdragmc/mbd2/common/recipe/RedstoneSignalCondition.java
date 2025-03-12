package com.lowdragmc.mbd2.common.recipe;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
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
import net.minecraft.world.item.Items;

import javax.annotation.Nonnull;

@Getter
@Setter
@NoArgsConstructor
public class RedstoneSignalCondition extends RecipeCondition {
    public static final MapCodec<RedstoneSignalCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("reverse", false).forGetter(val -> val.isReverse),
            Codec.INT.validate(v -> DataResult.success(Mth.clamp(v, 0, 15))).fieldOf("minSignal").forGetter(val -> val.minSignal),
            Codec.INT.validate(v -> DataResult.success(Mth.clamp(v, 0, 15))).fieldOf("maxSignal").forGetter(val -> val.maxSignal)
    ).apply(instance, RedstoneSignalCondition::new));

    public final static RedstoneSignalCondition INSTANCE = new RedstoneSignalCondition();
    @Configurable(name = "config.recipe.condition.redstone_signal.signal.min")
    @NumberRange(range = {0f, 15f})
    private int minSignal;
    @Configurable(name = "config.recipe.condition.redstone_signal.signal.max")
    @NumberRange(range = {0f, 15f})
    private int maxSignal;

    public RedstoneSignalCondition(int minSignal, int maxSignal) {
        this.minSignal = minSignal;
        this.maxSignal = maxSignal;
    }

    public RedstoneSignalCondition(boolean isReverse, int minSignal, int maxSignal) {
        super(isReverse);
        this.minSignal = minSignal;
        this.maxSignal = maxSignal;
    }

    @Override
    public String getType() {
        return "redstone_signal";
    }

    @Override
    public Component getTooltips() {
        return Component.translatable("recipe.condition.redstone_signal.tooltip", minSignal, maxSignal);
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(Items.REDSTONE_TORCH);
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        var pos = recipeLogic.getMachine().getPos();
        var signal = recipeLogic.getMachine().getLevel().getBestNeighborSignal(pos);
        return signal >= minSignal && signal <= maxSignal;
    }

    @Override
    public MapCodec<? extends RecipeCondition> codec() {
        return CODEC;
    }
}
