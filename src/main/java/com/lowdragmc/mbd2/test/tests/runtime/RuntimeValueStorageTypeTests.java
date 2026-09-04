package com.lowdragmc.mbd2.test.tests.runtime;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.runtime.RuntimeValueStorage;
import com.lowdragmc.mbd2.test.framework.MBDScenario;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The value <em>types</em> a {@link RuntimeValueStorage} can hold: their codecs, and the coercion that
 * lets a script or a blueprint node write one without knowing which it is.
 *
 * <p>Exercised on a storage the test builds itself rather than through a machine's slots, because most
 * of the types have no slot on a mod-independent trait — {@code max_pressure} is PneumaticCraft's, and
 * nothing in the base mod holds a {@code long} or a bare {@code String}. Building one here covers every
 * factory the same way and keeps the test honest about what it is testing: the storage, not a trait.</p>
 *
 * <p>A real machine is still the holder, so {@code markDirty} has somewhere to go — a storage with a
 * mock holder would not exercise the same {@code onSlotChanged} path.</p>
 */
@GameTestHolder(MBD2.MOD_ID)
public class RuntimeValueStorageTypeTests {
    static { @SuppressWarnings("unused") var ignored = RuntimeValueFixtures.MACHINE_ID; }

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    /** Every factory round-trips through NBT, and an absent key still means "not overridden". */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void every_value_type_round_trips_through_nbt(GameTestHelper h) {
        withStorage(h, storage -> {
            var flag = storage.ofBool("flag", () -> false);
            var count = storage.ofInt("count", () -> 1);
            var big = storage.ofLong("big", () -> 1L);
            var ratio = storage.ofFloat("ratio", () -> 1f);
            var precise = storage.ofDouble("precise", () -> 1d);
            var label = storage.ofString("label", () -> "authored");
            var names = storage.ofStringList("names", List::of);
            var direction = storage.ofEnum("direction", IO.class, () -> IO.NONE);
            var box = storage.ofAABB("box", () -> new AABB(0, 0, 0, 1, 1, 1));

            flag.set(true);
            count.set(7);
            big.set(9_000_000_000L);
            ratio.set(2.5f);
            precise.set(0.125d);
            label.set("overridden");
            names.set(List.of("alpha", "beta"));
            direction.set(IO.OUT);
            var expectedBox = new AABB(-1, -2, -3, 4, 5, 6);
            box.set(expectedBox);

            var provider = h.getLevel().registryAccess();
            var tag = storage.serializeNBT(provider);
            storage.clearAll();
            if (flag.isOverridden() || names.isOverridden()) {
                h.fail("clearAll should have dropped every override before the reload");
            }
            storage.deserializeNBT(provider, tag);

            assertEquals(h, "flag", true, flag.get());
            assertEquals(h, "count", 7, count.get());
            assertEquals(h, "big", 9_000_000_000L, big.get());
            assertEquals(h, "ratio", 2.5f, ratio.get());
            assertEquals(h, "precise", 0.125d, precise.get());
            assertEquals(h, "label", "overridden", label.get());
            assertEquals(h, "names", List.of("alpha", "beta"), names.get());
            assertEquals(h, "direction", IO.OUT, direction.get());
            assertEquals(h, "box", expectedBox, box.get());
        });
        h.succeed();
    }

