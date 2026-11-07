package com.lowdragmc.mbd2.test.tests.recipe.logic;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandler;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.capability.recipe.FluidRecipeCapability;
import com.lowdragmc.mbd2.common.capability.recipe.ItemRecipeCapability;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.Trait;
import com.lowdragmc.mbd2.common.trait.fluid.FluidTankCapabilityTrait;
import com.lowdragmc.mbd2.common.trait.item.ItemSlotCapabilityTrait;
import com.lowdragmc.mbd2.config.ConfigHolder;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.lowdragmc.mbd2.test.tests.recipe.logic.RecipeLogicGoldenFixtures.*;
import static com.lowdragmc.mbd2.test.tests.recipe.logic.RecipeLogicGoldenTests.*;

/**
 * The recipe index and the search that uses it: it must find exactly what a full scan finds, and an idle logic must
 * search only when something it uses changed — while still noticing conditions coming true, outputs freeing up and
 * recipes reloading.
 */
@GameTestHolder(MBD2.MOD_ID)
public class RecipeLookupTests {
    static { @SuppressWarnings("unused") var ignored = RecipeLookupFixtures.DIFF_MACHINE; }

    // region index vs full scan

    /**
     * 400 random machines against 150 generated recipes of every input kind: the recipes the index lets through and
     * that then match, in order, are exactly the content-matching recipes a full scan returns.
     */
    @GameTest(template = "empty_simple", timeoutTicks = 400)
    @PrefixGameTestTemplate(false)
    public static void the_index_finds_exactly_what_a_full_scan_finds(GameTestHelper h) {
        var machine = MBDScenario.of(h).placeMachine(RecipeLookupFixtures.DIFF_MACHINE, POS).machine();
        var logic = machine.getRecipeLogic();
        var type = (MBDRecipeType) MBDRegistries.RECIPE_TYPES.get(RecipeLookupFixtures.DIFF_TYPE);
        var manager = h.getLevel().getRecipeManager();
        var inputs = (ItemSlotCapabilityTrait) machine.getTraitByName("item_slot");
        var outputs = (ItemSlotCapabilityTrait) machine.getTraitByName("item_slot_1");
        var tanks = (FluidTankCapabilityTrait) machine.getTraitByName("fluid_tank");
        var energy = MBDTestHelper.capability(h, machine, Capabilities.EnergyStorage.BLOCK);
        int cases = 400;
        int pruned = 0;
        int matching = 0;
        for (int seed = 0; seed < cases; seed++) {
            var random = new Random(seed);
            for (int slot = 0; slot < 6; slot++) {
                inputs.storage.setStackInSlot(slot, random.nextInt(3) == 0 ? ItemStack.EMPTY
                        : new ItemStack(RecipeLookupFixtures.POOL.get(random.nextInt(RecipeLookupFixtures.POOL.size())), 1 + random.nextInt(4)));
            }
            boolean full = random.nextInt(5) == 0;
            for (int slot = 0; slot < 4; slot++) {
                outputs.storage.setStackInSlot(slot, full ? new ItemStack(Items.BEDROCK, 64) : ItemStack.EMPTY);
            }
            tanks.storages[0].setFluid(switch (random.nextInt(3)) {
                case 0 -> FluidStack.EMPTY;
                case 1 -> new FluidStack(Fluids.WATER, 100 * random.nextInt(8));
                default -> new FluidStack(Fluids.LAVA, 100 * random.nextInt(4));
            });
            tanks.storages[1].setFluid(random.nextBoolean() ? FluidStack.EMPTY : new FluidStack(Fluids.WATER, 300));
            energy.extractEnergy(Integer.MAX_VALUE, false);
            if (random.nextBoolean()) energy.receiveEnergy(50_000, false);

            var scanned = type.searchRecipe(manager, logic).stream().map(RecipeHolder::id).toList();
            var lookup = type.getLookup(manager);
            var candidates = lookup.candidates(logic.availableLookupKeys(lookup));
            var indexed = candidates.stream()
                    .filter(r -> r.value().matchRecipe(logic).isSuccess() && r.value().matchTickRecipe(logic).isSuccess())
                    .map(RecipeHolder::id)
                    .toList();
            check(h, scanned.equals(indexed), "seed " + seed + ": a full scan finds " + scanned + " but the index " + indexed);
            if (candidates.size() < lookup.getSorted().size()) pruned++;
            if (!scanned.isEmpty()) matching++;
        }
        MBD2.LOGGER.info("[lookup diff] {} cases, {} pruned by the index, {} matching at least one recipe", cases, pruned, matching);
        check(h, pruned >= cases * 9 / 10, "the index rarely ruled anything out: " + pruned + "/" + cases);
        check(h, matching >= cases / 20, "too few cases matched anything to compare: " + matching + "/" + cases);
        h.succeed();
    }

