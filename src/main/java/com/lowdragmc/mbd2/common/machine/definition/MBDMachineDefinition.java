package com.lowdragmc.mbd2.common.machine.definition;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.IPersistedSerializable;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.block.RotationState;
import com.lowdragmc.mbd2.client.renderer.MBDBESRenderer;
import com.lowdragmc.mbd2.client.renderer.MBDBlockRenderer;
import com.lowdragmc.mbd2.client.renderer.MBDItemRenderer;
import com.lowdragmc.mbd2.common.block.MBDMachineBlock;
import com.lowdragmc.mbd2.common.blockentity.MachineBlockEntity;
import com.lowdragmc.mbd2.common.gui.editor.MachineProject;
import com.lowdragmc.mbd2.common.item.MBDMachineItem;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.definition.config.*;
import com.lowdragmc.mbd2.common.trait.ITraitUIProvider;
import com.lowdragmc.mbd2.utils.WidgetUtils;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

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
    @Configurable(tips = "config.definition.id.tooltip", forceUpdate = false)
    protected final ResourceLocation id;
    protected final StateMachine stateMachine;
    @Configurable(name = "config.definition.block_properties", subConfigurable = true, tips = "config.definition.block_properties.tooltip", collapse = false)
    protected final ConfigBlockProperties blockProperties;
    @Configurable(name = "config.definition.item_properties", subConfigurable = true, tips = "config.definition.item_properties.tooltip", collapse = false)
    protected final ConfigItemProperties itemProperties;
    @Configurable(name = "config.definition.machine_settings", subConfigurable = true, tips = "config.definition.machine_settings.tooltip", collapse = false)
    protected final ConfigMachineSettings machineSettings;

    // runtime
    private MBDMachineBlock block;
    private MBDMachineItem item;
    private BlockEntityType<MachineBlockEntity> blockEntityType;
    private IRenderer blockRenderer;
    private IRenderer itemRenderer;
    private Function<MBDMachine, WidgetGroup> uiCreator;

    @Builder
    protected MBDMachineDefinition(ResourceLocation id,
                                   StateMachine stateMachine,
                                   ConfigBlockProperties blockProperties,
                                   ConfigItemProperties itemProperties,
                                   ConfigMachineSettings machineSettings) {
        this.id = id == null ? new ResourceLocation("mbd2", "undefined") : id;
        this.stateMachine = stateMachine == null ? StateMachine.createDefault() : stateMachine;
        this.blockProperties = blockProperties == null ? ConfigBlockProperties.builder().build() : blockProperties;
        this.itemProperties = itemProperties == null ? ConfigItemProperties.builder().build() : itemProperties;
        this.machineSettings = machineSettings == null ? ConfigMachineSettings.builder().build() : machineSettings;
    }

    public static MBDMachineDefinition createDefault() {
        return new MBDMachineDefinition(
                MBD2.id("dummy"),
                StateMachine.createDefault(),
                ConfigBlockProperties.builder().build(),
                ConfigItemProperties.builder().build(),
                ConfigMachineSettings.builder().build());
    }

    /**
     * return null if the machine definition is invalid.
     */
    public static MBDMachineDefinition fromTag(CompoundTag tag) {
        var definition = createDefault();
        definition.deserializeNBT(tag);
        return definition;
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
     * Called when the mod is loaded completed. To make sure all resources are available.
     * <br/>
     * e.g. items, blocks and other registries are ready.
     */
    public void postLoading(CompoundTag tag) {
        if (machineSettings.hasUI()) {
            var proj = new MachineProject();
            var resources = proj.loadResources(tag.getCompound("resources"));
            var uiTag = tag.getCompound("ui");
            uiCreator = machine -> {
                var machineUI = new WidgetGroup();
                IConfigurableWidget.deserializeNBT(machineUI, uiTag, resources, false);
                bindMachineUI(machine, machineUI);
                return machineUI;
            };
        }
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
            RotationState.set(blockProperties.rotationState());
            helper.register(id, block = new MBDMachineBlock(blockProperties.apply(BlockBehaviour.Properties.of()), this));
            RotationState.clear();
        });
        event.register(ForgeRegistries.ITEMS.getRegistryKey(), helper ->
                helper.register(id, item = new MBDMachineItem(block, itemProperties.apply(new Item.Properties()))));
        event.register(ForgeRegistries.BLOCK_ENTITY_TYPES.getRegistryKey(), helper ->
                helper.register(id, blockEntityType = BlockEntityType.Builder.of((pos, state) ->
                        new MachineBlockEntity(blockEntityType(), pos, state), block).build(null)));
    }

    @OnlyIn(Dist.CLIENT)
    public void initRenderer(EntityRenderersEvent.RegisterRenderers event) {
        blockRenderer = new MBDBlockRenderer(blockProperties::useAO);
        itemRenderer = new MBDItemRenderer(itemProperties::useBlockLight, itemProperties::isGui3d, () -> itemProperties.renderer().isEnable() ? itemProperties.renderer().getValue() : stateMachine.getRootState().getRenderer());
        event.registerBlockEntityRenderer(blockEntityType, MBDBESRenderer::getOrCreate);
    }

    public MachineState getState(String name) {
        return stateMachine.getState(name);
    }

    /**
     * Called periodically clientside on blocks near the player to show effects (like furnace fire particles).
     */
    public void animateTick(MBDMachine mbdMachine, RandomSource random) {
    }

    /**
     * Append the machine's tooltip.
     */
    public void appendHoverText(ItemStack stack, List<Component> tooltip) {
    }
}
