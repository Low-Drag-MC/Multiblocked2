package com.lowdragmc.mbd2.integration.botania.trait;

import com.lowdragmc.lowdraglib2.syncdata.IContentChangeAware;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;
import vazkii.botania.api.mana.ManaPool;
import vazkii.botania.api.mana.spark.SparkAttachable;
import vazkii.botania.common.block.BotaniaBlocks;

public class CopiableManaPool implements ManaPool, INBTSerializable<CompoundTag>, IContentChangeAware, SparkAttachable {
    @Getter
    @Setter
    public Runnable onContentsChanged = () -> {};

    private final MBDMachine machine;
    @Getter
    protected final int maxMana;
    protected int mana;
    protected boolean canAttachSpark;

    public CopiableManaPool(MBDMachine machine, int capacity, boolean canAttachSpark) {
        this(machine, capacity, canAttachSpark, 0);
    }

    public CopiableManaPool(MBDMachine machine, int capacity, boolean canAttachSpark, int mana) {
        this.machine = machine;
        this.maxMana = capacity;
        this.mana = mana;
        this.canAttachSpark = canAttachSpark;
    }

    public CopiableManaPool copy() {
        return new CopiableManaPool(machine, maxMana, canAttachSpark, mana);
    }

    @Override
    public Level getManaReceiverLevel() {
        return machine.getLevel();
    }

    @Override
    public BlockPos getManaReceiverPos() {
        return machine.getPos();
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
        var old = this.mana;
        this.mana = Math.max(0, Math.min(this.mana + mana, maxMana));
        if (old != this.mana) onContentsChanged.run();
    }

    @Override
    public boolean canReceiveManaFromBursts() {
        return !isFull();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        var tag = new CompoundTag();
        tag.putInt("mana", mana);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag nbt) {
        mana = Math.max(0, Math.min(nbt.getInt("mana"), maxMana));
    }

    @Override
    public boolean isOutputtingPower() {
        return false;
    }

    @Override
    public boolean canAttachSpark(ItemStack stack) {
        return canAttachSpark;
    }

    @Override
    public int getAvailableSpaceForMana() {
        int space = Math.max(0, getMaxMana() - getCurrentMana());
        if (space > 0) {
            return space;
        }
        if (machine.getLevel().getBlockState(machine.getPos().below()).is(BotaniaBlocks.manaVoid)) {
            return getMaxMana();
        }
        return 0;
    }

    @Override
    public boolean areIncomingTransfersDone() {
        return false;
    }
}
