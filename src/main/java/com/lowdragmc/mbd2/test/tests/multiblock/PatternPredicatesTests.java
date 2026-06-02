package com.lowdragmc.mbd2.test.tests.multiblock;

import com.lowdragmc.lowdraglib2.syncdata.AccessorRegistries;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.block.ProxyPartBlock;
import com.lowdragmc.mbd2.api.blockentity.ProxyPartBlockEntity;
import com.lowdragmc.mbd2.api.pattern.predicates.PredicatePartialState;
import com.lowdragmc.mbd2.api.pattern.predicates.PatternPredicate;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.mojang.serialization.JavaOps;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;

import java.util.List;

@GameTestHolder(MBD2.MOD_ID)
public class PatternPredicatesTests {
    static { @SuppressWarnings("unused") var ignored = PatternPredicatesFixtures.ANY_ID; }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void any_matches_dirt(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        MBDScenario.of(h)
                .placeBlock(controller.relative(Direction.WEST), Blocks.DIRT.defaultBlockState())
                .placeBlock(controller.relative(Direction.EAST), Blocks.DIRT.defaultBlockState())
                .placeMachine(PatternPredicatesFixtures.ANY_ID, controller)
                .assertFormed();
        if (h.getBlockState(controller.relative(Direction.WEST)).is(ProxyPartBlock.BLOCK) ||
                h.getBlockState(controller.relative(Direction.EAST)).is(ProxyPartBlock.BLOCK)) {
            h.fail("Plain predicate unexpectedly replaced blocks with proxies");
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void or_matches_stone(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        MBDScenario.of(h)
                .placeBlock(controller.relative(Direction.WEST), Blocks.STONE.defaultBlockState())
                .placeBlock(controller.relative(Direction.EAST), Blocks.STONE.defaultBlockState())
                .placeMachine(PatternPredicatesFixtures.BLOCKS_OR_ID, controller)
                .assertFormed()
                .succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void or_matches_dirt(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        MBDScenario.of(h)
                .placeBlock(controller.relative(Direction.WEST), Blocks.DIRT.defaultBlockState())
                .placeBlock(controller.relative(Direction.EAST), Blocks.DIRT.defaultBlockState())
                .placeMachine(PatternPredicatesFixtures.BLOCKS_OR_ID, controller)
                .assertFormed()
                .succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void or_rejects_iron(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        MBDScenario.of(h)
                .placeBlock(controller.relative(Direction.WEST), Blocks.IRON_BLOCK.defaultBlockState())
                .placeBlock(controller.relative(Direction.EAST), Blocks.IRON_BLOCK.defaultBlockState())
                .placeMachine(PatternPredicatesFixtures.BLOCKS_OR_ID, controller)
                .formNow()
                .assertNotFormed()
                .succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void air_matches_air(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        // surrounding cells are already air (default template)
        MBDScenario.of(h)
                .placeMachine(PatternPredicatesFixtures.AIR_ID, controller)
                .assertFormed()
                .succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void air_rejects_solid_block(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        MBDScenario.of(h)
                .placeBlock(controller.relative(Direction.WEST), Blocks.STONE.defaultBlockState())
                .placeMachine(PatternPredicatesFixtures.AIR_ID, controller)
                .formNow()
                .assertNotFormed()
                .succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void partial_state_allows_unspecified_properties_to_vary(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        MBDScenario.of(h)
                .placeBlock(controller.relative(Direction.WEST), stairs(Direction.EAST, Half.TOP))
                .placeBlock(controller.relative(Direction.EAST), stairs(Direction.EAST, Half.BOTTOM))
                .placeMachine(PatternPredicatesFixtures.PARTIAL_STATE_FACING_ID, controller)
                .assertFormed()
                .succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void partial_state_rejects_required_property_mismatch(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        MBDScenario.of(h)
                .placeBlock(controller.relative(Direction.WEST), stairs(Direction.WEST, Half.TOP))
                .placeBlock(controller.relative(Direction.EAST), stairs(Direction.WEST, Half.BOTTOM))
                .placeMachine(PatternPredicatesFixtures.PARTIAL_STATE_FACING_ID, controller)
                .formNow()
                .assertNotFormed()
                .succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void partial_state_rejects_different_block(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        var spruceFacingEast = Blocks.SPRUCE_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.EAST)
                .setValue(StairBlock.HALF, Half.BOTTOM);
        MBDScenario.of(h)
                .placeBlock(controller.relative(Direction.WEST), spruceFacingEast)
                .placeBlock(controller.relative(Direction.EAST), spruceFacingEast)
                .placeMachine(PatternPredicatesFixtures.PARTIAL_STATE_FACING_ID, controller)
                .formNow()
                .assertNotFormed()
                .succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void partial_state_requires_all_configured_properties(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        MBDScenario.of(h)
                .placeBlock(controller.relative(Direction.WEST), stairs(Direction.EAST, Half.TOP))
                .placeBlock(controller.relative(Direction.EAST), stairs(Direction.EAST, Half.TOP))
                .placeMachine(PatternPredicatesFixtures.PARTIAL_STATE_FACING_AND_HALF_ID, controller)
                .assertFormed()
                .succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void partial_state_rejects_when_any_configured_property_mismatches(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        MBDScenario.of(h)
                .placeBlock(controller.relative(Direction.WEST), stairs(Direction.EAST, Half.BOTTOM))
                .placeBlock(controller.relative(Direction.EAST), stairs(Direction.EAST, Half.BOTTOM))
                .placeMachine(PatternPredicatesFixtures.PARTIAL_STATE_FACING_AND_HALF_ID, controller)
                .formNow()
                .assertNotFormed()
                .succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void partial_state_without_requirements_matches_selected_block(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        MBDScenario.of(h)
                .placeBlock(controller.relative(Direction.WEST), Blocks.STONE.defaultBlockState())
                .placeBlock(controller.relative(Direction.EAST), Blocks.STONE.defaultBlockState())
                .placeMachine(PatternPredicatesFixtures.PARTIAL_STATE_BLOCK_ONLY_ID, controller)
                .assertFormed()
                .succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void partial_state_without_requirements_rejects_different_block(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        MBDScenario.of(h)
                .placeBlock(controller.relative(Direction.WEST), Blocks.DIRT.defaultBlockState())
                .placeBlock(controller.relative(Direction.EAST), Blocks.DIRT.defaultBlockState())
                .placeMachine(PatternPredicatesFixtures.PARTIAL_STATE_BLOCK_ONLY_ID, controller)
                .formNow()
                .assertNotFormed()
                .succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void partial_state_invalid_property_requirement_fails_closed(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        MBDScenario.of(h)
                .placeBlock(controller.relative(Direction.WEST), stairs(Direction.EAST, Half.TOP))
                .placeBlock(controller.relative(Direction.EAST), stairs(Direction.EAST, Half.TOP))
                .placeMachine(PatternPredicatesFixtures.PARTIAL_STATE_INVALID_ID, controller)
                .formNow()
                .assertNotFormed()
                .succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void partial_state_invalid_property_value_fails_closed(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        MBDScenario.of(h)
                .placeBlock(controller.relative(Direction.WEST), stairs(Direction.EAST, Half.TOP))
                .placeBlock(controller.relative(Direction.EAST), stairs(Direction.EAST, Half.TOP))
                .placeMachine(PatternPredicatesFixtures.PARTIAL_STATE_INVALID_VALUE_ID, controller)
                .formNow()
                .assertNotFormed()
                .succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void partial_state_property_requirement_codecs_round_trip(GameTestHelper h) {
        var requirement = new PredicatePartialState.PropertyRequirement(" facing ", " east ");
        var encoded = PredicatePartialState.PropertyRequirement.CODEC.encodeStart(JavaOps.INSTANCE, requirement).getOrThrow();
        var decoded = PredicatePartialState.PropertyRequirement.CODEC.parse(JavaOps.INSTANCE, encoded).getOrThrow();
        if (!"facing".equals(decoded.property()) || !"east".equals(decoded.value())) {
            h.fail("Codec did not preserve sanitized property requirement, got " + decoded.property() + "=" + decoded.value());
            return;
        }

        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess(), ConnectionType.OTHER);
        PredicatePartialState.PropertyRequirement.STREAM_CODEC.encode(buffer, requirement);
        var streamed = PredicatePartialState.PropertyRequirement.STREAM_CODEC.decode(buffer);
        if (!"facing".equals(streamed.property()) || !"east".equals(streamed.value())) {
            h.fail("Stream codec did not preserve sanitized property requirement, got " + streamed.property() + "=" + streamed.value());
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void partial_state_predicate_codec_round_trips_structured_requirements(GameTestHelper h) {
        var predicate = new PredicatePartialState(Blocks.OAK_STAIRS,
                new PredicatePartialState.PropertyRequirement("facing", "east"),
                new PredicatePartialState.PropertyRequirement("half", "top"));
        var encoded = PatternPredicate.CODEC.encodeStart(JavaOps.INSTANCE, predicate).getOrThrow();
        var decoded = PatternPredicate.CODEC.parse(JavaOps.INSTANCE, encoded).getOrThrow();
        var candidates = decoded.getCandidates();
        if (candidates == null || candidates.length != 1) {
            h.fail("Decoded partial-state predicate should expose one candidate");
            return;
        }
        var state = candidates[0].getBlockState();
        if (!state.is(Blocks.OAK_STAIRS) ||
                state.getValue(StairBlock.FACING) != Direction.EAST ||
                state.getValue(StairBlock.HALF) != Half.TOP) {
            h.fail("Decoded partial-state candidate did not preserve structured requirements: " + state);
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void partial_state_property_requirement_accessor_is_registered(GameTestHelper h) {
        var accessor = AccessorRegistries.findByClassOrNull(PredicatePartialState.PropertyRequirement.class);
        if (accessor == null) {
            h.fail("PropertyRequirement accessor was not registered");
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void proxy_while_formed_replaces_and_restores_original_block(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        BlockPos west = controller.relative(Direction.WEST);
        BlockPos east = controller.relative(Direction.EAST);
        var scenario = MBDScenario.of(h)
                .placeBlock(west, Blocks.STONE.defaultBlockState())
                .placeBlock(east, Blocks.STONE.defaultBlockState())
                .placeMachine(PatternPredicatesFixtures.PROXY_ID, controller)
                .assertFormed();

        assertProxy(h, west);
        assertProxy(h, east);

        if (scenario.machine() instanceof MBDMultiblockMachine multiblock) {
            multiblock.onStructureInvalid(false);
        } else {
            h.fail("Expected multiblock controller");
            return;
        }

        if (!h.getBlockState(west).is(Blocks.STONE) || !h.getBlockState(east).is(Blocks.STONE)) {
            h.fail("Proxy blocks did not restore their original stone blocks");
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void proxy_while_formed_default_state_machine_has_standard_states(GameTestHelper h) {
        var proxy = new PatternPredicate.ProxyWhileFormed();
        var stateMachine = proxy.getStateMachine();
        for (var state : List.of("base", "formed", "unformed", "working", "waiting", "suspend")) {
            if (!stateMachine.hasState(state)) {
                h.fail("Default proxy state machine is missing state " + state);
                return;
            }
        }
        h.succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void proxy_while_formed_state_follows_controller_name_with_root_fallback(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        BlockPos west = controller.relative(Direction.WEST);
        var scenario = MBDScenario.of(h)
                .placeBlock(west, Blocks.STONE.defaultBlockState())
                .placeBlock(controller.relative(Direction.EAST), Blocks.STONE.defaultBlockState())
                .placeMachine(PatternPredicatesFixtures.PROXY_ID, controller)
                .assertFormed();

        var proxy = assertProxy(h, west);
        assertProxyShapeAndLight(h, west, true, 1);
        if (proxy.getProxyState().getLightLevel() != 1) {
            h.fail("Proxy root state light expected 1 but was " + proxy.getProxyState().getLightLevel());
            return;
        }

        scenario.machine().setMachineState("working");
        assertProxyShapeAndLight(h, west, false, 7);
        if (proxy.getProxyState().getLightLevel() != 7) {
            h.fail("Proxy working state light expected 7 but was " + proxy.getProxyState().getLightLevel());
            return;
        }

        scenario.machine().setMachineState("suspend");
        assertProxyShapeAndLight(h, west, true, 1);
        if (proxy.getProxyState().getLightLevel() != 1) {
            h.fail("Proxy missing suspend state should fall back to root light 1 but was " + proxy.getProxyState().getLightLevel());
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void proxy_while_formed_still_applies_when_slot_name_is_set(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        BlockPos west = controller.relative(Direction.WEST);
        MBDScenario.of(h)
                .placeBlock(west, Blocks.STONE.defaultBlockState())
                .placeBlock(controller.relative(Direction.EAST), Blocks.STONE.defaultBlockState())
                .placeMachine(PatternPredicatesFixtures.SLOT_PROXY_ID, controller)
                .assertFormed();

        assertProxy(h, west);
        h.succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void proxy_while_formed_keeps_distinct_state_machines_per_position(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        BlockPos west = controller.relative(Direction.WEST);
        BlockPos east = controller.relative(Direction.EAST);
        var scenario = MBDScenario.of(h)
                .placeBlock(west, Blocks.STONE.defaultBlockState())
                .placeBlock(east, Blocks.STONE.defaultBlockState())
                .placeMachine(PatternPredicatesFixtures.MULTI_PROXY_ID, controller)
                .assertFormed();

        var westProxy = assertProxy(h, west);
        var eastProxy = assertProxy(h, east);
        if (westProxy.getProxyState().getLightLevel() != 4 || eastProxy.getProxyState().getLightLevel() != 1) {
            h.fail("Proxy root lights should stay per-position: west=" + westProxy.getProxyState().getLightLevel()
                    + ", east=" + eastProxy.getProxyState().getLightLevel());
            return;
        }

        scenario.machine().setMachineState("working");
        if (westProxy.getProxyState().getLightLevel() != 9 || eastProxy.getProxyState().getLightLevel() != 7) {
            h.fail("Proxy working lights should stay per-position: west=" + westProxy.getProxyState().getLightLevel()
                    + ", east=" + eastProxy.getProxyState().getLightLevel());
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void proxy_while_formed_applies_to_all_or_branches_when_configured_after_or(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        BlockPos west = controller.relative(Direction.WEST);
        BlockPos east = controller.relative(Direction.EAST);
        MBDScenario.of(h)
                .placeBlock(west, Blocks.DIRT.defaultBlockState())
                .placeBlock(east, Blocks.STONE.defaultBlockState())
                .placeMachine(PatternPredicatesFixtures.OR_PROXY_ID, controller)
                .assertFormed();

        assertProxy(h, west, Blocks.DIRT);
        assertProxy(h, east, Blocks.STONE);
        h.succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void proxy_while_formed_allows_rechecking_pattern_against_existing_proxy_blocks(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        BlockPos west = controller.relative(Direction.WEST);
        var scenario = MBDScenario.of(h)
                .placeBlock(west, Blocks.STONE.defaultBlockState())
                .placeBlock(controller.relative(Direction.EAST), Blocks.STONE.defaultBlockState())
                .placeMachine(PatternPredicatesFixtures.PROXY_ID, controller)
                .assertFormed();

        if (!(scenario.machine() instanceof MBDMultiblockMachine multiblock)) {
            h.fail("Expected multiblock controller");
            return;
        }
        if (!multiblock.checkPatternWithLock()) {
            h.fail("Pattern should still match after structure blocks were replaced by proxies");
            return;
        }
        assertProxy(h, west);
        h.succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void proxy_while_formed_restores_original_block_entity_data(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        BlockPos west = controller.relative(Direction.WEST);
        BlockPos east = controller.relative(Direction.EAST);
        placeChestWithItem(h, west, new ItemStack(Items.DIAMOND, 3));
        placeChestWithItem(h, east, new ItemStack(Items.EMERALD, 2));

        var scenario = MBDScenario.of(h)
                .placeMachine(PatternPredicatesFixtures.CHEST_PROXY_ID, controller)
                .assertFormed();

        var proxy = assertProxy(h, west, Blocks.CHEST);
        if (proxy.getOriginalData() == null || proxy.getOriginalData().isEmpty()) {
            h.fail("Proxy did not capture original chest NBT");
            return;
        }

        if (scenario.machine() instanceof MBDMultiblockMachine multiblock) {
            multiblock.onStructureInvalid(false);
        } else {
            h.fail("Expected multiblock controller");
            return;
        }

        assertChestItem(h, west, Items.DIAMOND, 3);
        assertChestItem(h, east, Items.EMERALD, 2);
        h.succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void proxy_while_formed_creative_style_removal_destroys_proxy_without_drops(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        BlockPos west = controller.relative(Direction.WEST);
        BlockPos east = controller.relative(Direction.EAST);
        var scenario = MBDScenario.of(h)
                .placeBlock(west, Blocks.STONE.defaultBlockState())
                .placeBlock(east, Blocks.STONE.defaultBlockState())
                .placeMachine(PatternPredicatesFixtures.PROXY_ID, controller)
                .assertFormed();

        assertProxy(h, west);

        var level = h.getLevel();
        var absoluteWest = h.absolutePos(west);
        var state = level.getBlockState(absoluteWest);
        if (!state.onDestroyedByPlayer(level, absoluteWest, null, false, level.getFluidState(absoluteWest))) {
            h.fail("Creative-style proxy removal returned false");
            return;
        }
        state.getBlock().destroy(level, absoluteWest, state);

        if (!h.getBlockState(west).isAir()) {
            h.fail("Creative-style proxy removal should leave air, got " + h.getBlockState(west));
            return;
        }
        assertNoDroppedItem(h, west, Items.STONE);
        assertBlock(h, east, Blocks.STONE, "Creative-style proxy removal should restore the other proxied block");
        scenario.assertNotFormed();
        h.succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void proxy_while_formed_non_player_destroy_without_drops_destroys_proxy(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        BlockPos west = controller.relative(Direction.WEST);
        BlockPos east = controller.relative(Direction.EAST);
        var scenario = MBDScenario.of(h)
                .placeBlock(west, Blocks.STONE.defaultBlockState())
                .placeBlock(east, Blocks.STONE.defaultBlockState())
                .placeMachine(PatternPredicatesFixtures.PROXY_ID, controller)
                .assertFormed();

        assertProxy(h, west);
        if (!h.getLevel().destroyBlock(h.absolutePos(west), false)) {
            h.fail("Non-player proxy destruction without drops returned false");
            return;
        }
        if (!h.getBlockState(west).isAir()) {
            h.fail("Non-player proxy destruction without drops should leave air, got " + h.getBlockState(west));
            return;
        }
        assertNoDroppedItem(h, west, Items.STONE);
        assertBlock(h, east, Blocks.STONE, "Non-player proxy destruction without drops should restore the other proxied block");
        scenario.assertNotFormed();
        h.succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void proxy_while_formed_non_player_destroy_with_drops_drops_original_block(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        BlockPos west = controller.relative(Direction.WEST);
        BlockPos east = controller.relative(Direction.EAST);
        var scenario = MBDScenario.of(h)
                .placeBlock(west, Blocks.DIRT.defaultBlockState())
                .placeBlock(east, Blocks.STONE.defaultBlockState())
                .placeMachine(PatternPredicatesFixtures.OR_PROXY_ID, controller)
                .assertFormed();

        assertProxy(h, west, Blocks.DIRT);
        if (!h.getLevel().destroyBlock(h.absolutePos(west), true)) {
            h.fail("Non-player proxy destruction with drops returned false");
            return;
        }
        if (!h.getBlockState(west).isAir()) {
            h.fail("Non-player proxy destruction with drops should leave air, got " + h.getBlockState(west));
            return;
        }
        assertDroppedItem(h, west, Items.DIRT, 1);
        assertBlock(h, east, Blocks.STONE, "Non-player proxy destruction with drops should restore the other proxied block");
        scenario.assertNotFormed();
        h.succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void proxy_while_formed_survival_style_harvest_drops_original_after_removal(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        BlockPos west = controller.relative(Direction.WEST);
        BlockPos east = controller.relative(Direction.EAST);
        var scenario = MBDScenario.of(h)
                .placeBlock(west, Blocks.DIRT.defaultBlockState())
                .placeBlock(east, Blocks.STONE.defaultBlockState())
                .placeMachine(PatternPredicatesFixtures.OR_PROXY_ID, controller)
                .assertFormed();

        assertProxy(h, west, Blocks.DIRT);
        var level = h.getLevel();
        var absoluteWest = h.absolutePos(west);
        var state = level.getBlockState(absoluteWest);
        var blockEntity = level.getBlockEntity(absoluteWest);
        if (!state.onDestroyedByPlayer(level, absoluteWest, null, true, level.getFluidState(absoluteWest))) {
            h.fail("Survival-style proxy removal returned false");
            return;
        }
        state.getBlock().destroy(level, absoluteWest, state);
        Block.dropResources(state, level, absoluteWest, blockEntity, null, ItemStack.EMPTY);

        if (!h.getBlockState(west).isAir()) {
            h.fail("Survival-style proxy harvest should leave air, got " + h.getBlockState(west));
            return;
        }
        assertDroppedItem(h, west, Items.DIRT, 1);
        assertBlock(h, east, Blocks.STONE, "Survival-style proxy harvest should restore the other proxied block");
        scenario.assertNotFormed();
        h.succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void proxy_while_formed_proxy_block_entity_persists_proxy_state_machine(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        BlockPos west = controller.relative(Direction.WEST);
        var scenario = MBDScenario.of(h)
                .placeBlock(west, Blocks.STONE.defaultBlockState())
                .placeBlock(controller.relative(Direction.EAST), Blocks.STONE.defaultBlockState())
                .placeMachine(PatternPredicatesFixtures.PROXY_ID, controller)
                .assertFormed();

        roundTripBlockEntity(h, west);
        var proxy = assertProxy(h, west);
        if (proxy.getProxyState().getLightLevel() != 1) {
            h.fail("Round-tripped proxy root state light expected 1 but was " + proxy.getProxyState().getLightLevel());
            return;
        }

        scenario.machine().setMachineState("working");
        if (proxy.getProxyState().getLightLevel() != 7) {
            h.fail("Round-tripped proxy working state light expected 7 but was " + proxy.getProxyState().getLightLevel());
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_multiblock")
    @PrefixGameTestTemplate(false)
    public static void controller_nbt_mismatch_rejects_pattern(GameTestHelper h) {
        BlockPos controller = new BlockPos(5, 1, 5);
        MBDScenario.of(h)
                .placeBlock(controller.relative(Direction.WEST), Blocks.STONE.defaultBlockState())
                .placeBlock(controller.relative(Direction.EAST), Blocks.STONE.defaultBlockState())
                .placeMachine(PatternPredicatesFixtures.CONTROLLER_NBT_ID, controller)
                .formNow()
                .assertNotFormed()
                .succeed();
    }

    private static ProxyPartBlockEntity assertProxy(GameTestHelper h, BlockPos pos) {
        return assertProxy(h, pos, Blocks.STONE);
    }

    private static ProxyPartBlockEntity assertProxy(GameTestHelper h, BlockPos pos, net.minecraft.world.level.block.Block expectedOriginalBlock) {
        if (!h.getBlockState(pos).is(ProxyPartBlock.BLOCK)) {
            h.fail("Expected ProxyPartBlock at " + pos + " but found " + h.getBlockState(pos));
            throw new AssertionError();
        }
        if (!(h.getBlockEntity(pos) instanceof ProxyPartBlockEntity proxy)) {
            h.fail("Expected ProxyPartBlockEntity at " + pos);
            throw new AssertionError();
        }
        if (proxy.getOriginalState() == null || !proxy.getOriginalState().is(expectedOriginalBlock)) {
            h.fail("Proxy original state should be " + expectedOriginalBlock + " at " + pos);
            throw new AssertionError();
        }
        if (proxy.getControllerPos() == null) {
            h.fail("Proxy controller position was not synced at " + pos);
            throw new AssertionError();
        }
        return proxy;
    }

    private static void assertBlock(GameTestHelper h, BlockPos pos, net.minecraft.world.level.block.Block expectedBlock, String message) {
        if (!h.getBlockState(pos).is(expectedBlock)) {
            h.fail(message + ", got " + h.getBlockState(pos));
        }
    }

    private static void assertProxyShapeAndLight(GameTestHelper h, BlockPos pos, boolean expectedEmptyShape, int expectedLight) {
        var absolutePos = h.absolutePos(pos);
        var state = h.getLevel().getBlockState(absolutePos);
        var shape = state.getShape(h.getLevel(), absolutePos, CollisionContext.empty());
        if (shape.isEmpty() != expectedEmptyShape) {
            h.fail("Proxy shape empty expected " + expectedEmptyShape + " but was " + shape.isEmpty());
        }
        int light = state.getLightEmission(h.getLevel(), absolutePos);
        if (light != expectedLight) {
            h.fail("Proxy light expected " + expectedLight + " but was " + light);
        }
    }

    private static void placeChestWithItem(GameTestHelper h, BlockPos pos, ItemStack stack) {
        h.setBlock(pos, Blocks.CHEST.defaultBlockState());
        if (h.getBlockEntity(pos) instanceof Container container) {
            container.setItem(0, stack.copy());
            ((BlockEntity) container).setChanged();
        } else {
            h.fail("Expected chest container at " + pos);
        }
    }

    private static void assertChestItem(GameTestHelper h, BlockPos pos, net.minecraft.world.item.Item item, int count) {
        if (!(h.getBlockEntity(pos) instanceof Container container)) {
            h.fail("Expected restored chest container at " + pos);
            return;
        }
        var stack = container.getItem(0);
        if (stack.getItem() != item || stack.getCount() != count) {
            h.fail("Expected restored chest slot 0 to contain " + count + " " + item + ", got " + stack);
        }
    }

    private static void assertDroppedItem(GameTestHelper h, BlockPos pos, Item item, int minCount) {
        int count = countDroppedItems(h, pos, item);
        if (count < minCount) {
            h.fail("Expected at least " + minCount + " dropped " + item + " near " + pos + ", got " + count);
        }
    }

    private static void assertNoDroppedItem(GameTestHelper h, BlockPos pos, Item item) {
        int count = countDroppedItems(h, pos, item);
        if (count != 0) {
            h.fail("Expected no dropped " + item + " near " + pos + ", got " + count);
        }
    }

    private static int countDroppedItems(GameTestHelper h, BlockPos pos, Item item) {
        var absolutePos = h.absolutePos(pos);
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(absolutePos).inflate(1.5),
                        entity -> entity.getItem().is(item))
                .stream()
                .mapToInt(entity -> entity.getItem().getCount())
                .sum();
    }

    private static void roundTripBlockEntity(GameTestHelper h, BlockPos pos) {
        var level = h.getLevel();
        var absolutePos = h.absolutePos(pos);
        var state = level.getBlockState(absolutePos);
        var original = level.getBlockEntity(absolutePos);
        if (original == null) {
            h.fail("No block entity at " + pos + " to round-trip");
            return;
        }
        CompoundTag saved = original.saveWithFullMetadata(level.registryAccess());
        original.setRemoved();
        level.removeBlockEntity(absolutePos);
        if (!(state.getBlock() instanceof EntityBlock entityBlock)) {
            h.fail("Block at " + pos + " is not an EntityBlock");
            return;
        }
        var fresh = entityBlock.newBlockEntity(absolutePos, state);
        if (fresh == null) {
            h.fail("Could not recreate block entity at " + pos);
            return;
        }
        level.setBlockEntity(fresh);
        fresh.loadWithComponents(saved, level.registryAccess());
        fresh.clearRemoved();
    }

    private static BlockState stairs(Direction facing, Half half) {
        return Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, facing)
                .setValue(StairBlock.HALF, half);
    }
}
