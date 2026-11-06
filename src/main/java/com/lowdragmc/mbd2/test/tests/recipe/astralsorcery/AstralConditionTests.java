package com.lowdragmc.mbd2.test.tests.recipe.astralsorcery;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.integration.astralsorcery.AstralSorceryIntegration;
import com.lowdragmc.mbd2.integration.astralsorcery.research.AstralResearchCondition;
import com.lowdragmc.mbd2.integration.astralsorcery.research.AstralResearcherTrait;
import com.lowdragmc.mbd2.integration.astralsorcery.sky.AstralCelestialEventCondition;
import com.lowdragmc.mbd2.integration.astralsorcery.sky.AstralConstellationCondition;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import com.lowdragmc.mbd2.test.framework.SyncMarks;
import com.mojang.authlib.GameProfile;
import hellfirepvp.astralsorcery.common.constellation.BaseConstellation;
import hellfirepvp.astralsorcery.common.constellation.level.LevelSkyHandler;
import hellfirepvp.astralsorcery.common.lib.ConstellationsAS;
import hellfirepvp.astralsorcery.common.research.PlayerProgress;
import hellfirepvp.astralsorcery.common.research.PlayerProgressFullAccess;
import hellfirepvp.astralsorcery.common.research.PlayerProgressTestAccess;
import hellfirepvp.astralsorcery.common.research.ResearchTier;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;
import java.util.UUID;

// No @GameTestHolder: registered via MBDTestRegistry#onRegisterGameTests (mod-load guarded)
// to avoid NeoForge force-loading this soft-dep class when the mod is absent.
public class AstralConditionTests {
    static { @SuppressWarnings("unused") var ignored = AstralRecipeCapabilityFixtures.RESEARCHER_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    /** Which constellations are up depends on the seed and day, so this compares against Astral Sorcery's sky. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void constellation_condition_agrees_with_the_sky(GameTestHelper h) {
        var machine = MBDScenario.of(h).placeMachine(AstralRecipeCapabilityFixtures.RESEARCHER_ID, POS).machine();
        var ctx = LevelSkyHandler.getContext(h.getLevel()).orElse(null);
        if (ctx == null) {
            h.fail("Astral Sorcery has no sky context for the test level");
            return;
        }
        var handler = ctx.getConstellationHandler();
        int active = 0, inactive = 0;
        for (var id : AstralSorceryIntegration.constellationIds()) {
            var constellation = AstralSorceryIntegration.constellation(id);
            var expected = handler.isCurrentlyActive(constellation);
            var actual = new AstralConstellationCondition(id, 0).test(MBDTestHelper.dummyRecipe(), machine.getRecipeLogic());
            if (expected != actual) {
                h.fail(id + ": condition says " + actual + ", Astral Sorcery says " + expected);
                return;
            }
            if (expected) active++; else inactive++;
            if (new AstralConstellationCondition(id, 1.01f).test(MBDTestHelper.dummyRecipe(), machine.getRecipeLogic())) {
                h.fail(id + " passed a minimum distribution above 1");
                return;
            }
        }
        if (active == 0 || inactive == 0) {
            h.fail("expected a mix of active and inactive constellations, got " + active + " active / " + inactive + " inactive");
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void celestial_event_condition_agrees_with_the_sky(GameTestHelper h) {
        var machine = MBDScenario.of(h).placeMachine(AstralRecipeCapabilityFixtures.RESEARCHER_ID, POS).machine();
        var events = LevelSkyHandler.getContext(h.getLevel()).orElseThrow().getCelestialEventHandler();
        boolean[][] expected = {
                {events.getSolarEclipse().isActiveNow(), events.getSolarEclipse().isActiveDay()},
                {events.getLunarEclipse().isActiveNow(), events.getLunarEclipse().isActiveDay()},
        };
        var kinds = AstralCelestialEventCondition.Event.values();
        for (int i = 0; i < kinds.length; i++) {
            for (int wholeDay = 0; wholeDay < 2; wholeDay++) {
                var actual = new AstralCelestialEventCondition(kinds[i], wholeDay == 1)
                        .test(MBDTestHelper.dummyRecipe(), machine.getRecipeLogic());
                if (actual != expected[i][wholeDay]) {
                    h.fail(kinds[i] + (wholeDay == 1 ? " (whole day)" : "") + ": condition says " + actual
                            + ", Astral Sorcery says " + expected[i][wholeDay]);
                    return;
                }
            }
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void research_condition_fails_without_a_researcher(GameTestHelper h) {
        var machine = MBDScenario.of(h).placeMachine(AstralRecipeCapabilityFixtures.RESEARCHER_ID, POS).machine();
        var condition = new AstralResearchCondition(AstralResearchCondition.Tier.SHIMMER, AstralSorceryIntegration.ANY, AstralSorceryIntegration.ANY);
        if (condition.test(MBDTestHelper.dummyRecipe(), machine.getRecipeLogic())) {
            h.fail("an unbound machine passed a research condition");
            return;
        }
        h.succeed();
    }

    /**
     * Progress is handed in: Astral Sorcery's own mutators sync to the client, which throws on a mock player's
     * connection. The random owner id is also the "offline owner" case.
     */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void research_condition_reads_the_owners_progress(GameTestHelper h) {
        var machine = MBDScenario.of(h).placeMachine(AstralRecipeCapabilityFixtures.RESEARCHER_ID, POS).machine();
        var snapshot = researcher(machine).getSnapshot();
        var any = AstralSorceryIntegration.ANY;
        var armara = ConstellationsAS.ARMARA.getId();
        var radiance = new AstralResearchCondition(AstralResearchCondition.Tier.RADIANCE, any, any);
        var knowsArmara = new AstralResearchCondition(AstralResearchCondition.Tier.SHIMMER, any, armara);
        var attunedArmara = new AstralResearchCondition(AstralResearchCondition.Tier.SHIMMER, armara, any);
        var attunedAevitas = new AstralResearchCondition(AstralResearchCondition.Tier.SHIMMER, ConstellationsAS.AEVITAS.getId(), any);
        var logic = machine.getRecipeLogic();

        snapshot.bind(UUID.randomUUID(), "researcher", PlayerProgressTestAccess.get());
        if (!new AstralResearchCondition(AstralResearchCondition.Tier.SHIMMER, any, any).test(MBDTestHelper.dummyRecipe(), logic)) {
            h.fail("a bound researcher did not pass the lowest tier");
            return;
        }
        if (radiance.test(MBDTestHelper.dummyRecipe(), logic) || knowsArmara.test(MBDTestHelper.dummyRecipe(), logic)) {
            h.fail("a blank researcher passed a radiance or discovery requirement");
            return;
        }

        snapshot.capture(PlayerProgressFullAccess.get(), "researcher");
        if (!radiance.test(MBDTestHelper.dummyRecipe(), logic) || !knowsArmara.test(MBDTestHelper.dummyRecipe(), logic)) {
            h.fail("a researcher with everything did not pass radiance and knowing armara");
            return;
        }
        if (attunedArmara.test(MBDTestHelper.dummyRecipe(), logic)) {
            h.fail("an unattuned researcher passed an attunement requirement");
            return;
        }

        var armaraConstellation = ConstellationsAS.ARMARA.get();
        snapshot.capture(new PlayerProgress() {
            @Override
            public Optional<BaseConstellation> getAttunedConstellation() {
                return Optional.of(armaraConstellation);
            }

            @Override
            public ResearchTier getTierReached() {
                return ResearchTier.RESONANCE;
            }
        }, "researcher");
        if (!attunedArmara.test(MBDTestHelper.dummyRecipe(), logic) || attunedAevitas.test(MBDTestHelper.dummyRecipe(), logic)) {
            h.fail("an armara-attuned researcher should pass armara and fail aevitas");
            return;
        }
        if (radiance.test(MBDTestHelper.dummyRecipe(), logic)) {
            h.fail("a resonance-tier researcher passed a radiance requirement");
            return;
        }
        h.succeed();
    }

    /** A bare {@link ServerPlayer} rather than a logged-in mock, whose login makes other mods sync payloads and throw. */
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void placing_the_machine_binds_the_placer_once(GameTestHelper h) {
        var machine = MBDScenario.of(h).placeMachine(AstralRecipeCapabilityFixtures.RESEARCHER_ID, POS).machine();
        var snapshot = researcher(machine).getSnapshot();
        var level = h.getLevel();

        machine.onMachinePlaced(FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "deployer")), ItemStack.EMPTY);
        if (snapshot.isBound()) {
            h.fail("a fake player was bound as the researcher");
            return;
        }

