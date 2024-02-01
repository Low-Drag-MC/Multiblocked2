package com.lowdragmc.mbd2.common.item;

import com.lowdragmc.lowdraglib.client.renderer.IItemRendererProvider;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.mbd2.common.block.MBDMachineBlock;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author KilaBash
 * @date 2023/2/18
 * @implNote MetaMachineItem
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MBDMachineItem extends BlockItem implements IItemRendererProvider {

    public MBDMachineItem(MBDMachineBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public MBDMachineBlock getBlock() {
        return (MBDMachineBlock) super.getBlock();
    }

    public MBDMachineDefinition getDefinition() {
        return getBlock().getDefinition();
    }

    @Nullable
    @Override
    public IRenderer getRenderer(ItemStack stack) {
        return getDefinition().itemRenderer();
    }

}
