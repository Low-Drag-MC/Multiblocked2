package com.lowdragmc.mbd2.test.tests.recipe.logic;

import com.google.common.collect.Tables;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeCapabilityHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandler;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.capability.recipe.ItemRecipeCapability;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineEvent;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineRecipeModifyEvent;
import com.lowdragmc.mbd2.common.trait.item.ItemSlotCapabilityTrait;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import com.lowdragmc.mbd2.test.framework.MachineEventRecorder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import static com.lowdragmc.mbd2.test.tests.recipe.logic.RecipeLogicGoldenFixtures.*;
import static com.lowdragmc.mbd2.test.tests.recipe.logic.RecipeLogicGoldenTests.*;

/**
 * One test per recipe logic bug, each written to fail against the code before its fix.
 */
@GameTestHolder(MBD2.MOD_ID)
public class RecipeLogicFixTests {
    static { @SuppressWarnings("unused") var ignored = RecipeLogicFixFixtures.GATED_MACHINE; }

    // region match verdicts

    /** {@code FAIL_NO_REASON} was built with {@code isSuccess = true}: a holder with no handlers matched anything. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void no_handlers_match_nothing_with_contents(GameTestHelper h) {
        IRecipeCapabilityHolder empty = () -> Tables.newCustomTable(new EnumMap<>(IO.class), HashMap::new);
        var press = recipe(h, PRESS);
        check(h, !MBDRecipe.ActionResult.FAIL_NO_REASON.isSuccess(), "FAIL_NO_REASON reports success");
        check(h, !press.matchRecipe(empty).isSuccess(), "a recipe with inputs matched a holder with no handlers");
        check(h, !press.matchTickRecipe(empty).isSuccess(), "a per-tick input matched a holder with no handlers");
        check(h, !press.handleRecipeIO(IO.IN, empty), "a holder with no handlers took a recipe's inputs");
        var nothing = MBDTestHelper.dummyRecipe();
        check(h, nothing.matchRecipe(empty).isSuccess() && nothing.matchTickRecipe(empty).isSuccess(),
                "a recipe asking for nothing does not match an empty holder");
        check(h, nothing.handleRecipeIO(IO.IN, empty) && nothing.handleRecipeIO(IO.OUT, empty),
                "a recipe asking for nothing cannot be handled by an empty holder");
        h.succeed();
    }

    /** Kept on purpose through that fix: a machine with no traits does not look for recipes at all. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void a_traitless_machine_never_starts_a_recipe(GameTestHelper h) {
        MBDScenario.of(h).placeMachine(RecipeLogicFixFixtures.TRAITLESS_MACHINE, POS)
                .runTicks(40)
                .check("a machine without traits started its contentless recipe", m -> logic(m).isIdle()
                        && logic(m).getLastRecipe() == null)
                .succeed();
    }

    // endregion

    // region events

    /** It was posted on the NeoForge bus only, so blueprint and KubeJS handlers never saw it. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void fuel_burning_out_reaches_blueprints(GameTestHelper h) {
        MBDScenario.of(h).placeMachine(RecipeLogicFixFixtures.FUEL_BLUEPRINT_MACHINE, POS)
                .insertItem(0, new ItemStack(Items.SAND))
                .insertItem(1, new ItemStack(Items.COAL))
                .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                .check("no marker before the fuel is out", m -> m.getAnalogOutputSignal() == 0)
                .runUntil(m -> logic(m).getFuelTime() == 0, 60)
                .check("the blueprint saw the fuel burn out", m -> m.getAnalogOutputSignal() == RecipeLogicFixFixtures.FUEL_OUT_MARKER)
                .succeed();
    }

    /**
     * A recipe that matches its contents but fails its condition was retried in the very tick it failed, posting
     * its modify events twice per search.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void a_condition_failing_recipe_is_modified_once_per_search(GameTestHelper h) {
        var s = MBDScenario.of(h).placeMachine(RecipeLogicFixFixtures.GATED_MACHINE, POS);
        var machine = s.machine();
        var ticks = new ArrayList<Long>();
        Consumer<MachineEvent> listener = event -> {
            if (event.machine == machine && event instanceof MachineRecipeModifyEvent.Before) {
                ticks.add(h.getLevel().getGameTime());
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, MachineEvent.class, listener);
        try {
            s.insertItem(0, new ItemStack(Items.STONE)).runTicks(60);
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        check(h, ticks.size() >= 2, "too few searches to tell: " + ticks);
        check(h, ticks.stream().distinct().count() == ticks.size(), "one search modified the recipe more than once: " + ticks);
        s.check("the gated recipe never ran", m -> logic(m).isIdle() && count(h, m, 0, Items.STONE) == 1).succeed();
    }

    // endregion

    // region recipe types

    /** A proxied recipe type that does not exist used to end the loop, dropping every proxy listed after it. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void a_missing_proxy_type_does_not_drop_the_rest(GameTestHelper h) {
        var type = MBDRegistries.RECIPE_TYPES.get(RecipeLogicFixFixtures.PROXY_TYPE);
        var recipes = h.getLevel().getRecipeManager().getAllRecipesFor((MBDRecipeType) type);
        check(h, !recipes.isEmpty(), "no smelting recipe was proxied after the missing type");
        h.succeed();
    }

    // endregion

    // region output space at the finish

    /** A recipe whose outputs no longer fit when it finishes used to void them; now it waits for room. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void full_outputs_at_the_finish_wait_instead_of_voiding(GameTestHelper h) {
        var s = MBDScenario.of(h).placeMachine(MACHINE, POS);
        try (var trace = MachineEventRecorder.start(h, POS)) {
            s.insertItem(0, new ItemStack(Items.STONE))
                    .insertEnergy(100_000)
                    .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                    .runTicks(5)
                    .with(m -> fillOutputs(m, Items.GRAVEL))
                    .runUntil(m -> !logic(m).isWorking(), 40)
                    .check("the finished recipe waits for room", m -> logic(m).isWaiting()
                            && logic(m).getProgress() == logic(m).getDuration()
                            && waitingKey(m).equals("mbd2.recipe_logic.insufficient_out"))
                    .runTicks(10)
                    .check("and keeps waiting at full progress", m -> logic(m).isWaiting()
                            && logic(m).getProgress() == 20 && count(h, m, 2, Items.DIRT) == 0)
                    .with(m -> output(m).storage.setStackInSlot(0, ItemStack.EMPTY))
                    .runUntil(m -> logic(m).isIdle(), 5)
                    .check("the dirt lands once there is room", m -> count(h, m, 2, Items.DIRT) == 1)
                    .check("only the 20 working ticks used energy", m -> energy(h, m) == 100_000 - 200);
            trace.assertTrace(h,
                    "RecipeModifyBefore(golden_press)",
                    "RecipeModifyAfter(golden_press)",
                    "BeforeRecipeWorking(golden_press)",
                    "StateChanged(base->working)",
                    "RecipeStatusChanged(IDLE->WORKING)",
                    "OnRecipeWorking(golden_press) ×20",
                    "StateChanged(working->waiting)",
                    "RecipeStatusChanged(WORKING->WAITING)",
                    "OnRecipeWaiting(golden_press) ×11",
                    "AfterRecipeWorking(golden_press)",
                    "OnRecipeFinish(golden_press)",
                    "StateChanged(waiting->base)",
                    "RecipeStatusChanged(WAITING->IDLE)");
        }
        s.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void a_blocked_finish_survives_a_reload(GameTestHelper h) {
        MBDScenario.of(h).placeMachine(MACHINE, POS)
                .insertItem(0, new ItemStack(Items.STONE))
                .insertEnergy(100_000)
                .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                .with(m -> fillOutputs(m, Items.GRAVEL))
                .runUntil(m -> logic(m).isWaiting(), 40)
                .assertPersistenceRoundTrip()
                .runTicks(5)
                .check("still waiting for room after the reload", m -> logic(m).isWaiting() && logic(m).getProgress() == 20)
                .with(m -> output(m).storage.setStackInSlot(1, ItemStack.EMPTY))
                .runUntil(m -> logic(m).isIdle(), 5)
                .check("the dirt lands once there is room", m -> count(h, m, 3, Items.DIRT) == 1)
                .succeed();
    }

    /**
     * A recipe taking its inputs when it finishes takes them before it waits for room: left in their slots, they
     * could be taken back while it waited, and the outputs still made once there was room.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void deferred_inputs_are_taken_before_waiting_for_room(GameTestHelper h) {
        MBDScenario.of(h).placeMachine(AFTER_MACHINE, POS)
                .insertItem(0, new ItemStack(Items.COBBLESTONE))
                .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                .with(RecipeLogicFixTests::finishIntoFullOutputs)
                .runTicks(1)
                .check("full outputs make it wait at full progress", m -> logic(m).isWaiting()
                        && logic(m).getProgress() == logic(m).getDuration())
                .check("its input already taken", m -> count(h, m, 0, Items.COBBLESTONE) == 0)
                .with(m -> output(m).storage.setStackInSlot(0, ItemStack.EMPTY))
                .runUntil(m -> logic(m).isIdle(), 5)
                .check("the gravel lands once there is room", m -> count(h, m, 2, Items.GRAVEL) == 1)
                .succeed();
    }

    /**
     * Deferred inputs are checked before they are taken: taking them outright took what was left of them and failed
     * on the rest, losing it.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void deferred_inputs_partly_gone_at_the_finish_are_left_alone(GameTestHelper h) {
        MBDScenario.of(h).placeMachine(RecipeLogicFixFixtures.PAIR_AFTER_MACHINE, POS)
                .insertItem(0, new ItemStack(Items.COBBLESTONE, 2))
                .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                .with(m -> {
                    input(m).storage.setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
                    logic(m).setProgress(logic(m).getDuration());
                })
                .runTicks(2)
                .check("the recipe was dropped", m -> logic(m).isIdle())
                .check("the cobblestone left is still there", m -> count(h, m, 0, Items.COBBLESTONE) == 1)
                .check("and nothing was made", m -> outputCount(m) == 0)
                .succeed();
    }

    /**
     * Deferred inputs that matched at the finish but could not all be taken still made the outputs. Only a broken
     * handler simulates one thing and does another, but the machine must not hand out outputs for it.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void a_finish_that_cannot_take_its_deferred_inputs_makes_nothing(GameTestHelper h) {
        var s = MBDScenario.of(h).placeMachine(AFTER_MACHINE, POS);
        s.machine().getRecipeLogic().getRecipeCapabilitiesProxy().get(IO.IN, ItemRecipeCapability.CAP).add(0, new OneShotLiar());
        s.runUntil(m -> outputCount(m) >= 1, START_BUDGET + 40)
                .runTicks(60)
                .check("only the finish that took its input made anything", m -> outputCount(m) == 1)
                .succeed();
    }

    /**
     * Inputs taken at the finish can be what fills the slots its outputs need. Waiting for room before taking them
     * waited for good; taking them first, as the finish always did, makes the room.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void deferred_inputs_in_the_way_of_the_outputs_are_taken_first(GameTestHelper h) {
        MBDScenario.of(h).placeMachine(RecipeLogicFixFixtures.SHARED_SLOTS_AFTER_MACHINE, POS)
                .insertItem(0, new ItemStack(Items.COBBLESTONE))
                .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                .with(m -> {
                    input(m).storage.setStackInSlot(1, new ItemStack(Items.DIRT, 64));
                    logic(m).setProgress(logic(m).getDuration());
                })
                .runTicks(2)
                .check("the cobblestone was taken and its gravel took its place", m -> count(h, m, 0, Items.GRAVEL) == 1)
                .check("nothing is left waiting", m -> logic(m).isIdle())
                .check("the dirt is untouched", m -> count(h, m, 1, Items.DIRT) == 64)
                .succeed();
    }

    /** A recipe whose duration came out 0 started and then worked forever, making nothing. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void a_recipe_without_a_duration_still_finishes(GameTestHelper h) {
        MBDScenario.of(h).placeMachine(RecipeLogicFixFixtures.INSTANT_MACHINE, POS)
                .insertItem(0, new ItemStack(Items.STONE, 2))
                .runUntil(m -> count(h, m, 1, Items.DIRT) == 2, START_BUDGET)
                .runUntil(m -> logic(m).isIdle(), 5)
                .check("both stones taken", m -> count(h, m, 0, Items.STONE) == 0)
                .succeed();
    }

    private static int outputCount(MBDMachine machine) {
        var storage = output(machine).storage;
        int count = 0;
        for (int slot = 0; slot < storage.getSlots(); slot++) {
            count += storage.getStackInSlot(slot).getCount();
        }
        return count;
    }

    /** Fill the outputs and bring the working recipe to its finish, which then finds no room. */
    private static void finishIntoFullOutputs(MBDMachine machine) {
        fillOutputs(machine, Items.DIRT);
        machine.getRecipeLogic().setProgress(machine.getRecipeLogic().getDuration());
    }

