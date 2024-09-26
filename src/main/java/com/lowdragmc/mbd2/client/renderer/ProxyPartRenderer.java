package com.lowdragmc.mbd2.client.renderer;

import com.lowdragmc.lowdraglib.client.model.forge.LDLRendererModel;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.mbd2.api.blockentity.ProxyPartBlockEntity;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import static com.lowdragmc.lowdraglib.client.model.forge.LDLRendererModel.RendererBakedModel.POS;
import static com.lowdragmc.lowdraglib.client.model.forge.LDLRendererModel.RendererBakedModel.WORLD;

public class ProxyPartRenderer implements IRenderer {
    public static final ProxyPartRenderer INSTANCE = new ProxyPartRenderer();

    private ProxyPartRenderer() {
    }

    @NotNull
    @Override
    @OnlyIn(Dist.CLIENT)
    public TextureAtlasSprite getParticleTexture() {
        var modelData = LDLRendererModel.RendererBakedModel.CURRENT_MODEL_DATA.get();
        if (modelData != null) {
            var world = modelData.get(WORLD);
            var pos = modelData.get(POS);
            if (world != null && pos != null && world.getBlockEntity(pos) instanceof ProxyPartBlockEntity blockEntity && blockEntity.getControllerPos() != null) {
                return IMachine.ofMachine(world, blockEntity.getControllerPos())
                        .filter(MBDMachine.class::isInstance)
                        .map(MBDMachine.class::cast)
                        .map(machine -> machine.getMachineState().getRealRenderer().getParticleTexture())
                        .orElseGet(IRenderer.super::getParticleTexture);
            }
        }
        return IRenderer.super.getParticleTexture();
    }
}
