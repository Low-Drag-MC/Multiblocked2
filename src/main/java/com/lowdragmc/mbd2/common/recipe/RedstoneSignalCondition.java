package com.lowdragmc.mbd2.common.recipe;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.math.Range;
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
@LDLRegister(name = "redstone_signal", registry = "mbd2:recipe_condition")
public class RedstoneSignalCondition extends RecipeCondition {
    @Configurable(name = "config.recipe.condition.redstone_signal.signal")
    @ConfigNumber(range = {0f, 15f}, type = ConfigNumber.Type.INTEGER)
    private Range signal = Range.of(0f, 15f);

    public RedstoneSignalCondition(int minSignal, int maxSignal) {
        this(false, minSignal, maxSignal);
    }

    public RedstoneSignalCondition(boolean isReverse, int minSignal, int maxSignal) {
        super(isReverse);
        this.signal = Range.of(minSignal, maxSignal);
    }

    @Override
    public Component getTooltips() {
        return Component.translatable("recipe.condition.redstone_signal.tooltip",
                signal.getMin().intValue(), signal.getMax().intValue());
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(Items.REDSTONE_TORCH);
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        var pos = recipeLogic.getMachine().getPos();
        var signal = recipeLogic.getMachine().getLevel().getBestNeighborSignal(pos);
        return signal >= this.signal.getMin().intValue() && signal <= this.signal.getMax().intValue();
    }
}
