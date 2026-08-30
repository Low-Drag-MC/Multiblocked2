package com.lowdragmc.mbd2.test.tests.blueprint;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.TypeConstant;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.pattern.FactoryBlockPattern;
import com.lowdragmc.mbd2.api.pattern.Predicates;
import com.lowdragmc.mbd2.common.blueprint.builtin.BuiltinBlueprints;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.machine.definition.config.blueprint.MachineBlueprintBinding;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import com.lowdragmc.mbd2.test.framework.TestRecipeTypeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

/**
 * Machines bound to the shipped built-in blueprints, for the tests that ask whether they <em>work</em>.
 *
 * <h2>Bound by path, not inlined</h2>
 * Every other blueprint fixture builds its graph in the fixture and inlines it. These reference
 * {@code built-in(mbd2:...)} instead, because the thing under test is the blueprint MBD2 ships — an
 * inlined copy would be a second graph that could drift from it, and a test that passes against a copy
 * of the code it is testing proves nothing.
 *
 * <p>That also puts the whole delivery chain under test: the builtin provider resolving the path, the
 * stored tag deserializing, and the parameter overrides seeding the executor's variable store.</p>
 *
 * <h2>Parameters are overridden, not defaulted</h2>
 * Where a test can pin a parameter to a value that makes the outcome deterministic — {@code chance = 1},
 * a tier that divides evenly — it does. A test that relies on a default would still pass if the
 * override mechanism were broken, which is half of what a parameterised blueprint is.
 */
public class BuiltinBlueprintBehaviourFixtures implements TestFixtureProvider {

    public static final ResourceLocation RECIPE_TYPE_ID = MBD2.id("builtin_bp_recipe_type");
    public static final ResourceLocation STONE_TO_DIRT = MBD2.id("builtin_bp_stone_to_dirt");
    /** The recipe's unmodified duration, in ticks. Divides evenly by the overclock factors below. */
    public static final int DURATION = 20;

    /** {@code redstone_control} at its defaults: runs until powered. */
    public static final ResourceLocation REDSTONE_DEFAULT_ID = MBD2.id("builtin_bp_redstone_default");
    /** The same blueprint with {@code requiresSignal} overridden: runs only while powered. */
    public static final ResourceLocation REDSTONE_REQUIRES_ID = MBD2.id("builtin_bp_redstone_requires");

    /** {@code comparator_progress} at its defaults. */
    public static final ResourceLocation COMPARATOR_ID = MBD2.id("builtin_bp_comparator");
    /** The same blueprint with {@code invert} overridden. */
    public static final ResourceLocation COMPARATOR_INVERTED_ID = MBD2.id("builtin_bp_comparator_inverted");

    /** {@code overclock} on a tier-{@link #OVERCLOCK_TIER} machine. */
    public static final ResourceLocation OVERCLOCK_ID = MBD2.id("builtin_bp_overclock");
    /** The same machine and tier with no blueprint — the control the overclocked duration is read against. */
    public static final ResourceLocation OVERCLOCK_CONTROL_ID = MBD2.id("builtin_bp_overclock_control");
    public static final int OVERCLOCK_TIER = 2;
    /** {@code speedPerTier}, chosen so {@link #DURATION} divides by {@code 2^2} without rounding. */
    public static final float SPEED_PER_TIER = 2f;
    /** The duration the overclocked machine must end up with: 20 / (2^2). */
    public static final int OVERCLOCKED_DURATION = 5;

    /** {@code chance_output} with the roll pinned to always win. */
    public static final ResourceLocation CHANCE_ALWAYS_ID = MBD2.id("builtin_bp_chance_always");
    /** The same, pinned to never win — the control that proves the roll is read at all. */
    public static final ResourceLocation CHANCE_NEVER_ID = MBD2.id("builtin_bp_chance_never");
    /** The bonus item, distinct from the recipe's own output so the two cannot be confused. */
    public static final net.minecraft.world.item.Item BONUS_ITEM = Items.GOLD_NUGGET;

