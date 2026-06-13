package com.lowdragmc.mbd2.common;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.ui.EditorWindow;
import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.block.ProxyPartBlock;
import com.lowdragmc.mbd2.api.blockentity.ProxyPartBlockEntity;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeSerializer;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.data.MBDDataComponents;
import com.lowdragmc.mbd2.common.data.MBDRecipeCapabilities;
import com.lowdragmc.mbd2.common.data.MBDMachineDefinitionTypes;
import com.lowdragmc.mbd2.common.data.MBDTraitDefinitionTypes;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.gui.editor.MBDEditor;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.MultiblockMachineDefinition;
import com.lowdragmc.mbd2.common.network.MBD2Network;
import com.lowdragmc.mbd2.config.ConfigHolder;
import com.lowdragmc.mbd2.integration.kubejs.events.MBDMachineRegistryEventJS;
import com.lowdragmc.mbd2.integration.kubejs.events.MBDRecipeTypeRegistryEventJS;
import com.lowdragmc.mbd2.integration.kubejs.events.MBDStartupEvents;
import com.lowdragmc.mbd2.test.framework.MBDTestRegistry;
import com.lowdragmc.mbd2.utils.FileUtils;
import dev.latvian.mods.kubejs.script.ScriptType;
import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedDeque;

public class CommonProxy {
    @Getter
    private static final ConcurrentLinkedDeque<Runnable> postTask = new ConcurrentLinkedDeque<>();

