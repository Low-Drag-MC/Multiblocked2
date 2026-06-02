package com.lowdragmc.mbd2.test.tests.multiblock;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.pattern.FactoryBlockPattern;
import com.lowdragmc.mbd2.api.pattern.Predicates;
import com.lowdragmc.mbd2.api.pattern.TraceabilityPredicate;
import com.lowdragmc.mbd2.api.pattern.predicates.PredicatePartialState;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState;
import com.lowdragmc.mbd2.common.machine.definition.config.StateMachine;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.Map;

public class PatternPredicatesFixtures implements TestFixtureProvider {
    public static final ResourceLocation ANY_ID = MBD2.id("test_pattern_any");
    public static final ResourceLocation BLOCKS_OR_ID = MBD2.id("test_pattern_blocks_or");
    public static final ResourceLocation AIR_ID = MBD2.id("test_pattern_air");
    public static final ResourceLocation PARTIAL_STATE_FACING_ID = MBD2.id("test_pattern_partial_state_facing");
    public static final ResourceLocation PARTIAL_STATE_FACING_AND_HALF_ID = MBD2.id("test_pattern_partial_state_facing_and_half");
    public static final ResourceLocation PARTIAL_STATE_BLOCK_ONLY_ID = MBD2.id("test_pattern_partial_state_block_only");
    public static final ResourceLocation PARTIAL_STATE_INVALID_ID = MBD2.id("test_pattern_partial_state_invalid");
    public static final ResourceLocation PARTIAL_STATE_INVALID_VALUE_ID = MBD2.id("test_pattern_partial_state_invalid_value");
    public static final ResourceLocation PROXY_ID = MBD2.id("test_pattern_proxy_while_formed");
    public static final ResourceLocation SLOT_PROXY_ID = MBD2.id("test_pattern_slot_proxy_while_formed");
    public static final ResourceLocation CHEST_PROXY_ID = MBD2.id("test_pattern_chest_proxy_while_formed");
    public static final ResourceLocation MULTI_PROXY_ID = MBD2.id("test_pattern_multi_proxy_while_formed");
    public static final ResourceLocation OR_PROXY_ID = MBD2.id("test_pattern_or_proxy_while_formed");
    public static final ResourceLocation CONTROLLER_NBT_ID = MBD2.id("test_pattern_controller_nbt");

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

        TestMachineBuilder.multiblock(PARTIAL_STATE_FACING_ID)
                .withBlockPattern(FactoryBlockPattern.start()
                        .aisle("XCX")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('X', Predicates.partialState(stairs(Direction.EAST, Half.BOTTOM), StairBlock.FACING))
                        .build())
                .register(event);

        TestMachineBuilder.multiblock(PARTIAL_STATE_FACING_AND_HALF_ID)
                .withBlockPattern(FactoryBlockPattern.start()
                        .aisle("XCX")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('X', Predicates.partialState(Blocks.OAK_STAIRS,
                                new PredicatePartialState.PropertyRequirement("facing", "east"),
                                new PredicatePartialState.PropertyRequirement("half", "top")))
                        .build())
                .register(event);

        TestMachineBuilder.multiblock(PARTIAL_STATE_BLOCK_ONLY_ID)
                .withBlockPattern(FactoryBlockPattern.start()
                        .aisle("XCX")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('X', Predicates.partialState(Blocks.STONE))
                        .build())
                .register(event);

        TestMachineBuilder.multiblock(PARTIAL_STATE_INVALID_ID)
                .withBlockPattern(FactoryBlockPattern.start()
                        .aisle("XCX")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('X', Predicates.partialState(Blocks.OAK_STAIRS, Map.of("missing_property", "east")))
                        .build())
                .register(event);

        TestMachineBuilder.multiblock(PARTIAL_STATE_INVALID_VALUE_ID)
                .withBlockPattern(FactoryBlockPattern.start()
                        .aisle("XCX")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('X', Predicates.partialState(Blocks.OAK_STAIRS, Map.of("facing", "sideways")))
                        .build())
                .register(event);

        TestMachineBuilder.multiblock(PROXY_ID)
                .withBlockPattern(FactoryBlockPattern.start()
                        .aisle("XCX")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('X', proxyStonePredicate(false))
                        .build())
                .register(event);

        TestMachineBuilder.multiblock(SLOT_PROXY_ID)
                .withBlockPattern(FactoryBlockPattern.start()
                        .aisle("XCX")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('X', proxyStonePredicate(true))
                        .build())
                .register(event);

        TestMachineBuilder.multiblock(CHEST_PROXY_ID)
                .withBlockPattern(FactoryBlockPattern.start()
                        .aisle("XCX")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('X', Predicates.blocks(Blocks.CHEST).proxyWhileFormed())
                        .build())
                .register(event);

        TestMachineBuilder.multiblock(MULTI_PROXY_ID)
                .withBlockPattern(FactoryBlockPattern.start()
                        .aisle("ACB")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('A', proxyStonePredicate(false))
                        .where('B', proxyStonePredicate(4, 9))
                        .build())
                .register(event);

        TestMachineBuilder.multiblock(OR_PROXY_ID)
                .withBlockPattern(FactoryBlockPattern.start()
                        .aisle("XCX")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('X', Predicates.blocks(Blocks.STONE).or(Predicates.blocks(Blocks.DIRT)).proxyWhileFormed())
                        .build())
                .register(event);

        var requiredControllerNbt = new CompoundTag();
        requiredControllerNbt.putString("missing_key", "expected_value");
        var controllerNbtPredicate = Predicates.blocks(Blocks.STONE);
        controllerNbtPredicate.common.forEach(predicate -> predicate.controllerNbt = requiredControllerNbt);
        TestMachineBuilder.multiblock(CONTROLLER_NBT_ID)
                .withBlockPattern(FactoryBlockPattern.start()
                        .aisle("XCX")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('X', controllerNbtPredicate)
                        .build())
                .register(event);
    }

    private static TraceabilityPredicate proxyStonePredicate(boolean withSlotName) {
        var predicate = proxyStonePredicate(1, 7);
        if (withSlotName) {
            predicate.setSlotName("proxy_slot");
        }
        return predicate;
    }

    private static TraceabilityPredicate proxyStonePredicate(int rootLight, int workingLight) {
        var predicate = Predicates.blocks(Blocks.STONE).proxyWhileFormed(proxy -> proxy.setStateMachine(
                new StateMachine<>(MachineState.baseBuilder()
                        .shape(Shapes.empty())
                        .lightLevel(rootLight)
                        .child("working", working -> working.shape(Shapes.block()).lightLevel(workingLight))
                        .build())));
        return predicate;
    }

    private static BlockState stairs(Direction facing, Half half) {
        return Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, facing)
                .setValue(StairBlock.HALF, half);
    }
}
