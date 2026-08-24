package com.lowdragmc.mbd2.integration.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import com.lowdragmc.kilagraph.graph.core.AnnotatedNode;
import com.lowdragmc.kilagraph.graph.core.InputPort;
import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.EvalContext;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.MachineEnvironment;
import com.lowdragmc.mbd2.common.blueprint.node.MachineActionNode;
import com.lowdragmc.mbd2.common.blueprint.node.trait.TraitCapabilityNode;
import net.minecraft.world.item.ItemStack;

/**
 * Blueprint nodes for an AE2 network reached through a machine's ME interface or pattern provider trait.
 *
 * <p>Gated by {@code modID = "ae2"}; see {@code MekanismBlueprintNodes} for why that makes the AE2 types
 * in these signatures safe on an install without it.</p>
 *
 * <h2>Counting by simulated extraction</h2>
 * {@link Count} asks the network for a simulated extraction rather than walking
 * {@code getAvailableStacks()}. The latter materialises the whole network inventory into a
 * {@code KeyCounter} — fine once in a GUI, ruinous on a node a blueprint runs every tick against a
 * large network. A simulated extract answers the same question at the cost of one lookup, and is what
 * AE2's own machines do.
 */
public final class AE2BlueprintNodes {

    private static final String GROUP = "mbd2/trait/ae2";
    private static final String MOD = "ae2";

    private AE2BlueprintNodes() {}

    /** A trait's ME network view, as AE2's {@code MEStorage}. */
    @NodeAttribute(name = "mbd2_ae2_me_storage", group = GROUP, modID = MOD,
            graphTypes = MachineBlueprintGraph.class)
    public static class MEStorageOf extends TraitCapabilityNode<MEStorage> {
        @OutputPort public MEStorage value;
        @OutputPort public boolean found;

        @Override
        protected Class<MEStorage> capabilityClass() {
            return MEStorage.class;
        }
    }

    /** How many of an item the network holds. */
    @NodeAttribute(name = "mbd2_ae2_count", group = GROUP, modID = MOD,
            graphTypes = MachineBlueprintGraph.class)
    public static class Count extends AnnotatedNode {
        @InputPort public MEStorage storage;
        @InputPort public ItemStack item = ItemStack.EMPTY;
        @OutputPort public long count;

        @Override
        public void evaluate(EvalContext ctx) {
            var storage = ctx.getInput("storage", MEStorage.class, null);
            var item = ctx.getInput("item", ItemStack.class, ItemStack.EMPTY);
            if (storage == null || item == null || item.isEmpty()) return;
            var key = AEItemKey.of(item);
            if (key == null) return;
            ctx.setOutput("count",
                    storage.extract(key, Long.MAX_VALUE, Actionable.SIMULATE, IActionSource.empty()));
        }
    }

    /** Push items into the network. Reports how many were accepted. */
    @NodeAttribute(name = "mbd2_ae2_insert", group = GROUP, modID = MOD,
            graphTypes = MachineBlueprintGraph.class)
    public static class Insert extends MachineActionNode {
        @InputPort public MEStorage storage;
        @InputPort public ItemStack stack = ItemStack.EMPTY;
        @InputPort public boolean simulate = false;
        @OutputPort public long inserted;

        @Override
        protected void run(ExecContext ctx, MachineEnvironment env) {
            var storage = ctx.getInput("storage", MEStorage.class, null);
            var stack = ctx.getInput("stack", ItemStack.class, ItemStack.EMPTY);
            if (storage == null || stack == null || stack.isEmpty()) return;
            var key = AEItemKey.of(stack);
            if (key == null) return;
            var mode = ctx.getInput("simulate", Boolean.class, false)
                    ? Actionable.SIMULATE : Actionable.MODULATE;
            ctx.setOutput("inserted", storage.insert(key, stack.getCount(), mode, IActionSource.empty()));
        }
    }

    /** Pull items out of the network. Reports how many were actually available. */
    @NodeAttribute(name = "mbd2_ae2_extract", group = GROUP, modID = MOD,
            graphTypes = MachineBlueprintGraph.class)
    public static class Extract extends MachineActionNode {
        @InputPort public MEStorage storage;
        @InputPort public ItemStack item = ItemStack.EMPTY;
        @InputPort public long amount = 1;
        @InputPort public boolean simulate = false;
        @OutputPort public long extracted;

        @Override
        protected void run(ExecContext ctx, MachineEnvironment env) {
            var storage = ctx.getInput("storage", MEStorage.class, null);
            var item = ctx.getInput("item", ItemStack.class, ItemStack.EMPTY);
            long amount = ctx.getInput("amount", Long.class, 0L);
            if (storage == null || item == null || item.isEmpty() || amount <= 0) return;
            var key = AEItemKey.of(item);
            if (key == null) return;
            var mode = ctx.getInput("simulate", Boolean.class, false)
                    ? Actionable.SIMULATE : Actionable.MODULATE;
            ctx.setOutput("extracted", storage.extract(key, amount, mode, IActionSource.empty()));
        }
    }
}
