package com.lowdragmc.mbd2.test.tests.blueprint;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.VariableKind;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.blueprint.builtin.BuiltinBlueprints;
import com.lowdragmc.mbd2.common.gui.editor.blueprint.MachineBlueprintResource;
import com.lowdragmc.mbd2.common.machine.definition.config.blueprint.MachineBlueprintBinding;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * The built-in blueprints, checked the only way a code-built graph can be.
 *
 * <h2>Why these are worth a test</h2>
 * A built-in blueprint is built by calling into the node model, so every mistake in it — a port id that
 * no longer exists, an ambiguous bare reference, a variable of a type with no handle — is a
 * {@link RuntimeException} at build time rather than a compile error. Nothing else would catch it: the
 * graphs are built lazily the first time the resource is touched, so without a test the first person to
 * find out would be a player opening the blueprint library.
 *
 * <p>What they assert is that each one builds, survives NBT, and exposes the parameters it advertises —
 * the part that silently rots when a node changes. Whether a blueprint actually <em>does</em> what its
 * notes claim is {@link BuiltinBlueprintBehaviourTests}, which places real machines: all of the above
 * can be true of a graph that is wired wrongly and does nothing.</p>
 */
@GameTestHolder(MBD2.MOD_ID)
public class BuiltinBlueprintTests {
    static { @SuppressWarnings("unused") var ignored = BlueprintFixtures.PLAIN_MACHINE_ID; }

    /** Names and the parameters each is expected to expose — the contract its documentation states. */
    private static final List<Expected> EXPECTED = List.of(
            new Expected("redstone_control", List.of("requiresSignal", "threshold")),
            new Expected("comparator_progress", List.of("invert")),
            new Expected("environment_gate", List.of("needsRain")),
            new Expected("overclock", List.of("speedPerTier", "costPerTier", "maxOverclocks")),
            new Expected("upgrade_slots", List.of("traitName", "slot", "upgradeItem",
                    "speedPerUpgrade", "maxUpgrades")),
            new Expected("part_count_bonus", List.of("speedPerPart", "maxSpeedup")),
            new Expected("upkeep", List.of("traitName", "amountPerTick", "reason")),
            new Expected("chance_output", List.of("bonusItem", "chance", "traitName")),
            new Expected("heat_buildup", List.of("heatPerTick", "coolPerTick", "maxHeat",
                    "bonusAtMaxHeat")),
            new Expected("debug_probe", List.of("probeItem")));

    private record Expected(String name, List<String> parameters) {}

