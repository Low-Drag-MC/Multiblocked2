package com.lowdragmc.mbd2.integration.ae2.trait;

import appeng.api.networking.IGridNodeListener;
import appeng.me.ManagedGridNode;
import com.lowdragmc.lowdraglib.syncdata.IContentChangeAware;
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;

@Getter
@Setter
public class SerializableManagedGridNode extends ManagedGridNode implements ITagSerializable<CompoundTag>, IContentChangeAware {

    private Runnable onContentsChanged = () -> {};

    public <T> SerializableManagedGridNode(T nodeOwner, IGridNodeListener<? super T> listener) {
        super(nodeOwner, listener);
    }

    @Override
    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();
        saveToNBT(tag);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        loadFromNBT(nbt);
    }
}
