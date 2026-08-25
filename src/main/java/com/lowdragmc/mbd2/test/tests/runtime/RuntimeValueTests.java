package com.lowdragmc.mbd2.test.tests.runtime;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import com.lowdragmc.mbd2.common.trait.item.ItemSlotCapabilityTrait;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Tests for {@link com.lowdragmc.mbd2.common.runtime.RuntimeValue} on {@link
 * com.lowdragmc.mbd2.common.machine.MBDMachine}: fallback to the definition, override, clear,
 * persistence, legacy NBT migration and forward compatibility.
 */
@GameTestHolder(MBD2.MOD_ID)
public class RuntimeValueTests {
    static { @SuppressWarnings("unused") var ignored = RuntimeValueFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    /** The machine level authored on the fixture definition — {@code ConfigMachineSettings}' default. */
    private static final int AUTHORED_LEVEL = 0;

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void unset_slot_reads_the_definition(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.MACHINE_ID, POS)
                .check("machine level should fall back to the definition",
                        m -> m.getMachineLevel() == AUTHORED_LEVEL)
                .check("no override should be recorded",
                        m -> !m.getRuntimeValues().isOverridden("machine_level"))
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void override_then_clear_restores_definition_value(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.MACHINE_ID, POS)
                .with(m -> m.setMachineLevel(5))
                .check("override should win over the definition", m -> m.getMachineLevel() == 5)
                .check("override should be recorded",
                        m -> m.getRuntimeValues().isOverridden("machine_level"))
                .with(m -> m.clearMachineLevel())
                .check("clear should restore the definition value",
                        m -> m.getMachineLevel() == AUTHORED_LEVEL)
                .check("override should be gone",
                        m -> !m.getRuntimeValues().isOverridden("machine_level"))
                .succeed();
    }

