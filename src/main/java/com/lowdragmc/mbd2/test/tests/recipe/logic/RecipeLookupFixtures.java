package com.lowdragmc.mbd2.test.tests.recipe.logic;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeCapabilityHolder;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeBuilder;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.capability.recipe.ForgeEnergyRecipeCapability;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.machine.MBDPartMachine;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import com.lowdragmc.mbd2.test.framework.TestRecipeTypeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixtures for {@link RecipeLookupTests}: a recipe type of generated recipes using every kind of input the index
 * treats differently, a machine with item, fluid and energy inputs to search it, and a machine whose recipe logic
 * overrides {@code searchRecipe()} the way an addon might.
 */
public class RecipeLookupFixtures implements TestFixtureProvider {
    public static final ResourceLocation DIFF_TYPE = MBD2.id("lookup_diff_recipes");
    /** Item inputs 0-5, fluid inputs (two tanks), item outputs 6-9, 100k FE. */
    public static final ResourceLocation DIFF_MACHINE = MBD2.id("lookup_diff_machine");
    /** {@link RecipeLogicGoldenFixtures#MACHINE} whose recipe logic overrides {@code searchRecipe()}. */
    public static final ResourceLocation LEGACY_SEARCH_MACHINE = MBD2.id("lookup_legacy_search_machine");
    /** An {@link OwnSearchRecipeType}: sand to glass, burning the golden coal. */
    public static final ResourceLocation OWN_SEARCH_TYPE = MBD2.id("lookup_own_search_recipes");
    /** 2 input slots (0, 1), 2 output slots, runs {@link #OWN_SEARCH_TYPE}. */
    public static final ResourceLocation OWN_SEARCH_MACHINE = MBD2.id("lookup_own_search_machine");
    /** The registered {@link #OWN_SEARCH_TYPE}, made with the recipe types. */
    public static OwnSearchRecipeType OWN_SEARCH;
    public static final int RECIPES = 150;

    /** The items the recipes and the machine contents are drawn from. */
    public static final List<Item> POOL = List.of(Items.STONE, Items.DIRT, Items.COBBLESTONE, Items.SAND, Items.GRAVEL,
            Items.APPLE, Items.BREAD, Items.IRON_INGOT, Items.GOLD_INGOT, Items.COPPER_INGOT, Items.REDSTONE, Items.COAL,
            Items.OAK_LOG, Items.BIRCH_LOG, Items.WHITE_WOOL, Items.RED_WOOL, Items.OAK_PLANKS, Items.SPRUCE_PLANKS);

    @Override
    public void registerRecipeTypes(MBDRegistryEvent.MBDRecipeType event) {
        var random = new Random(42);
        var builder = TestRecipeTypeBuilder.of(DIFF_TYPE);
        for (int i = 0; i < RECIPES; i++) {
            long seed = random.nextLong();
            builder.recipe("lookup_" + i, b -> generate(b, new Random(seed)));
        }
        builder.register(event);
        OWN_SEARCH = new OwnSearchRecipeType(OWN_SEARCH_TYPE);
        TestRecipeTypeBuilder.of(OWN_SEARCH)
                .configure(type -> {
                    type.getFuelRecipeConfig().setEnable(true);
                    type.getFuelRecipeConfig().getFuelRecipeTypes().add(RecipeLogicGoldenFixtures.FUEL_TYPE);
                })
                .recipe("lookup_own_search_smelt", b -> b
                        .inputItems(Items.SAND)
                        .outputItems(Items.GLASS)
                        .duration(20))
                .register(event);
    }