    /**
     * Every built-in is present in the builtin provider.
     *
     * <p>{@code BuiltinBlueprints.register} logs and skips a blueprint that throws, so a broken one is
     * a missing entry rather than an exception anywhere — which is the right behaviour at runtime and
     * exactly why it has to be asserted here.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void everyBuiltinIsRegistered(GameTestHelper helper) {
        var missing = new ArrayList<String>();
        for (var expected : EXPECTED) {
            if (resolve(expected.name()) == null) {
                missing.add(expected.name());
            }
        }
        if (!missing.isEmpty()) {
            helper.fail("built-in blueprint(s) missing — see the log for the build failure: " + missing);
            return;
        }
        helper.succeed();
    }

    /**
     * Every built-in loads back into a live graph.
     *
     * <p>Through {@link MachineBlueprintBinding} rather than the resource directly, because that is the
     * path a placed machine takes: resolve the path, read the tag, deserialize. A graph that serializes
     * but does not read back is exactly as broken as one that never built.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void everyBuiltinLoadsBack(GameTestHelper helper) {
        var failures = new ArrayList<String>();
        for (var expected : EXPECTED) {
            var binding = new MachineBlueprintBinding();
            binding.setBlueprintPath(BuiltinBlueprints.path(expected.name()));
            if (!binding.hasBlueprint()) {
                failures.add(expected.name() + ": binding does not consider the path set");
                continue;
            }
            try {
                var graph = binding.loadGraph();
                if (graph == null) {
                    failures.add(expected.name() + ": loadGraph returned null");
                } else if (graph.graphModel.getNodeModels().isEmpty()) {
                    failures.add(expected.name() + ": loaded with no nodes");
                }
            } catch (Throwable t) {
                failures.add(expected.name() + ": " + t);
            }
        }
        if (!failures.isEmpty()) {
            helper.fail(failures.size() + " built-in(s) failed to load: " + String.join(" | ", failures));
            return;
        }
        helper.succeed();
    }

    /**
     * Every built-in exposes the parameters its notes say it does.
     *
     * <p>The parameters are the whole interface: a built-in that loses one still runs, still looks
     * right on the canvas, and silently ignores the value someone set in the machine editor. The names
     * are also what a machine definition stores its overrides under, so renaming one is a breaking
     * change and should have to be made here too.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void everyBuiltinExposesItsParameters(GameTestHelper helper) {
        var failures = new ArrayList<String>();
        for (var expected : EXPECTED) {
            var binding = new MachineBlueprintBinding();
            binding.setBlueprintPath(BuiltinBlueprints.path(expected.name()));
            var graph = binding.loadGraph();
            if (graph == null) continue; // already reported by everyBuiltinLoadsBack
            var actual = MachineBlueprintBinding.inputVariables(graph).stream()
                    .map(variable -> variable.getName())
                    .toList();
            for (var parameter : expected.parameters()) {
                if (!actual.contains(parameter)) {
                    failures.add(expected.name() + " is missing '" + parameter + "' (has " + actual + ")");
                }
            }
            // Seeding is what the executor actually does with them, and it is a separate failure mode:
            // a variable declared with a default nothing can materialise resolves to nothing here.
            var seeded = binding.resolveVariableValues(graph, Platform.getFrozenRegistry());
            for (var parameter : expected.parameters()) {
                if (actual.contains(parameter) && !seeded.containsKey(parameter)) {
                    failures.add(expected.name() + "'s '" + parameter + "' did not seed a value");
                }
            }
        }
        if (!failures.isEmpty()) {
            helper.fail(String.join(" | ", failures));
            return;
        }
        helper.succeed();
    }

    /**
     * No built-in is pathologically large.
     *
     * <p>A graph's element count is nodes <em>and every port and wire</em>, so it runs well ahead of
     * what the canvas shows — but only by a constant factor. A built-in that blows past this bound is
     * generating models rather than declaring them, which is a bug that does not look like one: the
     * canvas renders correctly and the graph behaves, while the editor pushes thousands of elements
     * through its per-tick change set. That is how the {@code heat_buildup} crash was found.</p>
     */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void noBuiltinIsPathologicallyLarge(GameTestHelper helper) {
        var sizes = new java.util.LinkedHashMap<String, Integer>();
        for (var expected : EXPECTED) {
            var binding = new MachineBlueprintBinding();
            binding.setBlueprintPath(BuiltinBlueprints.path(expected.name()));
            var graph = binding.loadGraph();
            if (graph == null) continue;
            sizes.put(expected.name(), graph.graphModel.getNodeModels().size()
                    + graph.graphModel.getWireModels().size());
        }
        var oversized = sizes.entrySet().stream().filter(e -> e.getValue() > 400).toList();
        if (!oversized.isEmpty()) {
            helper.fail("built-in(s) far larger than they look: " + oversized + " (all: " + sizes + ")");
            return;
        }
        helper.succeed();
    }

    /** Built-ins are read-only: nothing in the editor may write one back. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void builtinsAreReadOnly(GameTestHelper helper) {
        var instance = MachineBlueprintResource.INSTANCE.getResourceInstance();
        var path = IResourcePath.parse(BuiltinBlueprints.path("redstone_control"));
        for (var providers : instance.getBuiltinProviders().values()) {
            for (var provider : providers) {
                if (!provider.hasResource(path)) continue;
                if (provider.canEdit(path) || provider.canRemove(path) || provider.canRename(path)
                        || provider.supportAdd()) {
                    helper.fail("the built-in blueprint provider is writable");
                    return;
                }
                if (!provider.canCopy(path)) {
                    // Copy is how a player forks one into their own library; without it a built-in is
                    // a dead end rather than a starting point.
                    helper.fail("a built-in blueprint cannot be copied out");
                    return;
                }
                helper.succeed();
                return;
            }
        }
        helper.fail("no builtin provider owns " + path);
    }

    /** An INPUT variable is what makes a parameter; nothing else should be declared as one. */
    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void builtinsDeclareNoStrayInputVariables(GameTestHelper helper) {
        var failures = new ArrayList<String>();
        for (var expected : EXPECTED) {
            var binding = new MachineBlueprintBinding();
            binding.setBlueprintPath(BuiltinBlueprints.path(expected.name()));
            var graph = binding.loadGraph();
            if (graph == null) continue;
            for (var variable : graph.graphModel.getGraphVariableModels()) {
                if (variable == null || variable.getVariableKind() != VariableKind.INPUT) continue;
                if (!expected.parameters().contains(variable.getName())) {
                    failures.add(expected.name() + " exposes an undocumented parameter '"
                            + variable.getName() + "'");
                }
            }
        }
        if (!failures.isEmpty()) {
            helper.fail(String.join(" | ", failures));
            return;
        }
        helper.succeed();
    }

    private static net.minecraft.nbt.CompoundTag resolve(String name) {
        var path = IResourcePath.parse(BuiltinBlueprints.path(name));
        return path == null ? null : MachineBlueprintResource.INSTANCE.getResourceInstance().getResource(path);
    }
}
