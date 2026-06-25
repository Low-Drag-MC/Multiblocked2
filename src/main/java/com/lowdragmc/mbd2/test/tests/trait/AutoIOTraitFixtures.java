package com.lowdragmc.mbd2.test.tests.trait;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

public class AutoIOTraitFixtures implements TestFixtureProvider {
    public static final ResourceLocation ITEM_AUTO_INPUT = MBD2.id("test_item_auto_input");
    public static final ResourceLocation ITEM_AUTO_OUTPUT = MBD2.id("test_item_auto_output");
    public static final ResourceLocation ITEM_WORLD_INPUT = MBD2.id("test_item_world_input");
    public static final ResourceLocation ITEM_WORLD_OUTPUT = MBD2.id("test_item_world_output");
    public static final ResourceLocation FLUID_AUTO_INPUT = MBD2.id("test_fluid_auto_input");
    public static final ResourceLocation FLUID_AUTO_OUTPUT = MBD2.id("test_fluid_auto_output");
    public static final ResourceLocation FLUID_WORLD_INPUT = MBD2.id("test_fluid_world_input");
    public static final ResourceLocation FLUID_WORLD_OUTPUT = MBD2.id("test_fluid_world_output");

    private static final AABB EAST_BLOCK = new AABB(1, 0, 0, 2, 1, 1);
    private static final AABB FAR_BLOCK = new AABB(4, 0, 0, 5, 1, 1);

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        TestMachineBuilder.simple(ITEM_AUTO_INPUT)
                .withItemSlots(1, IO.BOTH, definition -> configureAutoIO(definition.getAutoIO(), IO.IN))
                .register(event);
        TestMachineBuilder.simple(ITEM_AUTO_OUTPUT)
                .withItemSlots(1, IO.BOTH, definition -> configureAutoIO(definition.getAutoIO(), IO.OUT))
                .register(event);
        TestMachineBuilder.simple(ITEM_WORLD_INPUT)
                .withItemSlots(1, IO.BOTH, definition -> configureWorldIO(
                        definition.getAutoWorldInput(), definition.getAutoWorldOutput(), true))
                .register(event);
        TestMachineBuilder.simple(ITEM_WORLD_OUTPUT)
                .withItemSlots(1, IO.BOTH, definition -> configureWorldIO(
                        definition.getAutoWorldInput(), definition.getAutoWorldOutput(), false))
                .register(event);

        TestMachineBuilder.simple(FLUID_AUTO_INPUT)
                .withFluidTanks(1, 4000, definition -> configureAutoIO(definition.getAutoIO(), IO.IN))
                .register(event);
        TestMachineBuilder.simple(FLUID_AUTO_OUTPUT)
                .withFluidTanks(1, 4000, definition -> configureAutoIO(definition.getAutoIO(), IO.OUT))
                .register(event);
        TestMachineBuilder.simple(FLUID_WORLD_INPUT)
                .withFluidTanks(1, 4000, definition -> configureWorldIO(
                        definition.getAutoInput(), definition.getAutoOutput(), true))
                .register(event);
        TestMachineBuilder.simple(FLUID_WORLD_OUTPUT)
                .withFluidTanks(1, 4000, definition -> configureWorldIO(
                        definition.getAutoInput(), definition.getAutoOutput(), false))
                .register(event);
    }

    private static void configureAutoIO(com.lowdragmc.mbd2.common.trait.ToggleAutoIO autoIO, IO io) {
        autoIO.setEnable(true);
        autoIO.setInterval(1);
        autoIO.setRightIO(io);
    }

    private static void configureWorldIO(com.lowdragmc.mbd2.common.trait.AutoWorldIO input,
                                         com.lowdragmc.mbd2.common.trait.AutoWorldIO output,
                                         boolean enableInput) {
        input.setEnable(enableInput);
        input.setInterval(1);
        input.setSpeed(64);
        input.setRange(enableInput ? EAST_BLOCK : FAR_BLOCK);
        output.setEnable(!enableInput);
        output.setInterval(1);
        output.setSpeed(64);
        output.setRange(enableInput ? FAR_BLOCK : EAST_BLOCK);
    }
}
