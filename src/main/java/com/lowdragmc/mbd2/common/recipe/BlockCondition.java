package com.lowdragmc.mbd2.common.recipe;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.api.machine.IMultiController;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import java.util.List;

@Getter
@Setter
@LDLRegister(name = "block", registry = "mbd2:recipe_condition")
public class BlockCondition extends RecipeCondition {
    @Configurable(name = "config.recipe.condition.block.min")
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    private int minCount;
    @Configurable(name = "config.recipe.condition.block.max")
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    private int maxCount;
    @Configurable(name = "config.recipe.condition.block.blocks", collapse = false)
    private Block[] blocks;

    public BlockCondition() {
        this(0, 0);
    }

    public BlockCondition(int minLevel, int maxLevel, Block... blocks) {
        this.minCount = minLevel;
        this.maxCount = maxLevel;
        this.blocks = blocks;
    }

    public BlockCondition(boolean isReverse, int minLevel, int maxLevel, List<Block> blocks) {
        this(minLevel, maxLevel, blocks.toArray(new Block[0]));
        this.isReverse = isReverse;
    }

    @Override
    public Component getTooltips() {
        var blockNames = Component.empty();
        for (int i = 0; i < blocks.length; i++) {
            blockNames.append(blocks[i].getName());
            if (i < blocks.length - 1) {
                blockNames.append(Component.literal(" || "));
            }
        }
        return Component.translatable("recipe.condition.block.tooltip", blockNames, minCount, maxCount);
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        var amount = 0;
        if (recipeLogic.machine instanceof IMultiController controller) {
            var level = controller.getLevel();
            for (var pos : controller.getMultiblockState().getCache()) {
                if (ArrayUtils.contains(blocks, level.getBlockState(pos).getBlock())) {
                    amount++;
                }
            }
        }
        return amount >= minCount && amount <= maxCount;
    }

}
