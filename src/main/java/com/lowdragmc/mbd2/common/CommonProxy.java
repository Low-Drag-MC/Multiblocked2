package com.lowdragmc.mbd2.common;

import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.gui.factory.UIFactory;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.block.ProxyPartBlock;
import com.lowdragmc.mbd2.api.blockentity.ProxyPartBlockEntity;
import com.lowdragmc.mbd2.api.capability.MBDCapabilities;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeSerializer;
import com.lowdragmc.mbd2.api.recipe.ingredient.SizedIngredient;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.data.MBDRecipeCapabilities;
import com.lowdragmc.mbd2.common.data.MBDRecipeConditions;
import com.lowdragmc.mbd2.common.data.MBDTraitDefinitions;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.gui.editor.RecipeTypeProject;
import com.lowdragmc.mbd2.common.gui.factory.MachineUIFactory;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.MultiblockMachineDefinition;
import com.lowdragmc.mbd2.config.ConfigHolder;
import com.lowdragmc.mbd2.integration.create.machine.CreateKineticMachineDefinition;
import com.lowdragmc.mbd2.integration.kubejs.events.MBDMachineRegistryEventJS;
import com.lowdragmc.mbd2.integration.kubejs.events.MBDRecipeTypeRegistryEventJS;
import com.lowdragmc.mbd2.integration.kubejs.events.MBDStartupEvents;
import com.lowdragmc.mbd2.test.MBDTest;
import com.lowdragmc.mbd2.utils.FileUtils;
import dev.latvian.mods.kubejs.script.ScriptType;
import lombok.Getter;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedDeque;

public class CommonProxy {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MBD2.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MBD2.MOD_ID);

    @Getter
    private static final ConcurrentLinkedDeque<Runnable> postTask = new ConcurrentLinkedDeque<>();


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
        // Register blocks
        BLOCKS.register("proxy_part_block", () -> ProxyPartBlock.BLOCK);
        ProxyPartBlockEntity.TYPE = BLOCK_ENTITY_TYPES.register("proxy_part_block", () -> BlockEntityType.Builder.of(ProxyPartBlockEntity::new, ProxyPartBlock.BLOCK).build(null));
        BLOCKS.register(eventBus);
        BLOCK_ENTITY_TYPES.register(eventBus);
        if (MBD2.isCreateLoaded()) {
            CreateKineticMachineDefinition.registerStressProvider();
        }
    }

    public void registerRecipeType() {
        MBDRegistries.RECIPE_TYPES.unfreeze();
        var event = new MBDRegistryEvent.MBDRecipeType();
        MBD2.LOGGER.info("Loading recipe types");
        var path = new File(MBD2.getLocation(), "recipe_type");
        //load recipe type
        FileUtils.loadNBTFiles(path, ".rt", (file, tag) -> event.register(RecipeTypeProject.createProductFromProject(tag, postTask)));
        ModLoader.get().postEvent(event);
        if (MBD2.isKubeJSLoaded()) {
            KubeJSWrapper.postRecipeTypeEvent();
        }
        MBDRegistries.RECIPE_TYPES.freeze();
    }

    public void registerMachine() {
        MBDRegistries.MACHINE_DEFINITIONS.unfreeze();
        var event = new MBDRegistryEvent.Machine();
        MBD2.LOGGER.info("Loading machines");
        var path = new File(MBD2.getLocation(), "machine");
        // load single machine
        FileUtils.loadNBTFiles(path, ".sm", (file, tag) -> event.register(MBDMachineDefinition.createDefault().loadProductiveTag(file, tag, postTask)));
        // load multiblock machine
        path = new File(MBD2.getLocation(), "multiblock");
        FileUtils.loadNBTFiles(path, ".mb", (file, tag) -> event.register(MultiblockMachineDefinition.createDefault().loadProductiveTag(file, tag, postTask)));
        if (MBD2.isCreateLoaded()) {
            // load kinetic machine
            path = new File(MBD2.getLocation(), "kinetic_machine");
            FileUtils.loadNBTFiles(path, ".km", (file, tag) -> event.register(CreateKineticMachineDefinition.createDefault().loadProductiveTag(file, tag, postTask)));
        }
        ModLoader.get().postEvent(event);
        if (MBD2.isKubeJSLoaded()) {
            KubeJSWrapper.postMachineEvent();
        }
        MBDRegistries.MACHINE_DEFINITIONS.freeze();
    }

    public static class KubeJSWrapper {
        public static void postMachineEvent() {
            MBDMachineRegistryEventJS.BUILDERS.put("single", MBDMachineDefinition::builder);
            MBDMachineRegistryEventJS.BUILDERS.put("multiblock", MultiblockMachineDefinition::builder);
            if (MBD2.isCreateLoaded()) {
                MBDMachineRegistryEventJS.BUILDERS.put("kinetic", CreateKineticMachineDefinition::builder);
            }
            MBDStartupEvents.MACHINE.post(ScriptType.STARTUP, new MBDMachineRegistryEventJS());
        }

        public static void postRecipeTypeEvent() {
            MBDStartupEvents.RECIPE_TYPE.post(ScriptType.STARTUP, new MBDRecipeTypeRegistryEventJS());
        }
    }

    @SubscribeEvent
    public void constructMod(FMLConstructModEvent e) {
        e.enqueueWork(() -> {
            MBDRecipeConditions.init();
            MBDRecipeCapabilities.init();
            MBDTraitDefinitions.init();
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
            MBDRegistries.FAKE_MACHINE().loadFactory();
            MBDRegistries.MACHINE_DEFINITIONS.forEach(MBDMachineDefinition::loadFactory);
            postTask.forEach(Runnable::run);
            postTask.clear();
        });
    }

    @SubscribeEvent
    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        MBDCapabilities.register(event);
    }

    @SubscribeEvent
    public void register(RegisterEvent event) {
        MBDRegistries.FAKE_MACHINE().onRegistry(event);
        MBDRegistries.MACHINE_DEFINITIONS.forEach((definition) -> definition.onRegistry(event));
        // register items
        event.register(ForgeRegistries.ITEMS.getRegistryKey(), helper -> helper.register(MBD2.id("mbd_gadgets"), MBDRegistries.GADGETS_ITEM()));
    }

    @SubscribeEvent
    public void buildContents(BuildCreativeModeTabContentsEvent event) {
        var tabLoc = event.getTabKey().location();
        for (var machineDefinition : MBDRegistries.MACHINE_DEFINITIONS) {
            if (machineDefinition.itemProperties().creativeTab().isEnable() &&
                    tabLoc.equals(machineDefinition.itemProperties().creativeTab().getValue())) {
                event.accept(machineDefinition.item());
            }
        }
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(MBDRegistries.GADGETS_ITEM());
        }
    }
}
