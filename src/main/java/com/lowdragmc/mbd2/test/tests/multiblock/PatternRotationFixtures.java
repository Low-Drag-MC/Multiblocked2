package com.lowdragmc.mbd2.test.tests.multiblock;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.pattern.FactoryBlockPattern;
import com.lowdragmc.mbd2.api.pattern.Predicates;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fixtures exercising {@link com.lowdragmc.mbd2.api.pattern.predicates.PatternPredicate#rotateFollowController}.
 * Pattern layout (NORTH-authored): {@code [C][S][X]} along the pattern X axis, where
 * {@code S} is an OAK_STAIRS predicate that follows the controller facing.
 */
public class PatternRotationFixtures implements TestFixtureProvider {
    public static final ResourceLocation MACHINE_STAIRS_ID = MBD2.id("test_pattern_stairs_rotate");
    /** A pattern with a water cell sandwiched between stone cells. Used to verify
     *  {@code BlockPattern.autoBuild} places fluids last so the water doesn't flow into
     *  the stone cell that hasn't been built yet. */
    public static final ResourceLocation MACHINE_FLUID_AUTOBUILD = MBD2.id("test_pattern_fluid_autobuild");

    public static final BlockState STAIRS_NORTH =
            Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        TestMachineBuilder.multiblock(MACHINE_STAIRS_ID)
                .withBlockPattern(FactoryBlockPattern.start()
                        .aisle("CSX")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('S', Predicates.states(STAIRS_NORTH).rotateFollowController())
                        .where('X', Predicates.blocks(Blocks.STONE))
                        .build())
                .register(event);

        // Layout (charDir=LEFT, NORTH facing): [Controller][Water][Stone1][Stone2]
        // Water is at -x, stone cells at -2x and -3x. If the bucket fires before the stone
        // cells land, the water flows into stone1's empty slot and the test catches it.
        TestMachineBuilder.multiblock(MACHINE_FLUID_AUTOBUILD)
                .withBlockPattern(FactoryBlockPattern.start()
                        .aisle("CWSS")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('W', Predicates.fluids(net.minecraft.world.level.material.Fluids.WATER))
                        .where('S', Predicates.blocks(Blocks.STONE))
                        .build())
                .register(event);
    }
}
