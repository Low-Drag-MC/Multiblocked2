package com.lowdragmc.mbd2.test.tests.fx;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState;
import com.lowdragmc.mbd2.common.machine.definition.config.fx.MachineFXConfig;
import com.lowdragmc.mbd2.common.machine.definition.config.fx.ToggleMachineFXs;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Vector3f;

import java.io.DataInputStream;

/**
 * The machine FX <em>configuration</em> — which is deliberately Photon-free, and so is testable
 * headlessly on a dedicated server whether or not Photon is installed.
 *
 * <p>That is the property worth pinning: a definition authored with Photon has to load, round-trip
 * and save unchanged on a server without it, or a pack would quietly lose every effect the first
 * time it was saved by a server owner who did not install the mod.</p>
 *
 * @see MachineFXRuntimeTests for the side/no-op half
 */
@GameTestHolder(MBD2.MOD_ID)
public class MachineFXConfigTests {

    private static MachineFXConfig sample() {
        var fx = new MachineFXConfig("burst", ResourceLocation.fromNamespaceAndPath("photon", "fire"));
        fx.setOffset(new Vector3f(0.25f, 1.5f, -0.5f));
        fx.setRotation(new Vector3f(0, 90, 0));
        fx.setScale(new Vector3f(2, 2, 2));
        fx.setDelay(7);
        fx.setForcedDeath(true);
        fx.setReplaceExisting(true);
        fx.setFollowFacing(false);
        fx.setMaxDistance(32);
        return fx;
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void fx_config_round_trips_every_field(GameTestHelper h) {
        var provider = Platform.getFrozenRegistry();
        var original = sample();

        var copy = new MachineFXConfig();
        copy.deserializeNBT(provider, original.serializeNBT(provider));

        if (!original.getName().equals(copy.getName())) h.fail("name did not round trip");
        if (!original.getFxLocation().equals(copy.getFxLocation())) h.fail("fx location did not round trip");
        if (!original.getOffset().equals(copy.getOffset())) h.fail("offset did not round trip");
        if (!original.getRotation().equals(copy.getRotation())) h.fail("rotation did not round trip");
        if (!original.getScale().equals(copy.getScale())) h.fail("scale did not round trip");
        if (original.getDelay() != copy.getDelay()) h.fail("delay did not round trip");
        if (original.isForcedDeath() != copy.isForcedDeath()) h.fail("forcedDeath did not round trip");
        if (original.isReplaceExisting() != copy.isReplaceExisting()) h.fail("replaceExisting did not round trip");
        if (original.isFollowFacing() != copy.isFollowFacing()) h.fail("followFacing did not round trip");
        if (original.getMaxDistance() != copy.getMaxDistance()) h.fail("maxDistance did not round trip");

        h.succeed();
    }

    /**
     * The list is final, so it can only round trip if the persisted form carries its size — the same
     * {@code @ReadOnlyManaged} shape {@code ConfigMachineSettings.blueprints} uses. Getting this wrong
     * loses every entry but the ones that happen to already exist.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void toggle_list_round_trips_all_entries(GameTestHelper h) {
        var provider = Platform.getFrozenRegistry();
        var original = new ToggleMachineFXs();
        original.setEnable(true);
        original.getFxs().add(sample());
        original.getFxs().add(new MachineFXConfig("smoke",
                ResourceLocation.fromNamespaceAndPath("photon", "smoke")));

        var copy = new ToggleMachineFXs();
        copy.deserializeNBT(provider, original.serializeNBT(provider));

        if (!copy.isEnable()) h.fail("enable flag did not round trip");
        if (copy.getFxs().size() != 2) {
            h.fail("expected 2 effects after round trip, got " + copy.getFxs().size());
        }
        if (!"burst".equals(copy.getFxs().getFirst().getName())
                || !"smoke".equals(copy.getFxs().getLast().getName())) {
            h.fail("effect order or names did not round trip: " + copy.getFxs().stream()
                    .map(MachineFXConfig::getName).toList());
        }
        if (copy.getFxs().getFirst().getDelay() != 7) {
            h.fail("per-entry fields did not round trip inside the list");
        }
        h.succeed();
    }

    /**
     * A disabled toggle means "whatever my parent says", exactly like {@code renderer} and
     * {@code machineSound}. A state that genuinely wants nothing enables the toggle and leaves the
     * list empty — without that distinction there would be no way to silence a child state.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void disabled_state_inherits_parent_effects(GameTestHelper h) {
        var root = MachineState.baseBuilder().build();
        var child = root.addChild("working");
        var grandchild = child.addChild("waiting");

        root.machineFXs().setEnable(true);
        root.machineFXs().getFxs().add(new MachineFXConfig("idle",
                ResourceLocation.fromNamespaceAndPath("photon", "idle")));

        // child leaves its toggle off -> inherits
        if (child.getRealMachineFXs().size() != 1
                || !"idle".equals(child.getRealMachineFXs().getFirst().getName())) {
            h.fail("a disabled child state should inherit its parent's effects");
        }
        // and inheritance walks all the way up, not just one level
        if (grandchild.getRealMachineFXs().size() != 1) {
            h.fail("inheritance should walk the whole state chain");
        }

        child.machineFXs().setEnable(true);
        if (!child.getRealMachineFXs().isEmpty()) {
            h.fail("an enabled child with an empty list should show nothing, not inherit");
        }
        if (grandchild.getRealMachineFXs().size() != 0) {
            h.fail("the grandchild should now inherit the child's empty list");
        }
        if (root.getRealMachineFXs().size() != 1) {
            h.fail("the parent's own effects must be unaffected");
        }
        h.succeed();
    }

    /** The root falls back to nothing rather than to null — callers iterate it every state change. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void unconfigured_root_has_no_effects(GameTestHelper h) {
        var root = MachineState.baseBuilder().build();
        if (!root.getRealMachineFXs().isEmpty()) {
            h.fail("an unconfigured root state should have no effects");
        }
        h.succeed();
    }

    /** The named library is what the blueprint nodes resolve against. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void library_lookup_finds_entries_by_name(GameTestHelper h) {
        // machineSettings is behind a factory until loadFactory runs — a bare createDefault() has none.
        var definition = MBDMachineDefinition.createDefault();
        definition.loadFactory();
        definition.machineSettings().photonFXs().add(sample());

        if (definition.machineSettings().findFX("burst") == null) {
            h.fail("findFX should resolve a library entry by name");
        }
        if (definition.machineSettings().findFX("nope") != null) {
            h.fail("findFX should return null for an unknown name");
        }
        h.succeed();
    }

    /**
     * The whole point of keeping the config Photon-free: a definition survives a save/load on a
     * server that has never heard of Photon.
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void definition_round_trips_state_and_library_effects(GameTestHelper h) {
        var provider = Platform.getFrozenRegistry();
        var original = MBDMachineDefinition.createDefault();
        original.loadFactory();
        original.machineSettings().photonFXs().add(sample());
        var working = original.stateMachine().getRootState().addChild("working");
        working.machineFXs().setEnable(true);
        working.machineFXs().getFxs().add(new MachineFXConfig("smoke",
                ResourceLocation.fromNamespaceAndPath("photon", "smoke")));

        var copy = MBDMachineDefinition.createDefault();
        copy.loadFactory();
        copy.deserializeNBT(provider, original.serializeNBT(provider));

        if (copy.machineSettings().photonFXs().size() != 1
                || !"burst".equals(copy.machineSettings().photonFXs().getFirst().getName())) {
            h.fail("the machine FX library did not survive the definition round trip");
        }
        var copiedWorking = copy.stateMachine().getState("working");
        if (copiedWorking == null || copiedWorking == copy.stateMachine().getRootState()) {
            h.fail("the working state did not survive the definition round trip");
            return;
        }
        if (copiedWorking.getRealMachineFXs().size() != 1
                || !"smoke".equals(copiedWorking.getRealMachineFXs().getFirst().getName())) {
            h.fail("per-state effects did not survive the definition round trip");
        }
        if (!copy.stateMachine().getRootState().getRealMachineFXs().isEmpty()) {
            h.fail("the root state should still have no effects of its own");
        }
        h.succeed();
    }

    /**
     * A definition saved by an older build still loads its effects.
     *
     * <h2>Why a recorded file rather than a round trip</h2>
     * {@link #definition_round_trips_state_and_library_effects} writes and reads with the same code,
     * so it passes even if the persisted key for a field changes — both halves move together and
     * every project already on disk is silently emptied. This one reads bytes captured before the FX
     * lists were moved out of the inspectors ({@code @Configurable} to {@code @Persisted}), which is
     * exactly the change that could rename a key, so it is the only assertion here that can catch it.
     *
     * <p>{@code data/mbd2/test/fx_golden_definition.nbt} is a default definition carrying one library
     * entry ({@code burst}) and a {@code working} state with one of its own ({@code smoke}).</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void definition_saved_by_an_older_build_still_loads_its_effects(GameTestHelper h) {
        CompoundTag golden;
        try (var stream = MachineFXConfigTests.class
                .getResourceAsStream("/data/mbd2/test/fx_golden_definition.nbt")) {
            if (stream == null) {
                h.fail("fx_golden_definition.nbt is not on the classpath");
                return;
            }
            golden = NbtIo.read(new DataInputStream(stream), NbtAccounter.unlimitedHeap());
        } catch (Exception e) {
            h.fail("could not read the recorded definition: " + e);
            return;
        }

        var definition = MBDMachineDefinition.createDefault();
        definition.loadFactory();
        definition.deserializeNBT(Platform.getFrozenRegistry(), golden);

        var library = definition.machineSettings().photonFXs();
        if (library.size() != 1 || !"burst".equals(library.getFirst().getName())) {
            h.fail("the FX library was lost loading a definition saved by an older build: " + library.stream()
                    .map(MachineFXConfig::getName).toList());
            return;
        }
        // A per-entry field too: a list that survives as the right length but empty entries would
        // otherwise read as a pass.
        if (library.getFirst().getDelay() != 7) {
            h.fail("library entry fields were lost: delay=" + library.getFirst().getDelay());
        }
        var working = definition.stateMachine().getState("working");
        if (working == null || working == definition.stateMachine().getRootState()) {
            h.fail("the working state was lost loading a definition saved by an older build");
            return;
        }
        if (!working.machineFXs().isEnable()) {
            h.fail("the per-state FX toggle was lost");
        }
        if (working.getRealMachineFXs().size() != 1
                || !"smoke".equals(working.getRealMachineFXs().getFirst().getName())) {
            h.fail("per-state effects were lost loading a definition saved by an older build");
        }
        h.succeed();
    }
}
