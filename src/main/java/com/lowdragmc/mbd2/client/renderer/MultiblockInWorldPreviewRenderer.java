package com.lowdragmc.mbd2.client.renderer;


import com.lowdragmc.lowdraglib.client.scene.WorldSceneRenderer;
import com.lowdragmc.lowdraglib.client.scene.forge.WorldSceneRendererImpl;
import com.lowdragmc.lowdraglib.client.utils.RenderUtils;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.lowdragmc.mbd2.api.block.RotationState;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.machine.IMultiController;
import com.lowdragmc.mbd2.common.block.MBDMachineBlock;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import com.lowdragmc.mbd2.common.machine.definition.MultiblockMachineDefinition;
import com.lowdragmc.mbd2.utils.ControllerBlockInfo;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import lombok.Getter;
import net.minecraft.client.Camera;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static net.minecraft.world.level.block.RenderShape.INVISIBLE;

@OnlyIn(Dist.CLIENT)
public class MultiblockInWorldPreviewRenderer {

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
        if (shapeInfos.length == 0) return;
        var shapeInfo = shapeInfos[0];

        Map<BlockPos, BlockInfo> blockMap = new HashMap<>();
        IMultiController controllerBase = null;
        LEVEL = new TrackedDummyWorld();

        var blocks = shapeInfo.getBlocks();
        BlockPos controllerPatternPos = null;
        var controllerPatternFront = Direction.NORTH;
        var maxY = 0;

        // find the pos of controller
        for (int x = 0; x < blocks.length; x++) {
            BlockInfo[][] aisle = blocks[x];
            maxY = Math.max(maxY, aisle.length);
            for (int y = 0; y < aisle.length; y++) {
                BlockInfo[] column = aisle[y];
                for (int z = 0; z < column.length; z++) {
                    // if its controller record its position offset.
                    if (column[z] instanceof ControllerBlockInfo info) {
                        controllerPatternPos = new BlockPos(x, y, z);
                        controllerPatternFront = info.getFacing();
                    } else {
                        var blockState = column[z].getBlockState();
                        if (blockState != null && blockState.getBlock() instanceof MBDMachineBlock machineBlock &&
                                machineBlock.getDefinition() instanceof MultiblockMachineDefinition definition) {
                            controllerPatternPos = new BlockPos(x, y, z);
                            if (definition.blockProperties().rotationState().property.isPresent()) {
                                controllerPatternFront = blockState.getValue(definition.blockProperties().rotationState().property.get());
                            }
                        }
                    }
                }
            }
        }

        if (controllerPatternPos == null) { // if there is no controller found
            return;
        }

        if (LAST_POS != null && LAST_POS.equals(pos)) {
            LAST_LAYER++;
            if (LAST_LAYER >= maxY) {
                LAST_LAYER = -1;
            }
        } else {
            LAST_LAYER = -1;
        }
        LAST_POS = pos;