    /**
     * {@code authored()} reports what the definition says regardless of any override — the counterpart to
     * {@code isOverridden()}, for anything that wants to show "the definition says X, this machine says
     * Y" rather than just the effective value.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void authored_ignores_the_override(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.MACHINE_ID, POS)
                .with(m -> m.setMachineLevel(5))
                .check("get() reports the override", m -> m.getMachineLevel() == 5)
                .check("authored() still reports the definition",
                        m -> (Integer) m.getRuntimeValues().authored("machine_level") == AUTHORED_LEVEL)
                .check("and the two disagree, which is the whole point",
                        m -> !m.getRuntimeValues().get("machine_level")
                                .equals(m.getRuntimeValues().authored("machine_level")))
                .succeed();
    }

    /** A negative level is the documented "back to the definition" signal, kept from {@code SetTier}. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void negative_level_clears_the_override(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.MACHINE_ID, POS)
                .with(m -> m.setMachineLevel(9))
                .with(m -> m.setMachineLevel(-1))
                .check("negative level should clear rather than store",
                        m -> !m.getRuntimeValues().isOverridden("machine_level"))
                .check("machine level should be back to the definition",
                        m -> m.getMachineLevel() == AUTHORED_LEVEL)
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void override_survives_persistence_round_trip(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.MACHINE_ID, POS)
                .with(m -> m.setMachineLevel(5))
                .assertPersistenceRoundTrip()
                .check("override should survive a save/load cycle", m -> m.getMachineLevel() == 5)
                .check("override should still be marked as one",
                        m -> m.getRuntimeValues().isOverridden("machine_level"))
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void cleared_override_does_not_come_back(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.MACHINE_ID, POS)
                .with(m -> m.setMachineLevel(5))
                .assertPersistenceRoundTrip()
                .with(m -> m.clearMachineLevel())
                .assertPersistenceRoundTrip()
                .check("a cleared override must not survive a save/load cycle",
                        m -> !m.getRuntimeValues().isOverridden("machine_level"))
                .succeed();
    }

    /**
     * Worlds saved before the runtime value system stored the tier under {@code dynamicMachineLevel}.
     * {@code MBDMachine.migrateLegacyRuntimeOverrides} folds it into the slot on load, and the machine
     * re-saves it in the new shape.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void legacy_dynamic_machine_level_migrates(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.MACHINE_ID, POS)
                .assertPersistenceRoundTrip(tag -> RuntimeNbt.managed(tag).putInt("dynamicMachineLevel", 7))
                .check("legacy tier should be picked up on load", m -> m.getMachineLevel() == 7)
                .check("legacy tier should now live in the runtime value",
                        m -> m.getRuntimeValues().isOverridden("machine_level"))
                .assertPersistenceRoundTrip(tag -> {
                    var managed = RuntimeNbt.managed(tag);
                    if (!managed.getCompound("runtimeValues").contains("machine_level")) {
                        h.fail("migrated tier should be re-saved under managed.runtimeValues.machine_level, got "
                                + managed);
                    }
                    if (managed.getInt("dynamicMachineLevel") != -1) {
                        h.fail("legacy key should be reset to -1 after migration, got "
                                + managed.getInt("dynamicMachineLevel"));
                    }
                })
                .check("tier should still be 7 after the second round trip", m -> m.getMachineLevel() == 7)
                .succeed();
    }

    /**
     * A slot id this build does not know — a downgrade, or a trait type that is no longer loaded — must
     * round-trip untouched rather than being silently dropped.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void unknown_slot_id_is_preserved(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.MACHINE_ID, POS)
                .with(m -> m.setMachineLevel(3))
                .assertPersistenceRoundTrip(tag -> RuntimeNbt.machineValues(tag).putInt("future_slot", 42))
                .check("known slots must still load next to an unknown one", m -> m.getMachineLevel() == 3)
                .assertPersistenceRoundTrip(tag -> {
                    var values = RuntimeNbt.machineValues(tag);
                    if (values.getInt("future_slot") != 42) {
                        h.fail("unknown slot should be written back verbatim, got " + values);
                    }
                })
                .succeed();
    }

    // region trait slots — capability IO

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void capability_io_override_blocks_one_side_only(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.MACHINE_ID, POS)
                .assertExposes(Capabilities.ItemHandler.BLOCK, Direction.UP, RuntimeValueTests::canInsert)
                .with(m -> itemTrait(m).setCapabilityIOSide(Direction.UP, IO.NONE))
                .assertExposes(Capabilities.ItemHandler.BLOCK, Direction.UP, handler -> !canInsert(handler))
                .assertExposes(Capabilities.ItemHandler.BLOCK, Direction.NORTH, RuntimeValueTests::canInsert)
                .with(m -> itemTrait(m).clearCapabilityIO())
                .assertExposes(Capabilities.ItemHandler.BLOCK, Direction.UP, RuntimeValueTests::canInsert)
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void capability_io_override_survives_persistence(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.MACHINE_ID, POS)
                .with(m -> itemTrait(m).setCapabilityIOSide(Direction.UP, IO.NONE))
                .assertPersistenceRoundTrip()
                .assertExposes(Capabilities.ItemHandler.BLOCK, Direction.UP, handler -> !canInsert(handler))
                .assertExposes(Capabilities.ItemHandler.BLOCK, Direction.NORTH, RuntimeValueTests::canInsert)
                .succeed();
    }

    /**
     * The regression guard for the invalidation hook. A neighbour holding a {@link BlockCapabilityCache}
     * keeps handing out the handler it resolved earlier until {@code Level.invalidateCapabilities} is
     * called — which is why {@code capability_io.*} carries an {@code onChanged} hook rather than relying
     * on callers to re-query.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void capability_io_override_invalidates_neighbor_caches(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(RuntimeValueFixtures.MACHINE_ID, POS);
        var cache = BlockCapabilityCache.create(
                Capabilities.ItemHandler.BLOCK, h.getLevel(), h.absolutePos(POS), Direction.UP);

        var before = cache.getCapability();
        if (before == null || !canInsert(before)) {
            h.fail("expected an inserting item handler on UP before the override");
        }

        scenario.with(m -> itemTrait(m).setCapabilityIOSide(Direction.UP, IO.NONE));

        var after = cache.getCapability();
        if (after == null) {
            h.fail("capability should still be present, only its IO changed");
        } else if (canInsert(after)) {
            h.fail("cached handler was not invalidated after the capability IO override");
        }
        scenario.succeed();
    }

    // endregion

    // region trait slots — auto IO

    private static final BlockPos AUTO_IO_MACHINE = new BlockPos(2, 1, 2);
    private static final BlockPos AUTO_IO_SOURCE = AUTO_IO_MACHINE.relative(Direction.EAST);

    /** The headline case: a script turning auto-IO off for one machine. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void auto_io_enable_override_stops_pulling(GameTestHelper h) {
        var source = MBDTestHelper.placeMachine(h, RuntimeValueFixtures.MACHINE_ID, AUTO_IO_SOURCE);
        MBDTestHelper.insertItem(h, source, 0, new ItemStack(Items.IRON_INGOT, 12));

        MBDScenario.of(h)
                .placeMachineFacing(RuntimeValueFixtures.AUTO_IO_MACHINE_ID, AUTO_IO_MACHINE, Direction.NORTH)
                .with(m -> autoIOTrait(m).setAutoIOEnabled(false))
                .runTicks(4)
                .assertItem(0, ItemStack.EMPTY)
                // clearing the override puts the machine back on the definition, which has it enabled
                .with(m -> autoIOTrait(m).clearAutoIO())
                .runTicks(4)
                .assertItem(0, new ItemStack(Items.IRON_INGOT, 12))
                .succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void auto_io_enable_override_survives_persistence(GameTestHelper h) {
        var source = MBDTestHelper.placeMachine(h, RuntimeValueFixtures.MACHINE_ID, AUTO_IO_SOURCE);
        MBDTestHelper.insertItem(h, source, 0, new ItemStack(Items.IRON_INGOT, 12));

        MBDScenario.of(h)
                .placeMachineFacing(RuntimeValueFixtures.AUTO_IO_MACHINE_ID, AUTO_IO_MACHINE, Direction.NORTH)
                .with(m -> autoIOTrait(m).setAutoIOEnabled(false))
                .assertPersistenceRoundTrip()
                .runTicks(4)
                .assertItem(0, ItemStack.EMPTY)
                .succeed();
    }

    /**
     * The core guarantee of leaf-granular slots: overriding one leaf must not freeze its siblings.
     * With {@code auto_io.enable} overridden, editing {@code rightIO} on the definition still reaches
     * this machine.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void overriding_one_leaf_leaves_siblings_on_the_definition(GameTestHelper h) {
        var scenario = MBDScenario.of(h)
                .placeMachineFacing(RuntimeValueFixtures.AUTO_IO_MACHINE_ID, AUTO_IO_MACHINE, Direction.NORTH);
        var trait = autoIOTrait(scenario.machine());
        var authored = trait.getDefinition().getAutoIO();

        try {
            trait.setAutoIOEnabled(false);
            if (trait.autoIO.right.get() != IO.IN) {
                h.fail("sibling should still read the definition, got " + trait.autoIO.right.get());
            }

            authored.setRightIO(IO.OUT);
            if (trait.autoIO.right.get() != IO.OUT) {
                h.fail("an unoverridden sibling must follow the definition, got " + trait.autoIO.right.get());
            }
            if (trait.autoIO.enable.get()) {
                h.fail("the overridden leaf must stay overridden while its siblings move");
            }
        } finally {
            // a definition is shared by every machine of the type — never leave it edited
            authored.setRightIO(IO.IN);
        }
        scenario.succeed();
    }

    /** Two machines of the same definition must not share override state. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void overrides_are_per_machine(GameTestHelper h) {
        var first = MBDTestHelper.placeMachine(h, RuntimeValueFixtures.AUTO_IO_MACHINE_ID, AUTO_IO_MACHINE);
        var second = MBDTestHelper.placeMachine(h, RuntimeValueFixtures.AUTO_IO_MACHINE_ID, new BlockPos(4, 1, 2));

        autoIOTrait(first).setAutoIOEnabled(false);

        if (autoIOTrait(first).autoIO.enable.get()) {
            h.fail("the overridden machine should have auto IO off");
        }
        if (!autoIOTrait(second).autoIO.enable.get()) {
            h.fail("the other machine should still read the definition");
        }
        if (autoIOTrait(second).autoIO.enable.isOverridden()) {
            h.fail("the other machine should have no override at all");
        }
        h.succeed();
    }

    /** The blueprint graph node reaching a trait's runtime value, end to end. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void blueprint_node_disables_auto_io(GameTestHelper h) {
        var source = MBDTestHelper.placeMachine(h, RuntimeValueFixtures.MACHINE_ID, AUTO_IO_SOURCE);
        MBDTestHelper.insertItem(h, source, 0, new ItemStack(Items.IRON_INGOT, 12));

        MBDScenario.of(h)
                .placeMachineFacing(RuntimeValueFixtures.BLUEPRINT_AUTO_IO_ID, AUTO_IO_MACHINE, Direction.NORTH)
                .runTicks(4)
                .assertItem(0, ItemStack.EMPTY)
                .check("the blueprint node should have recorded an override",
                        m -> autoIOTrait(m).autoIO.enable.isOverridden())
                .succeed();
    }

    // endregion

    private static ItemSlotCapabilityTrait autoIOTrait(MBDMachine machine) {
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof ItemSlotCapabilityTrait itemTrait) {
                return itemTrait;
            }
        }
        throw new AssertionError("fixture machine has no item slot trait");
    }

    private static boolean canInsert(IItemHandler handler) {
        return handler.insertItem(0, new ItemStack(Items.IRON_INGOT, 1), true).isEmpty();
    }

    private static SimpleCapabilityTrait<?, ?> itemTrait(MBDMachine machine) {
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof SimpleCapabilityTrait<?, ?> capabilityTrait) {
                return capabilityTrait;
            }
        }
        throw new AssertionError("fixture machine has no capability trait");
    }

}
