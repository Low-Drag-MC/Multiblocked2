package com.lowdragmc.mbd2.integration.create.machine;

import com.jozufozu.flywheel.backend.Backend;
import com.lowdragmc.lowdraglib.client.renderer.IItemRendererProvider;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.gui.editor.ui.Editor;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import com.lowdragmc.mbd2.common.gui.editor.texture.IRendererSlotTexture;
import com.lowdragmc.mbd2.common.item.MBDMachineItem;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.BakedModelRenderHelper;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBufferCache;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.data.ModelData;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * used to render flywheel instance
 */
@OnlyIn(Dist.CLIENT)
public record KineticInstanceRenderer(IRenderer baseRenderer, BakedModel rotationModel) implements IRenderer {
    public static final SuperByteBufferCache.Compartment<Pair<Direction, BakedModel>> DIRECTIONAL_PARTIAL = new SuperByteBufferCache.Compartment<>();

    @Override
    public void renderItem(ItemStack stack, ItemDisplayContext transformType, boolean leftHand, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay, BakedModel model) {
        IItemRendererProvider.disabled.set(true);
        baseRenderer.renderItem(stack, transformType, leftHand, poseStack, bufferSource, combinedLight, combinedOverlay, model);
        CreateKineticMachineDefinition definition = null;
        if (stack.getItem() instanceof MBDMachineItem item && item.getDefinition() instanceof CreateKineticMachineDefinition createKineticMachineDefinition) {
            definition = createKineticMachineDefinition;
        } else if (IRendererSlotTexture.CURRENT_MACHINE_DEFINITION instanceof CreateKineticMachineDefinition createKineticMachineDefinition) {
            definition = createKineticMachineDefinition;
        }

        if (definition != null && stack.getItem() instanceof BlockItem blockItem) {
            poseStack.pushPose();
            ForgeHooksClient.handleCameraTransforms(poseStack, rotationModel, transformType, leftHand);
            poseStack.translate(-0.5, -0.5, -0.5);
            var rotationFacing = definition.kineticMachineSettings().getRotationFacing(Direction.NORTH);
            var axis = rotationFacing.getAxis();
            var angle = 3.1415927f * 0.3f;
            var state = blockItem.getBlock().defaultBlockState();
            var superByteBuffer = CreateClient.BUFFER_CACHE.get(DIRECTIONAL_PARTIAL, Pair.of(rotationFacing, rotationModel),
                    () -> BakedModelRenderHelper.standardModelRender(rotationModel, state, CachedBufferer.rotateToFaceVertical(rotationFacing).get()));
            superByteBuffer.light(combinedLight);
            superByteBuffer.rotateCentered(Direction.get(Direction.AxisDirection.POSITIVE, axis), angle);
            for (var renderType : rotationModel.getRenderTypes(state, RandomSource.create(49), ModelData.EMPTY)) {
                superByteBuffer.renderInto(poseStack, bufferSource.getBuffer(renderType));
            }
            poseStack.popPose();
        }
        IItemRendererProvider.disabled.set(false);
    }

    @Override
    public List<BakedQuad> renderModel(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return baseRenderer.renderModel(level, pos, state, side, rand);
    }

    private boolean isInvalid(BlockEntity te) {
        return !te.hasLevel() || te.getBlockState().getBlock() == Blocks.AIR;
    }

    @Override
    public boolean hasTESR(BlockEntity blockEntity) {
        if (baseRenderer.hasTESR(blockEntity)) return true;
        var machineOptional = IMachine.ofMachine(blockEntity).filter(MBDMachine.class::isInstance).map(MBDMachine.class::cast);
        if (machineOptional.isEmpty()) return false;
        var machine = machineOptional.get();
        if (machine.getDefinition() instanceof CreateKineticMachineDefinition definition) {
            return !definition.kineticMachineSettings().useFlywheel() || !Backend.canUseInstancing(blockEntity.getLevel());
        }
        return false;
    }

