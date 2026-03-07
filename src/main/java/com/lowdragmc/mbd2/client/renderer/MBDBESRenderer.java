package com.lowdragmc.mbd2.client.renderer;

import com.lowdragmc.lowdraglib2.client.renderer.ATESRRendererProvider;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author KilaBash
 * @date 2022/11/3
 * @implNote TCRendererProvider
 */
@OnlyIn(Dist.CLIENT)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MBDBESRenderer extends ATESRRendererProvider<BlockEntity> {
    private static MBDBESRenderer INSTANCE;

    private MBDBESRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static MBDBESRenderer getOrCreate(BlockEntityRendererProvider.Context context) {
        if (INSTANCE == null) {
            INSTANCE = new MBDBESRenderer(context);
        }
        return INSTANCE;
    }

    @Nullable
    public static MBDBESRenderer getInstance() {
        return INSTANCE;
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntity blockEntity) {
        var machine = IMachine.ofMachine(blockEntity)
                .filter(MBDMachine.class::isInstance)
                .map(MBDMachine.class::cast);
        if (machine.isPresent()) {
            var value = machine.get().getRenderBoundingBox();
            if (value != null) {
                return value;
            }
        }
        return super.getRenderBoundingBox(blockEntity);
    }
}
