package com.lowdragmc.mbd2.integration.pneumaticcraft.trait.heat;

import com.lowdragmc.lowdraglib2.syncdata.IContentChangeAware;
import lombok.Getter;
import lombok.Setter;
import me.desht.pneumaticcraft.common.heat.HeatExchangerLogicTicking;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

public class HeatExchanger extends HeatExchangerLogicTicking implements INBTSerializable<CompoundTag>, IContentChangeAware {
    @Setter
    @Getter
    public Runnable onContentsChanged = () -> {};

    @Override
    public void setTemperature(double temperature) {
        if (temperature != getTemperature()) {
            super.setTemperature(temperature);
            onContentsChanged.run();
        }
    }

    public void setTemperatureWithoutNotify(double temperature) {
        super.setTemperature(temperature);
    }

    public HeatExchanger copy() {
        var copy = new HeatExchanger();
        copy.setThermalCapacity(getThermalCapacity());
        copy.setThermalResistance(getThermalResistance());
        copy.setTemperatureWithoutNotify(getTemperature());
        return copy;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return super.serializeNBT();
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        super.deserializeNBT(nbt);
    }
}
