package com.lowdragmc.mbd2.api.pattern;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.Builder;
import com.lowdragmc.mbd2.utils.ControllerBlockInfo;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

@Getter
@Setter
public class MultiblockShapeInfo implements IConfigurable, ITagSerializable<CompoundTag> {

    private BlockInfo[][][] blocks;
    @Configurable(name = "editor.machine.multiblock.multiblock_shape_info.description", tips = "editor.machine.multiblock.multiblock_shape_info.description.tips")
    private List<String> description = new ArrayList<>();

    protected MultiblockShapeInfo() {

    }

    public static MultiblockShapeInfo loadFromTag(CompoundTag tag) {
        var info = new MultiblockShapeInfo();
        info.deserializeNBT(tag);
        return info;
    }

    public MultiblockShapeInfo(BlockInfo[][][] blocks, List<String> description) {
        this.blocks = blocks;
        this.description.addAll(description);
    }

    public MultiblockShapeInfo(BlockInfo[][][] blocks) {
        this(blocks, Collections.emptyList());
    }

    public static ShapeInfoBuilder builder() {
        return new ShapeInfoBuilder();
    }

    @Override
    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();
        tag.putInt("x", blocks.length);
        tag.putInt("y", blocks[0].length);
        tag.putInt("z", blocks[0][0].length);
        var blocks = new ListTag();
        for (var block : this.blocks) {
            for (var blockInfos : block) {
                for (var blockInfo : blockInfos) {
                    if (blockInfo instanceof ControllerBlockInfo controllerBlockInfo) {
                        var facing = controllerBlockInfo.getFacing();
                        var controllerTag = new CompoundTag();
                        controllerTag.putString("facing", facing == null ? Direction.NORTH.getSerializedName() : facing.getSerializedName());
                        blocks.add(controllerTag);
                    } else {
                        blocks.add(blockInfo.serializeNBT());
                    }
                }
            }
        }
        tag.put("blocks", blocks);
        var description = new ListTag();
        for (var desc : this.description) {
            description.add(StringTag.valueOf(desc));
        }
        tag.put("description", description);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        var x = tag.getInt("x");
        var y = tag.getInt("y");
        var z = tag.getInt("z");
        var blocks = new BlockInfo[x][y][z];
        var blockList = (ListTag) tag.get("blocks");
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                for (int k = 0; k < z; k++) {
                    var blockInfoTag = (CompoundTag) blockList.get(i * y * z + j * z + k);
                    if (blockInfoTag.contains("facing")) {
                        var controllerBlockInfo = new ControllerBlockInfo(Direction.byName(blockInfoTag.getString("facing")));
                        blocks[i][j][k] = controllerBlockInfo;
                    } else {
                        var blockInfo = new BlockInfo();
                        blockInfo.deserializeNBT(blockInfoTag);
                        blocks[i][j][k] = blockInfo;
                    }
                }
            }
        }
        this.blocks = blocks;
        var description = new ArrayList<String>();
        var descriptionList = tag.getList("description", Tag.TAG_STRING);
        for (var desc : descriptionList) {
            description.add(desc.getAsString());
        }
        this.description = description;
    }

    public static class ShapeInfoBuilder extends Builder<BlockInfo, ShapeInfoBuilder> {

        public ShapeInfoBuilder where(char symbol, BlockState blockState) {
            return where(symbol, BlockInfo.fromBlockState(blockState));
        }

        public ShapeInfoBuilder where(char symbol, Supplier<? extends Block> block) {
            return where(symbol, block.get());
        }

        public ShapeInfoBuilder where(char symbol, Block block) {
            return where(symbol, block.defaultBlockState());
        }

        private BlockInfo[][][] bake() {
            return this.bakeArray(BlockInfo.class, BlockInfo.EMPTY);
        }
        public MultiblockShapeInfo build() {
            return new MultiblockShapeInfo(bake());
        }

    }

}