    /**
     * A re-run from the finish whose inputs were not there after all (the match said yes, the real take found
     * nothing) used to leave the logic working at full progress: it finished again every tick, outputs for free.
     * Once that was fixed it still told the handlers the failed re-run started, and stopped them right after.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void a_rerun_that_cannot_take_its_inputs_goes_idle(GameTestHelper h) {
        var s = MBDScenario.of(h).placeMachine(MACHINE, POS).insertEnergy(100_000);
        var handlers = s.machine().getRecipeLogic().getRecipeCapabilitiesProxy()
                .get(IO.IN, ItemRecipeCapability.CAP);
        handlers.add(0, new OneShotLiar());
        var probe = WorkingProbe.attach(s.machine().getRecipeLogic());
        s.runUntil(m -> logic(m).isWorking(), START_BUDGET)
                .runUntil(m -> count(h, m, 2, Items.DIRT) >= 1, 40)
                .runTicks(40)
                .check("the failed re-run went idle", m -> logic(m).isIdle())
                .check("and handed out nothing more", m -> count(h, m, 2, Items.DIRT) == 1);
        check(h, probe.pre == 1 && probe.post == 1, "the failed re-run told the handlers it started: " + probe);
        h.succeed();
    }

    /**
     * An After Recipe Working listener interrupting the recipe it is told about was told about it again by the
     * interrupt, without end, and the stack overflow crashed the server.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void interrupting_from_after_recipe_working_drops_the_recipe(GameTestHelper h) {
        RecipeLogicFixFixtures.listenForInterrupter();
        droppedAtTheFinish(h, RecipeLogicFixFixtures.INTERRUPT_AFTER_MACHINE);
    }

    /** A blueprint resetting the recipe from After Recipe Working left the finish without a recipe to finish. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void resetting_from_after_recipe_working_drops_the_recipe(GameTestHelper h) {
        droppedAtTheFinish(h, RecipeLogicFixFixtures.RESET_AFTER_MACHINE);
    }

    private static void droppedAtTheFinish(GameTestHelper h, ResourceLocation machine) {
        MBDScenario.of(h).placeMachine(machine, POS)
                .insertItem(0, new ItemStack(Items.STONE))
                .insertEnergy(100_000)
                .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                .runUntil(m -> logic(m).isIdle(), 40)
                .runTicks(10)
                .check("the stone was taken when it started", m -> count(h, m, 0, Items.STONE) == 0)
                .check("and the dropped recipe made nothing", m -> count(h, m, 2, Items.DIRT) == 0)
                .succeed();
    }

    /** An item input that always claims to hold what is asked when simulated, but really gives it only once. */
    private static final class OneShotLiar implements IRecipeHandler<SizedIngredient> {
        private int taken;

