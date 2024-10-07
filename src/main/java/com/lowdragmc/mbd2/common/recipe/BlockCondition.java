package com.lowdragmc.mbd2.common.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.mbd2.api.machine.IMultiController;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import java.util.Optional;

@Getter
public class BlockCondition extends RecipeCondition {

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

    @Nonnull
    @Override
    public JsonObject serialize() {
        JsonObject config = super.serialize();
        config.addProperty("minCount", minCount);
        config.addProperty("maxCount", maxCount);
        var array = new JsonArray();
        for (Block block : blocks) {
            var key = ForgeRegistries.BLOCKS.getKey(block);
            if (key != null) {
                array.add(key.toString());
            }
        }
        config.add("blocks", array);
        return config;
    }

    @Override
    public RecipeCondition deserialize(@Nonnull JsonObject config) {
        super.deserialize(config);
        minCount = GsonHelper.getAsInt(config, "minCount", 0);
        maxCount = GsonHelper.getAsInt(config, "maxCount", 0);
        var array = GsonHelper.getAsJsonArray(config, "blocks", new JsonArray());
        blocks = new Block[array.size()];
        for (int i = 0; i < array.size(); i++) {
            var key = array.get(i).getAsString();
            blocks[i] = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(key));
            if (blocks[i] == null) {
                blocks[i] = Blocks.AIR;
            }
        }
        return this;
    }

    @Override
    public RecipeCondition fromNetwork(FriendlyByteBuf buf) {
        super.fromNetwork(buf);
        minCount = buf.readVarInt();
        maxCount = buf.readVarInt();
        int size = buf.readVarInt();
        blocks = new Block[size];
        for (int i = 0; i < size; i++) {
            blocks[i] = ForgeRegistries.BLOCKS.getValue(buf.readResourceLocation());
        }
        return this;
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        super.toNetwork(buf);
        buf.writeVarInt(minCount);
        buf.writeVarInt(maxCount);
        buf.writeVarInt(blocks.length);
        for (Block block : blocks) {
            buf.writeResourceLocation(Optional.ofNullable(ForgeRegistries.BLOCKS.getKey(block)).orElse(new ResourceLocation("minecraft:air")));
        }
    }

    @Override
    public CompoundTag toNBT() {
        var tag = super.toNBT();
        tag.putInt("minCount", minCount);
        tag.putInt("maxCount", maxCount);
        var array = new ListTag();
        for (Block block : blocks) {
            var key = ForgeRegistries.BLOCKS.getKey(block);
            if (key != null) {
                array.add(StringTag.valueOf(key.toString()));
            }
        }
        tag.put("blocks", array);
        return tag;
    }

    @Override
    public RecipeCondition fromNBT(CompoundTag tag) {
        super.fromNBT(tag);
        minCount = tag.getInt("minCount");
        maxCount = tag.getInt("maxCount");
        var array = tag.getList("blocks", Tag.TAG_STRING);
        blocks = new Block[array.size()];
        for (int i = 0; i < array.size(); i++) {
            var key = array.getString(i);
            blocks[i] = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(key));
            if (blocks[i] == null) {
                blocks[i] = Blocks.AIR;
            }
        }
        return this;
    }

}