        for (int x = 0; x < blocks.length; x++) {
            BlockInfo[][] aisle = blocks[x];
            for (int y = 0; y < aisle.length; y++) {
                BlockInfo[] column = aisle[y];
                if (LAST_LAYER != -1 && LAST_LAYER != y) {
                    continue;
                }
                for (int z = 0; z < column.length; z++) {
                    var blockState = column[z].getBlockState();
                    var offset = new BlockPos(x, y, z).subtract(controllerPatternPos);
                    if (blockState == null || offset.equals(new BlockPos(0, 0, 0))) continue;

                    // rotation
                    offset = switch (controllerPatternFront) {
                        case SOUTH -> offset.rotate(Rotation.CLOCKWISE_180);
                        case EAST -> offset.rotate(Rotation.COUNTERCLOCKWISE_90);
                        case WEST -> offset.rotate(Rotation.CLOCKWISE_90);
                        default -> offset.rotate(Rotation.NONE);
                    };
                    offset = switch (front) {
                        case SOUTH -> offset.rotate(Rotation.CLOCKWISE_180);
                        case EAST -> offset.rotate(Rotation.COUNTERCLOCKWISE_90);
                        case WEST -> offset.rotate(Rotation.CLOCKWISE_90);
                        default -> offset.rotate(Rotation.NONE);
                    };


                    // TODO rotation by front axis in the future
                    offset = rotateByFrontAxis(offset, front, Rotation.NONE);

                    if (blockState.getBlock() instanceof MBDMachineBlock machineBlock) {
                        var rotationState = machineBlock.getRotationState();
                        if (rotationState != RotationState.NONE && rotationState.property.isPresent()) {
                            var face = blockState.getValue(rotationState.property.get());
                            if (face.getAxis() != Direction.Axis.Y) {
                                face = switch (front) {
                                    case NORTH, UP, DOWN -> front;
                                    case SOUTH -> face.getOpposite();
                                    case WEST -> face.getCounterClockWise();
                                    case EAST -> face.getClockWise();
                                };
                            }
                            if (rotationState.test(face)) {
                                blockState = blockState.setValue(rotationState.property.get(), face);
                            }
                        }
                    }

                    BlockPos realPos = pos.offset(offset);

                    if (column[z].getBlockEntity(realPos) instanceof IMachineBlockEntity holder &&
                            holder.getMetaMachine() instanceof IMultiController cont) {
                        holder.getSelf().setLevel(LEVEL);
                        controllerBase = cont;
                    } else {
                        blockMap.put(realPos, BlockInfo.fromBlockState(blockState));
                    }
                }
            }
        }

        LEVEL.addBlocks(blockMap);
        if (controllerBase != null) {
            LEVEL.setInnerBlockEntity(controllerBase.getHolder());
        }

