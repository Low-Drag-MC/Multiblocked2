package com.lowdragmc.mbd2.common.machine.definition;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.syncdata.IPersistedSerializable;
import com.lowdragmc.mbd2.api.block.RotationState;
import com.lowdragmc.mbd2.client.renderer.MBDBlockRenderer;
import com.lowdragmc.mbd2.client.renderer.MBDItemRenderer;
import com.lowdragmc.mbd2.common.block.MBDMachineBlock;
import com.lowdragmc.mbd2.common.blockentity.MachineBlockEntity;
import com.lowdragmc.mbd2.common.item.MBDMachineItem;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.definition.config.*;
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
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Machine definition.
 * <br>
 * This is used to define a mbd machine's {@link MBDMachine#getDefinition()} behaviours.
 */
@Getter
@Accessors(fluent = true)
public class MBDMachineDefinition implements IConfigurable, IPersistedSerializable {
    @Configurable(tips = "config.definition.id.tooltip")
    protected final ResourceLocation id;
    protected final StateMachine stateMachine;
    @Configurable(name = "config.definition.block_properties", subConfigurable = true, tips = "config.definition.block_properties.tooltip", canCollapse = false)
    protected final ConfigBlockProperties blockProperties;
    @Configurable(name = "config.definition.item_properties", subConfigurable = true, tips = "config.definition.item_properties.tooltip", canCollapse = false)
    protected final ConfigItemProperties itemProperties;
    @Configurable(name = "config.definition.machine_settings", subConfigurable = true, tips = "config.definition.machine_settings.tooltip", canCollapse = false)
    protected final ConfigMachineSettings machineSettings;

    private MBDMachineBlock block;
    private MBDMachineItem item;
    private BlockEntityType<MachineBlockEntity> blockEntityType;
    private IRenderer blockRenderer;
    private IRenderer itemRenderer;

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

    /**
     * return null if the machine definition is invalid.
     */
    @Nullable
    public static MBDMachineDefinition fromTag(CompoundTag tag) {
        try {
            var definition = new MBDMachineDefinition(
                    new ResourceLocation(tag.getString("id")),
                    StateMachine.createDefault(),
                    ConfigBlockProperties.builder().build(),
                    ConfigItemProperties.builder().build(),
                    ConfigMachineSettings.builder().build());
            definition.deserializeNBT(tag);
            return definition;
        } catch (Exception e) {
            return null;
        }
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
    public void initRenderer() {
        blockRenderer = new MBDBlockRenderer(blockProperties::useAO);
        itemRenderer = new MBDItemRenderer(itemProperties::useBlockLight, itemProperties::isGui3d, () -> itemProperties.renderer().isEnable() ? itemProperties.renderer().getValue() : stateMachine.getRootState().getRenderer());
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
