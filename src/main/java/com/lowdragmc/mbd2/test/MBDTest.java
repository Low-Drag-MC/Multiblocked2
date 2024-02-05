package com.lowdragmc.mbd2.test;

import com.lowdragmc.lowdraglib.client.renderer.impl.IModelRenderer;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigBlockProperties;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigItemProperties;
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState;
import com.lowdragmc.mbd2.common.machine.definition.config.StateMachine;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class MBDTest {
    @SubscribeEvent
    public void onRegisterMachine(MBDRegistryEvent.Machine event) {
        var renderer = new IModelRenderer(MBD2.id("block/pedestal"));
        event.register(MBDMachineDefinition.builder()
                .id(MBD2.id("test_machine"))
                .stateMachine(new StateMachine(MachineState.builder()
                        .name("base")
                        .renderer(renderer)
                        .shape(Shapes.block())
                        .lightLevel(0)
                        .build()))
                .blockProperties(ConfigBlockProperties.builder().build())
                .itemProperties(ConfigItemProperties.builder().build())
                .build());
    }

    @SubscribeEvent
    public void onRegisterRecipeType(MBDRegistryEvent.MBDRecipeType event) {
    }

}
