package com.lowdragmc.mbd2.common.blueprint.builtin;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.resource.BuiltinResourceProvider;
import com.lowdragmc.mbd2.MBD2;
import net.minecraft.nbt.CompoundTag;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The blueprints MBD2 ships — worked examples that are also useful on their own.
 *
 * <h2>What belongs here</h2>
 * One job each, and a job most packs want. They are meant to be <em>stacked</em>: a machine's blueprint
 * list is an ordered pipeline, so redstone control plus an overclock plus a bonus output is three
 * bindings rather than one blueprint that does all three with switches. Anything that would need a
 * "mode" parameter to be worth shipping is two blueprints.
 *
 * <p>Everything they vary is an {@code INPUT} graph variable, which the machine editor turns into a
 * configurator row — so the common case is picking one and filling in numbers, with no graph editing at
 * all. The graph is there for the case after that: open it, read the sticky notes, copy it into your own
 * library and change it.</p>
 *
 * <h2>Why a builtin provider rather than files on disk</h2>
 * These are documentation, and documentation that a player can edit in place stops being documentation
 * the first time they do. Held in memory by a {@link BuiltinResourceProvider} they cannot be renamed,
 * deleted or saved over; the editor opens them read-only, and "Copy" is the supported way to fork one
 * into a writable library. It also means a machine referencing {@code built-in(mbd2:redstone_control)}
 * resolves on a dedicated server with no content directory at all.
 *
 * <h2>Failure is per blueprint</h2>
 * Each graph is built inside its own try/catch. A blueprint that cannot be built — a node whose ports
 * moved, a mod-gated node that is not present — is logged and skipped, because losing one built-in is a
 * missing entry in a list and losing the provider is an editor that cannot open blueprints at all.
 */
public final class BuiltinBlueprints {

    /**
     * The provider's name, which is also the namespace in a built-in's path — a machine references
     * {@code built-in(mbd2:redstone_control)}. Namespaced because another mod may add its own built-in
     * blueprint provider, and two blueprints called {@code overclock} must not be the same path.
     */
    public static final String PROVIDER_NAME = MBD2.MOD_ID;

    /** Every built-in, by the name it appears under. Insertion order is the order they are listed in. */
    private static final Map<String, Supplier<BlueprintBuilder>> BLUEPRINTS = new LinkedHashMap<>();

    static {
        // Roughly in order of how much a reader new to blueprints gets out of them: the shortest
        // complete graph first, the stateful one late, the debugging tool last.
        BLUEPRINTS.put("redstone_control", RedstoneControlBlueprint::build);
        BLUEPRINTS.put("comparator_progress", ComparatorProgressBlueprint::build);
        BLUEPRINTS.put("environment_gate", EnvironmentGateBlueprint::build);
        BLUEPRINTS.put("overclock", OverclockBlueprint::build);
        BLUEPRINTS.put("upgrade_slots", UpgradeSlotsBlueprint::build);
        BLUEPRINTS.put("part_count_bonus", PartCountBonusBlueprint::build);
        BLUEPRINTS.put("upkeep", UpkeepBlueprint::build);
        BLUEPRINTS.put("chance_output", ChanceOutputBlueprint::build);
        BLUEPRINTS.put("output_swap", OutputSwapBlueprint::build);
        BLUEPRINTS.put("heat_buildup", HeatBuildupBlueprint::build);
        BLUEPRINTS.put("debug_probe", DebugProbeBlueprint::build);
    }

    private BuiltinBlueprints() {}

    /** Fill {@code provider} with the built-in blueprints. Called once, when the resource is first used. */
    public static void register(BuiltinResourceProvider<CompoundTag> provider) {
        var registries = Platform.getFrozenRegistry();
        for (var entry : BLUEPRINTS.entrySet()) {
            try {
                var graph = entry.getValue().get().getGraph();
                provider.addResource(entry.getKey(), graph.graphModel.serializeNBT(registries));
            } catch (Exception e) {
                MBD2.LOGGER.error("Failed to build built-in blueprint '{}'", entry.getKey(), e);
            }
        }
    }

    /**
     * The path a machine definition references a built-in by, e.g.
     * {@code built-in(mbd2:redstone_control)}.
     *
     * <p>For code that binds a machine to a built-in without going through the editor — datapack and
     * script paths, and the tests.</p>
     */
    public static String path(String name) {
        return "built-in(%s:%s)".formatted(PROVIDER_NAME, name);
    }
}
