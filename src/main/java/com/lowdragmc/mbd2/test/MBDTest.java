package com.lowdragmc.mbd2.test;

import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class MBDTest {

    @SubscribeEvent
    public void onRegisterMachine(MBDRegistryEvent.Machine event) {
//        var renderer = new IModelRenderer(MBD2.id("block/pedestal"));
//        event.register(MBDMachineDefinition.builder()
//                .id(MBD2.id("test_machine"))
//                .stateMachine(new StateMachine(MachineState.builder()
//                        .name("base")
//                        .renderer(new ToggleRenderer(renderer))
//                        .shape(new ToggleShape(Shapes.block()))
//                        .lightLevel(new ToggleInteger(0))
//                        .build()))
//                .blockProperties(ConfigBlockProperties.builder().build())
//                .itemProperties(ConfigItemProperties.builder().build())
//                .build());
    }

    @SubscribeEvent
    public void onRegisterRecipeType(MBDRegistryEvent.MBDRecipeType event) {
        System.out.println("Registering recipe type");
    }

}
