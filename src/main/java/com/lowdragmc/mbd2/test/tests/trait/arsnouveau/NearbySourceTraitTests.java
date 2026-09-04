package com.lowdragmc.mbd2.test.tests.trait.arsnouveau;

import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.integration.arsnouveau.trait.NearbySourceTrait;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

// No @GameTestHolder: registered via MBDTestRegistry#onRegisterGameTests (mod-load guarded)
// to avoid NeoForge force-loading this soft-dep class when the mod is absent.
public class NearbySourceTraitTests {
    static { @SuppressWarnings("unused") var ignored = NearbySourceTraitFixtures.CONSUMER_ID; }

    /**
     * The machine sits at one end of the structure and the jar at the other, two blocks away on X.
     * <p>
     * Two rather than one so {@link #radius_override_changes_what_the_scan_sees} has somewhere to put a
     * jar that a radius of 1 cannot reach — Ars Nouveau's radius is a per-axis bound, so an adjacent
     * block is in range of every radius there is.
     */
    private static final BlockPos POS = new BlockPos(0, 1, 1);
    private static final BlockPos JAR = new BlockPos(2, 1, 1);
    private static final int RUN_TICKS = 160;

    /**
     * The scan counts what is actually there.
     *
     * <p>An IN-only machine has no reason to walk the neighbourhood a second time looking for room to
     * put source, so {@code freeSpace} staying zero is the assertion, not an oversight.</p>
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void scan_counts_nearby_jars(GameTestHelper h) {
        placeSourceJar(h, JAR, 1500);
        MBDScenario.of(h)
                .placeMachine(NearbySourceTraitFixtures.CONSUMER_ID, POS)
                .with(m -> trait(m).rescan())
                .check("the scan should see the jar's 1500 source",
                        m -> trait(m).getAvailableSource() == 1500)
                .check("an input-only handler should not scan for room to give",
                        m -> trait(m).getFreeSpace() == 0)
                .succeed();
    }

    /**
     * An output-only machine counts the source around it too.
     *
     * <p>Nothing in the recipe path needs that number — a producer only asks about room — but the
     * machine's UI shows it, and a scan that skipped it left a producer reading "Nearby Source: 0" for
     * ever while standing next to full jars.</p>
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void scan_counts_source_for_an_output_machine(GameTestHelper h) {
        placeSourceJar(h, JAR, 1500);
        MBDScenario.of(h)
                .placeMachine(NearbySourceTraitFixtures.PRODUCER_ID, POS)
                .with(m -> trait(m).rescan())
                .check("an output-only handler should still report what is around it",
                        m -> trait(m).getAvailableSource() == 1500)
                // read off the jar rather than hard-coded: its capacity is Ars Nouveau's to change
                .check("and the room it can actually use",
                        m -> trait(m).getFreeSpace() == sourceJar(h, JAR).getMaxSource() - 1500)
                .succeed();
    }

    /**
     * A recipe costing source runs off a nearby jar, and the jar pays for it.
     *
     * <p>End to end: the background recipe search reads the cached count, the recipe starts, and the
     * game-thread half spends real source through {@code SourceUtil}.</p>
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID, timeoutTicks = 300)
    @PrefixGameTestTemplate(false)
    public static void recipe_spends_source_from_a_nearby_jar(GameTestHelper h) {
        placeSourceJar(h, JAR, 1000);
        var scenario = MBDScenario.of(h)
                .placeMachine(NearbySourceTraitFixtures.CONSUMER_ID, POS)
                .insertItem(0, new ItemStack(Items.DIRT, 1))
                .runUntil(m -> !MBDTestHelper.readItem(h, m, 1).isEmpty(), RUN_TICKS)
                .assertItemCountAtLeast(1, Items.COBBLESTONE, 1);
        var expected = 1000 - NearbySourceTraitFixtures.COST;
        if (sourceJar(h, JAR).getSource() != expected) {
            h.fail("the jar holds " + sourceJar(h, JAR).getSource() + ", expected " + expected + " after one craft");
            return;
        }
        scenario.succeed();
    }

    /**
     * With no source in range the recipe never starts, and the input is not taken.
     *
     * <p>This is the assertion the cached scan exists for. An optimistic simulate — the shape
     * {@code AuraHandlerTrait} uses — would match here, and the dirt would be consumed before the
     * machine discovered it could not pay for the craft.</p>
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void recipe_does_not_run_without_source_nearby(GameTestHelper h) {
        placeSourceJar(h, JAR, 0);
        MBDScenario.of(h)
                .placeMachine(NearbySourceTraitFixtures.CONSUMER_ID, POS)
                .insertItem(0, new ItemStack(Items.DIRT, 1))
                .runTicks(60)
                .assertItem(0, new ItemStack(Items.DIRT, 1))
                .check("nothing should have been produced",
                        m -> MBDTestHelper.readItem(h, m, 1).isEmpty())
                .succeed();
    }

    /** A recipe producing source fills a nearby jar. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID, timeoutTicks = 300)
    @PrefixGameTestTemplate(false)
    public static void recipe_gives_source_to_a_nearby_jar(GameTestHelper h) {
        placeSourceJar(h, JAR, 0);
        var scenario = MBDScenario.of(h)
                .placeMachine(NearbySourceTraitFixtures.PRODUCER_ID, POS)
                .insertItem(0, new ItemStack(Items.DIRT, 1))
                .runUntil(m -> sourceJar(h, JAR).getSource() > 0, RUN_TICKS);
        if (sourceJar(h, JAR).getSource() < NearbySourceTraitFixtures.COST) {
            h.fail("the jar holds " + sourceJar(h, JAR).getSource() + ", expected at least "
                    + NearbySourceTraitFixtures.COST);
            return;
        }
        scenario.succeed();
    }

    /**
     * The radius is per machine, not per definition.
     *
     * <p>Asserted through {@code radiusBlocks()} because both readers — the scan and the recipe
     * handler — go through it, so it is the one place a reader could still be consulting the
     * definition.</p>
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void radius_override_replaces_the_definition_value(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(NearbySourceTraitFixtures.CONSUMER_ID, POS)
                .check("the radius starts on the definition",
                        m -> trait(m).radiusBlocks() == NearbySourceTraitFixtures.RADIUS)
                .with(m -> trait(m).radius.set(1))
                .check("an override should win", m -> trait(m).radiusBlocks() == 1)
                .assertPersistenceRoundTrip()
                .check("and survive a save/load cycle", m -> trait(m).radiusBlocks() == 1)
                .with(m -> trait(m).radius.clear())
                .check("clearing should go back to the definition",
                        m -> trait(m).radiusBlocks() == NearbySourceTraitFixtures.RADIUS)
                .succeed();
    }

    /**
     * Shrinking the radius takes a jar out of reach.
     *
     * <p>The override has to reach the scan and not only the recipe handler; a scan still using the
     * definition's radius would keep reporting source the machine can no longer spend.</p>
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void radius_override_changes_what_the_scan_sees(GameTestHelper h) {
        placeSourceJar(h, JAR, 1500);
        MBDScenario.of(h)
                .placeMachine(NearbySourceTraitFixtures.CONSUMER_ID, POS)
                .with(m -> trait(m).rescan())
                .check("a jar two blocks away is in reach at the definition's radius of 2",
                        m -> trait(m).getAvailableSource() == 1500)
                .with(m -> {
                    trait(m).radius.set(1);
                    trait(m).rescan();
                })
                .check("and out of reach at a radius of 1", m -> trait(m).getAvailableSource() == 0)
                .succeed();
    }

    private static void placeSourceJar(GameTestHelper h, BlockPos relPos, int source) {
        h.setBlock(relPos, BlockRegistry.SOURCE_JAR.get().defaultBlockState());
        if (source > 0) sourceJar(h, relPos).setSource(source);
    }

    private static SourceJarTile sourceJar(GameTestHelper h, BlockPos relPos) {
        if (h.getBlockEntity(relPos) instanceof SourceJarTile jar) return jar;
        throw new AssertionError("no source jar at " + relPos);
    }

    private static NearbySourceTrait trait(MBDMachine machine) {
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof NearbySourceTrait nearby) return nearby;
        }
        throw new AssertionError("fixture machine has no nearby source trait");
    }
}
