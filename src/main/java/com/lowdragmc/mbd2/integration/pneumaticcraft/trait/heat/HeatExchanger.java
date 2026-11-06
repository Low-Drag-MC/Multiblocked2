package com.lowdragmc.mbd2.integration.pneumaticcraft.trait.heat;

import com.lowdragmc.lowdraglib2.syncdata.IContentChangeAware;
import com.lowdragmc.lowdraglib2.syncdata.IContentVersion;
import lombok.Getter;
import lombok.Setter;
import me.desht.pneumaticcraft.common.heat.HeatExchangerLogicTicking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.function.BiPredicate;

/**
 * Every temperature change PneumaticCraft makes, from neighbours, ambient air and heat behaviours alike, goes through
 * {@link #setTemperature}, so counting there keeps LDLib2's async sync from saving PneumaticCraft's heat behaviours
 * off the server thread to compare them.
 */
public class HeatExchanger extends HeatExchangerLogicTicking implements INBTSerializable<CompoundTag>, IContentChangeAware, IContentVersion {
    @Setter
    @Getter
    public Runnable onContentsChanged = () -> {};
    private volatile long contentVersion;

    @Override
    public void setTemperature(double temperature) {
        if (temperature != getTemperature()) {
            super.setTemperature(temperature);
            contentVersion++;
            onContentsChanged.run();
        }
    }

    public void setTemperatureWithoutNotify(double temperature) {
        super.setTemperature(temperature);
        contentVersion++;
    }

    @Override
    public void initializeAsHull(Level level, BlockPos pos, BiPredicate<LevelAccessor, BlockPos> blockFilter, Direction... validSides) {
        super.initializeAsHull(level, pos, blockFilter, validSides);
        contentVersion++;
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
        contentVersion++;
    }

    @Override
    public long getContentVersion() {
        return contentVersion;
    }
}
