package com.lowdragmc.mbd2.common;

import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.mbd2.api.capability.MBDCapabilities;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeSerializer;
import com.lowdragmc.mbd2.api.recipe.ingredient.SizedIngredient;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.data.MBDRecipeCapabilities;
import com.lowdragmc.mbd2.common.data.MBDRecipeConditions;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.config.ConfigHolder;
import com.lowdragmc.mbd2.test.MBDTest;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

public class CommonProxy {

    public CommonProxy() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        eventBus.register(this);
        if (Platform.isDevEnv()) {
            eventBus.register(new MBDTest());
        }
        ConfigHolder.init();
        ForgeRegistries.RECIPE_SERIALIZERS.register("mbd_recipe_serializer", MBDRecipeSerializer.SERIALIZER);
    }

    public void registerRecipeType() {
        MBDRegistries.RECIPE_TYPES.unfreeze();
        ModLoader.get().postEvent(new MBDRegistryEvent.MBDRecipeType());
        MBDRegistries.RECIPE_TYPES.freeze();
    }

    public void registerMachine() {
        MBDRegistries.MACHINE_DEFINITIONS.unfreeze();
        ModLoader.get().postEvent(new MBDRegistryEvent.Machine());
        MBDRegistries.MACHINE_DEFINITIONS.freeze();
    }

    @SubscribeEvent
    public void constructMod(FMLConstructModEvent e) {
        e.enqueueWork(() -> {
            MBDRecipeConditions.init();
            MBDRecipeCapabilities.init();
            registerRecipeType();
            registerMachine();
        });
    }

    @SubscribeEvent
    public void commonSetup(FMLCommonSetupEvent e) {
        e.enqueueWork(() -> {
        });
        CraftingHelper.register(SizedIngredient.TYPE, SizedIngredient.SERIALIZER);
    }

    @SubscribeEvent
    public void loadComplete(FMLLoadCompleteEvent e) {
        e.enqueueWork(() -> {
        });
    }

    @SubscribeEvent
    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        MBDCapabilities.register(event);
    }

    @SubscribeEvent
    public void register(RegisterEvent event) {
        MBDRegistries.MACHINE_DEFINITIONS.forEach((definition) -> definition.onRegistry(event));
    }

    @SubscribeEvent
    public void buildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            MBDRegistries.MACHINE_DEFINITIONS.forEach((definition) -> event.accept(definition.item()));
        }
    }
}
