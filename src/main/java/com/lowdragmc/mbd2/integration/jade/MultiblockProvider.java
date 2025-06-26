package com.lowdragmc.mbd2.integration.jade;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.machine.IMultiControllerMachine;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.ArrayList;
import java.util.List;

public class MultiblockProvider implements IBlockComponentProvider {
    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor blockAccessor, IPluginConfig config) {
        IMultiControllerMachine.ofControllerMachine(blockAccessor.getBlockEntity()).ifPresent(controller -> {
            if (controller.isFormed()) return;
            tooltip.add(Component.translatable("multiblock.unformed"));
            if (controller instanceof MBDMultiblockMachine mbdMachine && mbdMachine.getDefinition().multiblockSettings().catalyst().isEnable()) {
                tooltip.add(Component.translatable("multiblock.catalyst"));
                List<ItemStack> catalystItems = new ArrayList<>();
                for (var stack : mbdMachine.getDefinition().multiblockSettings().catalyst().getFilterItems()) {
                    catalystItems.add(stack.copy());
                }
                for (var filterTag : mbdMachine.getDefinition().multiblockSettings().catalyst().getFilterTags()) {
                    BuiltInRegistries.ITEM.getTag(ItemTags.create(filterTag)).ifPresent(values -> {
                        for (var stack : values) {
                            catalystItems.add(stack.value().getDefaultInstance());
                        }
                    });
                }
                for (ItemStack catalystItem : catalystItems) {
                    tooltip.append(tooltip.getElementHelper().item(catalystItem));
                }
            }
        });
    }


    @Override
    public ResourceLocation getUid() {
        return MBD2.id("multiblock_provider");
    }
}