        var placer = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "placer"), ClientInformation.createDefault());
        machine.onMachinePlaced(placer, ItemStack.EMPTY);
        if (!placer.getUUID().equals(snapshot.getOwner()) || !"placer".equals(snapshot.getOwnerName())) {
            h.fail("the placer was not bound: owner " + snapshot.getOwner() + " (" + snapshot.getOwnerName() + ")");
            return;
        }

        var second = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "second"), ClientInformation.createDefault());
        machine.onMachinePlaced(second, ItemStack.EMPTY);
        if (!placer.getUUID().equals(snapshot.getOwner())) {
            h.fail("a second placement rebound an already bound machine");
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void researcher_snapshot_survives_a_reload(GameTestHelper h) {
        var owner = UUID.randomUUID();
        var scenario = MBDScenario.of(h).placeMachine(AstralRecipeCapabilityFixtures.RESEARCHER_ID, POS);
        researcher(scenario.machine()).getSnapshot().bind(owner, "researcher", PlayerProgressFullAccess.get());
        scenario.assertPersistenceRoundTrip()
                .check("the owner, tier and discoveries should survive a save/load cycle", m -> {
                    var snapshot = researcher(m).getSnapshot();
                    return owner.equals(snapshot.getOwner())
                            && "researcher".equals(snapshot.getOwnerName())
                            && snapshot.getTier() == ResearchTier.RADIANCE
                            && snapshot.getKnownConstellations().contains(ConstellationsAS.ARMARA.getId());
                })
                .succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void research_snapshot_is_compared_by_its_change_count(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(AstralRecipeCapabilityFixtures.RESEARCHER_ID, POS);
        var registries = h.getLevel().registryAccess();
        SyncMarks.of(h, researcher(scenario.machine()).getSnapshot())
                .changes("binding a researcher", s -> s.bind(UUID.randomUUID(), "researcher", PlayerProgressTestAccess.get()))
                .keeps("the same progress seen again", s -> s.capture(PlayerProgressTestAccess.get(), "researcher"))
                .changes("new progress", s -> s.capture(PlayerProgressFullAccess.get(), "researcher"))
                .changes("a load", s -> s.deserializeNBT(registries, s.serializeNBT(registries)));
        scenario.assertChangeGetsSaved("binding another researcher",
                        m -> researcher(m).getSnapshot().bind(UUID.randomUUID(), "other", PlayerProgressTestAccess.get()))
                .succeed();
    }

    private static AstralResearcherTrait researcher(MBDMachine machine) {
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof AstralResearcherTrait researcher) return researcher;
        }
        throw new AssertionError("fixture machine has no researcher trait");
    }
}
