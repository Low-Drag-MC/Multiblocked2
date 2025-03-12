package com.lowdragmc.mbd2.common.network.packets;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.client.renderer.MultiblockInWorldPreviewRenderer;
import com.lowdragmc.mbd2.config.ConfigHolder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@NoArgsConstructor
@AllArgsConstructor
public class SPatternErrorPosPacket implements CustomPacketPayload {
    public static final ResourceLocation ID = MBD2.id("pattern_error_pos");
    public static final Type<SPatternErrorPosPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, SPatternErrorPosPacket> CODEC = StreamCodec.ofMember(
            (packet, buf) -> buf.writeBlockPos(packet.pos), buf -> new SPatternErrorPosPacket(buf.readBlockPos()));


    public BlockPos pos;

    public static void execute(SPatternErrorPosPacket packet, IPayloadContext context) {
        if (LDLib.isClient()) {
            MultiblockInWorldPreviewRenderer.showPatternErrorPos(packet.pos, ConfigHolder.multiblockPatternErrorPosDuration * 20);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
