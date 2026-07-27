package com.lowdragmc.mbd2.client.renderer;


import com.lowdragmc.lowdraglib2.client.scene.WorldSceneRenderer;
import com.lowdragmc.lowdraglib2.client.utils.RenderUtils;
import com.lowdragmc.lowdraglib2.utils.data.BlockInfo;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.TrackedDummyWorld;
import com.lowdragmc.mbd2.api.block.RotationState;
import com.lowdragmc.mbd2.api.pattern.util.RotationHelper;
import com.lowdragmc.mbd2.common.block.MBDMachineBlock;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import com.lowdragmc.mbd2.common.machine.definition.MultiblockMachineDefinition;
import com.lowdragmc.mbd2.utils.ControllerBlockInfo;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static net.minecraft.world.level.block.RenderShape.INVISIBLE;

@OnlyIn(Dist.CLIENT)
public class MultiblockInWorldPreviewRenderer {

    /** Each preview block is drawn at this fraction of its cell, centered. */
    private static final float PREVIEW_SCALE = 0.8f;

    private enum CacheState {
        UNUSED,
        COMPILING,
        COMPILED
    }

    @Getter(lazy = true)
    private final static VertexBuffer[] BUFFERS = initBuffers();
    @Nullable
    private static TrackedDummyWorld LEVEL = null;
    @Nullable
    private static Thread THREAD = null;
    @Nullable
    private static Set<BlockPos> BLOCK_ENTITIES;
    private final static AtomicInteger PREVIEW_LEFT_TICK = new AtomicInteger(-1);
    @Nullable
    private static BlockPos PATTERN_ERROR_POS = null;
    private final static AtomicInteger PATTERN_ERROR_LEFT_TICK = new AtomicInteger(-1);

    /**
     * It will be cached by lombok#@Getter(lazy=true)
     */
    private static VertexBuffer[] initBuffers() {
        List<RenderType> layers = RenderType.chunkBufferLayers();
        var buffers = new VertexBuffer[layers.size()];
        for (int j = 0; j < layers.size(); ++j) {
            buffers[j] = new VertexBuffer(VertexBuffer.Usage.STATIC);
        }
        return buffers;
    }

    private final static AtomicReference<CacheState> CACHE_STATE = new AtomicReference<>(CacheState.UNUSED);

    @Nullable
    private static BlockPos LAST_POS = null;
    private static int LAST_LAYER = -1;

    public static void cleanPreview() {
        CACHE_STATE.set(CacheState.UNUSED);
        LEVEL = null;
        BLOCK_ENTITIES = null;
        PREVIEW_LEFT_TICK.set(-1);
        LAST_POS = null;
        LAST_LAYER = -1;
    }

    public static void removePreview(BlockPos pos) {
        if (LAST_POS != null && LAST_POS.equals(pos)) {
            cleanPreview();
        }
    }

    public static void clearPatternError() {
        PATTERN_ERROR_POS = null;
        PATTERN_ERROR_LEFT_TICK.set(-1);
    }

    public static void showPatternErrorPos(BlockPos pos, int duration) {
        PATTERN_ERROR_POS = pos;
        PATTERN_ERROR_LEFT_TICK.set(duration);
    }

