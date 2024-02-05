package com.lowdragmc.mbd2.common.machine.definition;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.mbd2.MBD2;
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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

import java.util.List;

/**
 * Machine definition.
 * <br>
 * This is used to define a mbd machine's {@link MBDMachine#getDefinition()} behaviours.
 */
@Getter
@Accessors(fluent = true)
public class MBDMachineDefinition implements IConfigurable {
    @Configurable(tips = "config.definition.id.tooltip")
    protected final ResourceLocation id;
    protected final StateMachine stateMachine;
    @Configurable(name = "config.definition.block_properties", subConfigurable = true, tips = "config.definition.block_properties.tooltip")
    protected final ConfigBlockProperties blockProperties;
    @Configurable(name = "config.definition.item_properties", subConfigurable = true, tips = "config.definition.item_properties.tooltip")
    protected final ConfigItemProperties itemProperties;
    @Configurable(name = "config.definition.machine_settings", subConfigurable = true, tips = "config.definition.machine_settings.tooltip")
    protected final ConfigMachineSettings machineSettings;

    private MBDMachineBlock block;
    private MBDMachineItem item;
    private BlockEntityType<MachineBlockEntity> blockEntityType;
    private IRenderer blockRenderer;
    private IRenderer itemRenderer;

    public static MBDMachineDefinition createRenderOnly(IRenderer renderer) {
        return MBDMachineDefinition.builder()
                .id(MBD2.id("render_only"))
                .stateMachine(new StateMachine(MachineState.builder()
                        .name("base")
                        .renderer(renderer)
                        .shape(Shapes.block())
                        .lightLevel(0)
                        .build()))
                .blockProperties(ConfigBlockProperties.builder().build())
                .itemProperties(ConfigItemProperties.builder().build())
                .build();
    }

    @Builder
    protected MBDMachineDefinition(ResourceLocation id, StateMachine stateMachine, ConfigBlockProperties blockProperties, ConfigItemProperties itemProperties, ConfigMachineSettings machineSettings) {
        this.id = id;
        this.stateMachine = stateMachine;
        this.blockProperties = blockProperties;
        this.itemProperties = itemProperties;
        this.machineSettings = machineSettings;
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
