package com.lowdragmc.mbd2.test.tests.recipe.arsnouveau;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.integration.arsnouveau.trait.CopiableSourceStorage;
import com.lowdragmc.mbd2.integration.arsnouveau.trait.SourceStorageCapabilityTrait;
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
public class ArsSourceRecipeCapabilityTests {
    static { @SuppressWarnings("unused") var ignored = ArsSourceRecipeCapabilityFixtures.CONSUMER_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);
    private static final int RUN_TICKS = 160;

    /** A recipe with a source cost spends the machine's own buffer. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID, timeoutTicks = 300)
    @PrefixGameTestTemplate(false)
    public static void recipe_consumes_source_from_the_buffer(GameTestHelper h) {
        var scenario = MBDScenario.of(h)
                .placeMachine(ArsSourceRecipeCapabilityFixtures.CONSUMER_ID, POS)
                .with(m -> storage(m).setSource(1000))
                .insertItem(0, new ItemStack(Items.DIRT, 1))
                .runUntil(m -> !MBDTestHelper.readItem(h, m, 1).isEmpty(), RUN_TICKS)
                .assertItemCountAtLeast(1, Items.COBBLESTONE, 1);
        var expected = 1000 - ArsSourceRecipeCapabilityFixtures.COST;
        var actual = storage(scenario.machine()).getSource();
        if (actual != expected) {
            h.fail("the buffer holds " + actual + ", expected " + expected + " after one craft");
            return;
        }
        scenario.succeed();
    }

    /**
     * With an empty buffer the recipe never starts, and the input is left alone.
     *
     * <p>The input matters as much as the output: inputs are taken when the recipe starts, so a match
     * that succeeded on a buffer it could not actually spend would eat the dirt for nothing.</p>
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void recipe_does_not_run_on_an_empty_buffer(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(ArsSourceRecipeCapabilityFixtures.CONSUMER_ID, POS)
                .insertItem(0, new ItemStack(Items.DIRT, 1))
                .runTicks(60)
                .assertItem(0, new ItemStack(Items.DIRT, 1))
                .check("nothing should have been produced",
                        m -> MBDTestHelper.readItem(h, m, 1).isEmpty())
                .succeed();
    }

    /** And a recipe producing source fills that buffer. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID, timeoutTicks = 300)
    @PrefixGameTestTemplate(false)
    public static void recipe_produces_source_into_the_buffer(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(ArsSourceRecipeCapabilityFixtures.PRODUCER_ID, POS)
                .insertItem(0, new ItemStack(Items.DIRT, 1))
                .runUntil(m -> storage(m).getSource() >= ArsSourceRecipeCapabilityFixtures.COST, RUN_TICKS)
                .succeed();
    }

    /**
     * A full buffer holds the recipe rather than dropping the output on the floor.
     *
     * <p>The output handler returns what did not fit, which is what stops the recipe — a handler
     * returning {@code null} regardless would silently destroy the produced source.</p>
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void recipe_does_not_run_on_a_full_buffer(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(ArsSourceRecipeCapabilityFixtures.PRODUCER_ID, POS)
                .with(m -> storage(m).setSource(ArsSourceRecipeCapabilityFixtures.CAPACITY))
                .insertItem(0, new ItemStack(Items.DIRT, 1))
                .runTicks(60)
                .assertItem(0, new ItemStack(Items.DIRT, 1))
                .check("the buffer should still be exactly full",
                        m -> storage(m).getSource() == ArsSourceRecipeCapabilityFixtures.CAPACITY)
                .succeed();
    }

    private static CopiableSourceStorage storage(MBDMachine machine) {
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof SourceStorageCapabilityTrait sourceTrait) return sourceTrait.getStorage();
        }
        throw new AssertionError("fixture machine has no source storage trait");
    }
}