    /**
     * Show the multiblock preview in the world by the given pos, side, and shape info.
     *
     * @param pos        the pos of the controller
     * @param controller the controller
     * @param duration   the duration of the preview. in ticks.
     */
    public static void showPreview(BlockPos pos, MBDMultiblockMachine controller, int duration) {
        var front = controller.getFrontFacing().orElse(Direction.NORTH);
        var shapeInfos = controller.getDefinition().shapeInfoFactory().apply(controller.getDefinition());
        if (shapeInfos == null || shapeInfos.length == 0) return;
        var shapeInfo = shapeInfos[0];
        var blocks = shapeInfo.getBlocks();
        if (blocks.length == 0) return;

        // Locate the controller cell within the pattern + its pattern-space facing.
        BlockPos controllerPatternPos = null;
        Direction controllerPatternFront = Direction.NORTH;
        int maxY = 0;
        for (int x = 0; x < blocks.length; x++) {
            BlockInfo[][] aisle = blocks[x];
            maxY = Math.max(maxY, aisle.length);
            for (int y = 0; y < aisle.length; y++) {
                BlockInfo[] column = aisle[y];
                for (int z = 0; z < column.length; z++) {
                    var info = column[z];
                    if (info instanceof ControllerBlockInfo cInfo) {
                        controllerPatternPos = new BlockPos(x, y, z);
                        controllerPatternFront = cInfo.getFacing();
                    } else if (info != null) {
                        var blockState = info.getBlockState();
                        if (blockState != null && blockState.getBlock() instanceof MBDMachineBlock machineBlock
                                && machineBlock.getDefinition() instanceof MultiblockMachineDefinition def) {
                            controllerPatternPos = new BlockPos(x, y, z);
                            var property = def.blockProperties().rotationState().property;
                            if (property.isPresent()) {
                                controllerPatternFront = blockState.getValue(property.get());
                            }
                        }
                    }
                }
            }
        }

        if (controllerPatternPos == null) return;

        // Repeated invocations at the same world pos cycle through Y-layers, then "all".
        if (LAST_POS != null && LAST_POS.equals(pos)) {
            LAST_LAYER++;
            if (LAST_LAYER >= maxY) {
                LAST_LAYER = -1;
            }
        } else {
            LAST_LAYER = -1;
        }
        LAST_POS = pos;

        LEVEL = new TrackedDummyWorld();
        Map<BlockPos, BlockInfo> blockMap = new HashMap<>();

        for (int x = 0; x < blocks.length; x++) {
            BlockInfo[][] aisle = blocks[x];
            for (int y = 0; y < aisle.length; y++) {
                if (LAST_LAYER != -1 && LAST_LAYER != y) continue;
                BlockInfo[] column = aisle[y];
                for (int z = 0; z < column.length; z++) {
                    var info = column[z];
                    if (info == null) continue;
                    var blockState = info.getBlockState();
                    if (blockState == null) continue;
                    var offset = new BlockPos(x, y, z).subtract(controllerPatternPos);
                    if (offset.equals(BlockPos.ZERO)) continue; // skip controller cell

                    offset = rotateForFacing(offset, controllerPatternFront);
                    offset = rotateForFacing(offset, front);

                    if (blockState.getBlock() instanceof MBDMachineBlock machineBlock) {
                        var rotationState = machineBlock.getRotationState();
                        if (rotationState != RotationState.NONE && rotationState.property.isPresent()) {
                            var property = rotationState.property.get();
                            var face = blockState.getValue(property);
                            if (face.getAxis() != Direction.Axis.Y) {
                                face = switch (front) {
                                    case SOUTH -> face.getOpposite();
                                    case WEST -> face.getCounterClockWise();
                                    case EAST -> face.getClockWise();
                                    default -> face;
                                };
                            }
                            if (rotationState.test(face)) {
                                blockState = blockState.setValue(property, face);
                            }
                        }
                    } else {
                        // Pattern data is authored canonical (controller facing NORTH); rotate the
                        // raw vanilla state onto the world controller's actual facing. MBD blocks
                        // are handled above via their explicit rotation property.
                        var stateRotation = RotationHelper.rotationFromFacing(front);
                        if (stateRotation != Rotation.NONE) {
                            blockState = blockState.rotate(stateRotation);
                        }
                    }

                    BlockPos realPos = pos.offset(offset);
                    BlockInfo resolved = blockState == info.getBlockState() ? info : BlockInfo.fromBlockState(blockState);
                    blockMap.put(realPos, resolved);
                    LEVEL.addBlock(realPos, resolved);
                }
            }
        }

        if (blockMap.isEmpty()) return;
        prepareBuffers(LEVEL, blockMap.keySet(), duration);
    }

    private static BlockPos rotateForFacing(BlockPos offset, Direction facing) {
        return RotationHelper.rotateOffset(offset, facing);
    }

