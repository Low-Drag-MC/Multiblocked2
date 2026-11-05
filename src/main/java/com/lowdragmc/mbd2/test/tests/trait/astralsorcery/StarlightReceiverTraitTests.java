package com.lowdragmc.mbd2.test.tests.trait.astralsorcery;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.integration.astralsorcery.starlight.MachineStarlightReceiverNode;
import com.lowdragmc.mbd2.integration.astralsorcery.starlight.StarlightBuffer;
import com.lowdragmc.mbd2.integration.astralsorcery.starlight.StarlightReceiverTrait;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import hellfirepvp.astralsorcery.common.lib.ConstellationsAS;
import hellfirepvp.astralsorcery.common.lib.DataAS;
import hellfirepvp.astralsorcery.common.lib.StarlightNetworkNodesAS;
import hellfirepvp.astralsorcery.common.starlight.StarlightNetworkLevelHelper;
import hellfirepvp.astralsorcery.common.starlight.api.TransmissionNode;
import hellfirepvp.astralsorcery.common.starlight.transmission.StarlightTransmissionPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

/** Deliveries call the node directly, as Astral Sorcery's transmission handler does each tick. */
// No @GameTestHolder: registered via MBDTestRegistry#onRegisterGameTests (mod-load guarded)
// to avoid NeoForge force-loading this soft-dep class when the mod is absent.
public class StarlightReceiverTraitTests {
    static { @SuppressWarnings("unused") var ignored = StarlightReceiverTraitFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID, timeoutTicks = 100)
    @PrefixGameTestTemplate(false)
    public static void machine_joins_the_starlight_network(GameTestHelper h) {
        var abs = h.absolutePos(POS);
        MBDScenario.of(h)
                .placeMachine(StarlightReceiverTraitFixtures.MACHINE_ID, POS)
                .runUntil(m -> node(h) != null, 60)
                .check("the node should sit at the machine's own position", m -> abs.equals(node(h).getNodePos()))
                .succeed();
    }

