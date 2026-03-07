package com.lowdragmc.mbd2.common.network;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.network.packets.SPatternErrorPosPacket;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class MBD2Network {

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MBD2.MOD_ID);
        registrar.playToClient(SPatternErrorPosPacket.TYPE, SPatternErrorPosPacket.CODEC, SPatternErrorPosPacket::execute);
    }
}
