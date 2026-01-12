package com.lowdragmc.mbd2.common.gui.editor.multiblock;

import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.mbd2.common.gui.editor.PredicateResource;
import com.mojang.datafixers.util.Either;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.io.File;
import java.util.*;

@Accessors(chain = true)
@EqualsAndHashCode
public class BlockPlaceholder implements IConfigurable, INBTSerializable<CompoundTag> {
    @EqualsAndHashCode.Exclude
    @Getter
    protected final PredicateResource predicateResource;
    @Getter
    protected Set<Either<String, File>> predicates = new LinkedHashSet<>() {
        @Override
        public boolean remove(Object o) {
            return super.remove(o);
        }

        @Override
        public void clear() {
            super.clear();
        }
    };
    @Getter
    @Setter
    protected boolean isController;
    @Getter
    @Setter
    protected Direction facing = Direction.NORTH;

    protected BlockPlaceholder(PredicateResource predicateResource) {
        this.predicateResource = predicateResource;
    }

    @SafeVarargs
    public static BlockPlaceholder create(PredicateResource predicateResource, Either<String, File>... predicates) {
        var holder = new BlockPlaceholder(predicateResource);
        holder.predicates.addAll(Arrays.asList(predicates));
        return holder;
    }

    @SafeVarargs
    public static BlockPlaceholder controller(PredicateResource predicateResource, Either<String, File>... predicates) {
        var holder = create(predicateResource, predicates);
        holder.isController = true;
        return holder;
    }

    public static BlockPlaceholder fromTag(PredicateResource predicateResource, CompoundTag tag) {
        var holder = new BlockPlaceholder(predicateResource);
        holder.deserializeNBT(Platform.getFrozenRegistry(), tag);
        return holder;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        var tag = new CompoundTag();
        var predicatesTag = new ListTag();
        for (var predicate : predicates) {
            predicatesTag.add(predicate.map(
                    l -> {
                        var key = new CompoundTag();
                        key.putString("key", l);
                        key.putString("type", "builtin");
                        return key;
                    }, r-> {
                        var key = new CompoundTag();
                        key.putString("key", r.getPath());
                        key.putString("type", "project");
                        return key;
                    }
            ));
        }
        tag.put("predicates", predicatesTag);
        tag.putBoolean("isController", isController);
        tag.putInt("facing", facing.get3DDataValue());
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        this.predicates.clear();
        var predicatesTag = nbt.getList("predicates", Tag.TAG_STRING);
        if (predicatesTag.isEmpty()) {
            predicatesTag = nbt.getList("predicates", Tag.TAG_COMPOUND);
            for (var tag : predicatesTag) {
                var compoundTag = (CompoundTag) tag;
                var key = compoundTag.getString("key");
                var type = compoundTag.getString("type");
                if ("builtin".equals(type)) {
                    predicates.add(Either.left(key));
                } else if ("project".equals(type)) {
                    predicates.add(Either.right(new File(key)));
                }
            }
        } else {
            for (var tag : predicatesTag) {
                predicates.add(Either.left(tag.getAsString()));
            }
        }
        isController = nbt.getBoolean("isController");
        facing = Direction.from3DDataValue(nbt.getInt("facing"));
    }
}
