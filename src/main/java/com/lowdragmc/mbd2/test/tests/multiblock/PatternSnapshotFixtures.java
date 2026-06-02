package com.lowdragmc.mbd2.test.tests.multiblock;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.pattern.FactoryBlockPattern;
import com.lowdragmc.mbd2.api.pattern.Predicates;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

public class PatternSnapshotFixtures implements TestFixtureProvider {
    /** 5×1×5 stone floor with controller in the center — 24 tracked positions, which is larger
     *  than {@link com.lowdragmc.mbd2.api.pattern.snapshot.PatternSnapshot#CAPTURES_PER_TICK}
     *  and therefore needs more than one main-thread tick to fully capture. */
    public static final ResourceLocation MACHINE_FLOOR_5X5 = MBD2.id("test_pattern_floor_5x5");

    /** Tiny pattern: 1 stone west of controller. Used for invalidation tests. */
    public static final ResourceLocation MACHINE_STONE_WEST = MBD2.id("test_pattern_stone_west");

    /** Pattern with one tracked position and one any() position, to verify any() positions are
     *  excluded from the tracked set. */
    public static final ResourceLocation MACHINE_STONE_AND_ANY = MBD2.id("test_pattern_stone_and_any");

    /** A controller whose pattern is read from {@link #dynamicPattern} on every {@code getPattern}
     *  call. Tests use this together with {@code notifyPatternDirty} to verify snapshot rebuild. */
    public static final ResourceLocation MACHINE_DYNAMIC = MBD2.id("test_pattern_dynamic");

    /** Backing store for the dynamic-pattern fixture. */
    public static volatile com.lowdragmc.mbd2.api.pattern.BlockPattern dynamicPattern =
            com.lowdragmc.mbd2.api.pattern.FactoryBlockPattern.start()
                    .aisle("CS")
                    .where('C', Predicates.controller(Predicates.any()))
                    .where('S', Predicates.blocks(Blocks.STONE))
                    .build();

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        TestMachineBuilder.multiblock(MACHINE_FLOOR_5X5)
                .withBlockPattern(FactoryBlockPattern.start()
                        .aisle("SSSSS")
                        .aisle("SSSSS")
                        .aisle("SSCSS")
                        .aisle("SSSSS")
                        .aisle("SSSSS")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('S', Predicates.blocks(Blocks.STONE))
                        .build())
                .register(event);

        TestMachineBuilder.multiblock(MACHINE_STONE_WEST)
                .withBlockPattern(FactoryBlockPattern.start()
                        .aisle("CS")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('S', Predicates.blocks(Blocks.STONE))
                        .build())
                .register(event);

        TestMachineBuilder.multiblock(MACHINE_STONE_AND_ANY)
                .withBlockPattern(FactoryBlockPattern.start()
                        .aisle("ACS")  // A = any (not tracked), C = controller, S = stone (tracked)
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('S', Predicates.blocks(Blocks.STONE))
                        .where('A', Predicates.any())
                        .build())
                .register(event);

        // Pattern factory dereferences the volatile field on every call, so tests can swap
        // the pattern and call notifyPatternDirty to trigger a snapshot rebuild.
        TestMachineBuilder.multiblock(MACHINE_DYNAMIC)
                .withBlockPattern(controller -> dynamicPattern)
                .register(event);
    }
}
