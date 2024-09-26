package com.lowdragmc.mbd2.integration.botania.trait;

import com.lowdragmc.mbd2.api.capability.recipe.IO;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import vazkii.botania.api.mana.ManaPool;

import java.util.Optional;

public class ManaPoolWrapper implements ManaPool {
    private final ManaPool manaPool;
    private final IO io;

    public ManaPoolWrapper(ManaPool receiver, IO io) {
        this.manaPool = receiver;
        this.io = io;
    }

    @Override
    public Level getManaReceiverLevel() {
        return manaPool.getManaReceiverLevel();
    }

    @Override
    public BlockPos getManaReceiverPos() {
        return manaPool.getManaReceiverPos();
    }

    @Override
    public int getCurrentMana() {
        return manaPool.getCurrentMana();
    }

    @Override
    public boolean isFull() {
        return manaPool.isFull();
    }

    @Override
    public void receiveMana(int mana) {
        if (mana > 0 && (io == IO.IN || io == IO.BOTH)) {
            manaPool.receiveMana(mana);
        } else if (mana < 0 && (io == IO.OUT || io == IO.BOTH)) {
            manaPool.receiveMana(mana);
        }
    }

    @Override
    public boolean canReceiveManaFromBursts() {
        return manaPool.canReceiveManaFromBursts() && (io == IO.IN || io == IO.BOTH);
    }

    @Override
    public boolean isOutputtingPower() {
        return false;
    }

    @Override
    public int getMaxMana() {
        return manaPool.getMaxMana();
    }

    @Override
    public Optional<DyeColor> getColor() {
        return Optional.empty();
    }

    @Override
    public void setColor(Optional<DyeColor> color) {

    }
}