        @Override
        public @Nullable List<SizedIngredient> handleRecipeInner(
                IO io, MBDRecipe recipe, List<SizedIngredient> left,
                @Nullable String slotName, boolean simulate) {
            if (io != IO.IN) return left;
            if (simulate) return null;
            return ++taken == 1 ? null : left;
        }

        @Override
        public RecipeCapability<SizedIngredient> getRecipeCapability() {
            return ItemRecipeCapability.CAP;
        }
    }

    // endregion

    // region preWorking / postWorking pairing

    /** Suspending left the handlers working (no post); resuming went back to working without a pre. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void suspending_stops_the_handlers_and_resuming_restarts_them(GameTestHelper h) {
        var s = MBDScenario.of(h).placeMachine(MACHINE, POS);
        var probe = WorkingProbe.attach(s.machine().getRecipeLogic());
        s.insertItem(0, new ItemStack(Items.STONE))
                .insertEnergy(100_000)
                .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                .runTicks(2);
        check(h, probe.active() == 1, "a working recipe's handlers are not working: " + probe);
        s.with(m -> logic(m).setWorkingEnabled(false)).runTicks(1);
        check(h, probe.active() == 0, "suspending left the handlers working: " + probe);
        s.with(m -> logic(m).setWorkingEnabled(true)).runTicks(1);
        check(h, probe.active() == 1 && probe.pre == 2, "resuming did not restart the handlers: " + probe);
        s.runUntil(m -> logic(m).isIdle(), 40);
        check(h, probe.active() == 0 && probe.pre == 2 && probe.post == 2, "unbalanced after the finish: " + probe);
        h.succeed();
    }

    /**
     * The handlers told the recipe started hear it stop even once they are no longer the logic's — a trait's
     * recipe handler IO or recipe logics changed, a part left — rather than whichever handlers the logic has then.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void handlers_that_left_still_hear_the_recipe_stop(GameTestHelper h) {
        var s = MBDScenario.of(h).placeMachine(MACHINE, POS);
        var probe = WorkingProbe.attach(s.machine().getRecipeLogic());
        s.insertItem(0, new ItemStack(Items.STONE))
                .insertEnergy(100_000)
                .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                .runTicks(2);
        check(h, probe.active() == 1, "a working recipe's handlers are not working: " + probe);
        // rebuilt from the traits, the logic's handlers no longer include the probe
        s.with(MBDMachine::initCapabilitiesProxy);
        check(h, !s.machine().getRecipeLogic().getRecipeCapabilitiesProxy().get(IO.IN, ItemRecipeCapability.CAP).contains(probe),
                "the rebuild kept the probe");
        s.runUntil(m -> logic(m).isIdle(), 40);
        check(h, probe.active() == 0 && probe.post == 1, "the probe never heard the recipe stop: " + probe);
        h.succeed();
    }

    /** Waiting posted once, then interrupting posted again for the same pre. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void interrupting_a_waiting_recipe_posts_once(GameTestHelper h) {
        var s = MBDScenario.of(h).placeMachine(MACHINE, POS);
        var probe = WorkingProbe.attach(s.machine().getRecipeLogic());
        s.insertItem(0, new ItemStack(Items.STONE))
                .insertEnergy(50)
                .runUntil(m -> logic(m).isWaiting(), START_BUDGET);
        check(h, probe.active() == 0 && probe.pre == 1, "a waiting recipe's handlers are still working: " + probe);
        s.with(m -> logic(m).interruptRecipe());
        check(h, probe.pre == 1 && probe.post == 1, "interrupting a waiting recipe posted again: " + probe);
        h.succeed();
    }

    /** A machine reloaded mid-recipe went on working without telling its handlers again. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void a_reloaded_working_machine_restarts_its_handlers(GameTestHelper h) {
        var s = MBDScenario.of(h).placeMachine(MACHINE, POS);
        var before = WorkingProbe.attach(s.machine().getRecipeLogic());
        s.insertItem(0, new ItemStack(Items.STONE))
                .insertEnergy(100_000)
                .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                .runTicks(3);
        check(h, before.active() == 1, "the recipe's handlers never started: " + before);
        s.assertPersistenceRoundTrip();
        check(h, before.active() == 0 && before.post == 1, "unloading did not stop the handlers: " + before);
        var after = WorkingProbe.attach(s.machine().getRecipeLogic());
        s.runTicks(1);
        check(h, after.active() == 1 && logic(s.machine()).isWorking(), "the reloaded recipe's handlers never started: " + after);
        h.succeed();
    }

    /** Every cycle is its own pre/post pair, also when the next one re-runs the same recipe from the finish. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void each_cycle_is_its_own_working_pair(GameTestHelper h) {
        var s = MBDScenario.of(h).placeMachine(MACHINE, POS);
        var probe = WorkingProbe.attach(s.machine().getRecipeLogic());
        s.insertItem(0, new ItemStack(Items.STONE, 3))
                .insertEnergy(100_000)
                .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                .runUntil(m -> logic(m).isIdle(), 80)
                .check("three dirt are produced", m -> count(h, m, 2, Items.DIRT) == 3);
        check(h, probe.pre == 3 && probe.post == 3, "three cycles are not three pairs: " + probe);
        check(h, probe.holders.stream().allMatch(holder -> holder == s.machine()),
                "handlers were not handed the machine as their holder");
        h.succeed();
    }

    /**
     * Random operations on a working machine, the handlers checked after every one: started exactly while the
     * logic works, and never stopped more often than started.
     */
    @GameTest(template = "empty_simple", timeoutTicks = 400)
    @PrefixGameTestTemplate(false)
    public static void handlers_work_exactly_while_the_logic_works(GameTestHelper h) {
        for (int seed = 0; seed < 8; seed++) {
            var random = new Random(seed);
            // a fresh machine per seed: placing the same block again would keep the last seed's block entity
            h.setBlock(POS, Blocks.AIR);
            var s = MBDScenario.of(h).placeMachine(MACHINE, POS);
            var probe = WorkingProbe.attach(s.machine().getRecipeLogic());
            var log = new ArrayList<String>();
            for (int step = 0; step < 50; step++) {
                var machine = s.machine();
                int op = random.nextInt(10);
                switch (op) {
                    case 0 -> { s.insertItem(0, new ItemStack(Items.STONE, 1 + random.nextInt(2))); log.add("stone"); }
                    case 1 -> { int fe = random.nextInt(300); s.insertEnergy(fe); log.add("fe+" + fe); }
                    case 2 -> { drainEnergy(h, machine); log.add("drain"); }
                    case 3 -> { logic(machine).setWorkingEnabled(false); log.add("suspend"); }
                    case 4 -> { logic(machine).setWorkingEnabled(true); log.add("resume"); }
                    case 5 -> { logic(machine).interruptRecipe(); log.add("interrupt"); }
                    case 6 -> { logic(machine).resetRecipeLogic(); log.add("reset"); }
                    case 7 -> {
                        s.assertPersistenceRoundTrip();
                        check(h, probe.active() == 0, "seed " + seed + " " + log + " reload: unloading left handlers working " + probe);
                        probe = WorkingProbe.attach(s.machine().getRecipeLogic());
                        log.add("reload");
                    }
                    default -> { int t = 1 + random.nextInt(12); s.runTicks(t); log.add("tick" + t); }
                }
                check(h, probe.post <= probe.pre && probe.active() <= 1,
                        "seed " + seed + " " + log + ": handlers stopped more often than started " + probe);
                if (op >= 8) {
                    // after a tick the handlers must have caught up with the status
                    int expected = logic(s.machine()).isWorking() ? 1 : 0;
                    check(h, probe.active() == expected, "seed " + seed + " " + log + ": handlers "
                            + (probe.active() == 1 ? "working" : "idle") + " while the logic is " + logic(s.machine()).getStatus() + " " + probe);
                }
            }
        }
        h.succeed();
    }