    /** {@code part_count_bonus} on a controller whose pattern has exactly {@link #PART_COUNT} parts. */
    public static final ResourceLocation PART_BONUS_CONTROLLER_ID = MBD2.id("builtin_bp_part_controller");
    public static final ResourceLocation PART_BONUS_PART_ID = MBD2.id("builtin_bp_part");
    /** Parts in the formed structure. The pattern below has two 'P' cells. */
    public static final int PART_COUNT = 2;
    /** {@code speedPerPart}: two parts give a factor of 1 + 2*0.5 = 2, so 20 ticks become 10. */
    public static final float SPEED_PER_PART = 0.5f;
    public static final int PART_BONUS_DURATION = 10;

    /** {@code debug_probe} with its probe item overridden away from the default stick. */
    public static final ResourceLocation DEBUG_PROBE_ID = MBD2.id("builtin_bp_debug_probe");
    /**
     * What the probe machine answers to. Deliberately <em>not</em> the blueprint's default, so a test
     * holding it proves the override was read rather than that the default happened to match.
     */
    public static final net.minecraft.world.item.Item PROBE_ITEM = Items.DIAMOND;

    /** {@code output_swap} pointed at an item the recipe never mentions. */
    public static final ResourceLocation OUTPUT_SWAP_ID = MBD2.id("builtin_bp_output_swap");
    /**
     * What the swapped machine makes instead of dirt. Deliberately unrelated to the recipe, so seeing
     * it proves the output came from the blueprint and not from the recipe under some other name.
     */
    public static final net.minecraft.world.item.Item SWAPPED_PRODUCT = Items.DIAMOND;

    /** {@code environment_gate} at its defaults: refuses to start while it is raining. */
    public static final ResourceLocation ENV_GATE_ID = MBD2.id("builtin_bp_env_gate");

    /** {@code upkeep} on a machine with a coolant tank. */
    public static final ResourceLocation UPKEEP_ID = MBD2.id("builtin_bp_upkeep");
    /** Drained per working tick. Large enough that {@link #UPKEEP_TANK} runs dry inside a test. */
    public static final int UPKEEP_PER_TICK = 100;
    public static final int UPKEEP_TANK = 2_000;

    /** {@code heat_buildup}, with cooling fast enough to observe inside a test. */
    public static final ResourceLocation HEAT_ID = MBD2.id("builtin_bp_heat");
    /**
     * The same blueprint with cooling switched off, for the half that asks what a given heat is worth.
     *
     * <p>Needed because heat is a moving target: on the cooling fixture, heat planted before the recipe
     * search has already drained by the time a recipe starts, and the duration lands somewhere between
     * the two answers rather than on either. Zero cooling makes the planted value hold still, so the
     * assertion can be an exact number instead of an inequality.</p>
     */
    public static final ResourceLocation HEAT_STEADY_ID = MBD2.id("builtin_bp_heat_steady");
    public static final float HEAT_PER_TICK = 10f;
    public static final float COOL_PER_TICK = 5f;
    public static final float MAX_HEAT = 100f;
    /** The key the blueprint keeps its heat under — asserted directly, so it is part of the contract. */
    public static final String HEAT_KEY = "heat";

    /** {@code upgrade_slots}, reading slot 0 of a dedicated third item trait. */
    public static final ResourceLocation UPGRADE_ID = MBD2.id("builtin_bp_upgrade");
    /** The upgrade trait's name — a third item-slot trait, so it is off the recipe IO. */
    public static final String UPGRADE_TRAIT = "item_slot_2";
    public static final net.minecraft.world.item.Item UPGRADE_ITEM = Items.SUGAR;
    public static final float SPEED_PER_UPGRADE = 0.5f;
    /** Two upgrades give 1 + 2*0.5 = 2, so the 20-tick recipe becomes 10. */
    public static final int UPGRADED_DURATION = 10;

    /** The output item trait's name, as {@link TestMachineBuilder} names a second item-slot trait. */
    public static final String OUTPUT_TRAIT = "item_slot_1";

    @Override
    public void registerRecipeTypes(MBDRegistryEvent.MBDRecipeType event) {
        TestRecipeTypeBuilder.of(RECIPE_TYPE_ID)
                .recipe(STONE_TO_DIRT, b -> b
                        .inputItems(Items.STONE)
                        .outputItems(Items.DIRT)
                        .duration(DURATION))
                .register(event);
    }

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        processor(REDSTONE_DEFAULT_ID)
                .withBlueprint(builtin("redstone_control"))
                .register(event);
        processor(REDSTONE_REQUIRES_ID)
                .withBlueprint(builtin("redstone_control")
                        .withVariable("requiresSignal", constant(TypeHandles.BOOL, true)))
                .register(event);

