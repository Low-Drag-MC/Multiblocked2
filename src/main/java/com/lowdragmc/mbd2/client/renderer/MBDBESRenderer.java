package com.lowdragmc.mbd2.client.renderer;

import com.lowdragmc.lowdraglib.client.renderer.ATESRRendererProvider;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

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
}