    @Override
    public void render(BlockEntity blockEntity, float partialTicks, PoseStack stack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        baseRenderer.render(blockEntity, partialTicks, stack, buffer, combinedLight, combinedOverlay);
        if (isInvalid(blockEntity)) return;
        renderSafe(blockEntity, stack, buffer, combinedLight, combinedOverlay, partialTicks);
    }

    public void renderSafe(BlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay, float partialTicks) {
        var machineOptional = IMachine.ofMachine(blockEntity).filter(MBDMachine.class::isInstance).map(MBDMachine.class::cast);
        if (machineOptional.isEmpty()) return;
        var machine = machineOptional.get();
        if (machine.getDefinition() instanceof CreateKineticMachineDefinition definition) {
            if (Backend.canUseInstancing(blockEntity.getLevel()) && definition.kineticMachineSettings.useFlywheel()) return;
            var state = blockEntity.getBlockState();
            var rotationFacing = definition.kineticMachineSettings().getRotationFacing(machine.getFrontFacing().orElse(Direction.NORTH));
            var axis = rotationFacing.getAxis();
            var angle = 0f;

            var superByteBuffer = CreateClient.BUFFER_CACHE.get(DIRECTIONAL_PARTIAL, Pair.of(rotationFacing, rotationModel),
                    () -> BakedModelRenderHelper.standardModelRender(rotationModel, state, CachedBufferer.rotateToFaceVertical(rotationFacing).get()));

            // its a real block entity
            var time = AnimationTickHolder.getRenderTime(blockEntity.getLevel());
            if (blockEntity instanceof KineticBlockEntity kineticBlockEntity) {
                var pos = blockEntity.getBlockPos();

                var offset = KineticBlockEntityRenderer.getRotationOffsetForPosition(kineticBlockEntity, pos, axis);
                angle = time * kineticBlockEntity.getSpeed() * 3.0F / 10.0F % 360.0F;
                angle += offset;
                angle = angle / 180.0F * 3.1415927F;

                KineticBlockEntityRenderer.kineticRotationTransform(superByteBuffer, kineticBlockEntity, axis, angle, light);
            } else {
                if (Editor.INSTANCE instanceof MachineEditor editor && editor.getCurrentProject() instanceof CraeteKinecticMachineProject project && project.isRotating()) {
                    angle = time * project.getSpeed() * 3.0F / 10.0F % 360.0F;
                    angle = angle / 180.0F * 3.1415927F;
                }
                superByteBuffer.light(light);
                superByteBuffer.rotateCentered(Direction.get(Direction.AxisDirection.POSITIVE, axis), angle);
            }

            for (var renderType : rotationModel.getRenderTypes(state, blockEntity.getLevel().getRandom(), ModelData.EMPTY)) {
                superByteBuffer.renderInto(poseStack, bufferSource.getBuffer(renderType));
            }
        }
    }

    @Override
    public boolean isGlobalRenderer(BlockEntity blockEntity) {
        return baseRenderer.isGlobalRenderer(blockEntity);
    }

    @Override
    public int getViewDistance() {
        return baseRenderer.getViewDistance();
    }

    @Override
    public boolean shouldRender(BlockEntity blockEntity, Vec3 cameraPos) {
        return baseRenderer.shouldRender(blockEntity, cameraPos);
    }

    @NotNull
    @Override
    public TextureAtlasSprite getParticleTexture() {
        return baseRenderer.getParticleTexture();
    }

    @Override
    public boolean useAO() {
        return baseRenderer.useAO();
    }

    @Override
    public boolean useAO(BlockState state) {
        return baseRenderer.useAO(state);
    }

    @Override
    public boolean useBlockLight(ItemStack stack) {
        return baseRenderer.useBlockLight(stack);
    }

    @Override
    public boolean reBakeCustomQuads() {
        return baseRenderer.reBakeCustomQuads();
    }

    @Override
    public float reBakeCustomQuadsOffset() {
        return baseRenderer.reBakeCustomQuadsOffset();
    }

    @Override
    public boolean isGui3d() {
        return baseRenderer.isGui3d();
    }
}
