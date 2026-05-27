package com.lowdragmc.mbd2.test.tests.multiblock;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.pattern.FactoryBlockPattern;
import com.lowdragmc.mbd2.api.pattern.Predicates;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

public class PatternBasicsFixtures implements TestFixtureProvider {
    public static final ResourceLocation MACHINE_FLOOR_ID = MBD2.id("test_pattern_floor");
    public static final ResourceLocation MACHINE_HOLLOW_ID = MBD2.id("test_pattern_hollow");

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        // 3x1x3 stone floor with controller in the center
        TestMachineBuilder.multiblock(MACHINE_FLOOR_ID)
                .withBlockPattern(FactoryBlockPattern.start()
                        .aisle("SSS")
                        .aisle("SCS")
                        .aisle("SSS")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('S', Predicates.blocks(Blocks.STONE))
                        .build())
                .register(event);

        // hollow 3x3x3 cube: controller in middle, stone walls around
        TestMachineBuilder.multiblock(MACHINE_HOLLOW_ID)
                .withBlockPattern(FactoryBlockPattern.start()
                        .aisle("SSS", "SSS", "SSS")
                        .aisle("SSS", "S S", "SSS") // controller on this aisle's middle
                        .aisle("SSS", "SSS", "SSS")
                        .where(' ', Predicates.controller(Predicates.any()))
                        .where('S', Predicates.blocks(Blocks.STONE))
                        .build())
                .register(event);
    }
}
