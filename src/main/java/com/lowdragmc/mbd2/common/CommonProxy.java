package com.lowdragmc.mbd2.common;

import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.client.renderer.impl.IModelRenderer;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.MBDCapabilities;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeSerializer;
import com.lowdragmc.mbd2.api.recipe.ingredient.SizedIngredient;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.data.MBDRecipeCapabilities;
import com.lowdragmc.mbd2.common.data.MBDRecipeConditions;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigBlockProperties;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigItemProperties;
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState;
import com.lowdragmc.mbd2.common.machine.definition.config.StateMachine;
import com.lowdragmc.mbd2.config.ConfigHolder;
import com.lowdragmc.mbd2.test.MBDTest;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

public class CommonProxy {

    public CommonProxy() {
        MBD2.getLocation();
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        eventBus.register(this);
        if (Platform.isDevEnv()) {
            eventBus.register(new MBDTest());
        }
        ForgeRegistries.RECIPE_SERIALIZERS.register("mbd_recipe_serializer", MBDRecipeSerializer.SERIALIZER);
        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ConfigHolder.SPEC);
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
        MBDRegistries.getFAKE_MACHINE().onRegistry(event);
        MBDRegistries.MACHINE_DEFINITIONS.forEach((definition) -> definition.onRegistry(event));
    }

    @SubscribeEvent
    public void buildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            MBDRegistries.MACHINE_DEFINITIONS.forEach((definition) -> event.accept(definition.item()));
        }
    }
}