    /** One to three inputs of mixed kinds, maybe a fluid and per-tick energy, one output, a random priority. */
    private static void generate(MBDRecipeBuilder b, Random random) {
        int inputs = 1 + random.nextInt(3);
        for (int i = 0; i < inputs; i++) {
            int kind = random.nextInt(20);
            if (kind < 11) {
                b.inputItems(pick(random), 1 + random.nextInt(3));
            } else if (kind < 14) {
                var tag = switch (random.nextInt(3)) {
                    case 0 -> ItemTags.LOGS;
                    case 1 -> ItemTags.WOOL;
                    default -> ItemTags.PLANKS;
                };
                b.inputItems(tag, 1 + random.nextInt(2));
            } else if (kind < 16) {
                // a custom ingredient: the index may not use it to rule anything out
                b.inputItems(CompoundIngredient.of(Ingredient.of(pick(random)), Ingredient.of(pick(random))));
            } else if (kind < 18) {
                b.chance(0.5f).inputItems(pick(random)).chance(1);
            } else {
                // not consumed, but still needed to be there
                b.chance(0).inputItems(pick(random)).chance(1);
            }
        }
        int fluid = random.nextInt(10);
        if (fluid < 2) {
            b.inputFluids(new FluidStack(Fluids.WATER, 100 * (1 + random.nextInt(5))));
        } else if (fluid < 3) {
            b.inputFluids(new FluidStack(Fluids.LAVA, 250));
        } else if (fluid < 4) {
            b.inputFluids(SizedFluidIngredient.of(FluidTags.WATER, 200));
        }
        if (random.nextInt(3) == 0) {
            b.perTick(true).input(ForgeEnergyRecipeCapability.CAP, 5).perTick(false);
        }
        b.outputItems(pick(random)).priority(random.nextInt(6)).duration(20);
    }

    private static Item pick(Random random) {
        return POOL.get(random.nextInt(POOL.size()));
    }

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        TestMachineBuilder.simple(DIFF_MACHINE)
                .withItemSlots(6, IO.IN)
                .withFluidTanks(2, 1_000, def -> def.setRecipeHandlerIO(IO.IN))
                .withItemSlots(4, IO.OUT)
                .withEnergy(100_000)
                .withRecipeType(DIFF_TYPE)
                .register(event);
        TestMachineBuilder.simple(LEGACY_SEARCH_MACHINE)
                .withItemSlots(2, IO.IN)
                .withItemSlots(2, IO.OUT)
                .withEnergy(100_000)
                .withRecipeType(RecipeLogicGoldenFixtures.RECIPE_TYPE)
                .withMachineFactory(LegacySearchMachine::new)
                .register(event);
        TestMachineBuilder.simple(OWN_SEARCH_MACHINE)
                .withItemSlots(2, IO.IN)
                .withItemSlots(2, IO.OUT)
                .withRecipeType(OWN_SEARCH_TYPE)
                .register(event);
    }

    /** A machine whose recipe logic overrides {@code searchRecipe()}, as an addon written before the index might. */
    public static class LegacySearchMachine extends MBDPartMachine {
        public LegacySearchMachine(IMachineBlockEntity holder, MBDMachineDefinition definition) {
            super(holder, definition);
        }

        @Override
        protected RecipeLogic createRecipeLogic(Object... args) {
            return new LegacySearchLogic(this);
        }
    }

    public static class LegacySearchLogic extends RecipeLogic {
        public int legacySearches;

        public LegacySearchLogic(IMachine machine) {
            super(machine);
        }

        @Override
        protected List<RecipeHolder<MBDRecipe>> searchRecipe() {
            legacySearches++;
            return super.searchRecipe();
        }
    }

    /**
     * A recipe type of an addon that searches its recipes, and its fuel, its own way: here, counting, and counting
     * the holders that were not the machine, as such a type has always been handed.
     */
    public static class OwnSearchRecipeType extends MBDRecipeType {
        public final AtomicInteger searches = new AtomicInteger();
        public final AtomicInteger fuelSearches = new AtomicInteger();
        public final AtomicInteger notHandedTheMachine = new AtomicInteger();

        public OwnSearchRecipeType(ResourceLocation id) {
            super(id);
        }

        @Override
        public List<RecipeHolder<MBDRecipe>> searchRecipe(RecipeManager recipeManager, IRecipeCapabilityHolder holder) {
            searches.incrementAndGet();
            if (!(holder instanceof IMachine)) notHandedTheMachine.incrementAndGet();
            return super.searchRecipe(recipeManager, holder);
        }

        @Override
        public List<RecipeHolder<MBDRecipe>> searchFuelRecipe(RecipeManager recipeManager, IRecipeCapabilityHolder holder) {
            fuelSearches.incrementAndGet();
            if (!(holder instanceof IMachine)) notHandedTheMachine.incrementAndGet();
            return super.searchFuelRecipe(recipeManager, holder);
        }
    }
}
