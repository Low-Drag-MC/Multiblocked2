package com.lowdragmc.mbd2.common.data;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.item.MBDGadgetsItem;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MBDDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MBD2.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> GADGET_RECIPE =
            COMPONENTS.registerComponentType("gadget_recipe", b -> b
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MBDGadgetsItem.Mode>> GADGET_MODE =
            COMPONENTS.registerComponentType("gadget_mode", b -> b
                    .persistent(MBDGadgetsItem.Mode.CODEC)
                    .networkSynchronized(MBDGadgetsItem.Mode.STREAM_CODEC));
}
