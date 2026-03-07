package com.lowdragmc.mbd2.common.machine.definition.config;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@Accessors(chain = true)
@EqualsAndHashCode
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockPlaceholder implements IConfigurable, IPersistedSerializable {
    @Getter
    @Persisted
    protected Set<IResourcePath> predicates = new LinkedHashSet<>();
    @Getter
    @Setter
    @Persisted
    protected boolean isController;
    @Getter
    @Setter
    @Persisted
    protected Direction facing = Direction.NORTH;

    protected BlockPlaceholder() {
    }

    public static BlockPlaceholder create(IResourcePath... predicates) {
        var holder = new BlockPlaceholder();
        holder.predicates.addAll(Arrays.asList(predicates));
        return holder;
    }

    public static BlockPlaceholder controller(IResourcePath... predicates) {
        var holder = create(predicates);
        holder.isController = true;
        return holder;
    }

    public static BlockPlaceholder fromTag(CompoundTag tag) {
        var holder = new BlockPlaceholder();
        holder.deserializeNBT(Platform.getFrozenRegistry(), tag);
        return holder;
    }
}
