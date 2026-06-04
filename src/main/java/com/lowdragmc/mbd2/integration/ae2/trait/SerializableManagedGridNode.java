package com.lowdragmc.mbd2.integration.ae2.trait;

import appeng.api.networking.IGridNodeListener;
import appeng.me.ManagedGridNode;
import com.google.common.util.concurrent.Runnables;
import com.lowdragmc.lowdraglib2.syncdata.IContentChangeAware;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
public class SerializableManagedGridNode extends ManagedGridNode implements INBTSerializable<CompoundTag>, IContentChangeAware {
    private Runnable onContentsChanged = Runnables.doNothing();

    public <T> SerializableManagedGridNode(T nodeOwner, IGridNodeListener<? super T> listener) {
        super(nodeOwner, listener);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        var tag = new CompoundTag();
        saveToNBT(tag);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag nbt) {
        loadFromNBT(nbt);
    }
}
