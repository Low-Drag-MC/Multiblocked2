package com.lowdragmc.mbd2.integration.astralsorcery.starlight;

import com.mojang.serialization.MapCodec;
import hellfirepvp.astralsorcery.common.starlight.api.provider.TransmissionReceiverNodeProvider;
import net.minecraft.core.BlockPos;

public class MachineStarlightReceiverNodeProvider implements TransmissionReceiverNodeProvider<MachineStarlightReceiverNode> {

    @Override
    public MachineStarlightReceiverNode provideNewNode(BlockPos pos) {
        return new MachineStarlightReceiverNode(pos);
    }

    @Override
    public MapCodec<MachineStarlightReceiverNode> nodeCodec() {
        return MachineStarlightReceiverNode.CODEC;
    }
}
