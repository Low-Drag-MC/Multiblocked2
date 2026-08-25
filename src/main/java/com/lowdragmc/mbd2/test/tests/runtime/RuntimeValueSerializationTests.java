package com.lowdragmc.mbd2.test.tests.runtime;

import com.lowdragmc.lowdraglib2.syncdata.field.ManagedKey;
import com.lowdragmc.lowdraglib2.utils.ShapeUtils;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import com.lowdragmc.mbd2.common.trait.item.ItemSlotCapabilityTrait;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Serialization edge cases for {@link com.lowdragmc.mbd2.common.runtime.RuntimeValueStorage}.
 *
 * <p>The happy path lives in {@link RuntimeValueTests}. These are the awkward ones: tags written by an
 * older or newer build, corrupt payloads, the world-load ordering, and whether an override actually
 * makes the chunk save.</p>
 */
@GameTestHolder(MBD2.MOD_ID)
public class RuntimeValueSerializationTests {
    static { @SuppressWarnings("unused") var ignored = RuntimeValueFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    /** A machine with nothing overridden must not grow junk across save/load cycles. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void empty_storage_round_trips_clean(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.MACHINE_ID, POS)
                .assertPersistenceRoundTrip(tag -> {
                    var values = RuntimeNbt.machineValuesIfPresent(tag);
                    if (values != null && !values.isEmpty()) {
                        h.fail("an unoverridden machine should save no runtime values, got " + values);
                    }
                })
                .check("still nothing overridden after a round trip",
                        m -> m.getRuntimeValues().slots().stream().noneMatch(s -> s.isOverridden()))
                .succeed();
    }

    /**
     * A world saved before the runtime value system has no {@code runtimeValues} key at all. Loading it
     * must leave every slot on its definition rather than throwing.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void missing_storage_key_loads_as_unoverridden(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.MACHINE_ID, POS)
                .with(m -> m.setMachineLevel(4))
                .assertPersistenceRoundTrip(tag -> RuntimeNbt.managed(tag).remove("runtimeValues"))
                .check("a machine with no storage tag reads its definition",
                        m -> m.getMachineLevel() == 0 && !m.getRuntimeValues().isOverridden("machine_level"))
                .succeed();
    }

    /** A payload of the wrong type must fall back to the definition, not crash the block entity. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void corrupt_int_payload_falls_back_to_definition(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.MACHINE_ID, POS)
                .with(m -> m.setMachineLevel(4))
                .assertPersistenceRoundTrip(tag ->
                        RuntimeNbt.machineValues(tag).put("machine_level", StringTag.valueOf("not a number")))
                .check("a corrupt payload should read as unoverridden",
                        m -> !m.getRuntimeValues().isOverridden("machine_level"))
                .check("and the definition value should be in force", m -> m.getMachineLevel() == 0)
                .succeed();
    }

    /** Same for an enum constant that no longer exists — a removed IO mode, a downgraded mod. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void corrupt_enum_payload_falls_back_to_definition(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.MACHINE_ID, POS)
                .with(m -> capabilityTrait(m).capabilityIO.top.set(IO.NONE))
                .assertPersistenceRoundTrip(tag -> RuntimeNbt
                        .traitValues(tag, RuntimeValueFixtures.ITEM_SLOT_TRAIT)
                        .put("capability_io.top", StringTag.valueOf("SIDEWAYS")))
                .check("an unknown enum constant should read as unoverridden",
                        m -> !capabilityTrait(m).capabilityIO.top.isOverridden())
                .check("and the definition value should be in force",
                        m -> capabilityTrait(m).capabilityIO.top.get() == IO.BOTH)
                .succeed();
    }

    /** Clearing an override has to remove it from NBT, not write a placeholder. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void cleared_override_is_absent_from_nbt(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.MACHINE_ID, POS)
                .with(m -> m.setMachineLevel(4))
                .with(m -> m.clearMachineLevel())
                .assertPersistenceRoundTrip(tag -> {
                    var values = RuntimeNbt.machineValuesIfPresent(tag);
                    if (values != null && values.contains("machine_level")) {
                        h.fail("a cleared override should not be written, got " + values);
                    }
                })
                .succeed();
    }

    /**
     * Trait overrides are namespaced by trait name, so two traits of the same type on one machine keep
     * separate state. The trait name is the persisted prefix, which is exactly why
     * {@code TestMachineBuilder} renames the second item-slot trait.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void two_traits_of_a_type_keep_separate_overrides(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.TWO_SLOT_TRAITS_ID, POS)
                .with(m -> trait(m, RuntimeValueFixtures.ITEM_SLOT_TRAIT).capabilityIO.top.set(IO.NONE))
                .assertPersistenceRoundTrip(tag -> {
                    var first = RuntimeNbt.traitValuesIfPresent(tag, RuntimeValueFixtures.ITEM_SLOT_TRAIT);
                    if (first == null || !first.contains("capability_io.top")) {
                        h.fail("the override should be saved under the first trait's prefix, got " + first);
                    }
                    var second = RuntimeNbt.traitValuesIfPresent(tag, RuntimeValueFixtures.SECOND_ITEM_SLOT_TRAIT);
                    if (second != null && second.contains("capability_io.top")) {
                        h.fail("the second trait should have no override, got " + second);
                    }
                })
                .check("the overridden trait keeps its override",
                        m -> trait(m, RuntimeValueFixtures.ITEM_SLOT_TRAIT).capabilityIO.top.get() == IO.NONE)
                .check("the other trait stays on the definition",
                        m -> trait(m, RuntimeValueFixtures.SECOND_ITEM_SLOT_TRAIT).capabilityIO.top.get() == IO.BOTH)
                .succeed();
    }

    /**
     * Vanilla reads a block entity's NBT <em>before</em> attaching it to a level, so every hook that
     * fires from deserialization has to survive a null level. The signal connection hook reaches
     * {@code MBDMachine.updateSignal}, which used to dereference {@code getLevel()} unguarded.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void world_load_order_survives_hooks_with_no_level(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.SIGNAL_MACHINE_ID, POS)
                .with(m -> m.signalConnection.top.set(true))
                .with(m -> capabilityTrait(m).capabilityIO.top.set(IO.NONE))
                .assertWorldLoadRoundTrip()
                .check("the signal override survived a world-load ordered reload",
                        m -> m.signalConnection.top.get())
                .check("so did the capability override",
                        m -> capabilityTrait(m).capabilityIO.top.get() == IO.NONE)
                .succeed();
    }

    /**
     * The {@code @LazyManaged} contract: nothing sweeps the storage for changes, so
     * {@code RuntimeValue.set} has to mark it dirty itself. If it did not, an override would look fine
     * in memory and be gone after a restart — and a test that saves the block entity explicitly, as
     * {@code assertPersistenceRoundTrip} does, would never notice.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void override_marks_the_chunk_for_saving(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(RuntimeValueFixtures.MACHINE_ID, POS);
        var chunk = h.getLevel().getChunkAt(h.absolutePos(POS));

        chunk.setUnsaved(false);
        scenario.with(m -> m.setMachineLevel(6));
        if (!chunk.isUnsaved()) {
            h.fail("setting a runtime value must mark the chunk unsaved, or the override is lost on restart");
        }

        chunk.setUnsaved(false);
        scenario.with(m -> m.clearMachineLevel());
        if (!chunk.isUnsaved()) {
            h.fail("clearing a runtime value must mark the chunk unsaved too");
        }
        scenario.succeed();
    }

    /**
     * The same guarantee for a <b>trait's</b> storage, which is a different {@code FieldManagedStorage}
     * with its own {@code ManagedFieldHolder} and reaches the block entity through
     * {@code Trait.asBlockEntity()}. This is exactly the path that would have broken silently if the
     * hand-written {@code MANAGED_FIELD_HOLDER}s had been left in place, and
     * {@code assertPersistenceRoundTrip} cannot catch it because it saves the block entity explicitly.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void a_trait_override_marks_the_chunk_for_saving(GameTestHelper h) {
        var scenario = MBDScenario.of(h).placeMachine(RuntimeValueFixtures.MACHINE_ID, POS);
        var chunk = h.getLevel().getChunkAt(h.absolutePos(POS));

        chunk.setUnsaved(false);
        scenario.with(m -> itemTrait(m).slotLimit.set(2));
        if (!chunk.isUnsaved()) {
            h.fail("a trait override must mark the chunk unsaved, or it is lost on restart");
        }
        scenario.succeed();
    }

    /** Writing the same value twice is a no-op, so it must not dirty the chunk a second time. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void writing_an_unchanged_value_does_not_dirty(GameTestHelper h) {
        var scenario = MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.MACHINE_ID, POS)
                .with(m -> m.setMachineLevel(6));
        var chunk = h.getLevel().getChunkAt(h.absolutePos(POS));

        chunk.setUnsaved(false);
        scenario.with(m -> m.setMachineLevel(6));
        if (chunk.isUnsaved()) {
            h.fail("re-writing the same value should not mark the chunk unsaved");
        }
        scenario.succeed();
    }

    /**
     * The AABB slot is the only compound codec in the system — every other leaf is a boolean, an int or
     * an enum name, so this is the one payload with real structure to get wrong. The rotation cache in
     * front of it has to pick up the reloaded box too: it keys on the effective value, and a reload
     * replaces that value without going through {@code set}.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void an_aabb_override_round_trips(GameTestHelper h) {
        // deliberately asymmetric and non-integral, so a transposed or truncated field shows up
        var box = new AABB(-2, 0, 1, 3, 1.5, 4);
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.AUTO_IO_MACHINE_ID, POS)
                .with(m -> itemTrait(m).autoWorldInput.range.set(box))
                .assertPersistenceRoundTrip()
                .check("the box survives with its exact bounds",
                        m -> itemTrait(m).autoWorldInput.range.get().equals(box))
                .check("the rotation cache serves the reloaded box unrotated for NORTH",
                        m -> itemTrait(m).autoWorldInput.getRotatedRange(Direction.NORTH).equals(box))
                .check("and rotates the reloaded box, not a stale one",
                        m -> itemTrait(m).autoWorldInput.getRotatedRange(Direction.EAST)
                                .equals(ShapeUtils.rotate(box, Direction.EAST)))
                .succeed();
    }

    /**
     * Runtime values are persistence only: the storage is a {@code @Persisted} field and deliberately
     * <b>not</b> {@code @DescSynced}. A client that wants a client-only value writes it on its own
     * machine instance, where it costs nothing and reaches nobody.
     * <p>
     * Pinned structurally rather than behaviourally, because the failure mode of getting this wrong is
     * silent: adding {@code @DescSynced} would compile, pass every other test, and quietly start
     * shipping auto IO config to every tracking client.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void the_storage_is_persisted_but_never_synced(GameTestHelper h) {
        MBDScenario.of(h)
                .placeMachine(RuntimeValueFixtures.MACHINE_ID, POS)
                .check("the machine's storage is persisted and not synced",
                        m -> isPersistedNotSynced(m.getFieldHolder().getFields()))
                .check("and so is the trait's",
                        m -> isPersistedNotSynced(capabilityTrait(m).getFieldHolder().getFields()))
                .succeed();
    }

    private static boolean isPersistedNotSynced(ManagedKey[] fields) {
        for (var key : fields) {
            if (key.getName().equals("runtimeValues")) {
                return key.isPersist() && !key.isDestSync();
            }
        }
        return false;   // the field vanished or was renamed — markDirty would throw too
    }

    private static ItemSlotCapabilityTrait itemTrait(MBDMachine machine) {
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof ItemSlotCapabilityTrait itemSlot) return itemSlot;
        }
        throw new AssertionError("fixture machine has no item slot trait");
    }

    private static SimpleCapabilityTrait<?, ?> capabilityTrait(MBDMachine machine) {
        for (var t : machine.getAdditionalTraits()) {
            if (t instanceof SimpleCapabilityTrait<?, ?> capabilityTrait) return capabilityTrait;
        }
        throw new AssertionError("fixture machine has no capability trait");
    }

    private static SimpleCapabilityTrait<?, ?> trait(MBDMachine machine, String name) {
        var t = machine.getTraitByName(name);
        if (t instanceof SimpleCapabilityTrait<?, ?> capabilityTrait) return capabilityTrait;
        throw new AssertionError("no capability trait named " + name + " on " + machine.getDefinition().id());
    }
}
