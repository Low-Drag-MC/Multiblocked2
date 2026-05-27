package com.lowdragmc.mbd2.test.tests.multiblock;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.pattern.FactoryBlockPattern;
import com.lowdragmc.mbd2.api.pattern.Predicates;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

public class PatternRepetitionFixtures implements TestFixtureProvider {
    public static final ResourceLocation MACHINE_ID = MBD2.id("test_pattern_repetition");

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        // 3-wide controller aisle with an aisleRepeatable wall on each side (1-3 layers).
        TestMachineBuilder.multiblock(MACHINE_ID)
                .withBlockPattern(FactoryBlockPattern.start()
                        .aisleRepeatable(1, 3, "SSS")
                        .aisle("SCS")
                        .aisleRepeatable(1, 3, "SSS")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('S', Predicates.blocks(Blocks.STONE))
                        .build())
                .register(event);
    }
}
