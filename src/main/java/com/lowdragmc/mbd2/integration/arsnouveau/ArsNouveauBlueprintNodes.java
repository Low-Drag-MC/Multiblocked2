package com.lowdragmc.mbd2.integration.arsnouveau;

import com.hollingsworth.arsnouveau.api.source.ISpecialSourceProvider;
import com.hollingsworth.arsnouveau.api.util.SourceUtil;
import com.lowdragmc.kilagraph.graph.core.AnnotatedNode;
import com.lowdragmc.kilagraph.graph.core.InputPort;
import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.EvalContext;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.MachineEnvironment;
import com.lowdragmc.mbd2.common.blueprint.node.MachineActionNode;
import com.lowdragmc.mbd2.common.blueprint.node.MachineNodes;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Blueprint nodes for Ars Nouveau's Source.
 *
 * <h2>Why these are world nodes rather than a trait bridge</h2>
 * Same reason as {@code NaturesAuraBlueprintNodes}: the interesting thing is not in the machine. Source
 * lives in the jars around it, and {@code SourceUtil} is how every Ars Nouveau device reaches them, so
 * these take a position and a radius rather than bridging a capability. A blueprint can therefore look
 * somewhere other than its own block. A machine's <em>own</em> Source buffer needs no node of its own —
 * that is {@code ars_source_storage}, and the generic trait nodes already read it.
 *
 * <p>Level and position both fall back to the blueprint's own machine, so the common case needs no
 * wires. Gated by {@code modID = "ars_nouveau"}.</p>
 */
public final class ArsNouveauBlueprintNodes {

    private static final String GROUP = "mbd2/ars_nouveau";
    private static final String MOD = "ars_nouveau";

    private ArsNouveauBlueprintNodes() {}

    /** How much Source an area holds, and across how many providers. */
    @NodeAttribute(name = "mbd2_ars_source_in_area", group = GROUP, modID = MOD,
            graphTypes = MachineBlueprintGraph.class)
    public static class InArea extends AnnotatedNode {
        @InputPort public Level level;
        @InputPort public BlockPos pos;
        @InputPort public int radius = 10;
        @OutputPort public int source;
        @OutputPort public int providers;

        @Override
        public void evaluate(EvalContext ctx) {
            var level = resolveLevel(ctx.getInput("level", Level.class, null),
                    MachineNodes.ownMachine(ctx.getExecutor()));
            var pos = resolvePos(ctx.getInput("pos", BlockPos.class, null),
                    MachineNodes.ownMachine(ctx.getExecutor()));
            int radius = Math.max(1, ctx.getInput("radius", Integer.class, 10));
            if (level == null || pos == null) return;
            var found = SourceUtil.canTakeSource(pos, level, radius);
            ctx.setOutput("source", total(found));
            ctx.setOutput("providers", found.size());
        }
    }

    /**
     * Take Source out of the jars in an area.
     *
     * <p>All or nothing, because {@code SourceUtil.takeSourceMultiple} is — it puts everything back if
     * the area could not cover the whole amount. {@code taken} is therefore either {@code amount} or
     * zero, and {@code success} says which.</p>
     */
    @NodeAttribute(name = "mbd2_ars_source_take", group = GROUP, modID = MOD,
            graphTypes = MachineBlueprintGraph.class)
    public static class Take extends MachineActionNode {
        @InputPort public Level level;
        @InputPort public BlockPos pos;
        @InputPort public int radius = 10;
        @InputPort public int amount = 100;
        @InputPort public boolean particles = true;
        @OutputPort public int taken;
        @OutputPort public boolean success;

        @Override
        protected void run(ExecContext ctx, MachineEnvironment env) {
            var machine = env.getMachine();
            var level = resolveLevel(ctx.getInput("level", Level.class, null), machine);
            var pos = resolvePos(ctx.getInput("pos", BlockPos.class, null), machine);
            int amount = ctx.getInput("amount", Integer.class, 0);
            if (level == null || level.isClientSide || pos == null || amount <= 0) return;
            int radius = Math.max(1, ctx.getInput("radius", Integer.class, 10));
            var drained = ctx.getInput("particles", Boolean.class, true)
                    ? SourceUtil.takeSourceMultipleWithParticles(pos, level, radius, amount)
                    : SourceUtil.takeSourceMultiple(pos, level, radius, amount);
            ctx.setOutput("taken", drained == null ? 0 : amount);
            ctx.setOutput("success", drained != null);
        }
    }

    /**
     * Put Source into the jars in an area, filling them in the order {@code SourceUtil} reports.
     *
     * <p>Best-effort, unlike {@link Take}: {@code given} is how much actually fitted. The amount moved is
     * measured rather than read off the return value — {@code ISourceTile#addSource(int)} answers with
     * the resulting total, not the amount added.</p>
     */
    @NodeAttribute(name = "mbd2_ars_source_give", group = GROUP, modID = MOD,
            graphTypes = MachineBlueprintGraph.class)
    public static class Give extends MachineActionNode {
        @InputPort public Level level;
        @InputPort public BlockPos pos;
        @InputPort public int radius = 10;
        @InputPort public int amount = 100;
        @OutputPort public int given;

        @Override
        protected void run(ExecContext ctx, MachineEnvironment env) {
            var machine = env.getMachine();
            var level = resolveLevel(ctx.getInput("level", Level.class, null), machine);
            var pos = resolvePos(ctx.getInput("pos", BlockPos.class, null), machine);
            int amount = ctx.getInput("amount", Integer.class, 0);
            if (level == null || level.isClientSide || pos == null || amount <= 0) return;
            int radius = Math.max(1, ctx.getInput("radius", Integer.class, 10));
            int remaining = amount;
            for (var provider : SourceUtil.canGiveSource(pos, level, radius)) {
                if (remaining <= 0) break;
                var tile = provider.getSource();
                var before = tile.getSource();
                var toAdd = Math.min(remaining, tile.getMaxSource() - before);
                if (toAdd <= 0) continue;
                tile.addSource(toAdd);
                remaining -= Math.max(0, tile.getSource() - before);
            }
            ctx.setOutput("given", amount - remaining);
        }
    }

    private static int total(List<ISpecialSourceProvider> providers) {
        long sum = 0;
        for (var provider : providers) {
            sum += Math.max(0, provider.getSource().getSource());
            if (sum >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        }
        return (int) sum;
    }

    @Nullable
    private static Level resolveLevel(@Nullable Level wired, @Nullable MBDMachine machine) {
        if (wired != null) return wired;
        return machine == null ? null : machine.getLevel();
    }

    @Nullable
    private static BlockPos resolvePos(@Nullable BlockPos wired, @Nullable MBDMachine machine) {
        if (wired != null) return wired;
        return machine == null ? null : machine.getPos();
    }
}
