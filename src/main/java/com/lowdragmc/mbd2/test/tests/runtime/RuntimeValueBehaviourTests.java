package com.lowdragmc.mbd2.test.tests.runtime;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.item.ItemSlotCapabilityTrait;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import com.lowdragmc.mbd2.test.tests.multiblock.ProxyAutoIOFixtures;
import com.lowdragmc.mbd2.test.tests.trait.AutoIOTraitFixtures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What an override actually <em>does</em> — as opposed to what it stores.
 *
 * <p>Every test here asserts an observable difference in the machine's behaviour: an item that moves or
 * does not move, a stack size that caps, a pickup area that shifts. Asserting only that
 * {@code isOverridden()} flipped would pass for a slot nothing reads.</p>
 */
@GameTestHolder(MBD2.MOD_ID)
public class RuntimeValueBehaviourTests {
    static { @SuppressWarnings("unused") var ignored = RuntimeValueFixtures.MACHINE_ID; }

    private static final BlockPos MACHINE = new BlockPos(2, 1, 2);
    private static final BlockPos EAST = MACHINE.relative(Direction.EAST);
    private static final BlockPos NORTH = MACHINE.relative(Direction.NORTH);

    // region auto IO

    /**
     * The fixture pulls every tick. Stretching the interval to 20 must stop it pulling, and clearing the
     * override must let it resume — which is what distinguishes a working override from a broken machine.
     *
     * <p>The tick gate is {@code getOffsetTimer() % interval == 0}, and the offset timer is absolute game
     * time plus a per-position offset, not a per-machine stopwatch. So "run 4 ticks and expect nothing"
     * is only true if those 4 ticks contain no multiple of the interval — a 1-in-5 coin flip otherwise.
     * Align to just past a boundary first, and the window is deterministic.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void auto_io_interval_override_changes_the_rate(GameTestHelper h) {
        var source = MBDTestHelper.placeMachine(h, RuntimeValueFixtures.MACHINE_ID, EAST);
        var scenario = MBDScenario.of(h)
                .placeMachineFacing(RuntimeValueFixtures.AUTO_IO_MACHINE_ID, MACHINE, Direction.NORTH)
                .with(m -> autoIOTrait(m).setAutoIOInterval(20));

        // the source is still empty, so ticking here cannot move anything
        var machine = scenario.machine();
        for (int i = 0; i < 20 && Math.floorMod(machine.getOffsetTimer(), 20) != 1; i++) {
            MBDTestHelper.runTicks(h, 1);
        }
        if (Math.floorMod(machine.getOffsetTimer(), 20) != 1) {
            h.fail("could not align to the interval boundary");
            return;
        }
        MBDTestHelper.insertItem(h, source, 0, new ItemStack(Items.IRON_INGOT, 12));

        scenario.runTicks(4)                                   // 19 ticks short of the next boundary
                .assertItem(0, ItemStack.EMPTY)
                .with(m -> autoIOTrait(m).autoIO.interval.clear())
                .runTicks(4)                                   // back to every tick
                .assertItem(0, new ItemStack(Items.IRON_INGOT, 12))
                .succeed();
    }

    /**
     * An interval of zero would be a division by zero in the tick check. The definition's own editor
     * clamps to 1, but a script and an older saved definition do not, so the read clamps too.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void auto_io_interval_zero_is_clamped_not_fatal(GameTestHelper h) {
        var source = MBDTestHelper.placeMachine(h, RuntimeValueFixtures.MACHINE_ID, EAST);
        MBDTestHelper.insertItem(h, source, 0, new ItemStack(Items.IRON_INGOT, 12));

        MBDScenario.of(h)
                .placeMachineFacing(RuntimeValueFixtures.AUTO_IO_MACHINE_ID, MACHINE, Direction.NORTH)
                .with(m -> autoIOTrait(m).autoIO.interval.set(0))
                .runTicks(4)
                .assertItem(0, new ItemStack(Items.IRON_INGOT, 12))
                .succeed();
    }

    /**
     * The fixture pulls from its right (east). Overriding the front side must make it pull from the
     * north as well, without touching the side it already had.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void auto_io_side_override_opens_a_new_side(GameTestHelper h) {
        var north = MBDTestHelper.placeMachine(h, RuntimeValueFixtures.MACHINE_ID, NORTH);
        MBDTestHelper.insertItem(h, north, 0, new ItemStack(Items.DIAMOND, 5));

        MBDScenario.of(h)
                .placeMachineFacing(RuntimeValueFixtures.AUTO_IO_MACHINE_ID, MACHINE, Direction.NORTH)
                .runTicks(4)
                .assertItem(0, ItemStack.EMPTY)
                .with(m -> autoIOTrait(m).setAutoIOSide(Direction.NORTH, IO.IN))
                .runTicks(4)
                .assertItem(0, new ItemStack(Items.DIAMOND, 5))
                .succeed();
    }

    /**
     * Overrides are stored per machine-relative side, not per world direction, so they rotate with the
     * machine exactly as the definition's own per-side config does.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void auto_io_side_override_is_machine_relative(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachineFacing(RuntimeValueFixtures.AUTO_IO_MACHINE_ID, MACHINE, Direction.NORTH)
                // facing north, "east" is the machine's right
                .with(m -> autoIOTrait(m).setAutoIOSide(Direction.EAST, IO.OUT))
                .check("the right side should be overridden while facing north",
                        m -> autoIOTrait(m).autoIO.getIO(Direction.NORTH, Direction.EAST) == IO.OUT)
                // facing east, the machine's right is south — the override must follow it
                .check("and follow the machine when it faces east",
                        m -> autoIOTrait(m).autoIO.getIO(Direction.EAST, Direction.SOUTH) == IO.OUT)
                .check("leaving the world-east side reading the definition once rotated",
                        m -> autoIOTrait(m).autoIO.getIO(Direction.EAST, Direction.EAST) == IO.NONE)
                .succeed();
    }

    /** Overriding the world-IO pickup box must move where loose items are collected from. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void auto_world_io_range_override_moves_the_pickup_area(GameTestHelper h) {
        // the fixture's input range covers the block to the east; drop the item to the north instead
        var absolute = h.absolutePos(NORTH);
        h.getLevel().addFreshEntity(new ItemEntity(h.getLevel(),
                absolute.getX() + 0.5, absolute.getY() + 0.25, absolute.getZ() + 0.5,
                new ItemStack(Items.DIAMOND, 7)));

        MBDScenario.of(h)
                .placeMachineFacing(AutoIOTraitFixtures.ITEM_WORLD_INPUT, MACHINE, Direction.NORTH)
                .runTicks(4)
                .assertItem(0, ItemStack.EMPTY)
                // machine-relative box one block towards the front, which is north at this facing
                .with(m -> autoIOTrait(m).autoWorldInput.range.set(new AABB(0, 0, -1, 1, 1, 0)))
                .runTicks(4)
                .assertItem(0, new ItemStack(Items.DIAMOND, 7))
                .succeed();
    }

    /**
     * A multiblock port drives auto IO on the controller's trait using the <em>port's</em> config, so
     * without an explicit rule "turn this machine's auto IO off" would stop the trait's own auto IO and
     * leave the port moving the same items — half the feature silently not working.
     *
     * @see com.lowdragmc.mbd2.common.trait.IProxyAutoIOTrait#isAutoIOSuppressed
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void disabling_auto_io_also_stops_a_proxy_port(GameTestHelper h) {
        var controllerPos = new BlockPos(1, 1, 1);
        var chestPos = new BlockPos(0, 2, 1);
        MBDTestHelper.placeChestWithItems(h, chestPos);

        var scenario = MBDScenario.of(h)
                .placeMachine(ProxyAutoIOFixtures.BLOCK_PORT_OUTPUT_ID, controllerPos)
                .placeBlock(new BlockPos(0, 1, 1), Blocks.IRON_BLOCK.defaultBlockState())
                .placeBlock(new BlockPos(2, 1, 1), Blocks.STONE.defaultBlockState())
                .target(controllerPos)
                .formNow()
                .assertFormed()
                .with(m -> autoIOTrait(m).setAutoIOEnabled(false))
                .insertItem(0, new ItemStack(Items.GOLD_INGOT, 9))
                .runTicks(4);

        if (countInChest(h, chestPos, Items.GOLD_INGOT) != 0) {
            h.fail("the port kept pushing items after the trait's auto IO was overridden off");
            return;
        }

        // clearing puts the machine back on its definition, and the port resumes
        scenario.with(m -> autoIOTrait(m).clearAutoIO()).runTicks(4);

        int pushed = countInChest(h, chestPos, Items.GOLD_INGOT);
        if (pushed != 9) {
            h.fail("clearing the override should let the port push all 9 ingots again, got " + pushed);
            return;
        }
        scenario.succeed();
    }

    /**
     * The suppression rule keys on the override being present <em>and</em> false, not on the effective
     * value. Overriding it to {@code true} must therefore leave the port alone — otherwise the rule could
     * be simplified to {@code isOverridden()} and nobody would notice until a pack re-enabled a
     * definition-disabled trait and its port stopped working.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void an_override_of_true_does_not_stop_a_proxy_port(GameTestHelper h) {
        var controllerPos = new BlockPos(1, 1, 1);
        var chestPos = new BlockPos(0, 2, 1);
        MBDTestHelper.placeChestWithItems(h, chestPos);

        MBDScenario.of(h)
                .placeMachine(ProxyAutoIOFixtures.BLOCK_PORT_OUTPUT_ID, controllerPos)
                .placeBlock(new BlockPos(0, 1, 1), Blocks.IRON_BLOCK.defaultBlockState())
                .placeBlock(new BlockPos(2, 1, 1), Blocks.STONE.defaultBlockState())
                .target(controllerPos)
                .formNow()
                .assertFormed()
                .with(m -> autoIOTrait(m).setAutoIOEnabled(true))
                .check("the override is recorded, so the rule is actually exercised",
                        m -> autoIOTrait(m).autoIO.enable.isOverridden())
                .insertItem(0, new ItemStack(Items.GOLD_INGOT, 9))
                .runTicks(4)
                .check("the port still pushes",
                        m -> countInChest(h, chestPos, Items.GOLD_INGOT) == 9)
                .succeed();
    }

    private static int countInChest(GameTestHelper h, BlockPos chestPos, net.minecraft.world.item.Item item) {
        int total = 0;
        for (var stack : MBDTestHelper.readChestItems(h, chestPos)) {
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    // endregion

    // region storage-shaped values

    /** {@code slot_limit} is read live by the storage, so an override caps insertion immediately. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void slot_limit_override_caps_insertion(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.MACHINE_ID, new BlockPos(1, 1, 1))
                .with(m -> itemTrait(m).slotLimit.set(1))
                .assertExposes(Capabilities.ItemHandler.BLOCK, null, handler -> {
                    var left = handler.insertItem(0, new ItemStack(Items.IRON_INGOT, 10), false);
                    return left.getCount() == 9 && handler.getStackInSlot(0).getCount() == 1;
                })
                .succeed();
    }

    /** And clearing it restores the definition's limit for the same storage instance. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void clearing_slot_limit_restores_the_definition_limit(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.MACHINE_ID, new BlockPos(1, 1, 1))
                .with(m -> itemTrait(m).slotLimit.set(1))
                .with(m -> itemTrait(m).slotLimit.clear())
                .assertExposes(Capabilities.ItemHandler.BLOCK, null, handler ->
                        handler.insertItem(0, new ItemStack(Items.IRON_INGOT, 10), true).isEmpty())
                .succeed();
    }

    /**
     * {@code max_receive} is baked into the wrapper handed out when a neighbour resolves the capability,
     * and that neighbour caches the wrapper. Without the invalidation hook the override is invisible to
     * everything already connected — and correct again after a relog, which reads as a desync bug.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void max_receive_override_reaches_a_cached_capability(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(RuntimeValueFixtures.ENERGY_MACHINE_ID, MACHINE);
        var cache = BlockCapabilityCache.create(
                Capabilities.EnergyStorage.BLOCK, h.getLevel(), h.absolutePos(MACHINE), Direction.NORTH);

        var before = cache.getCapability();
        if (before == null || before.receiveEnergy(5000, true) != 5000) {
            h.fail("expected the definition's full 5000 transfer rate before the override, got "
                    + (before == null ? "no capability" : before.receiveEnergy(5000, true)));
            return;
        }

        scenario.with(m -> energyTrait(m).maxReceive.set(7));

        var after = cache.getCapability();
        if (after == null) {
            h.fail("capability should still be present");
        } else if (after.receiveEnergy(5000, true) != 7) {
            h.fail("cached capability was not invalidated: still accepts "
                    + after.receiveEnergy(5000, true) + " per insert, expected 7");
        }
        scenario.succeed();
    }

    // endregion

    // region machine-level values

    /** {@code recipe_logic.enable} gates the recipe logic every tick. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void recipe_logic_enable_override_gates_the_logic(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.RECIPE_LOGIC_MACHINE_ID, new BlockPos(1, 1, 1))
                .check("recipe logic runs by default", MBDMachine::runRecipeLogic)
                .with(m -> m.recipeLogicEnabled.set(false))
                .check("the override switches it off", m -> !m.runRecipeLogic())
                .with(m -> m.recipeLogicEnabled.clear())
                .check("clearing puts it back", MBDMachine::runRecipeLogic)
                .succeed();
    }

    /** {@code signal_connection.*} is what neighbouring redstone asks about. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void signal_connection_override_changes_redstone_connectivity(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachineFacing(RuntimeValueFixtures.SIGNAL_MACHINE_ID, MACHINE, Direction.NORTH)
                .check("no side connects by default", m -> !m.canConnectRedstone(Direction.UP))
                .with(m -> m.signalConnection.top.set(true))
                .check("the override connects the top", m -> m.canConnectRedstone(Direction.UP))
                .check("without touching the others", m -> !m.canConnectRedstone(Direction.EAST))
                .succeed();
    }

    // endregion

    // region by-name API, as scripts reach it

    /** KubeJS hands over JS values, so the string-addressed API coerces rather than demanding exact types. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void by_name_api_coerces_script_values(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.MACHINE_ID, new BlockPos(1, 1, 1))
                .with(m -> m.getRuntimeValues().set("machine_level", 7.0d))
                .check("a JS number should land as an int", m -> m.getMachineLevel() == 7)
                .with(m -> itemTrait(m).getRuntimeValues().set("capability_io.top", "NONE"))
                .check("an enum should be settable by name",
                        m -> itemTrait(m).capabilityIO.top.get() == IO.NONE)
                .succeed();
    }

    /**
     * {@code intervalTicks()} has to keep the old {@code timer % interval} semantics for every input the
     * old code accepted. Negative intervals gave a period of {@code |interval|} and are still reachable
     * from KubeJS and from definitions predating the editor's clamp; only 0 changes, and that used to
     * throw.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void interval_clamp_preserves_the_old_period(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.AUTO_IO_MACHINE_ID, new BlockPos(1, 1, 1))
                .with(m -> autoIOTrait(m).autoIO.interval.set(-5))
                .check("a negative interval keeps its magnitude as the period",
                        m -> autoIOTrait(m).autoIO.intervalTicks() == 5)
                .with(m -> autoIOTrait(m).autoIO.interval.set(0))
                .check("zero becomes 1 rather than dividing by zero",
                        m -> autoIOTrait(m).autoIO.intervalTicks() == 1)
                .with(m -> autoIOTrait(m).autoIO.interval.set(7))
                .check("a normal interval is untouched",
                        m -> autoIOTrait(m).autoIO.intervalTicks() == 7)
                .succeed();
    }

    /**
     * {@code setMachineLevel} treats a negative as "clear", but the by-name API is a second door that
     * stores what it is given. A negative tier is out of contract for the definition field either way
     * ({@code @ConfigNumber(range = {0, MAX})}), so the read clamps.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void a_negative_machine_level_never_escapes_the_getter(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.MACHINE_ID, new BlockPos(1, 1, 1))
                .with(m -> m.getRuntimeValues().set("machine_level", -3))
                .check("the getter clamps rather than reporting a negative tier",
                        m -> m.getMachineLevel() == 0)
                .succeed();
    }

    /**
     * {@code setValue} is the door scripts use on a slot directly. It exists because {@code set(T)}
     * erases to {@code set(Object)} and KubeJS's Rhino will not bind a JS primitive to that — a script
     * writing {@code slot.set(false)} gets {@code "Can't find method ...CachedClassInfo.set(boolean)"}.
     * A Java gametest cannot reproduce that binding failure, so what it pins instead is the coercion
     * contract {@code setValue} promises.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void set_value_coerces_like_the_by_name_api(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.AUTO_IO_MACHINE_ID, new BlockPos(1, 1, 1))
                .with(m -> autoIOTrait(m).autoIO.enable.setValue(Boolean.FALSE))
                .check("a boxed boolean lands", m -> !autoIOTrait(m).autoIO.enable.get())
                .with(m -> autoIOTrait(m).autoIO.interval.setValue(9.0d))
                .check("a JS-style double narrows to the int slot",
                        m -> autoIOTrait(m).autoIO.interval.get() == 9)
                .with(m -> autoIOTrait(m).autoIO.front.setValue("OUT"))
                .check("an enum constant name resolves",
                        m -> autoIOTrait(m).autoIO.front.get() == IO.OUT)
                .succeed();
    }

    /** An unknown name is a script bug, and has to say so rather than failing silently. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void by_name_api_rejects_an_unknown_key(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(RuntimeValueFixtures.MACHINE_ID, new BlockPos(1, 1, 1));
        var machine = scenario.machine();
        try {
            machine.getRuntimeValues().set("no_such_value", 1);
            h.fail("setting an unknown runtime value should throw");
        } catch (IllegalArgumentException expected) {
            if (!expected.getMessage().contains("machine_level")) {
                h.fail("the error should list the available values, got: " + expected.getMessage());
            }
        }
        // and it must not have half-applied anything
        if (machine.getRuntimeValues().slots().stream().anyMatch(s -> s.isOverridden())) {
            h.fail("a rejected write should leave no override behind");
        }
        scenario.succeed();
    }

    /** {@code clearAll} is the "put this machine back how its definition says" button. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void clear_all_drops_every_override(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.MACHINE_ID, new BlockPos(1, 1, 1))
                .with(m -> {
                    m.setMachineLevel(3);
                    m.dropMachineItem.set(false);
                    itemTrait(m).slotLimit.set(2);
                })
                .with(m -> {
                    m.getRuntimeValues().clearAll();
                    itemTrait(m).getRuntimeValues().clearAll();
                })
                .check("the machine is back on its definition",
                        m -> m.getMachineLevel() == 0 && m.dropMachineItem.get())
                .check("and so is the trait", m -> !itemTrait(m).slotLimit.isOverridden())
                .succeed();
    }

    // endregion

    private static com.lowdragmc.mbd2.common.trait.forgeenergy.ForgeEnergyCapabilityTrait energyTrait(MBDMachine machine) {
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof com.lowdragmc.mbd2.common.trait.forgeenergy.ForgeEnergyCapabilityTrait energy) {
                return energy;
            }
        }
        throw new AssertionError("fixture machine has no energy trait");
    }

    private static ItemSlotCapabilityTrait itemTrait(MBDMachine machine) {
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof ItemSlotCapabilityTrait itemSlot) return itemSlot;
        }
        throw new AssertionError("fixture machine has no item slot trait");
    }

    private static ItemSlotCapabilityTrait autoIOTrait(MBDMachine machine) {
        return itemTrait(machine);
    }
}
