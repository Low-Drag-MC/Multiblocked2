package com.lowdragmc.mbd2.integration.botania.trait;

import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.IntTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import vazkii.botania.api.mana.ManaPool;

import java.util.Optional;

public class CopiableManaPool implements ManaPool, ITagSerializable<IntTag> {
    private final Level level;
    private final BlockPos pos;
    @Getter
    protected final int maxMana;

    protected int mana;

    public CopiableManaPool(Level level, BlockPos pos, int capacity) {
        this(level, pos, capacity, 0);
    }

    public CopiableManaPool(Level level, BlockPos pos, int capacity, int mana) {
        this.level = level;
        this.pos = pos;
        this.maxMana = capacity;
        this.mana = mana;
    }

    public CopiableManaPool copy() {
        return new CopiableManaPool(level, pos, maxMana, mana);
    }

    @Override
    public Level getManaReceiverLevel() {
        return level;
    }

    @Override
    public BlockPos getManaReceiverPos() {
        return pos;
    }

    @Override
    public int getCurrentMana() {
        return mana;
    }


    @Override
    public boolean isFull() {
        return mana >= maxMana;
    }

    @Override
    public void receiveMana(int mana) {
        this.mana = Math.max(0, Math.min(this.mana + mana, maxMana));
    }

    @Override
    public boolean canReceiveManaFromBursts() {
        return !isFull();
    }

    @Override
    public IntTag serializeNBT() {
        return IntTag.valueOf(mana);
    }

    @Override
    public void deserializeNBT(IntTag nbt) {
        mana = nbt.getAsInt();
    }

    @Override
    public boolean isOutputtingPower() {
        return false;
    }

    @Override
    public Optional<DyeColor> getColor() {
        return Optional.empty();
    }

    @Override
    public void setColor(Optional<DyeColor> color) {

    }
}
