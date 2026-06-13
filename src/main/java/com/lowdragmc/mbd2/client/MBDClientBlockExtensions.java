package com.lowdragmc.mbd2.client;

import com.lowdragmc.mbd2.api.blockentity.ProxyPartBlockEntity;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import net.neoforged.neoforge.client.model.data.ModelData;

@OnlyIn(Dist.CLIENT)
public final class MBDClientBlockExtensions {
    public static final IClientBlockExtensions MACHINE = new IClientBlockExtensions() {
        @Override
        public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine manager) {
            if (!state.shouldSpawnTerrainParticles()) return true;
            if (level instanceof ClientLevel clientLevel && isFormedMultiblockController(level, pos)) {
                addDestroyParticles(manager, clientLevel, pos, state, Shapes.block());
                return true;
            }
            return false;
        }

        @Override
        public boolean addHitEffects(BlockState state, Level level, HitResult target, ParticleEngine manager) {
            if (!state.shouldSpawnTerrainParticles()) return true;
            if (level instanceof ClientLevel clientLevel && target instanceof BlockHitResult hit && isFormedMultiblockController(level, hit.getBlockPos())) {
                addHitParticle(manager, clientLevel, hit.getBlockPos(), hit.getDirection(), state, Shapes.block());
                return true;
            }
            return false;
        }
    };

    public static final IClientBlockExtensions PROXY_PART = new IClientBlockExtensions() {
        @Override
        public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine manager) {
            var originalState = getProxyOriginalState(level, pos);
            if (originalState == null || !originalState.shouldSpawnTerrainParticles()) return true;
            if (level instanceof ClientLevel clientLevel) {
                addDestroyParticles(manager, clientLevel, pos, originalState, originalState.getShape(level, pos));
            }
            return true;
        }

        @Override
        public boolean addHitEffects(BlockState state, Level level, HitResult target, ParticleEngine manager) {
            if (!(target instanceof BlockHitResult hit)) return true;
            var originalState = getProxyOriginalState(level, hit.getBlockPos());
            if (originalState == null || !originalState.shouldSpawnTerrainParticles()) return true;
            if (level instanceof ClientLevel clientLevel) {
                addHitParticle(manager, clientLevel, hit.getBlockPos(), hit.getDirection(), originalState, originalState.getShape(level, hit.getBlockPos()));
            }
            return true;
        }
    };

    private MBDClientBlockExtensions() {
    }

    public static TextureAtlasSprite getBlockParticleTexture(BlockState state, BlockAndTintGetter level, BlockPos pos, ModelData fallbackData) {
        var shaper = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper();
        if (level instanceof Level realLevel) {
            return shaper.getTexture(state, realLevel, pos);
        }
        return shaper.getBlockModel(state).getParticleIcon(fallbackData);
    }

    public static boolean isMissingTexture(TextureAtlasSprite sprite) {
        return MissingTextureAtlasSprite.getLocation().equals(sprite.contents().name());
    }

    private static boolean isFormedMultiblockController(Level level, BlockPos pos) {
        return IMachine.ofMachine(level, pos)
                .filter(MBDMultiblockMachine.class::isInstance)
                .map(MBDMultiblockMachine.class::cast)
                .map(MBDMultiblockMachine::isFormed)
                .orElse(false);
    }

    private static BlockState getProxyOriginalState(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof ProxyPartBlockEntity proxy) {
            return proxy.getOriginalState();
        }
        return null;
    }

    private static void addDestroyParticles(ParticleEngine manager, ClientLevel level, BlockPos pos, BlockState particleState, VoxelShape shape) {
        if (shape.isEmpty()) return;
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            double sizeX = Math.min(1.0, maxX - minX);
            double sizeY = Math.min(1.0, maxY - minY);
            double sizeZ = Math.min(1.0, maxZ - minZ);
            int countX = Math.max(2, Mth.ceil(sizeX / 0.25));
            int countY = Math.max(2, Mth.ceil(sizeY / 0.25));
            int countZ = Math.max(2, Mth.ceil(sizeZ / 0.25));

            for (int x = 0; x < countX; x++) {
                for (int y = 0; y < countY; y++) {
                    for (int z = 0; z < countZ; z++) {
                        double offsetX = ((double) x + 0.5) / (double) countX;
                        double offsetY = ((double) y + 0.5) / (double) countY;
                        double offsetZ = ((double) z + 0.5) / (double) countZ;
                        double particleX = offsetX * sizeX + minX;
                        double particleY = offsetY * sizeY + minY;
                        double particleZ = offsetZ * sizeZ + minZ;
                        manager.add(new TerrainParticle(
                                level,
                                pos.getX() + particleX,
                                pos.getY() + particleY,
                                pos.getZ() + particleZ,
                                offsetX - 0.5,
                                offsetY - 0.5,
                                offsetZ - 0.5,
                                particleState,
                                pos
                        ).updateSprite(particleState, pos));
                    }
                }
            }
        });
    }

    private static void addHitParticle(ParticleEngine manager, ClientLevel level, BlockPos pos, Direction side, BlockState particleState, VoxelShape shape) {
        if (shape.isEmpty()) return;
        RandomSource random = level.random;
        AABB bounds = shape.bounds();
        double x = pos.getX() + random.nextDouble() * (bounds.maxX - bounds.minX - 0.2F) + 0.1F + bounds.minX;
        double y = pos.getY() + random.nextDouble() * (bounds.maxY - bounds.minY - 0.2F) + 0.1F + bounds.minY;
        double z = pos.getZ() + random.nextDouble() * (bounds.maxZ - bounds.minZ - 0.2F) + 0.1F + bounds.minZ;

        if (side == Direction.DOWN) {
            y = pos.getY() + bounds.minY - 0.1F;
        } else if (side == Direction.UP) {
            y = pos.getY() + bounds.maxY + 0.1F;
        } else if (side == Direction.NORTH) {
            z = pos.getZ() + bounds.minZ - 0.1F;
        } else if (side == Direction.SOUTH) {
            z = pos.getZ() + bounds.maxZ + 0.1F;
        } else if (side == Direction.WEST) {
            x = pos.getX() + bounds.minX - 0.1F;
        } else if (side == Direction.EAST) {
            x = pos.getX() + bounds.maxX + 0.1F;
        }

        manager.add(new TerrainParticle(level, x, y, z, 0, 0, 0, particleState, pos)
                .updateSprite(particleState, pos)
                .setPower(0.2F)
                .scale(0.6F));
    }
}
