package com.lowdragmc.mbd2.test.tests.multiblock;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.pattern.FactoryBlockPattern;
import com.lowdragmc.mbd2.api.pattern.Predicates;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState;
import com.lowdragmc.mbd2.common.machine.definition.config.StateMachine;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import com.lowdragmc.mbd2.test.framework.TestRecipeTypeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class MultiblockWithPartsFixtures implements TestFixtureProvider {
    public static final ResourceLocation CONTROLLER_ID = MBD2.id("test_mb_with_parts_controller");
    public static final ResourceLocation PROXY_CONTROLLER_ID = MBD2.id("test_mb_with_parts_proxy_controller");
    public static final ResourceLocation PART_ID = MBD2.id("test_mb_with_parts_part");
    public static final ResourceLocation RECIPE_TYPE_ID = MBD2.id("test_mb_with_parts_recipes");

    public static MBDRecipeType recipeType;

    @Override
    public void registerRecipeTypes(MBDRegistryEvent.MBDRecipeType event) {
        recipeType = TestRecipeTypeBuilder.of(RECIPE_TYPE_ID)
                .recipe("mb_with_parts_stone_to_dirt", b -> b
                        .inputItems(Items.STONE)
                        .outputItems(Items.DIRT)
                        .duration(20))
                .register(event);
    }

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        // Part: a simple machine with item slots; every simple TestMachineBuilder machine is
        // already an MBDPartMachine, so the controller's pattern matcher will collect it
        // and aggregate its recipe handlers via initCapabilitiesProxy().
        var partDef = TestMachineBuilder.simple(PART_ID)
                .withRootState(MachineState.baseBuilder()
                        .lightLevel(2)
                        .child("formed", formed -> formed.lightLevel(4))
                        .child("working", working -> working.lightLevel(6))
                        .build())
                .withItemSlots(1, IO.IN)   // slot 0
                .withItemSlots(1, IO.OUT)  // slot 1
                .register(event);

        // Controller: no traits of its own — the recipe runs by aggregating the part's handlers.
        // The factory is evaluated lazily so partDef.block() resolves once blocks are registered.
        // Pattern is a 3-block line. FactoryBlockPattern.start() uses charDir=LEFT; with the
        // controller's default NORTH facing, the LEFTMOST char in the string sits at world +X
        // and the RIGHTMOST char at world -X. So "SCP" places stone east and part west.
        TestMachineBuilder.multiblock(CONTROLLER_ID)
                .withRecipeType(RECIPE_TYPE_ID)
                .withBlockPattern(controller -> FactoryBlockPattern.start()
                        .aisle("SCP")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('P', Predicates.blocks(partDef.block()))
                        .where('S', Predicates.blocks(Blocks.STONE))
                        .build())
                .register(event);

        TestMachineBuilder.multiblock(PROXY_CONTROLLER_ID)
                .withRecipeType(RECIPE_TYPE_ID)
                .withBlockPattern(controller -> FactoryBlockPattern.start()
                        .aisle("SCP")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('P', Predicates.blocks(partDef.block()).proxyWhileFormed(proxy -> proxy.setStateMachine(
                                new StateMachine<>(MachineState.baseBuilder()
                                        .lightLevel(1)
                                        .child("formed", formed -> formed.lightLevel(11))
                                        .child("waiting", waiting -> waiting.lightLevel(13))
                                        .build()))))
                        .where('S', Predicates.blocks(Blocks.STONE))
                        .build())
                .register(event);
    }
}
