package com.lowdragmc.mbd2.common.machine.definition;

import com.google.common.collect.Queues;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.client.renderer.impl.UIResourceRenderer;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib.gui.editor.data.resource.IRendererResource;
import com.lowdragmc.lowdraglib.gui.editor.data.resource.Resource;
import com.lowdragmc.lowdraglib.gui.editor.data.resource.TexturesResource;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.client.renderer.MBDBESRenderer;
import com.lowdragmc.mbd2.client.renderer.MBDBlockRenderer;
import com.lowdragmc.mbd2.client.renderer.MBDItemRenderer;
import com.lowdragmc.mbd2.common.block.MBDMachineBlock;
import com.lowdragmc.mbd2.common.blockentity.MachineBlockEntity;
import com.lowdragmc.mbd2.common.item.MBDMachineItem;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.MBDPartMachine;
import com.lowdragmc.mbd2.common.machine.definition.config.*;
import com.lowdragmc.mbd2.common.trait.ITraitUIProvider;
import com.lowdragmc.mbd2.utils.WidgetUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;

/**
 * Machine definition.
 * <br>
 * This is used to define a mbd machine's {@link MBDMachine#getDefinition()} behaviours.
 */
@Getter
@Accessors(fluent = true)
public class MBDMachineDefinition implements IConfigurable, IPersistedSerializable {
    /**
     * used for block initialization.
     */
    static final ThreadLocal<MBDMachineDefinition> STATE = new ThreadLocal<>();

    public static MBDMachineDefinition get() {
        return STATE.get();
    }

    public static void set(MBDMachineDefinition state) {
        STATE.set(state);
    }

    public static void clear() {
        STATE.remove();
    }

    @Configurable(tips = "config.definition.id.tooltip", forceUpdate = false)
    private ResourceLocation id;
    protected final StateMachine<?> stateMachine;
    @Configurable(name = "config.definition.block_properties", subConfigurable = true, tips = "config.definition.block_properties.tooltip", collapse = false)
    protected final ConfigBlockProperties blockProperties;
    @Configurable(name = "config.definition.item_properties", subConfigurable = true, tips = "config.definition.item_properties.tooltip", collapse = false)
    protected final ConfigItemProperties itemProperties;
    @Configurable(name = "config.definition.machine_settings", subConfigurable = true, tips = "config.definition.machine_settings.tooltip", collapse = false)
    protected final ConfigMachineSettings machineSettings;
    @Persisted(subPersisted = true)
    protected final ConfigMachineEvents machineEvents;
    @Nullable
    @Configurable(name = "config.definition.part_settings", subConfigurable = true, tips = {
            "config.definition.part_settings.tooltip.0",
            "config.definition.part_settings.tooltip.1",
            "config.definition.part_settings.tooltip.2",
    })
    protected final ConfigPartSettings partSettings;

    // runtime
    @Nullable
    private File projectFile;
    private Block block;
    private Item item;
    private BlockEntityType<?> blockEntityType;
    private IRenderer blockRenderer;
    private IRenderer itemRenderer;
    private Function<MBDMachine, WidgetGroup> uiCreator;

    protected MBDMachineDefinition(ResourceLocation id,
                                   StateMachine<?> stateMachine,
                                   ConfigBlockProperties blockProperties,
                                   ConfigItemProperties itemProperties,
                                   ConfigMachineSettings machineSettings,
                                   @Nullable ConfigPartSettings partSettings) {
        this.id = id == null ? new ResourceLocation("mbd2", "undefined") : id;
        this.stateMachine = stateMachine == null ? createDefaultStateMachine() : stateMachine;
        this.blockProperties = blockProperties == null ? ConfigBlockProperties.builder().build() : blockProperties;
        this.itemProperties = itemProperties == null ? ConfigItemProperties.builder().build() : itemProperties;
        this.machineSettings = machineSettings == null ? ConfigMachineSettings.builder().build() : machineSettings;
        this.partSettings = partSettings;
        this.machineEvents = createMachineEvents();
    }

    public ConfigMachineEvents createMachineEvents() {
        return new ConfigMachineEvents().registerEventGroup("MachineEvent");
    }

