package com.lowdragmc.mbd2.client.renderer;

import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.mbd2.api.blockentity.ProxyPartBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ProxyPartRenderer implements IRenderer {
    public static final ProxyPartRenderer INSTANCE = new ProxyPartRenderer();

    private ProxyPartRenderer() {
    }

    private Optional<ProxyPartBlockEntity> getProxy(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos) {
        if (level == null || pos == null) return Optional.empty();
        return level.getBlockEntity(pos) instanceof ProxyPartBlockEntity proxy ? Optional.of(proxy) : Optional.empty();
    }

    private Optional<ProxyPartBlockEntity> getProxy(@Nullable BlockEntity blockEntity) {
        return blockEntity instanceof ProxyPartBlockEntity proxy ? Optional.of(proxy) : Optional.empty();
    }

    @Override
    public List<BakedQuad> renderModel(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData modelData, RenderType renderType) {
        return getProxy(level, pos)
                .map(proxy -> proxy.getProxyState().getRealRenderer().renderModel(level, pos, state, side, rand, modelData, renderType))
                .orElseGet(Collections::emptyList);
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleTexture(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, ModelData modelData) {
        return getProxy(level, pos)
                .map(proxy -> proxy.getProxyState().getRealRenderer().getParticleTexture(level, pos, modelData))
                .orElseGet(() -> IRenderer.super.getParticleTexture(level, pos, modelData));
    }

    @Override
    public boolean hasBlockEntityRenderer(BlockEntity blockEntity) {
        return getProxy(blockEntity)
                .map(proxy -> proxy.getProxyState().getRealRenderer().hasBlockEntityRenderer(blockEntity))
                .orElse(false);
    }

    @Override
    public boolean shouldRenderOffScreen(BlockEntity blockEntity) {
        return getProxy(blockEntity)
                .map(proxy -> proxy.getProxyState().isGlobalVisible() ||
                        proxy.getProxyState().getRealRenderer().shouldRenderOffScreen(blockEntity))
                .orElse(false);
    }

    @Override
    public boolean shouldRender(BlockEntity blockEntity, Vec3 cameraPos) {
        return getProxy(blockEntity)
                .map(proxy -> Vec3.atCenterOf(blockEntity.getBlockPos()).closerThan(cameraPos, proxy.getProxyState().renderingRadius()))
                .orElse(false);
    }

    @Override
    public void render(BlockEntity blockEntity, float partialTicks, PoseStack stack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        getProxy(blockEntity).ifPresent(proxy ->
                proxy.getProxyState().getRealRenderer().render(blockEntity, partialTicks, stack, buffer, combinedLight, combinedOverlay));
    }
}
