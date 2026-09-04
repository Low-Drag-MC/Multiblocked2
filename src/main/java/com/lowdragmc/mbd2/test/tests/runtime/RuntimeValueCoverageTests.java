package com.lowdragmc.mbd2.test.tests.runtime;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.capability.recipe.ItemRecipeCapability;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.fluid.FluidTankCapabilityTrait;
import com.lowdragmc.mbd2.common.trait.forgeenergy.ForgeEnergyCapabilityTrait;
import com.lowdragmc.mbd2.common.trait.item.ItemSlotCapabilityTrait;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Set;

/**
 * The runtime values added after the first pass: the recipe-handler triple
 * ({@code recipe_handler_io}, {@code distinct}, {@code slot_names}), the storage capacities, the filter
 * toggles, {@code recipe_logic.consume_inputs_after_working}, and the blueprint nodes that reach the
 * value types the first pass could not express.
 *
 * @see RuntimeValueTests for the mechanics of a slot itself — fallback, override, clear, persistence
 * @see RuntimeValueStorageTypeTests for the codecs and coercion behind the new value types
 */
@GameTestHolder(MBD2.MOD_ID)
public class RuntimeValueCoverageTests {
    static { @SuppressWarnings("unused") var ignored = RuntimeValueFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    // region recipe handler triple

    /**
     * The headline of this group: an override moves a handler between the recipe engine's IO buckets.
     *
     * <p>{@code recipe_handler_io} is not read on the recipe engine's hot path — it is the key
     * {@code initCapabilitiesProxy} files each handler under, once, when the traits load. Without the
     * {@code onChanged} hook the override would be recorded, {@code getHandlerIO()} would report it, and
     * the recipe engine would carry on using the old bucket.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void recipe_handler_io_override_rebuckets_the_proxy(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(RuntimeValueFixtures.RECIPE_HANDLER_MACHINE_ID, POS);
        var machine = scenario.machine();
        var inputTrait = inputTrait(machine);

        assertItemHandlerCount(h, machine, IO.IN, 1, "before the override");
        assertItemHandlerCount(h, machine, IO.OUT, 1, "before the override");

        inputTrait.recipeHandlerIO.set(IO.OUT);
        if (inputTrait.getHandlerIO() != IO.OUT) {
            h.fail("the trait should report the override, got " + inputTrait.getHandlerIO());
        }
        assertItemHandlerCount(h, machine, IO.IN, 0, "after moving the input trait to OUT");
        assertItemHandlerCount(h, machine, IO.OUT, 2, "after moving the input trait to OUT");

        inputTrait.recipeHandlerIO.clear();
        assertItemHandlerCount(h, machine, IO.IN, 1, "after clearing the override");
        assertItemHandlerCount(h, machine, IO.OUT, 1, "after clearing the override");
        scenario.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void recipe_handler_io_override_survives_persistence(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.RECIPE_HANDLER_MACHINE_ID, POS)
                .with(m -> inputTrait(m).recipeHandlerIO.set(IO.OUT))
                .assertPersistenceRoundTrip()
                .check("the override should survive a save/load cycle",
                        m -> inputTrait(m).getHandlerIO() == IO.OUT)
                .check("and the reloaded machine should have re-bucketed too",
                        m -> itemHandlerCount(m, IO.IN) == 0 && itemHandlerCount(m, IO.OUT) == 2)
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void distinct_override_reaches_the_recipe_handler(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.RECIPE_HANDLER_MACHINE_ID, POS)
                .check("the fixture definition is not distinct", m -> !inputTrait(m).isDistinct())
                .check("nor is the handler the recipe engine sees",
                        m -> inputTrait(m).getRecipeHandlerTraits().stream().noneMatch(handler -> handler.isDistinct()))
                .with(m -> inputTrait(m).distinct.set(true))
                .check("the trait should report the override", m -> inputTrait(m).isDistinct())
                // the handler delegates on every call, which is what makes the recipe engine see it
                .check("and so should every handler it exposes",
                        m -> inputTrait(m).getRecipeHandlerTraits().stream().allMatch(handler -> handler.isDistinct()))
                .assertPersistenceRoundTrip()
                .check("the override should survive a save/load cycle", m -> inputTrait(m).isDistinct())
                .with(m -> inputTrait(m).distinct.clear())
                .check("clearing should go back to the definition", m -> !inputTrait(m).isDistinct())
                .succeed();
    }

    /**
     * The list slot, including the comma-separated form a script or a text node writes.
     *
     * <p>{@code getSlotNames()} returns a {@code Set} while the slot holds a {@code List}, so this also
     * pins down that the conversion happens and that the stored value is not the caller's own list.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void slot_names_override_from_a_list_and_from_text(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.RECIPE_HANDLER_MACHINE_ID, POS)
                .check("the fixture definition names no slots", m -> inputTrait(m).getSlotNames().isEmpty())
                .with(m -> inputTrait(m).slotNames.set(List.of("alpha", "beta")))
                .check("the override should be visible as a set",
                        m -> inputTrait(m).getSlotNames().equals(Set.of("alpha", "beta")))
                .check("and the handler should report it too",
                        m -> inputTrait(m).getRecipeHandlerTraits().stream()
                                .allMatch(handler -> handler.getSlotNames().equals(Set.of("alpha", "beta"))))
                // the form a text node or a KubeJS string writes; blanks and stray spaces are dropped
                .with(m -> inputTrait(m).getRuntimeValues().set("slot_names", " gamma , delta ,, "))
                .check("a comma-separated string should become a trimmed list",
                        m -> inputTrait(m).getSlotNames().equals(Set.of("gamma", "delta")))
                .assertPersistenceRoundTrip()
                .check("the list should survive a save/load cycle",
                        m -> inputTrait(m).getSlotNames().equals(Set.of("gamma", "delta")))
                .with(m -> inputTrait(m).slotNames.clear())
                .check("clearing should go back to the definition's empty list",
                        m -> inputTrait(m).getSlotNames().isEmpty())
                .succeed();
    }

    /** A slot holding a list must not hand back something a caller can mutate underneath it. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void slot_names_override_is_immutable(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(RuntimeValueFixtures.RECIPE_HANDLER_MACHINE_ID, POS);
        var trait = inputTrait(scenario.machine());
        var mutable = new java.util.ArrayList<>(List.of("alpha"));

        trait.getRuntimeValues().set("slot_names", mutable);
        mutable.add("beta");
        if (!trait.getSlotNames().equals(Set.of("alpha"))) {
            h.fail("the slot copied nothing: mutating the caller's list changed it to " + trait.getSlotNames());
        }
        try {
            trait.slotNames.get().add("gamma");
            h.fail("the stored list should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // what we want
        }
        scenario.succeed();
    }

    // endregion

    // region consume inputs after working

    /**
     * The regression guard for the whole point of the fix: {@code MBDMachine} never overrode
     * {@code consumeInputsAfterWorking(MBDRecipe)}, so the editor setting was authored and then read
     * from {@code IMachine}'s {@code false} default forever.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void consume_inputs_after_working_is_read_from_the_definition(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.CONSUME_AFTER_MACHINE_ID, POS)
                // null recipe: the machine's answer does not depend on which recipe is asking
                .check("a machine authored with the setting should report it",
                        m -> m.consumeInputsAfterWorking(null))
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void consume_inputs_after_working_defaults_to_before(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.CONSUME_BEFORE_MACHINE_ID, POS)
                .check("a machine that did not ask for it should consume inputs up front",
                        m -> !m.consumeInputsAfterWorking(null))
                .with(m -> m.getRuntimeValues().set("recipe_logic.consume_inputs_after_working", true))
                .check("and an override should switch it on for this machine",
                        m -> m.consumeInputsAfterWorking(null))
                .assertPersistenceRoundTrip()
                .check("the override should survive a save/load cycle",
                        m -> m.consumeInputsAfterWorking(null))
                .with(m -> m.getRuntimeValues().clear("recipe_logic.consume_inputs_after_working"))
                .check("clearing should go back to the definition",
                        m -> !m.consumeInputsAfterWorking(null))
                .succeed();
    }

    /**
     * End to end: the input is still in the slot while the recipe runs, and gone once it finishes.
     *
     * <p>The observable difference the setting is for — with it off the stone would already be gone the
     * first tick the machine reported {@code WORKING}.</p>
     */
    @GameTest(template = "empty_simple", timeoutTicks = 400)
    @PrefixGameTestTemplate(false)
    public static void consume_inputs_after_working_keeps_inputs_until_the_end(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.CONSUME_AFTER_MACHINE_ID, POS)
                .insertItem(0, new ItemStack(Items.STONE, 1))
                .runUntil(m -> m.getRecipeLogic().isWorking(), 200)
                // the assertion the fix is for: with the setting off this slot would already be empty
                .assertItem(0, new ItemStack(Items.STONE, 1))
                .runUntil(m -> !outputSlot(m, 0).isEmpty(), 200)
                .check("the output should be what the recipe makes",
                        m -> outputSlot(m, 0).is(Items.DIRT))
                .check("and the input should have been taken by then",
                        m -> inputSlot(m, 0).isEmpty())
                .succeed();
    }

