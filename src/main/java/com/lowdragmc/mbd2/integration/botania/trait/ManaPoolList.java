package com.lowdragmc.mbd2.integration.botania.trait;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import vazkii.botania.api.mana.ManaPool;
import vazkii.botania.api.mana.ManaReceiver;

import java.util.Arrays;
import java.util.Optional;

public record ManaPoolList(ManaPool[] manaPools) implements ManaPool {

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
        for (var receiver : manaPools) {
            if (!receiver.isFull() && receiver.canReceiveManaFromBursts()) {
                receiver.receiveMana(mana);
                return;
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
        return Arrays.stream(manaPools).mapToInt(ManaPool::getMaxMana).sum();
    }

    @Override
    public Optional<DyeColor> getColor() {
        return Optional.empty();
    }

    @Override
    public void setColor(Optional<DyeColor> color) {

    }
}
