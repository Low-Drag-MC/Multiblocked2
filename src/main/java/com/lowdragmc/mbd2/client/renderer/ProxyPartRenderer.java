package com.lowdragmc.mbd2.client.renderer;

import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.mbd2.api.blockentity.ProxyPartBlockEntity;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProxyPartRenderer implements IRenderer {
    public static final ProxyPartRenderer INSTANCE = new ProxyPartRenderer();

    private ProxyPartRenderer() {
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleTexture(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, ModelData modelData) {
        if (modelData != null && level != null && pos != null) {
            if (level.getBlockEntity(pos) instanceof ProxyPartBlockEntity blockEntity && blockEntity.getControllerPos() != null) {
                return IMachine.ofMachine(level, blockEntity.getControllerPos())
                        .filter(MBDMachine.class::isInstance)
                        .map(MBDMachine.class::cast)
                        .map(machine -> machine.getMachineState().getRealRenderer().getParticleTexture(level, pos, modelData))
                        .orElseGet(() -> IRenderer.super.getParticleTexture(level, pos, modelData));
            }
        }
        return IRenderer.super.getParticleTexture(level, pos, modelData);
    }
}