        processor(COMPARATOR_ID)
                .withBlueprint(builtin("comparator_progress"))
                .register(event);
        processor(COMPARATOR_INVERTED_ID)
                .withBlueprint(builtin("comparator_progress")
                        .withVariable("invert", constant(TypeHandles.BOOL, true)))
                .register(event);

        processor(OVERCLOCK_ID)
                .withMachineLevel(OVERCLOCK_TIER)
                .withBlueprint(builtin("overclock")
                        .withVariable("speedPerTier", constant(TypeHandles.FLOAT, SPEED_PER_TIER))
                        // Inputs left unscaled: the test is about the duration, and a machine whose
                        // recipe suddenly needs four stone would stop running for a reason that has
                        // nothing to do with what is being measured.
                        .withVariable("costPerTier", constant(TypeHandles.FLOAT, 1f)))
                .register(event);
        // Same tier, same recipe, no blueprint. Without it, "maxProgress is 5" only says the machine
        // ran a 5-tick recipe, not that the blueprint is what made it one.
        processor(OVERCLOCK_CONTROL_ID)
                .withMachineLevel(OVERCLOCK_TIER)
                .register(event);

        bonusProcessor(CHANCE_ALWAYS_ID)
                .withBlueprint(chanceOutput(1f))
                .register(event);
        bonusProcessor(CHANCE_NEVER_ID)
                .withBlueprint(chanceOutput(0f))
                .register(event);

        processor(DEBUG_PROBE_ID)
                .withBlueprint(builtin("debug_probe")
                        .withVariable("probeItem", constant(TypeHandles.ITEM_STACK,
                                new net.minecraft.world.item.ItemStack(PROBE_ITEM))))
                .register(event);

        // Two output slots so "made diamonds" and "made no dirt" are independent facts: with one slot
        // the second is free, because whichever landed first would block the other.
        bonusProcessor(OUTPUT_SWAP_ID)
                .withBlueprint(builtin("output_swap")
                        .withVariable("product", constant(TypeHandles.ITEM_STACK,
                                new net.minecraft.world.item.ItemStack(SWAPPED_PRODUCT, 2))))
                .register(event);

        processor(ENV_GATE_ID)
                .withBlueprint(builtin("environment_gate"))
                .register(event);

        // The tank is not on the recipe IO — an upkeep fluid is a running cost, not an ingredient, and
        // a tank the recipe could see would change which recipes match.
        TestMachineBuilder.simple(UPKEEP_ID)
                .withItemSlots(1, IO.IN)
                .withItemSlots(1, IO.OUT)
                .withFluidTanks(1, UPKEEP_TANK, def -> def.setRecipeHandlerIO(IO.NONE))
                .withRecipeType(RECIPE_TYPE_ID)
                .withBlueprint(builtin("upkeep")
                        .withVariable("traitName", constant(TypeHandles.STRING, "fluid_tank"))
                        .withVariable("amountPerTick", constant(TypeHandles.INT, UPKEEP_PER_TICK)))
                .register(event);

        processor(HEAT_ID)
                .withBlueprint(builtin("heat_buildup")
                        .withVariable("heatPerTick", constant(TypeHandles.FLOAT, HEAT_PER_TICK))
                        .withVariable("coolPerTick", constant(TypeHandles.FLOAT, COOL_PER_TICK))
                        .withVariable("maxHeat", constant(TypeHandles.FLOAT, MAX_HEAT)))
                .register(event);
        processor(HEAT_STEADY_ID)
                .withBlueprint(builtin("heat_buildup")
                        .withVariable("heatPerTick", constant(TypeHandles.FLOAT, HEAT_PER_TICK))
                        .withVariable("coolPerTick", constant(TypeHandles.FLOAT, 0f))
                        .withVariable("maxHeat", constant(TypeHandles.FLOAT, MAX_HEAT)))
                .register(event);

