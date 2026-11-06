package com.lowdragmc.mbd2.test.tests.trait.astralsorcery;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.integration.astralsorcery.lumen.LumenHandlerList;
import com.lowdragmc.mbd2.integration.astralsorcery.lumen.LumenHandlerWrapper;
import com.lowdragmc.mbd2.integration.astralsorcery.lumen.LumenStorage;
import com.lowdragmc.mbd2.integration.astralsorcery.lumen.LumenStorageCapabilityTrait;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import com.lowdragmc.mbd2.test.framework.SyncMarks;
import hellfirepvp.astralsorcery.common.lib.LumenAS;
import hellfirepvp.astralsorcery.common.lumen.ILumenHandler;
import hellfirepvp.astralsorcery.common.lumen.LumenStack;
import hellfirepvp.astralsorcery.common.lumen.transfer.LumenNetworkHelper;
import hellfirepvp.astralsorcery.common.lumen.transfer.LumenRequestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

// No @GameTestHolder: registered via MBDTestRegistry#onRegisterGameTests (mod-load guarded)
// to avoid NeoForge force-loading this soft-dep class when the mod is absent.
public class LumenStorageTraitTests {
    static { @SuppressWarnings("unused") var ignored = LumenStorageTraitFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);
    /** The lumen network is shared world data and a request reaches 16 blocks, further than the gap between tests. */
    private static final String NETWORK_BATCH = "astral_lumen_network";

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void lumen_capability_is_exposed(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(LumenStorageTraitFixtures.MACHINE_ID, POS)
                .assertExposes(ILumenHandler.BLOCK, null,
                        cap -> cap.getCapacity(LumenAS.AEVITAS.get()) == LumenStorageTraitFixtures.CAPACITY)
                .assertExposes(ILumenHandler.BLOCK, Direction.NORTH)
                .succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void capability_transfers_and_respects_simulate(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(LumenStorageTraitFixtures.MACHINE_ID, POS);
        var storage = storage(scenario.machine());
        var cap = MBDTestHelper.capability(h, POS, ILumenHandler.BLOCK, null);
        var aevitas = LumenAS.AEVITAS.get();
        var armara = LumenAS.ARMARA.get();
        if (cap == null) {
            h.fail("no lumen capability");
            return;
        }
        if (cap.fill(aevitas.stack(500), ILumenHandler.Action.SIMULATE) != 500 || storage.getAmount(aevitas) != 0) {
            h.fail("a simulated fill should report 500 and store nothing");
            return;
        }
        if (cap.fill(aevitas.stack(500), ILumenHandler.Action.EXECUTE) != 500 || storage.getAmount(aevitas) != 500) {
            h.fail("filling 500 aevitas did not store 500");
            return;
        }
        if (cap.fill(armara.stack(300), ILumenHandler.Action.EXECUTE) != 300 || storage.getAmount(armara) != 300) {
            h.fail("a second type did not get its own 300");
            return;
        }
        if (cap.drain(aevitas, 200, ILumenHandler.Action.SIMULATE).getAmount() != 200 || storage.getAmount(aevitas) != 500) {
            h.fail("a simulated drain took lumen anyway");
            return;
        }
        var drained = cap.drain(aevitas, 200, ILumenHandler.Action.EXECUTE);
        if (drained.getAmount() != 200 || !drained.is(aevitas) || storage.getAmount(aevitas) != 300) {
            h.fail("draining 200 aevitas left " + storage.getAmount(aevitas) + ", expected 300");
            return;
        }
        if (cap.isEmpty() || cap.getContainedLumen().size() != 2) {
            h.fail("the handler should list two stored types and not report empty");
            return;
        }
        scenario.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void type_filter_rejects_other_lumen(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(LumenStorageTraitFixtures.FILTERED_MACHINE_ID, POS);
        var cap = MBDTestHelper.capability(h, POS, ILumenHandler.BLOCK, null);
        var aevitas = LumenAS.AEVITAS.get();
        var armara = LumenAS.ARMARA.get();
        if (cap == null) {
            h.fail("no lumen capability");
            return;
        }
        if (cap.getCapacity(armara) != 0 || cap.fill(armara.stack(100), ILumenHandler.Action.EXECUTE) != 0) {
            h.fail("an aevitas-only buffer accepted armara");
            return;
        }
        if (cap.fill(aevitas.stack(100), ILumenHandler.Action.EXECUTE) != 100) {
            h.fail("an aevitas-only buffer refused aevitas");
            return;
        }
        scenario.succeed();
    }

    /** The internal side is the one a network request drains through. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void capability_io_gates_the_wrapper(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(LumenStorageTraitFixtures.EXTRACT_ONLY_MACHINE_ID, POS);
        var aevitas = LumenAS.AEVITAS.get();
        storage(scenario.machine()).setAmount(aevitas, 1000);
        var cap = MBDTestHelper.capability(h, POS, ILumenHandler.BLOCK, null);
        if (cap == null) {
            h.fail("no lumen capability");
            return;
        }
        if (cap.fill(aevitas.stack(100), ILumenHandler.Action.EXECUTE) != 0) {
            h.fail("an OUT-only machine accepted lumen");
            return;
        }
        if (cap.drain(aevitas, 100, ILumenHandler.Action.EXECUTE).getAmount() != 100) {
            h.fail("an OUT-only machine refused to give lumen");
            return;
        }
        scenario.succeed();
    }

    /** What a part proxying several controllers hands out: one handler spilling over into the next. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void handler_list_spreads_over_proxied_storages(GameTestHelper h) {
        var aevitas = LumenAS.AEVITAS.get();
        var first = new LumenStorage(100, List.of());
        var second = new LumenStorage(100, List.of());
        var inputOnly = new LumenStorage(100, List.of());
        inputOnly.setAmount(aevitas, 100);
        var list = new LumenHandlerList(new ILumenHandler[]{
                new LumenHandlerWrapper(first, IO.BOTH, 1000, 1000),
                new LumenHandlerWrapper(inputOnly, IO.IN, 1000, 1000),
                new LumenHandlerWrapper(second, IO.BOTH, 1000, 1000)});
        if (list.isEmpty() || list.getCapacity(aevitas) != 300) {
            h.fail("three 100 buffers should report a capacity of 300 and not be empty");
            return;
        }
        if (list.fill(aevitas.stack(150), ILumenHandler.Action.EXECUTE) != 150
                || first.getAmount(aevitas) != 100 || second.getAmount(aevitas) != 50) {
            h.fail("150 should fill the first buffer and spill 50 into the last");
            return;
        }
        var drained = list.drain(aevitas, 1000, ILumenHandler.Action.EXECUTE);
        if (drained.getAmount() != 150 || inputOnly.getAmount(aevitas) != 100) {
            h.fail("draining should take the 150 from the two extractable buffers, took " + drained.getAmount());
            return;
        }
        if (list.getContainedLumen(aevitas).map(LumenStack::getAmount).orElse(0) != 100) {
            h.fail("the list should still report the input-only buffer's 100");
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void persistence_preserves_lumen(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(LumenStorageTraitFixtures.MACHINE_ID, POS);
        var aevitas = LumenAS.AEVITAS.get();
        var armara = LumenAS.ARMARA.get();
        storage(scenario.machine()).setAmount(aevitas, 1234);
        storage(scenario.machine()).setAmount(armara, 56);
        scenario.assertPersistenceRoundTrip()
                .check("stored lumen should survive a save/load cycle",
                        m -> storage(m).getAmount(aevitas) == 1234 && storage(m).getAmount(armara) == 56)
                .succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void capacity_override_resizes_and_clamps(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(LumenStorageTraitFixtures.MACHINE_ID, POS);
        var aevitas = LumenAS.AEVITAS.get();
        storage(scenario.machine()).setAmount(aevitas, 3000);
        trait(scenario.machine()).capacity.set(1000);
        scenario.check("shrinking the capacity should clamp the stored amount",
                        m -> storage(m).getAmount(aevitas) == 1000 && storage(m).getCapacity(aevitas) == 1000)
                .succeed();
    }

    /** The storage is read back before the capacity override, so clamping on load would lose the excess. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void grown_capacity_survives_a_reload(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(LumenStorageTraitFixtures.MACHINE_ID, POS);
        var aevitas = LumenAS.AEVITAS.get();
        var grown = LumenStorageTraitFixtures.CAPACITY * 2;
        trait(scenario.machine()).capacity.set(grown);
        storage(scenario.machine()).setAmount(aevitas, grown - 500);
        scenario.assertPersistenceRoundTrip()
                .check("the lumen above the definition's capacity should survive a save/load cycle",
                        m -> storage(m).getAmount(aevitas) == grown - 500 && storage(m).getCapacity() == grown)
                .succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID, timeoutTicks = 100)
    @PrefixGameTestTemplate(false)
    public static void machine_off_the_network_has_no_node(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(LumenStorageTraitFixtures.MACHINE_ID, POS);
        storage(scenario.machine()).setAmount(LumenAS.AEVITAS.get(), 1000);
        scenario.runTicks(45)
                .check("a machine kept off the network should have no lumen node",
                        m -> LumenNetworkHelper.getNode(h.getLevel(), h.absolutePos(POS)).isEmpty())
                .succeed();
    }

    // ----- the lumen network -----

    /** The exact request an altar or crystallizer makes. */
    @GameTest(template = "empty_multiblock", templateNamespace = MBD2.MOD_ID, batch = NETWORK_BATCH, timeoutTicks = 200)
    @PrefixGameTestTemplate(false)
    public static void astral_request_finds_the_machine_as_a_source(GameTestHelper h) {
        var pos = new BlockPos(3, 1, 5);
        var provided = LumenStorageTraitFixtures.PROVIDED_LUMEN.get();
        var scenario = MBDScenario.of(h).placeMachine(LumenStorageTraitFixtures.SOURCE_MACHINE_ID, pos);
        storage(scenario.machine()).setAmount(provided, 1000);
        var abs = h.absolutePos(pos);
        scenario.runUntil(m -> LumenNetworkHelper.getNode(h.getLevel(), abs)
                .map(node -> node.getProvidedLumenTypes().contains(provided)).orElse(false), 60);
        var chain = LumenRequestHelper.requestRelayed(h.getLevel(), h.absolutePos(new BlockPos(7, 1, 5)), provided.stack(100));
        if (chain.isEmpty() || !chain.get().getEndNode().getPos().equals(abs)) {
            h.fail("a lumen request 4 blocks away did not end at the machine");
            return;
        }
        var source = h.getLevel().getCapability(ILumenHandler.BLOCK, chain.get().getEndNode().getPos(), null);
        if (source == null || source.drain(provided, 100, ILumenHandler.Action.EXECUTE).getAmount() != 100) {
            h.fail("the requester could not drain the machine at the end of the chain");
            return;
        }
        if (storage(scenario.machine()).getAmount(provided) != 900) {
            h.fail("the machine should have 900 left after giving 100");
            return;
        }
        scenario.succeed();
    }

    @GameTest(template = "empty_multiblock", templateNamespace = MBD2.MOD_ID, batch = NETWORK_BATCH, timeoutTicks = 300)
    @PrefixGameTestTemplate(false)
    public static void machine_draws_from_a_source_machine(GameTestHelper h) {
        var sinkLumen = LumenStorageTraitFixtures.SINK_LUMEN.get();
        var scenario = MBDScenario.of(h).placeMachine(LumenStorageTraitFixtures.SOURCE_MACHINE_ID, new BlockPos(2, 1, 2));
        var source = scenario.machine();
        storage(source).setAmount(sinkLumen, 1000);
        scenario.placeMachine(LumenStorageTraitFixtures.SINK_MACHINE_ID, new BlockPos(8, 1, 2))
                .runUntil(m -> storage(m).getAmount(sinkLumen) >= 3 * LumenStorageTraitFixtures.SINK_RATE, 200);
        var drawn = storage(scenario.machine()).getAmount(sinkLumen);
        var left = storage(source).getAmount(sinkLumen);
        if (drawn + left != 1000) {
            h.fail("lumen was not conserved: " + drawn + " drawn + " + left + " left != 1000");
            return;
        }
        scenario.succeed();
    }

    /** Requested from its position instead of as its own node, Astral Sorcery's line-of-sight check throws. */
    @GameTest(template = "empty_multiblock", templateNamespace = MBD2.MOD_ID, batch = NETWORK_BATCH, timeoutTicks = 300)
    @PrefixGameTestTemplate(false)
    public static void drawing_source_pulls_without_offering_what_it_draws(GameTestHelper h) {
        var drawn = LumenStorageTraitFixtures.DRAWN_BY_SOURCE_LUMEN.get();
        var scenario = MBDScenario.of(h).placeMachine(LumenStorageTraitFixtures.SOURCE_MACHINE_ID, new BlockPos(2, 1, 8));
        var source = scenario.machine();
        storage(source).setAmount(drawn, 1000);
        var drawerPos = new BlockPos(8, 1, 8);
        var drawerAbs = h.absolutePos(drawerPos);
        scenario.placeMachine(LumenStorageTraitFixtures.DRAWING_SOURCE_MACHINE_ID, drawerPos)
                .runUntil(m -> storage(m).getAmount(drawn) == 1000
                        && LumenNetworkHelper.getNode(h.getLevel(), drawerAbs).isPresent(), 200);
        if (storage(source).getAmount(drawn) != 0) {
            h.fail("lumen was not conserved: the source still holds " + storage(source).getAmount(drawn));
            return;
        }
        if (LumenNetworkHelper.getNode(h.getLevel(), drawerAbs).orElseThrow().getProvidedLumenTypes().contains(drawn)) {
            h.fail("a machine drawing " + drawn.getName().getString() + " also offered it to the network");
            return;
        }
        scenario.succeed();
    }

    /** Drawing switched on at runtime, before the next node sync stops advertising the drawn type. */
    @GameTest(template = "empty_multiblock", templateNamespace = MBD2.MOD_ID, batch = NETWORK_BATCH, timeoutTicks = 200)
    @PrefixGameTestTemplate(false)
    public static void pulling_a_type_the_machine_still_offers_does_not_throw(GameTestHelper h) {
        var pos = new BlockPos(8, 1, 5);
        var abs = h.absolutePos(pos);
        var offered = LumenAS.AION.get();
        var scenario = MBDScenario.of(h).placeMachine(LumenStorageTraitFixtures.SOURCE_MACHINE_ID, pos)
                .with(m -> storage(m).setAmount(offered, 500))
                .runUntil(m -> LumenNetworkHelper.getNode(h.getLevel(), abs)
                        .map(node -> node.getProvidedLumenTypes().contains(offered)).orElse(false), 60);
        var trait = trait(scenario.machine());
        trait.lumenTypes.set(List.of(LumenAS.AION.getId().toString()));
        trait.drawFromNetwork.set(true);
        trait.pullFromNetwork(h.getLevel());
        scenario.succeed();
    }

    @GameTest(template = "empty_multiblock", templateNamespace = MBD2.MOD_ID, batch = NETWORK_BATCH, timeoutTicks = 200)
    @PrefixGameTestTemplate(false)
    public static void removing_the_machine_removes_its_node(GameTestHelper h) {
        var pos = new BlockPos(5, 1, 8);
        var abs = h.absolutePos(pos);
        MBDScenario.of(h)
                .placeMachine(LumenStorageTraitFixtures.SOURCE_MACHINE_ID, pos)
                .runUntil(m -> LumenNetworkHelper.getNode(h.getLevel(), abs).isPresent(), 60);
        h.setBlock(pos, Blocks.AIR);
        if (LumenNetworkHelper.getNode(h.getLevel(), abs).isPresent()) {
            h.fail("the lumen node outlived the machine");
            return;
        }
        h.succeed();
    }

    /** {@link LumenStorage} counts changes to what it saves, so the sync compares that count instead of saving it. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void lumen_is_compared_by_its_change_count(GameTestHelper h) {
        var machine = MBDScenario.of(h).placeMachine(LumenStorageTraitFixtures.MACHINE_ID, POS).machine();
        var registries = h.getLevel().registryAccess();
        var aevitas = LumenAS.AEVITAS.get();
        var armara = LumenAS.ARMARA.get();
        SyncMarks.of(h, storage(machine))
                .keeps("a simulated fill", s -> s.fill(aevitas, 500, true))
                .keeps("draining what is not there", s -> s.drain(armara, 10, false))
                .changes("a fill", s -> s.fill(aevitas, 500, false))
                .changes("a drain", s -> s.drain(aevitas, 100, false))
                .keeps("setting the amount it already has", s -> s.setAmount(aevitas, 400))
                .changes("a new amount", s -> s.setAmount(aevitas, 50))
                .keeps("a capacity that spills nothing, as the capacity is not saved", s -> s.setCapacity(60))
                .changes("a capacity that spills", s -> s.setCapacity(10))
                .changes("a load", s -> s.deserializeNBT(registries, s.serializeNBT(registries)));
        h.succeed();
    }

    private static LumenStorageCapabilityTrait trait(MBDMachine machine) {
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof LumenStorageCapabilityTrait lumenTrait) return lumenTrait;
        }
        throw new AssertionError("fixture machine has no lumen storage trait");
    }

    private static LumenStorage storage(MBDMachine machine) {
        return trait(machine).getStorage();
    }
}
