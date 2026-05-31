package com.lowdragmc.mbd2.integration.create.machine;

import com.lowdragmc.lowdraglib2.client.renderer.IItemRendererProvider;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.item.MBDMachineItem;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.animation.AnimationTickHolder;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperBufferFactory;
import net.createmod.catnip.render.SuperByteBufferCache;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Composes a base {@link IRenderer} (the user-configured block/state renderer) with a Create-style
 * spinning kinetic shaft model rendered on top. Wraps the base renderer so the block draws normally,
 * and adds the rotation pass driven by the BE's kinetic speed when present (or a fixed angle for
 * item/preview rendering).
 */
@OnlyIn(Dist.CLIENT)
public record KineticInstanceRenderer(IRenderer baseRenderer, PartialModel rotationPartial) implements IRenderer {
    public static final SuperByteBufferCache.Compartment<Pair<Direction, BakedModel>> DIRECTIONAL_PARTIAL =
            new SuperByteBufferCache.Compartment<>();

    /**
     * Editor-side hook: when set, the renderer treats a non-{@link KineticBlockEntity} block
     * entity (i.e. the editor scene's preview BE) as spinning at this RPM. Set by
     * {@code CreateMachineConfigView.renderAfterWorld}, cleared in {@code finally}.
     */
    public static final ThreadLocal<Float> EDITOR_PREVIEW_SPEED = new ThreadLocal<>();

    @Override
    public void renderItem(ItemStack stack, ItemDisplayContext transformType, boolean leftHand,
                           PoseStack poseStack, MultiBufferSource bufferSource,
                           int combinedLight, int combinedOverlay, BakedModel model) {
        IItemRendererProvider.disabled.set(true);
        baseRenderer.renderItem(stack, transformType, leftHand, poseStack, bufferSource,
                combinedLight, combinedOverlay, model);
        if (stack.getItem() instanceof MBDMachineItem item
                && item.getDefinition() instanceof CreateKineticMachineDefinition definition
                && stack.getItem() instanceof BlockItem blockItem) {
            var rotationBakedModel = rotationPartial.get();
            poseStack.pushPose();
            ClientHooks.handleCameraTransforms(poseStack, rotationBakedModel, transformType, leftHand);
            poseStack.translate(-0.5, -0.5, -0.5);
            var rotationFacing = definition.kineticMachineSettings().getRotationFacing(Direction.NORTH);
            var axis = rotationFacing.getAxis();
            var angle = 3.1415927f * 0.3f;
            var state = blockItem.getBlock().defaultBlockState();
            var superByteBuffer = SuperByteBufferCache.getInstance().get(DIRECTIONAL_PARTIAL,
                    Pair.of(rotationFacing, rotationBakedModel),
                    () -> SuperBufferFactory.getInstance().createForBlock(rotationBakedModel, state,
                            CachedBuffers.rotateToFaceVertical(rotationFacing).get()));
            superByteBuffer.light(combinedLight);
            superByteBuffer.rotateCentered(angle, Direction.get(Direction.AxisDirection.POSITIVE, axis));
            for (var renderType : rotationBakedModel.getRenderTypes(state, RandomSource.create(49), ModelData.EMPTY)) {
                superByteBuffer.renderInto(poseStack, bufferSource.getBuffer(renderType));
            }
            poseStack.popPose();
        }
        IItemRendererProvider.disabled.set(false);
    }

    @Override
    public void render(BlockEntity blockEntity, float partialTicks, PoseStack poseStack,
                       MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
        baseRenderer.render(blockEntity, partialTicks, poseStack, bufferSource, combinedLight, combinedOverlay);
        if (blockEntity == null || !blockEntity.hasLevel()
                || blockEntity.getBlockState().getBlock() == Blocks.AIR) return;
        var machineOpt = IMachine.ofMachine(blockEntity).filter(MBDMachine.class::isInstance).map(MBDMachine.class::cast);
        if (machineOpt.isEmpty()) return;
        var machine = machineOpt.get();
        if (!(machine.getDefinition() instanceof CreateKineticMachineDefinition definition)) return;
        // When Flywheel can render this BE, skip — the MBDKineticInstance handles it.
        if (VisualizationManager.supportsVisualization(blockEntity.getLevel())) return;

        var rotationBakedModel = rotationPartial.get();
        var state = blockEntity.getBlockState();
        var rotationFacing = definition.kineticMachineSettings().getRotationFacing(
                machine.getFrontFacing().orElse(Direction.NORTH));
        var axis = rotationFacing.getAxis();

        var superByteBuffer = SuperByteBufferCache.getInstance().get(DIRECTIONAL_PARTIAL,
                Pair.of(rotationFacing, rotationBakedModel),
                () -> SuperBufferFactory.getInstance().createForBlock(rotationBakedModel, state,
                        CachedBuffers.rotateToFaceVertical(rotationFacing).get()));

        if (blockEntity instanceof KineticBlockEntity kineticBE) {
            var pos = blockEntity.getBlockPos();
            var angle = KineticBlockEntityRenderer.getAngleForBe(kineticBE, pos, axis);
            KineticBlockEntityRenderer.kineticRotationTransform(superByteBuffer, kineticBE, axis, angle, combinedLight);
        } else {
            superByteBuffer.light(combinedLight);
            Float previewSpeed = EDITOR_PREVIEW_SPEED.get();
            if (previewSpeed != null) {
                // Mirror Create's angle math: angle(rad) = time / 20 * speed * PI / 30 (PI/30 = 6deg per rpm-tick).
                float angle = (AnimationTickHolder.getRenderTime() / 20f) * previewSpeed * ((float) Math.PI / 30f);
                superByteBuffer.rotateCentered(angle, Direction.get(Direction.AxisDirection.POSITIVE, axis));
            } else {
                superByteBuffer.rotateCentered(3.1415927f * 0.3f, Direction.get(Direction.AxisDirection.POSITIVE, axis));
            }
        }

        for (var renderType : rotationBakedModel.getRenderTypes(state, blockEntity.getLevel().getRandom(), ModelData.EMPTY)) {
            superByteBuffer.renderInto(poseStack, bufferSource.getBuffer(renderType));
        }
    }

    @Override
    public boolean hasBlockEntityRenderer(BlockEntity blockEntity) {
        return true;
    }

    @Override
    public List<BakedQuad> renderModel(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos,
                                       @Nullable BlockState state, @Nullable Direction side,
                                       RandomSource rand, ModelData data, @Nullable net.minecraft.client.renderer.RenderType renderType) {
        return baseRenderer.renderModel(level, pos, state, side, rand, data, renderType);
    }

    @Override
    public int getViewDistance() {
        return baseRenderer.getViewDistance();
    }

    @Override
    public boolean shouldRender(BlockEntity blockEntity, Vec3 cameraPos) {
        return baseRenderer.shouldRender(blockEntity, cameraPos);
    }

    @Override
    @NotNull
    public TextureAtlasSprite getParticleTexture(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, ModelData modelData) {
        return baseRenderer.getParticleTexture(level, pos, modelData);
    }

    @Override
    public boolean useBlockLight(ItemStack stack) {
        return baseRenderer.useBlockLight(stack);
    }

    @Override
    public boolean isGui3d() {
        return baseRenderer.isGui3d();
    }
}