    // endregion

    // region helpers

    private static MBDRecipe recipe(GameTestHelper h, String id) {
        return (MBDRecipe) h.getLevel().getRecipeManager().byKey(MBD2.id(id)).orElseThrow().value();
    }

    /** The input trait of {@link RecipeLogicGoldenFixtures#MACHINE}: its first item trait. */
    public static ItemSlotCapabilityTrait input(MBDMachine machine) {
        return (ItemSlotCapabilityTrait) machine.getTraitByName("item_slot");
    }

    /** The output trait of {@link RecipeLogicGoldenFixtures#MACHINE}: its second item trait. */
    public static ItemSlotCapabilityTrait output(MBDMachine machine) {
        return (ItemSlotCapabilityTrait) machine.getTraitByName("item_slot_1");
    }

    public static void fillOutputs(MBDMachine machine, Item item) {
        var storage = output(machine).storage;
        for (int slot = 0; slot < storage.getSlots(); slot++) {
            storage.setStackInSlot(slot, new ItemStack(item, 64));
        }
    }

    private static void drainEnergy(GameTestHelper h, MBDMachine machine) {
        var storage = MBDTestHelper.capability(h, machine, Capabilities.EnergyStorage.BLOCK);
        if (storage != null) storage.extractEnergy(Integer.MAX_VALUE, false);
    }

    private static void check(GameTestHelper h, boolean condition, String message) {
        if (!condition) {
            h.fail(message);
        }
    }

    // endregion
}
