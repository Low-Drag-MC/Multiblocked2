package com.lowdragmc.mbd2.integration.ae2.trait;

import appeng.api.networking.IManagedGridNode;
import appeng.helpers.InterfaceLogic;
import appeng.helpers.InterfaceLogicHost;
import com.lowdragmc.lowdraglib.syncdata.IContentChangeAware;
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;

@Getter
@Setter
public class SerializableInterfaceLogic extends InterfaceLogic implements ITagSerializable<CompoundTag>, IContentChangeAware {
    private Runnable onContentsChanged = () -> {};

    public SerializableInterfaceLogic(IManagedGridNode gridNode, InterfaceLogicHost host, Item is, int slots) {
        super(gridNode, host, is, slots);
    }

    @Override
    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();
        writeToNBT(tag);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        readFromNBT(nbt);
    }
}
