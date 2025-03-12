package com.lowdragmc.mbd2.common.recipe;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.mbd2.api.machine.IMultiController;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.Block;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import java.util.List;

@Getter
@Setter
public class BlockCondition extends RecipeCondition {

    public static final MapCodec<BlockCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("reverse", false).forGetter(val -> val.isReverse),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("minCount", 0).forGetter(val -> val.minCount),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("maxCount", Integer.MAX_VALUE).forGetter(val -> val.maxCount),
            BuiltInRegistries.BLOCK.byNameCodec().listOf().optionalFieldOf("blocks", List.of()).forGetter(val -> List.of(val.blocks))
    ).apply(instance, BlockCondition::new));

    public final static BlockCondition INSTANCE = new BlockCondition();
    @Configurable(name = "config.recipe.condition.block.min")
    @NumberRange(range = {0, Integer.MAX_VALUE})
    private int minCount;
    @Configurable(name = "config.recipe.condition.block.max")
    @NumberRange(range = {0, Integer.MAX_VALUE})
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
    public String getType() {
        return "block";
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

    @Override
    public MapCodec<? extends RecipeCondition> codec() {
        return CODEC;
    }

}