        prepareBuffers(LEVEL, blockMap.keySet(), duration);
    }

    private static BlockPos rotateByFrontAxis(BlockPos pos, Direction front, Rotation rotation) {
        if (front.getAxis() == Direction.Axis.X) {
            return switch (rotation) {
                case CLOCKWISE_90 -> new BlockPos(-pos.getX(), -front.getAxisDirection().getStep() * pos.getZ(),
                        front.getAxisDirection().getStep() * -pos.getY());
                case CLOCKWISE_180 -> new BlockPos(-pos.getX(), -pos.getY(), pos.getZ());
                case COUNTERCLOCKWISE_90 -> new BlockPos(-pos.getX(), front.getAxisDirection().getStep() * pos.getZ(),
                        front.getAxisDirection().getStep() * pos.getY());
                default -> new BlockPos(-pos.getX(), pos.getY(), -pos.getZ());
            };
        } else if (front.getAxis() == Direction.Axis.Y) {
            return switch (rotation) {
                case CLOCKWISE_90 -> new BlockPos(pos.getY(),
                        -front.getAxisDirection().getStep() * pos.getZ(),
                        -front.getAxisDirection().getStep() * pos.getX());
                case CLOCKWISE_180 -> new BlockPos(front.getAxisDirection().getStep() * pos.getX(),
                        -front.getAxisDirection().getStep() * pos.getZ(),
                        pos.getY());
                case COUNTERCLOCKWISE_90 -> new BlockPos(-pos.getY(),
                        -front.getAxisDirection().getStep() * pos.getZ(),
                        front.getAxisDirection().getStep() * pos.getX());
                default -> new BlockPos(-front.getAxisDirection().getStep() * pos.getX(),
                        -front.getAxisDirection().getStep() * pos.getZ(),
                        -pos.getY());
            };
        } else if (front.getAxis() == Direction.Axis.Z) {
            return switch (rotation) {
                case CLOCKWISE_90 -> new BlockPos(front.getAxisDirection().getStep() * pos.getY(),
                        -front.getAxisDirection().getStep() * pos.getX(), pos.getZ());
                case CLOCKWISE_180 -> new BlockPos(-pos.getX(), -pos.getY(), pos.getZ());
                case COUNTERCLOCKWISE_90 -> new BlockPos(front.getAxisDirection().getStep() * -pos.getY(),
                        front.getAxisDirection().getStep() * pos.getX(), pos.getZ());
                default -> pos;
            };
        }
        return pos;
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

    public static void renderInWorldPreview(PoseStack poseStack, Camera camera, float partialTicks) {
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
                            poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
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
                poseStack.pushPose();
                ShaderInstance shaderInstance = RenderSystem.getShader();

                for (int j = 0; j < 12; ++j) {
                    int k = RenderSystem.getShaderTexture(j);
                    shaderInstance.setSampler("Sampler" + j, k);
                }

                // setup shader uniform
                if (shaderInstance.MODEL_VIEW_MATRIX != null) {
                    shaderInstance.MODEL_VIEW_MATRIX.set(poseStack.last().pose());
                }

                if (shaderInstance.PROJECTION_MATRIX != null) {
                    shaderInstance.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
                }

                if (shaderInstance.COLOR_MODULATOR != null) {
                    shaderInstance.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
                }

                if (shaderInstance.FOG_START != null) {
                    shaderInstance.FOG_START.set(Float.MAX_VALUE);
                }

                if (shaderInstance.FOG_END != null) {
                    shaderInstance.FOG_END.set(RenderSystem.getShaderFogEnd());
                }

                if (shaderInstance.FOG_COLOR != null) {
                    shaderInstance.FOG_COLOR.set(RenderSystem.getShaderFogColor());
                }

                if (shaderInstance.FOG_SHAPE != null) {
                    shaderInstance.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
                }

                if (shaderInstance.TEXTURE_MATRIX != null) {
                    shaderInstance.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
                }

                if (shaderInstance.GAME_TIME != null) {
                    shaderInstance.GAME_TIME.set(RenderSystem.getShaderGameTime());
                }

                RenderSystem.setupShaderLights(shaderInstance);
                shaderInstance.apply();

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

                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

                vertexbuffer.bind();
                vertexbuffer.draw();

                poseStack.popPose();

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
                var buffer = new BufferBuilder(layer.bufferSize());
                buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
                renderBlocks(level, poseStack, dispatcher, layer, new WorldSceneRenderer.VertexConsumerWrapper(buffer),
                        renderedBlocks, randomSource);
                var builder = buffer.end();
                var vertexBuffer = getBUFFERS()[i];
                Runnable toUpload = () -> {
                    if (!vertexBuffer.isInvalid()) {
                        vertexBuffer.bind();
                        vertexBuffer.upload(builder);
                        VertexBuffer.unbind();
                    }
                };
                CompletableFuture.runAsync(toUpload, runnable -> {
                    RenderSystem.recordRenderCall(runnable::run);
                });

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

    private static void renderBlocks(TrackedDummyWorld level, PoseStack poseStack, BlockRenderDispatcher dispatcher,
                                     RenderType layer, WorldSceneRenderer.VertexConsumerWrapper wrapperBuffer,
                                     Collection<BlockPos> renderedBlocks, RandomSource randomSource) {
        for (BlockPos pos : renderedBlocks) {
            BlockState state = level.getBlockState(pos);
            FluidState fluidState = state.getFluidState();
            Block block = state.getBlock();
            BlockEntity te = level.getBlockEntity(pos);

            if (block == Blocks.AIR) continue;

            // render blocks
            if (state.getRenderShape() != INVISIBLE && WorldSceneRendererImpl.canRenderInLayer(dispatcher, state, pos, level, layer, randomSource)) {
                poseStack.pushPose();
                poseStack.translate(pos.getX(), pos.getY(), pos.getZ());

                poseStack.translate(0.5, 0.5, 0.5);
                poseStack.scale(0.8f, 0.8f, 0.8f);
                poseStack.translate(-0.5, -0.5, -0.5);

                level.setRenderFilter(p -> p.equals(pos));
                WorldSceneRendererImpl.renderBlocksForge(dispatcher, state, pos, level, poseStack, wrapperBuffer, randomSource, layer);
                level.setRenderFilter(p -> true);
                poseStack.popPose();
            }

            // render fluids
            if (!fluidState.isEmpty() && ItemBlockRenderTypes.getRenderLayer(fluidState) == layer) {
                wrapperBuffer.addOffset((pos.getX() - (pos.getX() & 15)), (pos.getY() - (pos.getY() & 15)),
                        (pos.getZ() - (pos.getZ() & 15)));
                dispatcher.renderLiquid(pos, level, wrapperBuffer, state, fluidState);
            }

            wrapperBuffer.clerOffset();
            wrapperBuffer.clearColor();
        }
    }
}