    /**
     * The script and blueprint entry point. Rhino hands every JS number over as a {@code Double} and a
     * blueprint's text node only ever has a {@code String}, so a value has to be converted to whatever
     * the slot holds rather than rejected.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void untyped_values_are_coerced_to_the_slot_type(GameTestHelper h) {
        withStorage(h, storage -> {
            var big = storage.ofLong("big", () -> 0L);
            var ratio = storage.ofFloat("ratio", () -> 0f);
            var precise = storage.ofDouble("precise", () -> 0d);
            var label = storage.ofString("label", () -> "");
            var names = storage.ofStringList("names", List::of);
            var direction = storage.ofEnum("direction", IO.class, () -> IO.NONE);

            // a whole number arriving as a Double, which is all Rhino ever produces
            big.setValue(42.0);
            assertEquals(h, "long from a double", 42L, big.get());
            // and a fraction, which the integral slots refuse but these keep
            ratio.setValue(2.5);
            assertEquals(h, "float from a double", 2.5f, ratio.get());
            precise.setValue(3);
            assertEquals(h, "double from an int", 3d, precise.get());

            label.setValue("hello");
            assertEquals(h, "string", "hello", label.get());
            direction.setValue("out");
            assertEquals(h, "enum by name, case-insensitively", IO.OUT, direction.get());

            // the form a blueprint's text node and a KubeJS string literal produce
            names.setValue(" gamma , delta ,, ");
            assertEquals(h, "list from a comma-separated string", List.of("gamma", "delta"), names.get());
            names.setValue(new String[]{"one", "two"});
            assertEquals(h, "list from an array", List.of("one", "two"), names.get());
        });
        h.succeed();
    }

    /**
     * A list slot must copy what it is given, both ways.
     *
     * <p>{@code serializeNBT} runs on LDLib's async persistence thread while writes come from the game
     * thread; a slot holding the caller's own {@code ArrayList} would be a data race whose cause is
     * nowhere near the crash.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void list_values_are_copied_in_and_handed_out_unmodifiable(GameTestHelper h) {
        withStorage(h, storage -> {
            var names = storage.ofStringList("names", List::of);
            var mutable = new ArrayList<>(List.of("alpha"));

            names.setValue(mutable);
            mutable.add("beta");
            assertEquals(h, "the slot should have copied the caller's list", List.of("alpha"), names.get());

            try {
                names.get().add("gamma");
                h.fail("a stored list should be unmodifiable");
            } catch (UnsupportedOperationException expected) {
                // what we want
            }

            // the fallback is copied too, so a definition handing back a mutable list is equally safe
            var authored = new ArrayList<>(List.of("authored"));
            var fromDefinition = storage.ofStringList("from_definition", () -> authored);
            var readBack = fromDefinition.get();
            authored.add("added later");
            assertEquals(h, "the fallback should have been copied", List.of("authored"), readBack);
        });
        h.succeed();
    }

    /** A value the slot cannot hold is refused loudly, rather than silently stored as something else. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void unusable_values_are_refused(GameTestHelper h) {
        withStorage(h, storage -> {
            var count = storage.ofInt("count", () -> 0);
            var flag = storage.ofBool("flag", () -> false);
            var direction = storage.ofEnum("direction", IO.class, () -> IO.NONE);
            var names = storage.ofStringList("names", List::of);

            assertRefused(h, "a fraction into a whole-number slot", () -> count.setValue(7.5));
            assertRefused(h, "a typo'd boolean", () -> flag.setValue("ture"));
            assertRefused(h, "an unknown enum constant", () -> direction.setValue("sideways"));
            assertRefused(h, "a number into a list slot", () -> names.setValue(7));
            assertRefused(h, "an unknown key", () -> storage.set("nope", 1));

            if (count.isOverridden() || flag.isOverridden() || names.isOverridden()) {
                h.fail("a refused write must not leave an override behind");
            }
        });
        h.succeed();
    }

    // region helpers

    /**
     * Run {@code body} against a storage of its own, hung off a real machine.
     *
     * <p>Slots are normally registered from an owner's field initialisers and never afterwards; a
     * storage created here is not the machine's own, so registering on it cannot disturb anything the
     * machine actually reads.</p>
     */
    private static void withStorage(GameTestHelper h, Consumer<RuntimeValueStorage> body) {
        var machine = MBDScenario.of(h).placeMachine(RuntimeValueFixtures.MACHINE_ID, POS).machine();
        body.accept(new RuntimeValueStorage(machine));
    }

    private static void assertEquals(GameTestHelper h, String what, Object expected, Object actual) {
        if (!expected.equals(actual)) {
            h.fail("%s: expected %s, got %s".formatted(what, expected, actual));
        }
    }

    private static void assertRefused(GameTestHelper h, String what, Runnable write) {
        try {
            write.run();
            h.fail(what + " should have been refused");
        } catch (IllegalArgumentException expected) {
            // what we want
        }
    }

    // endregion
}