    /** A node left behind by a block that stood here before, e.g. an altar, is replaced rather than kept. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID, timeoutTicks = 100)
    @PrefixGameTestTemplate(false)
    public static void machine_replaces_a_stale_node_at_its_position(GameTestHelper h) {
        DataAS.DOMAIN_AS.getData(h.getLevel(), DataAS.KEY_STARLIGHT_NETWORK_DATA)
                .addTransmissionNode(StarlightNetworkNodesAS.FORWARDING_RECEIVER_NODE.get().provideNewNode(h.absolutePos(POS)));
        if (StarlightNetworkLevelHelper.get(h.getLevel()).getNode(h.absolutePos(POS)).isEmpty()) {
            h.fail("the stale node was not added");
            return;
        }
        MBDScenario.of(h)
                .placeMachine(StarlightReceiverTraitFixtures.MACHINE_ID, POS)
                .runUntil(m -> node(h) != null, 60)
                .succeed();
    }

    /** Astral Sorcery saves its network through this codec; a misregistered provider loses every receiver on save. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void node_survives_the_network_codec(GameTestHelper h) {
        var pos = h.absolutePos(POS);
        var encoded = TransmissionNode.CODEC.encodeStart(NbtOps.INSTANCE, new MachineStarlightReceiverNode(pos)).getOrThrow();
        var decoded = TransmissionNode.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        if (!(decoded instanceof MachineStarlightReceiverNode node) || !node.getNodePos().equals(pos)) {
            h.fail("the node did not come back as itself: " + encoded + " -> " + decoded);
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID, timeoutTicks = 100)
    @PrefixGameTestTemplate(false)
    public static void deliveries_fill_the_buffer_per_constellation(GameTestHelper h) {
        var scenario = MBDScenario.of(h)
                .placeMachine(StarlightReceiverTraitFixtures.MACHINE_ID, POS)
                .runUntil(m -> node(h) != null, 60);
        var node = node(h);
        node.receiveStarlight(h.getLevel(), new StarlightTransmissionPacket(ConstellationsAS.ARMARA.get(), 30f));
        node.receiveStarlight(h.getLevel(), new StarlightTransmissionPacket(ConstellationsAS.ARMARA.get(), 30f));
        node.receiveStarlight(h.getLevel(), new StarlightTransmissionPacket(ConstellationsAS.VICIO.get(), 5f));
        var buffer = buffer(scenario.machine());
        if (Math.abs(buffer.getAmount(ConstellationsAS.ARMARA.getId()) - 60) > 1e-3
                || Math.abs(buffer.getAmount(ConstellationsAS.VICIO.getId()) - 5) > 1e-3) {
            h.fail("expected 60 armara and 5 vicio, got " + buffer.getContents());
            return;
        }
        scenario.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID, timeoutTicks = 100)
    @PrefixGameTestTemplate(false)
    public static void constellation_filter_drops_other_starlight(GameTestHelper h) {
        var scenario = MBDScenario.of(h)
                .placeMachine(StarlightReceiverTraitFixtures.FILTERED_MACHINE_ID, POS)
                .runUntil(m -> node(h) != null, 60);
        var node = node(h);
        node.receiveStarlight(h.getLevel(), new StarlightTransmissionPacket(ConstellationsAS.ARMARA.get(), 30f));
        node.receiveStarlight(h.getLevel(), new StarlightTransmissionPacket(ConstellationsAS.AEVITAS.get(), 20f));
        var buffer = buffer(scenario.machine());
        if (buffer.getAmount(ConstellationsAS.ARMARA.getId()) != 0
                || Math.abs(buffer.getAmount(ConstellationsAS.AEVITAS.getId()) - 20) > 1e-3) {
            h.fail("an aevitas-only receiver should keep 20 aevitas and no armara, got " + buffer.getContents());
            return;
        }
        scenario.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID, timeoutTicks = 100)
    @PrefixGameTestTemplate(false)
    public static void buffer_stops_at_its_capacity(GameTestHelper h) {
        var scenario = MBDScenario.of(h)
                .placeMachine(StarlightReceiverTraitFixtures.MACHINE_ID, POS)
                .runUntil(m -> node(h) != null, 60);
        node(h).receiveStarlight(h.getLevel(), new StarlightTransmissionPacket(ConstellationsAS.ARMARA.get(), 5000f));
        var stored = buffer(scenario.machine()).getAmount(ConstellationsAS.ARMARA.getId());
        if (stored != StarlightReceiverTraitFixtures.CAPACITY) {
            h.fail("5000 starlight into a " + StarlightReceiverTraitFixtures.CAPACITY + " buffer left " + stored);
            return;
        }
        scenario.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void persistence_preserves_starlight(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(StarlightReceiverTraitFixtures.MACHINE_ID, POS);
        buffer(scenario.machine()).setAmount(ConstellationsAS.ARMARA.getId(), 12.5f);
        scenario.assertPersistenceRoundTrip()
                .check("stored starlight should survive a save/load cycle",
                        m -> buffer(m).getAmount(ConstellationsAS.ARMARA.getId()) == 12.5f)
                .succeed();
    }

    /** As the lumen storage's: the buffer is read back before the override that makes it big enough. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void grown_capacity_survives_a_reload(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(StarlightReceiverTraitFixtures.MACHINE_ID, POS);
        var grown = StarlightReceiverTraitFixtures.CAPACITY * 2;
        trait(scenario.machine()).capacity.set(grown);
        buffer(scenario.machine()).setAmount(ConstellationsAS.ARMARA.getId(), grown - 100);
        scenario.assertPersistenceRoundTrip()
                .check("the starlight above the definition's capacity should survive a save/load cycle",
                        m -> buffer(m).getAmount(ConstellationsAS.ARMARA.getId()) == grown - 100)
                .succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID, timeoutTicks = 100)
    @PrefixGameTestTemplate(false)
    public static void removing_the_machine_removes_its_node(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(StarlightReceiverTraitFixtures.MACHINE_ID, POS)
                .runUntil(m -> node(h) != null, 60);
        h.setBlock(POS, Blocks.AIR);
        if (StarlightNetworkLevelHelper.get(h.getLevel()).getNode(h.absolutePos(POS)).isPresent()) {
            h.fail("the starlight node outlived the machine");
            return;
        }
        h.succeed();
    }

    @Nullable
    private static MachineStarlightReceiverNode node(GameTestHelper h) {
        return StarlightNetworkLevelHelper.get(h.getLevel()).getNode(h.absolutePos(POS))
                .filter(MachineStarlightReceiverNode.class::isInstance)
                .map(MachineStarlightReceiverNode.class::cast)
                .orElse(null);
    }

    private static StarlightReceiverTrait trait(MBDMachine machine) {
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof StarlightReceiverTrait receiver) return receiver;
        }
        throw new AssertionError("fixture machine has no starlight receiver trait");
    }

    private static StarlightBuffer buffer(MBDMachine machine) {
        return trait(machine).getBuffer();
    }
}
