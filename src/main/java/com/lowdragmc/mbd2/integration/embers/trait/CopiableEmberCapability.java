package com.lowdragmc.mbd2.integration.embers.trait;

import com.lowdragmc.lowdraglib.syncdata.IContentChangeAware;
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;
import com.rekindled.embers.power.DefaultEmberCapability;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;

public class CopiableEmberCapability extends DefaultEmberCapability implements ITagSerializable<CompoundTag>, IContentChangeAware {
    @Getter
    @Setter
    public Runnable onContentsChanged = () -> {};

    public CopiableEmberCapability(double capacity) {
        this(capacity, 0);
    }

    public CopiableEmberCapability(double capacity, double ember) {
        setEmberCapacity(capacity);
        setEmber(ember);
    }

    public CopiableEmberCapability copy() {
        return new CopiableEmberCapability(getEmberCapacity(), getEmber());
    }

    @Override
    public void onContentsChanged() {
        onContentsChanged.run();
    }
}
