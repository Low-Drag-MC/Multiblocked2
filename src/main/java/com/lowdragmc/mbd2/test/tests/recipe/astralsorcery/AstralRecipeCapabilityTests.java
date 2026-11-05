package com.lowdragmc.mbd2.test.tests.recipe.astralsorcery;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.integration.astralsorcery.lumen.LumenStorage;
import com.lowdragmc.mbd2.integration.astralsorcery.lumen.LumenStorageCapabilityTrait;
import com.lowdragmc.mbd2.integration.astralsorcery.starlight.StarlightBuffer;
import com.lowdragmc.mbd2.integration.astralsorcery.starlight.StarlightReceiverTrait;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import com.lowdragmc.mbd2.test.tests.trait.astralsorcery.LumenStorageTraitFixtures;
import hellfirepvp.astralsorcery.common.lib.ConstellationsAS;
import hellfirepvp.astralsorcery.common.lib.LumenAS;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Each "does not run" test then fixes what was missing and waits for the output, so it cannot pass vacuously. */
// No @GameTestHolder: registered via MBDTestRegistry#onRegisterGameTests (mod-load guarded)
// to avoid NeoForge force-loading this soft-dep class when the mod is absent.
public class AstralRecipeCapabilityTests {
    static { @SuppressWarnings("unused") var ignored = AstralRecipeCapabilityFixtures.LUMEN_CONSUMER_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);
    private static final int RUN_TICKS = 160;