    public static void onClientTick() {
        if (PREVIEW_LEFT_TICK.get() > 0) {
            if (PREVIEW_LEFT_TICK.decrementAndGet() <= 0) {
                cleanPreview();
            }
        }
        if (PATTERN_ERROR_LEFT_TICK.get() > 0) {
            if (PATTERN_ERROR_LEFT_TICK.decrementAndGet() <= 0) {
                clearPatternError();
            }
        }
    }

    public static void renderInWorldPreview(RenderLevelStageEvent event) {
        var poseStack = event.getPoseStack();
        var camera = event.getCamera();
        var partialTicks = event.getPartialTick().getGameTimeDeltaTicks();
        if (PATTERN_ERROR_POS != null) {
            poseStack.pushPose();
            Vec3 projectedView = camera.getPosition();
            poseStack.translate(-projectedView.x, -projectedView.y, -projectedView.z);

            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);

            RenderUtils.renderBlockOverLay(poseStack, PATTERN_ERROR_POS, 0.6f, 0, 0, 1.01f);

            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();

            poseStack.popPose();
        }
        if (CACHE_STATE.get() == CacheState.COMPILED && LEVEL != null) {
            poseStack.pushPose();
            Vec3 projectedView = camera.getPosition();
            poseStack.translate(-projectedView.x, -projectedView.y, -projectedView.z);

            for (int i = 0; i < RenderType.chunkBufferLayers().size(); i++) {
                var layer = RenderType.chunkBufferLayers().get(i);
                // render TESR before translucent
                if (layer == RenderType.translucent() && BLOCK_ENTITIES != null) { // render tesr before translucent
                    var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
                    for (BlockPos pos : BLOCK_ENTITIES) {
                        BlockEntity tile = LEVEL.getBlockEntity(pos);
                        if (tile != null) {
                            poseStack.pushPose();
                            poseStack.translate(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f);
                            poseStack.scale(PREVIEW_SCALE, PREVIEW_SCALE, PREVIEW_SCALE);
                            poseStack.translate(-0.5f, -0.5f, -0.5f);
                            BlockEntityRenderer<BlockEntity> ber = Minecraft.getInstance()
                                    .getBlockEntityRenderDispatcher().getRenderer(tile);
                            if (ber != null) {
                                if (tile.hasLevel() && tile.getType().isValid(tile.getBlockState())) {
                                    ber.render(tile, partialTicks, poseStack, buffers, 0xF000F0,
                                            OverlayTexture.NO_OVERLAY);
                                }
                            }
                            poseStack.popPose();
                        }
                    }
                    buffers.endBatch();
                }

                VertexBuffer vertexbuffer = getBUFFERS()[i];
                // some of stupid mod doesn't check if the buffer is invalid
                if (vertexbuffer.isInvalid() || vertexbuffer.getFormat() == null) continue;

                // render cache vbo
                layer.setupRenderState();
                ShaderInstance shaderInstance = RenderSystem.getShader();

                // Vanilla's RenderLevelStageEvent hands us a fresh, identity PoseStack — the
                // camera/view rotation lives in event.getModelViewMatrix() (== frustumMatrix).
                // Our poseStack only carries T(-cameraPos). Compose the two so geometry lands at
                // its real world position instead of stuck to the screen.
                var modelView = new Matrix4f(event.getModelViewMatrix()).mul(poseStack.last().pose());
                shaderInstance.setDefaultUniforms(VertexFormat.Mode.QUADS,
                        modelView, event.getProjectionMatrix(), Minecraft.getInstance().getWindow());

                if (shaderInstance.FOG_START != null) {
                    shaderInstance.FOG_START.set(Float.MAX_VALUE);
                }

                RenderSystem.setShaderColor(1, 1, 1, 1);
                if (layer == RenderType.translucent()) { // TRANSLUCENT
                    RenderSystem.enableBlend();
                    RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                    RenderSystem.depthMask(false);
                } else { // SOLID
                    RenderSystem.enableDepthTest();
                    RenderSystem.disableBlend();
                    RenderSystem.depthMask(true);
                }

                shaderInstance.apply();
                vertexbuffer.bind();
                vertexbuffer.draw();
                shaderInstance.clear();
                VertexBuffer.unbind();
                layer.clearRenderState();
            }
            poseStack.popPose();
        }
    }

    private static void prepareBuffers(TrackedDummyWorld level, Collection<BlockPos> renderedBlocks, int duration) {
        if (THREAD != null) {
            THREAD.interrupt();
        }
        CACHE_STATE.set(CacheState.COMPILING);
        // call it to init the buffers
        getBUFFERS();
        THREAD = new Thread(() -> {
            var dispatcher = Minecraft.getInstance().getBlockRenderer();
            ModelBlockRenderer.enableCaching();
            PoseStack poseStack = new PoseStack();
            var randomSource = RandomSource.createNewThreadLocalInstance();
            for (int i = 0; i < RenderType.chunkBufferLayers().size(); i++) {
                if (Thread.interrupted())
                    return;
                var layer = RenderType.chunkBufferLayers().get(i);
                var buffer = new BufferBuilder(new ByteBufferBuilder(layer.bufferSize()), VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
                renderBlocks(level, poseStack, dispatcher, layer, new WorldSceneRenderer.VertexConsumerWrapper(buffer),
                        renderedBlocks, randomSource);
                var meshData = buffer.build();
                if (meshData != null) {
                    var vertexBuffer = getBUFFERS()[i];
                    CompletableFuture.runAsync(() -> {
                        if (!vertexBuffer.isInvalid()) {
                            vertexBuffer.bind();
                            vertexBuffer.upload(meshData);
                            VertexBuffer.unbind();
                        }
                    }, runnable -> RenderSystem.recordRenderCall(runnable::run));
                }
            }
            ModelBlockRenderer.clearCache();

            // record all BlockEntities having TESR.
            Set<BlockPos> poses = new HashSet<>();
            for (BlockPos pos : renderedBlocks) {
                if (Thread.interrupted())
                    return;
                BlockEntity tile = level.getBlockEntity(pos);
                if (tile != null) {
                    if (Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(tile) != null) {
                        poses.add(pos);
                    }
                }
            }

            if (Thread.interrupted())
                return;
            BLOCK_ENTITIES = poses;
            CACHE_STATE.set(CacheState.COMPILED);
            THREAD = null;
            PREVIEW_LEFT_TICK.set(duration);
        });
        THREAD.start();
    }

    private static void renderBlocks(TrackedDummyWorld level, PoseStack poseStack, BlockRenderDispatcher brd,
                                     RenderType layer, WorldSceneRenderer.VertexConsumerWrapper wrapperBuffer,
                                     Collection<BlockPos> renderedBlocks, RandomSource randomSource) {
        for (BlockPos pos : renderedBlocks) {
            BlockState state = level.getBlockState(pos);
            FluidState fluidState = state.getFluidState();
            Block block = state.getBlock();
            BlockEntity te = level.getBlockEntity(pos);

            if (block == Blocks.AIR) continue;

            if (state.getRenderShape() != INVISIBLE) {
                var model = brd.getBlockModel(state);
                var modelData = level.getModelData(pos);
                modelData = model.getModelData(level, pos, state, modelData);
                randomSource.setSeed(state.getSeed(pos));
                modelData = model.getModelData(level, pos, state, modelData);
                if (model.getRenderTypes(state, randomSource, modelData).contains(layer)) {
                    poseStack.pushPose();
                    poseStack.translate(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f);
                    poseStack.scale(PREVIEW_SCALE, PREVIEW_SCALE, PREVIEW_SCALE);
                    poseStack.translate(-0.5f, -0.5f, -0.5f);
                    brd.renderBatched(state, pos, level, poseStack, wrapperBuffer, false, randomSource, modelData, layer);
                    poseStack.popPose();
                }
            }

            if (!fluidState.isEmpty() && ItemBlockRenderTypes.getRenderLayer(fluidState) == layer) {
                wrapperBuffer.addOffset((pos.getX() - (pos.getX() & 15)), (pos.getY() - (pos.getY() & 15)), (pos.getZ() - (pos.getZ() & 15)));
                brd.renderLiquid(pos, level, wrapperBuffer, state, fluidState);
            }

            wrapperBuffer.clearOffset();
            wrapperBuffer.clearColor();
        }
    }
}
