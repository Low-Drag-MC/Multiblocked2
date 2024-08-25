package com.lowdragmc.mbd2.common.gui.editor.multiblock;

import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;
import com.lowdragmc.mbd2.common.gui.editor.PredicateResource;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.*;

@Accessors(chain = true)
@EqualsAndHashCode
public class BlockPlaceholder implements IConfigurable, ITagSerializable<CompoundTag> {
    @EqualsAndHashCode.Exclude
    protected final PredicateResource predicateResource;
    @Getter
    protected Set<String> predicates = new HashSet<>();
    @Getter
    @Setter
    protected boolean isController;
    @Getter
    @Setter
    protected Direction facing = Direction.NORTH;

    protected BlockPlaceholder(PredicateResource predicateResource) {
        this.predicateResource = predicateResource;
    }

    public static BlockPlaceholder create(PredicateResource predicateResource, String... predicates) {
        var holder = new BlockPlaceholder(predicateResource);
        holder.predicates.addAll(Arrays.asList(predicates));
        return holder;
    }

    public static BlockPlaceholder controller(PredicateResource predicateResource, String... predicates) {
        var holder = create(predicateResource, predicates);
        holder.isController = true;
        return holder;
    }

    public static BlockPlaceholder fromTag(PredicateResource predicateResource, CompoundTag tag) {
        var holder = new BlockPlaceholder(predicateResource);
        holder.deserializeNBT(tag);
        return holder;
    }

    @Override
    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();
        var predicatesTag = new ListTag();
        for (var predicate : predicates) {
            predicatesTag.add(StringTag.valueOf(predicate));
        }
        tag.put("predicates", predicatesTag);
        tag.putBoolean("isController", isController);
        tag.putInt("facing", facing.get3DDataValue());
        return null;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.predicates.clear();
        var predicatesTag = nbt.getList("predicates", Tag.TAG_STRING);
        for (var tag : predicatesTag) {
            predicates.add(tag.getAsString());
        }
        isController = nbt.getBoolean("isController");
        facing = Direction.from3DDataValue(nbt.getInt("facing"));
    }
}