    public CommonProxy(IEventBus eventBus, ModContainer modContainer) {
        eventBus.register(this);
        if (Platform.isDevEnv()) {
            if (GameTestHooks.isGametestServer()) {
                eventBus.register(new MBDTestRegistry());
                MBDTestRegistry.init();
            }
        }
        eventBus.addListener(MBD2Network::registerPayloads);
        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, ConfigHolder.SPEC);
        // Register blocks
        MBDRegistries.init();
        MBDRegistries.BLOCKS.register("proxy_part_block", () -> ProxyPartBlock.BLOCK);
        ProxyPartBlockEntity.TYPE = MBDRegistries.BLOCK_ENTITY_TYPES.register("proxy_part_block", () -> BlockEntityType.Builder.of(ProxyPartBlockEntity::new, ProxyPartBlock.BLOCK).build(null));
        MBDRegistries.BLOCKS.register(eventBus);
        MBDRegistries.BLOCK_ENTITY_TYPES.register(eventBus);
        MBDRegistries.RECIPE_SERIALIZERS.register("mbd_recipe_serializer", () -> MBDRecipeSerializer.SERIALIZER);
        MBDRegistries.RECIPE_SERIALIZERS.register(eventBus);
        MBDDataComponents.COMPONENTS.register(eventBus);
        // Register Editor UI
        PlayerUIMenuType.register(MBDEditor.WINDOW_ID, ignored -> player -> {
            if (player.level().isClientSide) {
                return new ModularUI(UI.of(EditorWindow.open(MBDEditor.WINDOW_ID, MBDEditor::new)))
                        .shouldCloseOnEsc(false)
                        .shouldCloseOnKeyInventory(false);
            }
            return new ModularUI(UI.empty());
        });
    }

    public void registerRecipeType() {
        MBDRegistries.RECIPE_TYPES.unfreeze();
        var event = new MBDRegistryEvent.MBDRecipeType();
        MBD2.LOGGER.info("Loading recipe types");
        var path = new File(MBD2.getLocation(), "recipe_type");
        //load recipe type
        FileUtils.loadNBTFiles(path, ".rt", (file, tag) -> event.register(MBDRecipeType.createDefault().loadProductiveTag(file, tag, postTask)));
        ModLoader.postEvent(event);
        if (MBD2.isKubeJSLoaded()) {
            KubeJSWrapper.postRecipeTypeEvent();
        }
        MBDRegistries.RECIPE_TYPES.freeze();
    }

    public void registerMachine() {
        MBDRegistries.MACHINE_DEFINITIONS.unfreeze();
        var event = new MBDRegistryEvent.Machine();
        MBD2.LOGGER.info("Loading machines");
        for (var type : MBDRegistries.MACHINE_DEFINITION_TYPES) {
            var path = new File(MBD2.getLocation(), type.folder);
            FileUtils.loadNBTFiles(path, type.fileSuffix, (file, tag) -> event.register(type.createDefinition().loadProductiveTag(file, tag, postTask)));
        }
//        if (MBD2.isCreateLoaded()) {
//            // load kinetic machine
//            path = new File(MBD2.getLocation(), "kinetic_machine");
//            FileUtils.loadNBTFiles(path, ".km", (file, tag) -> event.register(CreateKineticMachineDefinition.createDefault().loadProductiveTag(file, tag, postTask)));
//        }
        ModLoader.postEvent(event);
        if (MBD2.isKubeJSLoaded()) {
            KubeJSWrapper.postMachineEvent();
        }
        MBDRegistries.MACHINE_DEFINITIONS.freeze();
    }

    public static class KubeJSWrapper {
        public static void postMachineEvent() {
            MBDMachineRegistryEventJS.BUILDERS.put("single", MBDMachineDefinition::builder);
            MBDMachineRegistryEventJS.BUILDERS.put("multiblock", MultiblockMachineDefinition::builder);
//            if (MBD2.isCreateLoaded()) {
//                MBDMachineRegistryEventJS.BUILDERS.put("kinetic", CreateKineticMachineDefinition::builder);
//            }
            MBDStartupEvents.MACHINE.post(ScriptType.STARTUP, new MBDMachineRegistryEventJS());
        }

        public static void postRecipeTypeEvent() {
            MBDStartupEvents.RECIPE_TYPE.post(ScriptType.STARTUP, new MBDRecipeTypeRegistryEventJS());
        }
    }

    @SubscribeEvent
    public void constructMod(FMLConstructModEvent e) {
        e.enqueueWork(() -> {
            MBDMachineDefinitionTypes.init();
            MBDTraitDefinitionTypes.init();
            MBDRecipeCapabilities.init();
            registerRecipeType();
            registerMachine();
        });
    }

    @SubscribeEvent
    public void commonSetup(FMLCommonSetupEvent e) {
        e.enqueueWork(() -> {});
    }

    @SubscribeEvent
    public void loadComplete(FMLLoadCompleteEvent e) {
        e.enqueueWork(() -> {
            if (LDLib2.isClient()) {
                postTask.forEach(Runnable::run);
                postTask.clear();
            }
        });
    }

    @SubscribeEvent
    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        // does it good place to loadFactory?
        MBDRegistries.FAKE_MACHINE().loadFactory();
        MBDRegistries.MACHINE_DEFINITIONS.forEach(MBDMachineDefinition::loadFactory);
        MBDRegistries.getFakeMachineDefinition().registerCapabilities(event);
        MBDRegistries.MACHINE_DEFINITIONS.forEach((definition) -> definition.registerCapabilities(event));
        MBDRegistries.TRAIT_DEFINITION_TYPES.forEach(type -> type.registerGlobalCapabilities(event));
    }

    @SubscribeEvent
    public void register(RegisterEvent event) {
        MBDRegistries.FAKE_MACHINE().onRegistry(event);
        MBDRegistries.MACHINE_DEFINITIONS.forEach((definition) -> definition.onRegistry(event));
        // register items
        event.register(BuiltInRegistries.ITEM.key(), helper -> helper.register(MBD2.id("mbd_gadgets"), MBDRegistries.GADGETS_ITEM()));
        // register recipe types
        event.register(Registries.RECIPE_TYPE, registry -> MBDRegistries.RECIPE_TYPES.forEach((type) ->
                registry.register(type.getRegistryName(), type)));
        event.register(Registries.RECIPE_SERIALIZER, registry -> MBDRegistries.RECIPE_TYPES.forEach((type) ->
                registry.register(type.getRegistryName(), new MBDRecipeSerializer())));
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