    // ----- lumen -----

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID, timeoutTicks = 300)
    @PrefixGameTestTemplate(false)
    public static void recipe_consumes_lumen_from_the_buffer(GameTestHelper h) {
        var aevitas = LumenAS.AEVITAS.get();
        var scenario = MBDScenario.of(h)
                .placeMachine(AstralRecipeCapabilityFixtures.LUMEN_CONSUMER_ID, POS)
                .with(m -> lumen(m).setAmount(aevitas, 1000))
                .insertItem(0, new ItemStack(Items.DIRT, 1))
                .runUntil(m -> !MBDTestHelper.readItem(h, m, 1).isEmpty(), RUN_TICKS)
                .assertItemCountAtLeast(1, Items.COBBLESTONE, 1);
        var left = lumen(scenario.machine()).getAmount(aevitas);
        if (left != 1000 - AstralRecipeCapabilityFixtures.LUMEN_COST) {
            h.fail("the buffer holds " + left + " aevitas, expected " + (1000 - AstralRecipeCapabilityFixtures.LUMEN_COST));
            return;
        }
        scenario.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID, timeoutTicks = 300)
    @PrefixGameTestTemplate(false)
    public static void recipe_does_not_run_on_the_wrong_lumen(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(AstralRecipeCapabilityFixtures.LUMEN_CONSUMER_ID, POS)
                .with(m -> lumen(m).setAmount(LumenAS.ARMARA.get(), LumenStorageTraitFixtures.CAPACITY))
                .insertItem(0, new ItemStack(Items.DIRT, 1))
                .runTicks(60)
                .assertItem(0, new ItemStack(Items.DIRT, 1))
                .check("nothing should have been produced", m -> MBDTestHelper.readItem(h, m, 1).isEmpty())
                .with(m -> lumen(m).setAmount(LumenAS.AEVITAS.get(), AstralRecipeCapabilityFixtures.LUMEN_COST))
                .runUntil(m -> !MBDTestHelper.readItem(h, m, 1).isEmpty(), RUN_TICKS)
                .succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID, timeoutTicks = 300)
    @PrefixGameTestTemplate(false)
    public static void recipe_produces_lumen_into_the_buffer(GameTestHelper h) {
        var armara = LumenAS.ARMARA.get();
        MBDScenario.of(h)
                .placeMachine(AstralRecipeCapabilityFixtures.LUMEN_PRODUCER_ID, POS)
                .insertItem(0, new ItemStack(Items.DIRT, 1))
                .runUntil(m -> lumen(m).getAmount(armara) >= AstralRecipeCapabilityFixtures.LUMEN_COST, RUN_TICKS)
                .succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID, timeoutTicks = 300)
    @PrefixGameTestTemplate(false)
    public static void recipe_does_not_run_on_a_full_buffer(GameTestHelper h) {
        var armara = LumenAS.ARMARA.get();
        MBDScenario.of(h)
                .placeMachine(AstralRecipeCapabilityFixtures.LUMEN_PRODUCER_ID, POS)
                .with(m -> lumen(m).setAmount(armara, LumenStorageTraitFixtures.CAPACITY))
                .insertItem(0, new ItemStack(Items.DIRT, 1))
                .runTicks(60)
                .assertItem(0, new ItemStack(Items.DIRT, 1))
                .check("the buffer should still be exactly full",
                        m -> lumen(m).getAmount(armara) == LumenStorageTraitFixtures.CAPACITY)
                .with(m -> lumen(m).setAmount(armara, 0))
                .runUntil(m -> lumen(m).getAmount(armara) >= AstralRecipeCapabilityFixtures.LUMEN_COST, RUN_TICKS)
                .succeed();
    }

    // ----- starlight -----

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID, timeoutTicks = 300)
    @PrefixGameTestTemplate(false)
    public static void recipe_consumes_constellation_starlight(GameTestHelper h) {
        var armara = ConstellationsAS.ARMARA.getId();
        var scenario = MBDScenario.of(h)
                .placeMachine(AstralRecipeCapabilityFixtures.STARLIGHT_CONSUMER_ID, POS)
                .with(m -> starlight(m).setAmount(armara, 100))
                .insertItem(0, new ItemStack(Items.DIRT, 1))
                .runUntil(m -> !MBDTestHelper.readItem(h, m, 1).isEmpty(), RUN_TICKS)
                .assertItemCountAtLeast(1, Items.COBBLESTONE, 1);
        var left = starlight(scenario.machine()).getAmount(armara);
        var expected = 100 - AstralRecipeCapabilityFixtures.ARMARA_STARLIGHT_COST;
        if (Math.abs(left - expected) > 1e-3) {
            h.fail("the buffer holds " + left + " armara starlight, expected " + expected);
            return;
        }
        scenario.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID, timeoutTicks = 300)
    @PrefixGameTestTemplate(false)
    public static void recipe_ignores_other_constellations(GameTestHelper h) {
        var aevitas = ConstellationsAS.AEVITAS.getId();
        MBDScenario.of(h)
                .placeMachine(AstralRecipeCapabilityFixtures.STARLIGHT_CONSUMER_ID, POS)
                .with(m -> starlight(m).setAmount(aevitas, 500))
                .insertItem(0, new ItemStack(Items.DIRT, 1))
                .runTicks(60)
                .assertItem(0, new ItemStack(Items.DIRT, 1))
                .check("aevitas starlight should not have paid for an armara recipe",
                        m -> starlight(m).getAmount(aevitas) == 500)
                .with(m -> starlight(m).setAmount(ConstellationsAS.ARMARA.getId(), AstralRecipeCapabilityFixtures.ARMARA_STARLIGHT_COST))
                .runUntil(m -> !MBDTestHelper.readItem(h, m, 1).isEmpty(), RUN_TICKS)
                .succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID, timeoutTicks = 300)
    @PrefixGameTestTemplate(false)
    public static void any_constellation_recipe_pools_the_buffer(GameTestHelper h) {
        var scenario = MBDScenario.of(h)
                .placeMachine(AstralRecipeCapabilityFixtures.ANY_STARLIGHT_CONSUMER_ID, POS)
                .with(m -> {
                    starlight(m).setAmount(ConstellationsAS.ARMARA.getId(), 30);
                    starlight(m).setAmount(ConstellationsAS.AEVITAS.getId(), 30);
                })
                .insertItem(0, new ItemStack(Items.GRAVEL, 1))
                .runUntil(m -> !MBDTestHelper.readItem(h, m, 1).isEmpty(), RUN_TICKS)
                .assertItemCountAtLeast(1, Items.SAND, 1);
        var total = starlight(scenario.machine()).getTotal();
        var expected = 60 - AstralRecipeCapabilityFixtures.ANY_STARLIGHT_COST;
        if (Math.abs(total - expected) > 1e-3) {
            h.fail("the buffer holds " + total + " starlight in all, expected " + expected);
            return;
        }
        scenario.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID, timeoutTicks = 300)
    @PrefixGameTestTemplate(false)
    public static void starlight_recipe_waits_on_an_empty_buffer(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(AstralRecipeCapabilityFixtures.ANY_STARLIGHT_CONSUMER_ID, POS)
                .insertItem(0, new ItemStack(Items.GRAVEL, 1))
                .runTicks(60)
                .assertItem(0, new ItemStack(Items.GRAVEL, 1))
                .check("nothing should have been produced", m -> MBDTestHelper.readItem(h, m, 1).isEmpty())
                .with(m -> starlight(m).setAmount(ConstellationsAS.VICIO.getId(), AstralRecipeCapabilityFixtures.ANY_STARLIGHT_COST))
                .runUntil(m -> !MBDTestHelper.readItem(h, m, 1).isEmpty(), RUN_TICKS)
                .succeed();
    }

    private static LumenStorage lumen(MBDMachine machine) {
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof LumenStorageCapabilityTrait lumenTrait) return lumenTrait.getStorage();
        }
        throw new AssertionError("fixture machine has no lumen storage trait");
    }

    private static StarlightBuffer starlight(MBDMachine machine) {
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof StarlightReceiverTrait receiver) return receiver.getBuffer();
        }
        throw new AssertionError("fixture machine has no starlight receiver trait");
    }
}
