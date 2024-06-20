package com.lowdragmc.mbd2.common.gui.factory;

import com.lowdragmc.lowdraglib.gui.factory.UIFactory;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class MachineUIFactory extends UIFactory<MBDMachine> {
    public static final MachineUIFactory INSTANCE  = new MachineUIFactory();

    public MachineUIFactory() {
        super(MBD2.id("machine"));
    }

    @Override
    protected ModularUI createUITemplate(MBDMachine machine, Player entityPlayer) {
        return machine.createUI(entityPlayer);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    protected MBDMachine readHolderFromSyncData(FriendlyByteBuf syncData) {
        var world = Minecraft.getInstance().level;
        if (world == null) return null;
        return IMachine.ofMachine(world, syncData.readBlockPos()).filter(MBDMachine.class::isInstance).map(MBDMachine.class::cast).orElse(null);
    }

    @Override
    protected void writeHolderToSyncData(FriendlyByteBuf syncData, MBDMachine holder) {
        syncData.writeBlockPos(holder.getPos());
    }
}
