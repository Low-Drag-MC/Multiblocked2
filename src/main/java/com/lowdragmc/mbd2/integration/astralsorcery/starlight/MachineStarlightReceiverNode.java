package com.lowdragmc.mbd2.integration.astralsorcery.starlight;

import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.integration.astralsorcery.AstralSorceryIntegration;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import hellfirepvp.astralsorcery.common.starlight.api.TransmissionReceiverNode;
import hellfirepvp.astralsorcery.common.starlight.api.provider.TransmissionNodeProvider;
import hellfirepvp.astralsorcery.common.starlight.transmission.StarlightTransmissionPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Hands delivered packets to the machine's {@link StarlightReceiverTrait}. The network persists nodes as world
 * data, so this holds only a position and looks the machine up per delivery; unloaded or missing machines drop it.
 */
public class MachineStarlightReceiverNode implements TransmissionReceiverNode {
    public static final MapCodec<MachineStarlightReceiverNode> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(MachineStarlightReceiverNode::getNodePos)
    ).apply(instance, MachineStarlightReceiverNode::new));

    private final BlockPos pos;

    public MachineStarlightReceiverNode(BlockPos pos) {
        this.pos = pos.immutable();
    }

    @Override
    public void receiveStarlight(ServerLevel level, StarlightTransmissionPacket packet) {
        if (packet.constellation() == null || packet.amount() <= 0 || !level.isLoaded(pos)) return;
        IMachine.ofMachine(level, pos).ifPresent(machine -> {
            if (machine instanceof MBDMachine mbdMachine) {
                for (var trait : mbdMachine.getAdditionalTraits()) {
                    if (trait instanceof StarlightReceiverTrait receiver) {
                        receiver.receive(packet);
                    }
                }
            }
        });
    }

    @Override
    public BlockPos getNodePos() {
        return pos;
    }

    @Override
    public TransmissionNodeProvider<?> getProvider() {
        return AstralSorceryIntegration.STARLIGHT_RECEIVER_NODE.get();
    }
}
