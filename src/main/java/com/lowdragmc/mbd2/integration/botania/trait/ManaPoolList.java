package com.lowdragmc.mbd2.integration.botania.trait;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import vazkii.botania.api.mana.ManaPool;
import vazkii.botania.api.mana.ManaReceiver;

import java.util.Arrays;

public record ManaPoolList(ManaReceiver[] manaPools) implements ManaPool {
    @Override
    public Level getManaReceiverLevel() {
        return manaPools[0].getManaReceiverLevel();
    }

    @Override
    public BlockPos getManaReceiverPos() {
        return manaPools[0].getManaReceiverPos();
    }

    @Override
    public int getCurrentMana() {
        return Arrays.stream(manaPools).mapToInt(ManaReceiver::getCurrentMana).sum();
    }

    @Override
    public boolean isFull() {
        return Arrays.stream(manaPools).allMatch(ManaReceiver::isFull);
    }

    @Override
    public void receiveMana(int mana) {
        if (mana > 0) {
            for (var receiver : manaPools) {
                if (!receiver.isFull() && receiver.canReceiveManaFromBursts()) {
                    receiver.receiveMana(mana);
                    return;
                }
            }
        } else if (mana < 0) {
            for (var receiver : manaPools) {
                if (receiver.getCurrentMana() > 0) {
                    receiver.receiveMana(mana);
                    return;
                }
            }
        }
    }

    @Override
    public boolean canReceiveManaFromBursts() {
        return Arrays.stream(manaPools).anyMatch(ManaReceiver::canReceiveManaFromBursts);
    }

    @Override
    public boolean isOutputtingPower() {
        return false;
    }

    @Override
    public int getMaxMana() {
        return Arrays.stream(manaPools).filter(ManaPool.class::isInstance).map(ManaPool.class::cast).mapToInt(ManaPool::getMaxMana).sum();
    }
}