    /** Keys held, keys unknown, nothing held: what the index lets through for each. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void the_index_rules_out_only_what_cannot_match(GameTestHelper h) throws ReflectiveOperationException {
        var type = (MBDRecipeType) MBDRegistries.RECIPE_TYPES.get(RECIPE_TYPE);
        var lookup = type.getLookup(h.getLevel().getRecipeManager());
        var item = ItemRecipeCapability.CAP;
        check(h, ids(lookup.candidates(Map.of(item, Set.of(Items.STONE)))).equals(List.of(PRESS)),
                "holding stone should leave only the press: " + ids(lookup.candidates(Map.of(item, Set.of(Items.STONE)))));
        var unknown = new HashMap<RecipeCapability<?>, Collection<?>>();
        unknown.put(item, null);
        check(h, lookup.candidates(unknown).size() == lookup.getSorted().size(), "unknown inputs ruled a recipe out");
        check(h, lookup.candidates(Map.of(item, Set.of())).isEmpty(), "holding nothing let a recipe with item inputs through");
        check(h, lookup.candidates(Map.of()).isEmpty(), "a capability with no inputs at all let a recipe through");
        check(h, lookup == type.getLookup(h.getLevel().getRecipeManager()), "the lookup was rebuilt for no reason");
        MBDRecipeType.invalidateLookups();
        var held = MBDRecipeType.class.getDeclaredField("lookup");
        held.setAccessible(true);
        check(h, held.get(type) == null, "invalidating kept the stale lookup, and the recipe manager it holds");
        check(h, lookup != type.getLookup(h.getLevel().getRecipeManager()), "invalidating did not rebuild the lookup");
        h.succeed();
    }

    // endregion

    // region searching only on change

    /** An idle machine whose inputs do not change searches once, then only at the fallback poll; a change starts it. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void an_idle_logic_searches_only_when_something_changes(GameTestHelper h) {
        var searches = new int[1];
        MBDScenario.of(h).placeMachine(MACHINE, POS)
                .insertItem(0, new ItemStack(Items.APPLE))
                .insertEnergy(100_000)
                .runTicks(10)
                .with(m -> searches[0] = logic(m).getSearchCount())
                .check("it searched", m -> searches[0] >= 1)
                .runTicks(90)
                .check("and not again while nothing changed", m -> logic(m).getSearchCount() == searches[0])
                .runTicks(20)
                .check("but once more at the fallback poll", m -> logic(m).getSearchCount() == searches[0] + 1)
                .insertItem(1, new ItemStack(Items.STONE))
                .runUntil(m -> logic(m).isWorking(), 6)
                .check("the new input was searched for at the next poll", m -> logic(m).getSearchCount() == searches[0] + 2)
                .succeed();
    }

    /** A handler that cannot report its changes keeps the logic polling every 5 ticks, as before. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void an_untracked_handler_keeps_the_logic_polling(GameTestHelper h) {
        var searches = new int[1];
        MBDScenario.of(h).placeMachine(MACHINE, POS)
                .with(m -> m.getRecipeCapabilitiesProxy().get(IO.IN, ItemRecipeCapability.CAP).add(new UntrackedItemInput(Set.of())))
                .insertItem(0, new ItemStack(Items.APPLE))
                .runTicks(10)
                .with(m -> searches[0] = logic(m).getSearchCount())
                .runTicks(50)
                .check("it searched at every poll", m -> logic(m).getSearchCount() >= searches[0] + 9)
                .succeed();
    }

    /**
     * Polling with an input the index cannot read means matching every recipe at every poll: when async searching
     * is on, that runs off the server thread, as every search did before the index.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void a_polling_logic_blind_to_an_input_matches_off_the_server_thread(GameTestHelper h) {
        var blind = new UntrackedItemInput(null);
        MBDScenario.of(h).placeMachine(MACHINE, POS)
                .with(m -> m.getRecipeCapabilitiesProxy().get(IO.IN, ItemRecipeCapability.CAP).add(0, blind))
                .insertItem(0, new ItemStack(Items.STONE))
                .insertEnergy(100_000)
                .runUntil(m -> logic(m).isWorking(), START_BUDGET)
                .check("matched off the server thread exactly when async searching is on",
                        m -> blind.matchedOffThread == ConfigHolder.ASYNC_RECIPE_SEARCHING.get())
                .succeed();
    }

    /** A recipe whose condition comes true starts at the next poll, by re-checking it, not by searching. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void a_condition_coming_true_starts_the_recipe_without_a_search(GameTestHelper h) {
        var searches = new int[1];
        MBDScenario.of(h).placeMachine(RecipeLogicFixFixtures.POWERED_MACHINE, POS)
                .insertItem(0, new ItemStack(Items.STONE))
                .runTicks(20)
                .check("the recipe waits for a signal", m -> logic(m).isIdle())
                .with(m -> searches[0] = logic(m).getSearchCount())
                .with(m -> h.setBlock(STONE_POS, Blocks.REDSTONE_BLOCK))
                .runUntil(m -> logic(m).isWorking(), 6)
                .check("no search was needed", m -> logic(m).getSearchCount() == searches[0])
                .succeed();
    }

    /** Room freed in the outputs is a change too: a recipe blocked on output space starts. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void freeing_output_space_lets_the_recipe_start(GameTestHelper h) {
        MBDScenario.of(h).placeMachine(MACHINE, POS)
                .with(m -> RecipeLogicFixTests.fillOutputs(m, Items.GRAVEL))
                .insertItem(0, new ItemStack(Items.STONE))
                .insertEnergy(100_000)
                .runTicks(30)
                .check("no room, no start", m -> logic(m).isIdle())
                .with(m -> RecipeLogicFixTests.output(m).storage.setStackInSlot(0, ItemStack.EMPTY))
                .runUntil(m -> logic(m).isWorking(), 6)
                .succeed();
    }

    /** After a recipe reload every idle logic searches again: its last search was against the old recipes. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void a_recipe_reload_makes_idle_logics_search_again(GameTestHelper h) {
        var searches = new int[1];
        MBDScenario.of(h).placeMachine(MACHINE, POS)
                .insertItem(0, new ItemStack(Items.APPLE))
                .runTicks(10)
                .with(m -> searches[0] = logic(m).getSearchCount())
                .with(m -> MBDRecipeType.invalidateLookups())
                .runTicks(6)
                .check("it searched against the reloaded recipes", m -> logic(m).getSearchCount() == searches[0] + 1)
                .succeed();
    }

    /** A runtime value can decide whether a recipe fits, a slot limit or a filter say, so changing one is a change. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void a_runtime_value_change_makes_idle_logics_search_again(GameTestHelper h) {
        var searches = new int[1];
        MBDScenario.of(h).placeMachine(MACHINE, POS)
                .insertItem(0, new ItemStack(Items.APPLE))
                .runTicks(10)
                .with(m -> searches[0] = logic(m).getSearchCount())
                .with(m -> m.getRuntimeValues().set("recipe_logic.damping", 5))
                .runTicks(6)
                .check("it searched again", m -> logic(m).getSearchCount() == searches[0] + 1)
                .succeed();
    }

    /** A suspended logic does not search at all. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void a_suspended_logic_does_not_search(GameTestHelper h) {
        var searches = new int[1];
        MBDScenario.of(h).placeMachine(MACHINE, POS)
                .with(m -> logic(m).setWorkingEnabled(false))
                .with(m -> searches[0] = logic(m).getSearchCount())
                .insertItem(0, new ItemStack(Items.STONE))
                .insertEnergy(100_000)
                .runTicks(40)
                .check("no searches while suspended", m -> logic(m).getSearchCount() == searches[0])
                .succeed();
    }

    /** Listening to the handlers again after their table changes does not leave the old listeners behind. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void listeners_do_not_pile_up(GameTestHelper h) throws ReflectiveOperationException {
        var s = MBDScenario.of(h).placeMachine(MACHINE, POS).runTicks(6);
        var machine = s.machine();
        var input = (Trait) machine.getTraitByName("item_slot");
        var field = Trait.class.getDeclaredField("listeners");
        field.setAccessible(true);
        int before = ((List<?>) field.get(input)).size();
        var extra = new UntrackedItemInput(Set.of());
        for (int i = 0; i < 30; i++) {
            var handlers = machine.getRecipeCapabilitiesProxy().get(IO.IN, ItemRecipeCapability.CAP);
            if (i % 2 == 0) handlers.add(extra); else handlers.remove(extra);
            s.runTicks(5);
        }
        int after = ((List<?>) field.get(input)).size();
        check(h, after == before, "listeners on the input went from " + before + " to " + after);
        h.succeed();
    }

    /** An addon logic overriding searchRecipe() keeps the search it had, and still runs recipes. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void an_overridden_search_is_still_used(GameTestHelper h) {
        MBDScenario.of(h).placeMachine(RecipeLookupFixtures.LEGACY_SEARCH_MACHINE, POS)
                .insertItem(0, new ItemStack(Items.STONE))
                .insertEnergy(100_000)
                .runUntil(m -> count(h, m, 2, Items.DIRT) == 1, START_BUDGET + 40)
                .check("its own search ran", m -> ((RecipeLookupFixtures.LegacySearchLogic) logic(m)).legacySearches > 0)
                .check("the index search did not", m -> logic(m).getSearchCount() == 0)
                .succeed();
    }

    /** An addon recipe type overriding searchRecipe() and searchFuelRecipe() is still asked, as before the index. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void a_recipe_type_searching_its_own_way_is_still_asked(GameTestHelper h) {
        var type = RecipeLookupFixtures.OWN_SEARCH;
        int searches = type.searches.get();
        int fuelSearches = type.fuelSearches.get();
        int notHandedTheMachine = type.notHandedTheMachine.get();
        MBDScenario.of(h).placeMachine(RecipeLookupFixtures.OWN_SEARCH_MACHINE, POS)
                .insertItem(0, new ItemStack(Items.SAND))
                .insertItem(1, new ItemStack(Items.COAL))
                .runUntil(m -> count(h, m, 2, Items.GLASS) == 1, START_BUDGET + 40)
                .check("its recipe search was asked", m -> type.searches.get() > searches)
                .check("and its fuel search", m -> type.fuelSearches.get() > fuelSearches)
                .check("the index search was not", m -> logic(m).getSearchCount() == 0)
                .check("both were handed the machine, as they always were",
                        m -> type.notHandedTheMachine.get() == notHandedTheMachine)
                .succeed();
    }

    /** A legacy search still running when the machine unloads is cancelled and let go. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void unloading_drops_a_search_in_flight(GameTestHelper h) {
        if (!ConfigHolder.ASYNC_RECIPE_SEARCHING.get()) {
            h.succeed();
            return;
        }
        var running = new CompletableFuture<?>[1];
        MBDScenario.of(h).placeMachine(RecipeLookupFixtures.LEGACY_SEARCH_MACHINE, POS)
                .runUntil(m -> (running[0] = searchInFlight(logic(m))) != null, 12)
                .with(MBDMachine::onUnload)
                .check("the search is let go", m -> searchInFlight(logic(m)) == null)
                .check("and cancelled, unless it was already done", m -> running[0].isCancelled() || running[0].isDone())
                .with(MBDMachine::onLoad)
                .succeed();
    }

    /**
     * A fluid tag ingredient is keyed by the tag as it is now. Its getStacks() is cached for the ingredient's life,
     * which for a built-in recipe outlasts a tag reload, while matching tests the tag live: keys from the cache hid
     * recipes a reload had made runnable.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void fluid_tag_keys_follow_the_live_tag(GameTestHelper h) throws ReflectiveOperationException {
        var sized = SizedFluidIngredient.of(FluidTags.WATER, 100);
        // a cache filled before the tag changed
        var cache = FluidIngredient.class.getDeclaredField("stacks");
        cache.setAccessible(true);
        cache.set(sized.ingredient(), new FluidStack[]{new FluidStack(Fluids.LAVA, 1000)});
        var keys = FluidRecipeCapability.CAP.getLookupKeys(sized);
        check(h, keys != null && keys.contains(Fluids.WATER) && keys.contains(Fluids.FLOWING_WATER) && !keys.contains(Fluids.LAVA),
                "keyed by the ingredient's cache, not the tag: " + keys);
        h.succeed();
    }

    /** Energy cables simulate transfers all the time; a simulation changes nothing and must not wake an idle logic. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void simulated_energy_transfers_do_not_wake_an_idle_logic(GameTestHelper h) {
        var searches = new int[1];
        var s = MBDScenario.of(h).placeMachine(MACHINE, POS)
                .insertItem(0, new ItemStack(Items.APPLE))
                .insertEnergy(1_000)
                .runTicks(10)
                .with(m -> searches[0] = logic(m).getSearchCount())
                .check("it searched", m -> searches[0] >= 1);
        for (int tick = 0; tick < 40; tick++) {
            s.with(m -> {
                var energy = MBDTestHelper.capability(h, m, Capabilities.EnergyStorage.BLOCK);
                check(h, energy != null && energy.receiveEnergy(100, true) > 0, "the buffer takes no energy");
                energy.extractEnergy(100, true);
            }).runTicks(1);
        }
        s.check("no search for what was only simulated", m -> logic(m).getSearchCount() == searches[0])
                .succeed();
    }

    // endregion

    // region helpers

    private static List<String> ids(List<RecipeHolder<MBDRecipe>> recipes) {
        return recipes.stream().map(r -> r.id().getPath()).toList();
    }

    private static void check(GameTestHelper h, boolean condition, String message) {
        if (!condition) {
            h.fail(message);
        }
    }

    @Nullable
    private static CompletableFuture<?> searchInFlight(RecipeLogic logic) {
        try {
            var field = RecipeLogic.class.getDeclaredField("completableFuture");
            field.setAccessible(true);
            return (CompletableFuture<?>) field.get(logic);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * An item input that handles nothing and is no trait, so it cannot report changes, holding {@code keys}, or
     * unknown ones for {@code null}. Notes whether it was ever matched off the server thread.
     */
    private static final class UntrackedItemInput implements IRecipeHandler<SizedIngredient> {
        @Nullable
        private final Collection<?> keys;
        private volatile boolean matchedOffThread;

        private UntrackedItemInput(@Nullable Collection<?> keys) {
            this.keys = keys;
        }

        @Override
        public @Nullable List<SizedIngredient> handleRecipeInner(IO io, MBDRecipe recipe, List<SizedIngredient> left,
                                                                 @Nullable String slotName, boolean simulate) {
            if (!ServerLifecycleHooks.getCurrentServer().isSameThread()) {
                matchedOffThread = true;
            }
            return left;
        }

        @Override
        public @Nullable Collection<?> getAvailableLookupKeys() {
            return keys;
        }

        @Override
        public RecipeCapability<SizedIngredient> getRecipeCapability() {
            return ItemRecipeCapability.CAP;
        }
    }

    // endregion
}
