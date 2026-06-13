package com.lowdragmc.mbd2.integration.ae2.trait;

import appeng.api.networking.IManagedGridNode;
import appeng.helpers.InterfaceLogic;
import appeng.helpers.InterfaceLogicHost;
import com.google.common.util.concurrent.Runnables;
import com.lowdragmc.lowdraglib2.syncdata.IContentChangeAware;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
public class SerializableInterfaceLogic extends InterfaceLogic implements INBTSerializable<CompoundTag>, IContentChangeAware {
    private Runnable onContentsChanged = Runnables.doNothing();

    public SerializableInterfaceLogic(IManagedGridNode gridNode, InterfaceLogicHost host, Item item, int slots) {
        super(gridNode, host, item, slots);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        var tag = new CompoundTag();
        writeToNBT(tag, provider);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag nbt) {
        readFromNBT(nbt, provider);
    }
}
