package com.lowdragmc.mbd2.common;

import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.factory.UIFactory;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.MBDCapabilities;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeSerializer;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.recipe.ingredient.SizedIngredient;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.data.MBDRecipeCapabilities;
import com.lowdragmc.mbd2.common.data.MBDRecipeConditions;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.gui.factory.MachineUIFactory;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.config.ConfigHolder;
import com.lowdragmc.mbd2.test.MBDTest;
import com.lowdragmc.mbd2.utils.FileUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
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

import java.io.File;
import java.util.concurrent.ConcurrentLinkedDeque;

public class CommonProxy {

    private final ConcurrentLinkedDeque<Runnable> postTask = new ConcurrentLinkedDeque<>();


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
        // Register UI Factory
        UIFactory.register(MachineUIFactory.INSTANCE);
    }

    public void registerRecipeType() {
        MBDRegistries.RECIPE_TYPES.unfreeze();
        var event = new MBDRegistryEvent.MBDRecipeType();
        MBD2.LOGGER.info("Loading recipe types");
        var path = new File(MBD2.getLocation(), "recipe_type");
        FileUtils.loadNBTFiles(path, ".rt", (file, tag) -> {
            var registryName = tag.getCompound("recipe_type").getString("registryName");
            if (!registryName.isEmpty() && ResourceLocation.isValidResourceLocation(registryName)) {
                var recipeType = new MBDRecipeType(new ResourceLocation(registryName));
                event.register(recipeType);
                postTask.add(() -> recipeType.postLoading(tag));
            }
        });
        ModLoader.get().postEvent(event);
        MBDRegistries.RECIPE_TYPES.freeze();
    }

    public void registerMachine() {
        MBDRegistries.MACHINE_DEFINITIONS.unfreeze();
        var event = new MBDRegistryEvent.Machine();
        MBD2.LOGGER.info("Loading machines");
        var path = new File(MBD2.getLocation(), "machine");
        FileUtils.loadNBTFiles(path, ".sm", (file, tag) -> {
            if (tag.contains("definition")) {
                var definition = MBDMachineDefinition.fromTag(tag.getCompound("definition"));
                event.register(definition);
                postTask.add(() -> definition.postLoading(tag));
            }
        });
        ModLoader.get().postEvent(event);
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
            postTask.forEach(Runnable::run);
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
