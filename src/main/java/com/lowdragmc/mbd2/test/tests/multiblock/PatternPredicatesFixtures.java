package com.lowdragmc.mbd2.test.tests.multiblock;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.pattern.FactoryBlockPattern;
import com.lowdragmc.mbd2.api.pattern.Predicates;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

public class PatternPredicatesFixtures implements TestFixtureProvider {
    public static final ResourceLocation ANY_ID = MBD2.id("test_pattern_any");
    public static final ResourceLocation BLOCKS_OR_ID = MBD2.id("test_pattern_blocks_or");
    public static final ResourceLocation AIR_ID = MBD2.id("test_pattern_air");

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        // any predicate: just needs *some* block next to controller
        TestMachineBuilder.multiblock(ANY_ID)
                .withBlockPattern(FactoryBlockPattern.start()
                        .aisle("XCX")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('X', Predicates.any())
                        .build())
                .register(event);

        // OR'd block list: matches STONE or DIRT
        TestMachineBuilder.multiblock(BLOCKS_OR_ID)
                .withBlockPattern(FactoryBlockPattern.start()
                        .aisle("XCX")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('X', Predicates.blocks(Blocks.STONE).or(Predicates.blocks(Blocks.DIRT)))
                        .build())
                .register(event);

        // air predicate: 'X' cells must be air
        TestMachineBuilder.multiblock(AIR_ID)
                .withBlockPattern(FactoryBlockPattern.start()
                        .aisle("XCX")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('X', Predicates.air())
                        .build())
                .register(event);
    }
}