    // endregion

    // region capacities

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void energy_capacity_override_resizes_and_spills(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.ENERGY_MACHINE_ID, POS)
                .insertEnergy(5000)
                .check("the fixture should be full at its authored capacity",
                        m -> energyTrait(m).getStorage().getEnergyStored() == 5000)
                .with(m -> energyTrait(m).capacity.set(10_000))
                .check("growing the buffer should be visible to a reader",
                        m -> energyTrait(m).getStorage().getMaxEnergyStored() == 10_000)
                .check("and should not change what is stored",
                        m -> energyTrait(m).getStorage().getEnergyStored() == 5000)
                .with(m -> energyTrait(m).capacity.set(1000))
                .check("shrinking below what is stored must spill the excess, not report over-full",
                        m -> energyTrait(m).getStorage().getEnergyStored() == 1000)
                .assertPersistenceRoundTrip()
                .check("the capacity override should survive a save/load cycle",
                        m -> energyTrait(m).getStorage().getMaxEnergyStored() == 1000)
                .check("and the stored energy must not come back over-full",
                        m -> energyTrait(m).getStorage().getEnergyStored() <= 1000)
                .with(m -> energyTrait(m).capacity.clear())
                .check("clearing should go back to the definition",
                        m -> energyTrait(m).getStorage().getMaxEnergyStored() == 5000)
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void fluid_capacity_override_resizes_and_spills(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.TANK_MACHINE_ID, POS)
                .insertFluid(new FluidStack(Fluids.WATER, 1000))
                .assertFluid(0, new FluidStack(Fluids.WATER, 1000))
                .with(m -> fluidTrait(m).capacity.set(4000))
                .check("growing the tank should be visible to a reader",
                        m -> fluidTrait(m).storages[0].getCapacity() == 4000)
                .insertFluid(new FluidStack(Fluids.WATER, 3000))
                .assertFluid(0, new FluidStack(Fluids.WATER, 4000))
                .with(m -> fluidTrait(m).capacity.set(500))
                .check("shrinking below the contents must spill the excess",
                        m -> fluidTrait(m).storages[0].getFluidAmount() == 500)
                .assertPersistenceRoundTrip()
                .check("the capacity override should survive a save/load cycle",
                        m -> fluidTrait(m).storages[0].getCapacity() == 500)
                .with(m -> fluidTrait(m).capacity.clear())
                .check("clearing should go back to the definition",
                        m -> fluidTrait(m).storages[0].getCapacity() == 1000)
                .succeed();
    }

    /**
     * {@code FluidStorage} saves its capacity and restores it verbatim, so before this change a tank kept
     * whatever size it had when it was last saved even after the definition was edited. An unoverridden
     * slot has to follow the definition — that is the contract the whole system rests on.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void unoverridden_fluid_capacity_follows_an_edited_definition(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(RuntimeValueFixtures.TANK_MACHINE_ID, POS);
        var definition = fluidTrait(scenario.machine()).getDefinition();
        try {
            scenario.assertPersistenceRoundTrip();
            definition.setCapacity(7000);
            scenario.assertPersistenceRoundTrip()
                    .check("a tank with no override must pick up the definition's new capacity",
                            m -> fluidTrait(m).storages[0].getCapacity() == 7000);
        } finally {
            // a definition is shared by every machine of the type — never leave it edited
            definition.setCapacity(1000);
        }
        scenario.succeed();
    }

    // endregion

    // region filter toggle

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void filter_enable_override_can_switch_a_filter_off(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.FILTERED_MACHINE_ID, POS)
                .check("the authored filter should reject iron", m -> !accepts(m, Items.IRON_INGOT))
                .check("and accept diamonds", m -> accepts(m, Items.DIAMOND))
                .with(m -> inputTrait(m).filterEnabled.set(false))
                .check("switching the filter off should let iron in", m -> accepts(m, Items.IRON_INGOT))
                .assertPersistenceRoundTrip()
                .check("the override should survive a save/load cycle", m -> accepts(m, Items.IRON_INGOT))
                .with(m -> inputTrait(m).filterEnabled.clear())
                .check("clearing should put the filter back", m -> !accepts(m, Items.IRON_INGOT))
                .succeed();
    }

    /**
     * The other direction, which is the one that catches the subtle bug: the settings object's own
     * {@code test} short-circuits on its {@code enable} flag, so a trait that only asked
     * {@code filter.test(stack)} would filter nothing on a machine that had switched the filter
     * <b>on</b> over a definition that has it off.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void filter_enable_override_can_switch_a_filter_on(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.UNFILTERED_MACHINE_ID, POS)
                .check("with the filter authored off, anything goes in", m -> accepts(m, Items.IRON_INGOT))
                .with(m -> inputTrait(m).filterEnabled.set(true))
                .check("switching it on should start rejecting iron", m -> !accepts(m, Items.IRON_INGOT))
                .check("while still accepting what the filter whitelists", m -> accepts(m, Items.DIAMOND))
                .succeed();
    }

    // endregion

    // region blueprint nodes

    /** Drives the graph {@code RuntimeValueFixtures.exerciseValueNodes()} builds. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void blueprint_nodes_write_and_read_the_new_value_types(GameTestHelper h) {
        var box = new AABB(
                RuntimeValueFixtures.BLUEPRINT_BOX[0], RuntimeValueFixtures.BLUEPRINT_BOX[1],
                RuntimeValueFixtures.BLUEPRINT_BOX[2], RuntimeValueFixtures.BLUEPRINT_BOX[3],
                RuntimeValueFixtures.BLUEPRINT_BOX[4], RuntimeValueFixtures.BLUEPRINT_BOX[5]);

        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.BLUEPRINT_TEXT_AND_BOX_ID, POS)
                .runTicks(3)
                .check("Set Runtime Value (Text) should have written the slot names",
                        m -> inputTrait(m).getSlotNames().equals(Set.of("alpha", "beta")))
                .check("Set Runtime Value (Box) should have written the scan range",
                        m -> inputTrait(m).autoWorldInput.range.get().equals(box))
                .check("Runtime Value Names should have counted the trait's own slots",
                        m -> inputTrait(m).slotLimit.get() == inputTrait(m).getRuntimeValues().slots().size())
                .check("Get Runtime Text should have copied the names to the second trait",
                        m -> secondTrait(m).getSlotNames().equals(Set.of("alpha", "beta")))
                .check("Get Runtime Box should have copied the range to the second trait",
                        m -> secondTrait(m).autoWorldInput.range.get().equals(box))
                .succeed();
    }

    /**
     * The auto world IO nodes, driven end to end: a graph switches world scanning on, points it at the
     * machine's neighbourhood and picks a dropped item up.
     */
    @GameTest(template = "empty_simple", timeoutTicks = 200)
    @PrefixGameTestTemplate(false)
    public static void blueprint_nodes_drive_auto_world_input(GameTestHelper h) {
        var machinePos = new BlockPos(2, 1, 2);
        MBDTestHelper.spawnEntity(h, EntityType.ITEM, machinePos.above(),
                entity -> entity.setItem(new ItemStack(Items.IRON_INGOT, 5)));

        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.BLUEPRINT_WORLD_IO_ID, machinePos)
                .check("world input starts off on the definition",
                        m -> !inputTrait(m).autoWorldInput.enable.get())
                .runTicks(10)
                .check("Set Auto World IO Enabled should have switched it on",
                        m -> inputTrait(m).autoWorldInput.enable.get())
                .check("Set Auto World IO Interval should have reached the value",
                        m -> inputTrait(m).autoWorldInput.intervalTicks() == 1)
                .check("Set Auto World IO Speed should have reached the value",
                        m -> inputTrait(m).autoWorldInput.speed.get() == 64)
                .assertItem(0, new ItemStack(Items.IRON_INGOT, 5))
                .succeed();
    }

    // endregion

    // region helpers

    private static void assertItemHandlerCount(GameTestHelper h, MBDMachine machine, IO io, int expected, String when) {
        var actual = itemHandlerCount(machine, io);
        if (actual != expected) {
            h.fail("expected %d item recipe handler(s) in the %s bucket %s, got %d"
                    .formatted(expected, io, when, actual));
        }
    }

    private static int itemHandlerCount(MBDMachine machine, IO io) {
        var proxy = machine.getRecipeCapabilitiesProxy();
        if (!proxy.contains(io, ItemRecipeCapability.CAP)) return 0;
        return proxy.get(io, ItemRecipeCapability.CAP).size();
    }

    /**
     * The fixture's first item-slot trait. Every fixture here names it {@code item_slot} — only the
     * second and later ones get a suffix, see {@code TestMachineBuilder.withItemSlots}.
     */
    private static ItemSlotCapabilityTrait inputTrait(MBDMachine machine) {
        return requireTrait(machine, RuntimeValueFixtures.ITEM_SLOT_TRAIT);
    }

    private static ItemSlotCapabilityTrait secondTrait(MBDMachine machine) {
        return requireTrait(machine, RuntimeValueFixtures.SECOND_ITEM_SLOT_TRAIT);
    }

    /** By name rather than by first-of-type: these fixtures have two, and which one matters. */
    private static ItemSlotCapabilityTrait requireTrait(MBDMachine machine, String name) {
        if (machine.getTraitByName(name) instanceof ItemSlotCapabilityTrait trait) {
            return trait;
        }
        throw new AssertionError("fixture machine has no item slot trait named " + name);
    }

    private static FluidTankCapabilityTrait fluidTrait(MBDMachine machine) {
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof FluidTankCapabilityTrait fluidTrait) return fluidTrait;
        }
        throw new AssertionError("fixture machine has no fluid tank trait");
    }

    private static ForgeEnergyCapabilityTrait energyTrait(MBDMachine machine) {
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof ForgeEnergyCapabilityTrait energyTrait) return energyTrait;
        }
        throw new AssertionError("fixture machine has no energy trait");
    }

    /** Whether the machine's item handler would take {@code item} — the filter's observable effect. */
    private static boolean accepts(MBDMachine machine, net.minecraft.world.item.Item item) {
        var level = machine.getLevel();
        if (level == null) throw new AssertionError("fixture machine is not in a level");
        var handler = level.getCapability(Capabilities.ItemHandler.BLOCK, machine.getPos(), null);
        if (handler == null) throw new AssertionError("fixture machine exposes no item handler");
        return handler.insertItem(0, new ItemStack(item, 1), true).isEmpty();
    }

    /** A slot of the fixture's input item-slot trait. */
    private static ItemStack inputSlot(MBDMachine machine, int slot) {
        return requireTrait(machine, RuntimeValueFixtures.ITEM_SLOT_TRAIT).storage.getStackInSlot(slot);
    }

    /** A slot of the fixture's output item-slot trait — indices are per trait, not per machine. */
    private static ItemStack outputSlot(MBDMachine machine, int slot) {
        return requireTrait(machine, RuntimeValueFixtures.SECOND_ITEM_SLOT_TRAIT).storage.getStackInSlot(slot);
    }

    // endregion
}
