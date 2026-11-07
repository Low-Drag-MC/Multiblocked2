package com.lowdragmc.mbd2.test.tests.bench;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.capability.recipe.ForgeEnergyRecipeCapability;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import com.lowdragmc.mbd2.test.framework.TestRecipeTypeBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The workload {@link RecipeLogicBench} measures: one recipe type with {@link #RECIPES} generated two-ingredient
 * recipes over a fixed pool of vanilla items, and a 9-in / 9-out machine running it.
 *
 * <p>Deterministic by construction — the item pool is the vanilla registry sorted by id, and recipe {@code i}'s
 * ingredients are a fixed function of {@code i} — so two builds benchmark exactly the same recipes.</p>
 */
public class RecipeLogicBenchFixtures implements TestFixtureProvider {
    public static final ResourceLocation RECIPE_TYPE = MBD2.id("bench_recipes");
    public static final ResourceLocation MACHINE = MBD2.id("bench_machine");
    public static final int RECIPES = RecipeLogicBench.intSetting("MBD2_BENCH_RECIPES", 1000);
    public static final int POOL_SIZE = 256;
    public static final int DURATION = 20;
    public static final int ENERGY_PER_TICK = 5;
    public static final int IN_SLOTS = 9;
    public static final int OUT_SLOTS = 9;

    /** One generated recipe: two distinct ingredients, one product. */
    public record BenchRecipe(int index, Item a, Item b, Item out) {}

    /** The items recipes are built from. */
    public static final List<Item> POOL = new ArrayList<>();
    /** Vanilla items no recipe uses, for machines that must never match. */
    public static final List<Item> NON_POOL = new ArrayList<>();
    public static final List<BenchRecipe> BENCH_RECIPES = new ArrayList<>();

    @Override
    public void registerRecipeTypes(MBDRegistryEvent.MBDRecipeType event) {
        var vanilla = BuiltInRegistries.ITEM.stream()
                .filter(item -> item != Items.AIR)
                .filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("minecraft"))
                .filter(item -> item.getDefaultMaxStackSize() == 64)
                .sorted(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()))
                .toList();
        POOL.clear();
        NON_POOL.clear();
        BENCH_RECIPES.clear();
        POOL.addAll(vanilla.subList(0, POOL_SIZE));
        NON_POOL.addAll(vanilla.subList(POOL_SIZE, vanilla.size()));

        var builder = TestRecipeTypeBuilder.of(RECIPE_TYPE);
        for (int i = 0; i < RECIPES; i++) {
            var a = POOL.get((i * 7) % POOL_SIZE);
            var b = POOL.get((i * 13 + 5) % POOL_SIZE);
            if (a == b) b = POOL.get((i * 13 + 6) % POOL_SIZE);
            var out = POOL.get((i * 3 + 1) % POOL_SIZE);
            var recipe = new BenchRecipe(i, a, b, out);
            BENCH_RECIPES.add(recipe);
            int priority = i % 7;
            builder.recipe("bench_" + i, r -> r
                    .inputItems(recipe.a())
                    .inputItems(recipe.b())
                    .perTick(true).input(ForgeEnergyRecipeCapability.CAP, ENERGY_PER_TICK).perTick(false)
                    .outputItems(recipe.out())
                    .priority(priority)
                    .duration(DURATION));
        }
        builder.register(event);
    }

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        TestMachineBuilder.simple(MACHINE)
                .withItemSlots(IN_SLOTS, IO.IN)
                .withItemSlots(OUT_SLOTS, IO.OUT)
                .withEnergy(100_000_000)
                .withRecipeType(RECIPE_TYPE)
                .withMachineFactory(BenchMachine::new)
                .register(event);
    }
}
