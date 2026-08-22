package com.lowdragmc.mbd2.client.renderer;

import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Draws a vanilla beacon beam out of the machine. Useful on its own, and a reference for what a well behaved
 * {@link IRenderer} that draws far outside of its own block has to override.
 */
@LDLRegisterClient(name = "beacon_beam", registry = "ldlib2:renderer")
@Getter
@Setter
@OnlyIn(Dist.CLIENT)
public class BeaconBeamRenderer implements IRenderer {

    @Configurable(name = "config.renderer.beacon_beam.texture")
    private ResourceLocation texture = BeaconRenderer.BEAM_LOCATION;

    @Configurable(name = "config.renderer.beacon_beam.color")
    @ConfigColor
    private int color = 0xFFFFFFFF;

    @Configurable(name = "config.renderer.beacon_beam.y_offset", tips = "config.renderer.beacon_beam.y_offset.tooltip")
    @ConfigNumber(range = {-512, 512})
    private int yOffset = 0;

    @Configurable(name = "config.renderer.beacon_beam.height", tips = "config.renderer.beacon_beam.height.tooltip")
    @ConfigNumber(range = {-512, 512})
    private int height = 256;

    @Configurable(name = "config.renderer.beacon_beam.beam_radius")
    @ConfigNumber(range = {0, 16})
    private float beamRadius = 0.2f;

    @Configurable(name = "config.renderer.beacon_beam.glow_radius")
    @ConfigNumber(range = {0, 16})
    private float glowRadius = 0.25f;

    @Configurable(name = "config.renderer.beacon_beam.texture_scale")
    @ConfigNumber(range = {0.01, 64})
    private float textureScale = 1f;

    @Configurable(name = "config.renderer.beacon_beam.view_distance", tips = "config.renderer.beacon_beam.view_distance.tooltip")
    @ConfigNumber(range = {16, 1024})
    private int viewDistance = 256;

    @Override
    public boolean hasBlockEntityRenderer(BlockEntity blockEntity) {
        return true;
    }

    @Override
    public void render(BlockEntity blockEntity, float partialTicks, PoseStack stack, MultiBufferSource buffer,
                       int combinedLight, int combinedOverlay) {
        var level = blockEntity.getLevel();
        if (level == null) return;
        BeaconRenderer.renderBeaconBeam(stack, buffer, texture, partialTicks, textureScale, level.getGameTime(),
                yOffset, height, color, beamRadius, glowRadius);
    }

    @Override
    public int getViewDistance() {
        return viewDistance;
    }

    /**
     * The beam is drawn far above (or below) the block itself, so the default one block box would cull it away
     * as soon as the machine leaves the frustum.
     */
    @Override
    public AABB getRenderBoundingBox(BlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        var radius = Math.max(beamRadius, glowRadius);
        var bottom = Math.min(yOffset, yOffset + height);
        var top = Math.max(yOffset, yOffset + height);
        return new AABB(
                pos.getX() - radius, pos.getY() + bottom, pos.getZ() - radius,
                pos.getX() + 1 + radius, pos.getY() + top + 1, pos.getZ() + 1 + radius);
    }
}