    public StateMachine<?> createDefaultStateMachine() {
        return StateMachine.createDefault(MachineState::builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static MBDMachineDefinition createDefault() {
        return new MBDMachineDefinition(
                MBD2.id("dummy"),
                StateMachine.createDefault(MachineState::builder),
                ConfigBlockProperties.builder().build(),
                ConfigItemProperties.builder().build(),
                ConfigMachineSettings.builder().build(),
                ConfigPartSettings.builder().build());
    }

    @Override
    public CompoundTag serializeNBT() {
        var tag = IPersistedSerializable.super.serializeNBT();
        tag.put("stateMachine", stateMachine.serializeNBT());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        IPersistedSerializable.super.deserializeNBT(tag);
        stateMachine.deserializeNBT(tag.getCompound("stateMachine"));
    }

    /**
     * Load definition from project tag for product usage.
     * only {@link MBDMachineDefinition#blockProperties}, {@link MBDMachineDefinition#itemProperties} and {@link MBDMachineDefinition#stateMachine}
     * will be loaded immediately, others will be loaded during the postTask.
     * @param projectTag project tag.
     * @param postTask Called when the mod is loaded completed. To make sure all resources are available.
     *                 <br/> e.g. items, blocks and other registries are ready.
     */
    public MBDMachineDefinition loadProductiveTag(File file, CompoundTag projectTag, Deque<Runnable> postTask) {
        this.projectFile = file;
        var rendererResource = new IRendererResource();
        rendererResource.deserializeNBT(projectTag.getCompound("resources").getCompound(IRendererResource.RESOURCE_NAME));
        UIResourceRenderer.setCurrentResource(rendererResource, false);
        var definitionTag = projectTag.getCompound("definition");
        id = new ResourceLocation(definitionTag.getString("id"));
        blockProperties.deserializeNBT(definitionTag.getCompound("blockProperties"));
        itemProperties.deserializeNBT(definitionTag.getCompound("itemProperties"));
        stateMachine.deserializeNBT(definitionTag.getCompound("stateMachine"));
        UIResourceRenderer.clearCurrentResource();
        postTask.add(() -> {
            if (partSettings != null) {
                partSettings.deserializeNBT(definitionTag.getCompound("partSettings"));
            }
            machineSettings.deserializeNBT(definitionTag.getCompound("machineSettings"));
            machineEvents.deserializeNBT(definitionTag.getCompound("machineEvents"));
            if (machineSettings().hasUI()) {
                var texturesResource = new TexturesResource();
                texturesResource.deserializeNBT(projectTag.getCompound("resources").getCompound(TexturesResource.RESOURCE_NAME));
                var uiTag = projectTag.getCompound("ui");
                uiCreator = machine -> {
                    var machineUI = new WidgetGroup();
                    IConfigurableWidget.deserializeNBT(machineUI, uiTag, texturesResource, false);
                    bindMachineUI(machine, machineUI);
                    return machineUI;
                };
            }
        });
        return this;
    }

    /**
     * Indicate if the definition is created from project file.
     */
    public boolean isCreatedFromProjectFile() {
        return projectFile != null;
    }

    /**
     * Reload definition from project file. Not all properties will be updated, because the block and item are already registered.
     */
    public void reloadFromProjectFile() {
        if (projectFile != null) {
            try {
                var tag = NbtIo.read(projectFile);
                if (tag != null) {
                    Deque<Runnable> postTask = Queues.newArrayDeque();
                    loadProductiveTag(projectFile, tag, postTask);
                    postTask.forEach(Runnable::run);
                }
            } catch (IOException ignored) {}
        }
    }

    /**
     * Setup ui by project. Called after loading the machine definition.
     */
    public void setUiCreator(CompoundTag uiTag, Resource<IGuiTexture> texturesResource) {
        uiCreator = machine -> {
            var machineUI = new WidgetGroup();
            IConfigurableWidget.deserializeNBT(machineUI, uiTag, texturesResource, false);
            bindMachineUI(machine, machineUI);
            return machineUI;
        };
    }

    protected void bindMachineUI(MBDMachine machine, WidgetGroup ui) {
        WidgetUtils.widgetByIdForEach(ui, "ui:progress_bar", ProgressWidget.class,
                progressWidget -> progressWidget.setProgressSupplier(() -> machine.getRecipeLogic().getProgressPercent()));
        WidgetUtils.widgetByIdForEach(ui, "ui:fuel_bar", ProgressWidget.class,
                progressWidget -> progressWidget.setProgressSupplier(() -> machine.getRecipeLogic().getFuelProgressPercent()));
        for (var traitDefinition : machineSettings.traitDefinitions()) {
            if (traitDefinition instanceof ITraitUIProvider provider) {
                var trait = machine.getTraitByDefinition(traitDefinition);
                if (trait != null)
                    provider.initTraitUI(trait, ui);
            }
        }
    }

    public void onRegistry(RegisterEvent event) {
        event.register(ForgeRegistries.BLOCKS.getRegistryKey(), helper -> {
            MBDMachineDefinition.set(this);
            helper.register(id, block = createBlock());
            MBDMachineDefinition.clear();
        });
        event.register(ForgeRegistries.ITEMS.getRegistryKey(), helper -> helper.register(id, item = createItem(block)));
        event.register(ForgeRegistries.BLOCK_ENTITY_TYPES.getRegistryKey(), helper ->
                helper.register(id, blockEntityType = BlockEntityType.Builder.of(this::createBlockEntity, block).build(null)));
    }

    public Block createBlock() {
        return new MBDMachineBlock(blockProperties.apply(stateMachine, BlockBehaviour.Properties.of()), this);
    }

    public Item createItem(Block block) {
        return new MBDMachineItem((MBDMachineBlock)block, itemProperties.apply(new Item.Properties()));
    }

    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MachineBlockEntity(blockEntityType(), pos, state, this::createMachine);
    }

    public MBDMachine createMachine(IMachineBlockEntity blockEntity) {
        return (partSettings != null && partSettings.canShare()) ? new MBDPartMachine(blockEntity, this) : new MBDMachine(blockEntity, this);
    }

    @OnlyIn(Dist.CLIENT)
    public void initRenderer(EntityRenderersEvent.RegisterRenderers event) {
        blockRenderer = createBlockRenderer();
        itemRenderer = createItemRenderer();
        event.registerBlockEntityRenderer(blockEntityType, createBESRR());
        ItemBlockRenderTypes.setRenderLayer(block(), renderType -> {
            if (renderType == RenderType.translucent()) {
                return blockProperties.renderTypes().translucent();
            } else if (renderType == RenderType.cutout()) {
                return blockProperties.renderTypes().cutout();
            } else if (renderType == RenderType.cutoutMipped()) {
                return blockProperties.renderTypes().cutoutMipped();
            } else if (renderType == RenderType.solid()) {
                return blockProperties.renderTypes().solid();
            }
            return false;
        });
    }

    @OnlyIn(Dist.CLIENT)
    public IRenderer createBlockRenderer() {
        return new MBDBlockRenderer(blockProperties::useAO);
    }

    @OnlyIn(Dist.CLIENT)
    public IRenderer createItemRenderer() {
        return new MBDItemRenderer(itemProperties::useBlockLight, itemProperties::isGui3d, () -> itemProperties.renderer().isEnable() ? itemProperties.renderer().getValue() : stateMachine.getRootState().getRealRenderer());
    }

    @OnlyIn(Dist.CLIENT)
    public BlockEntityRendererProvider<BlockEntity> createBESRR() {
        return MBDBESRenderer::getOrCreate;
    }

    public MachineState getState(String name) {
        return stateMachine.getState(name);
    }

    public String getDescriptionId() {
        return block().getDescriptionId();
    }

    public ItemStack asStack() {
        return new ItemStack(item());
    }

    public ItemStack asStack(int count) {
        return new ItemStack(item(), count);
    }

    /**
     * Append the machine's tooltip.
     */
    public void appendHoverText(ItemStack stack, List<Component> tooltip) {
        tooltip.addAll(itemProperties().itemTooltips());
    }

    @Setter
    @Accessors(chain = true, fluent = true)
    public static class Builder {
        protected ResourceLocation id;
        protected StateMachine<?> stateMachine;
        protected ConfigBlockProperties blockProperties;
        protected ConfigItemProperties itemProperties;
        protected ConfigMachineSettings machineSettings;
        protected ConfigPartSettings partSettings;

        protected Builder() {
        }

        public MBDMachineDefinition build() {
            return new MBDMachineDefinition(id, stateMachine, blockProperties, itemProperties, machineSettings, partSettings);
        }
    }
}
