package com.lowdragmc.mbd2.test.tests.recipe.arsnouveau;

import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.integration.arsnouveau.ArsSourceNearbyCondition;
import com.lowdragmc.mbd2.integration.arsnouveau.trait.SourceStorageCapabilityTrait;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

// No @GameTestHolder: registered via MBDTestRegistry#onRegisterGameTests (mod-load guarded)
// to avoid NeoForge force-loading this soft-dep class when the mod is absent.
public class ArsSourceNearbyConditionTests {
    static { @SuppressWarnings("unused") var ignored = ArsSourceRecipeCapabilityFixtures.CONSUMER_ID; }

    private static final BlockPos POS = new BlockPos(0, 1, 1);
    private static final BlockPos JAR = new BlockPos(2, 1, 1);
    /** Small, so a scan never reaches the machine of the test running beside it. See the storage tests. */
    private static final int RADIUS = 2;

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void condition_matches_a_well_stocked_area(GameTestHelper h) {
        placeSourceJar(h, JAR, 2000);
        var machine = MBDScenario.of(h)
                .placeMachine(ArsSourceRecipeCapabilityFixtures.PRODUCER_ID, POS)
                .machine();
        var condition = new ArsSourceNearbyCondition(RADIUS, 1000, Integer.MAX_VALUE);
        if (!condition.test(MBDTestHelper.dummyRecipe(), machine.getRecipeLogic())) {
            h.fail("the condition did not see 2000 source in a jar two blocks away");
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void condition_fails_below_the_minimum(GameTestHelper h) {
        placeSourceJar(h, JAR, 500);
        var machine = MBDScenario.of(h)
                .placeMachine(ArsSourceRecipeCapabilityFixtures.PRODUCER_ID, POS)
                .machine();
        var condition = new ArsSourceNearbyCondition(RADIUS, 1000, Integer.MAX_VALUE);
        if (condition.test(MBDTestHelper.dummyRecipe(), machine.getRecipeLogic())) {
            h.fail("the condition matched 500 source against a minimum of 1000");
            return;
        }
        h.succeed();
    }

    /**
     * The upper bound is a real bound, not decoration — it is what a "only run in a depleted area"
     * recipe is written with, and the early exit that implements it is easy to get inverted.
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void condition_fails_above_the_maximum(GameTestHelper h) {
        placeSourceJar(h, JAR, 5000);
        var machine = MBDScenario.of(h)
                .placeMachine(ArsSourceRecipeCapabilityFixtures.PRODUCER_ID, POS)
                .machine();
        var condition = new ArsSourceNearbyCondition(RADIUS, 0, 1000);
        if (condition.test(MBDTestHelper.dummyRecipe(), machine.getRecipeLogic())) {
            h.fail("the condition matched 5000 source against a maximum of 1000");
            return;
        }
        // and the same area passes once the window is opened
        if (!new ArsSourceNearbyCondition(RADIUS, 0, 10000).test(MBDTestHelper.dummyRecipe(), machine.getRecipeLogic())) {
            h.fail("the condition rejected 5000 source against a maximum of 10000");
            return;
        }
        h.succeed();
    }

    /**
     * The machine's own buffer counts, because Ars Nouveau's devices would count it too.
     *
     * <p>The producer fixture carries a storage trait with {@code expose_to_devices} on, so it is one of
     * the providers {@code SourceUtil} reports — the condition would be lying if it disagreed with what
     * an Enchanting Apparatus standing on the same block would find.</p>
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void condition_counts_the_machines_own_buffer(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(ArsSourceRecipeCapabilityFixtures.PRODUCER_ID, POS);
        var machine = scenario.machine();
        var condition = new ArsSourceNearbyCondition(RADIUS, 1000, Integer.MAX_VALUE);
        if (condition.test(MBDTestHelper.dummyRecipe(), machine.getRecipeLogic())) {
            h.fail("an empty area matched a minimum of 1000");
            return;
        }
        scenario.with(m -> {
            for (var trait : m.getAdditionalTraits()) {
                if (trait instanceof SourceStorageCapabilityTrait storage) {
                    storage.getStorage().setSource(1500);
                }
            }
        });
        if (!condition.test(MBDTestHelper.dummyRecipe(), machine.getRecipeLogic())) {
            h.fail("the condition did not count the machine's own 1500 source");
            return;
        }
        h.succeed();
    }

    private static void placeSourceJar(GameTestHelper h, BlockPos relPos, int source) {
        h.setBlock(relPos, BlockRegistry.SOURCE_JAR.get().defaultBlockState());
        if (source > 0 && h.getBlockEntity(relPos) instanceof SourceJarTile jar) {
            jar.setSource(source);
        }
    }
}