        TestMachineBuilder.simple(UPGRADE_ID)
                .withItemSlots(1, IO.IN)
                .withItemSlots(1, IO.OUT)
                // The third trait holds the upgrades. IO.NONE keeps them out of recipe matching, which
                // is the arrangement the blueprint's own note tells a pack author to make.
                .withItemSlots(1, IO.NONE)
                .withRecipeType(RECIPE_TYPE_ID)
                .withBlueprint(builtin("upgrade_slots")
                        .withVariable("traitName", constant(TypeHandles.STRING, UPGRADE_TRAIT))
                        .withVariable("upgradeItem", constant(TypeHandles.ITEM_STACK,
                                new net.minecraft.world.item.ItemStack(UPGRADE_ITEM)))
                        .withVariable("speedPerUpgrade", constant(TypeHandles.FLOAT, SPEED_PER_UPGRADE)))
                .register(event);

        registerPartBonus(event);
    }

    /** A machine that turns stone into dirt in {@link #DURATION} ticks: slot 0 in, slot 1 out. */
    private static TestMachineBuilder processor(ResourceLocation id) {
        return TestMachineBuilder.simple(id)
                .withItemSlots(1, IO.IN)
                .withItemSlots(1, IO.OUT)
                .withRecipeType(RECIPE_TYPE_ID);
    }

    /**
     * The same, with a <em>two</em>-slot output — for {@code chance_output}, whose bonus is a different
     * item from the recipe's own product.
     *
     * <p>A one-slot output cannot hold both: an {@code ItemStackHandler} will not put a gold nugget in
     * a slot already holding dirt, so the insert would fail and the blueprint would look broken when
     * the only thing wrong was the machine it was bound to. That is a real trap for a pack author too,
     * which is why the blueprint's own note says a full slot drops the bonus.</p>
     */
    private static TestMachineBuilder bonusProcessor(ResourceLocation id) {
        return TestMachineBuilder.simple(id)
                .withItemSlots(1, IO.IN)
                .withItemSlots(2, IO.OUT)
                .withRecipeType(RECIPE_TYPE_ID);
    }

    private static MachineBlueprintBinding chanceOutput(float chance) {
        return builtin("chance_output")
                .withVariable("chance", constant(TypeHandles.FLOAT, chance))
                .withVariable("traitName", constant(TypeHandles.STRING, OUTPUT_TRAIT))
                .withVariable("bonusItem", constant(TypeHandles.ITEM_STACK,
                        new net.minecraft.world.item.ItemStack(BONUS_ITEM)));
    }

    /**
     * A 1x3 line: part, controller, part. Two parts, so the bonus is a factor the test can state
     * exactly rather than a direction it can only assert the sign of.
     */
    private void registerPartBonus(MBDRegistryEvent.Machine event) {
        var partDef = TestMachineBuilder.simple(PART_BONUS_PART_ID).register(event);
        TestMachineBuilder.multiblock(PART_BONUS_CONTROLLER_ID)
                .withItemSlots(1, IO.IN)
                .withItemSlots(1, IO.OUT)
                .withRecipeType(RECIPE_TYPE_ID)
                .withBlueprint(builtin("part_count_bonus")
                        .withVariable("speedPerPart", constant(TypeHandles.FLOAT, SPEED_PER_PART)))
                .withBlockPattern(controller -> FactoryBlockPattern.start()
                        .aisle("PCP")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('P', Predicates.blocks(partDef.block()))
                        .build())
                .register(event);
    }

    /** A binding referencing one of the shipped built-ins by path. */
    private static MachineBlueprintBinding builtin(String name) {
        var binding = new MachineBlueprintBinding();
        binding.setBlueprintPath(BuiltinBlueprints.path(name));
        return binding;
    }

    /** A parameter override value, in the form {@code MachineBlueprintBinding} stores. */
    private static TypeConstant constant(TypeHandle handle, Object value) {
        var constant = new TypeConstant();
        constant.init(handle);
        constant.setValue(value);
        return constant;
    }

    /** Referenced so the fixture class is initialised before the gametest server starts. */
    public static ResourceLocation recipeType() {
        return RECIPE_TYPE_ID;
    }
}
