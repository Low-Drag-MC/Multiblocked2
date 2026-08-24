package com.lowdragmc.mbd2.integration.naturesaura;

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
import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Blueprint nodes for Nature's Aura, the ambient aura a region of the world holds.
 *
 * <h2>Why these are world nodes rather than a trait bridge</h2>
 * Every other integration here hangs off {@code SimpleCapabilityTrait.getCapContent}, because the thing
 * being read is stored <em>in</em> the machine. Aura is not: it lives in the world's aura chunks, and
 * MBD2's own {@code AuraHandlerTrait} reaches it by asking {@code IAuraChunk} about a radius around the
 * machine. So there is no capability to bridge and these take a position instead — which is also more
 * capable, since a blueprint can look at somewhere other than its own block.
 *
 * <p>Level and position both fall back to the blueprint's own machine, so the common case needs no
 * wires. Gated by {@code modID = "naturesaura"}.</p>
 */
public final class NaturesAuraBlueprintNodes {

    private static final String GROUP = "mbd2/naturesaura";
    private static final String MOD = "naturesaura";

    private NaturesAuraBlueprintNodes() {}

    /** How much aura an area holds, and over how many spots. */
    @NodeAttribute(name = "mbd2_aura_in_area", group = GROUP, modID = MOD,
            graphTypes = MachineBlueprintGraph.class)
    public static class InArea extends AnnotatedNode {
        @InputPort public Level level;
        @InputPort public BlockPos pos;
        @InputPort public int radius = 30;
        @OutputPort public int aura;
        @OutputPort public int spots;

        @Override
        public void evaluate(EvalContext ctx) {
            var level = resolveLevel(ctx.getInput("level", Level.class, null),
                    MachineNodes.ownMachine(ctx.getExecutor()));
            var pos = resolvePos(ctx.getInput("pos", BlockPos.class, null),
                    MachineNodes.ownMachine(ctx.getExecutor()));
            int radius = Math.max(1, ctx.getInput("radius", Integer.class, 30));
            if (level == null || pos == null) return;
            ctx.setOutput("aura", IAuraChunk.getAuraInArea(level, pos, radius));
            ctx.setOutput("spots", IAuraChunk.getSpotAmountInArea(level, pos, radius));
        }
    }

    /**
     * Take aura out of an area, from its most aura-rich spot.
     *
     * <p>The same spot-picking MBD2's aura trait uses, so a blueprint and a recipe drain a region the
     * same way rather than fighting over different spots.</p>
     */
    @NodeAttribute(name = "mbd2_aura_drain", group = GROUP, modID = MOD,
            graphTypes = MachineBlueprintGraph.class)
    public static class Drain extends MachineActionNode {
        @InputPort public Level level;
        @InputPort public BlockPos pos;
        @InputPort public int radius = 30;
        @InputPort public int amount = 100;
        @OutputPort public int drained;

        @Override
        protected void run(ExecContext ctx, MachineEnvironment env) {
            var machine = env.getMachine();
            var level = resolveLevel(ctx.getInput("level", Level.class, null), machine);
            var pos = resolvePos(ctx.getInput("pos", BlockPos.class, null), machine);
            int amount = ctx.getInput("amount", Integer.class, 0);
            if (level == null || level.isClientSide || pos == null || amount <= 0) return;
            int radius = Math.max(1, ctx.getInput("radius", Integer.class, 30));
            var spot = IAuraChunk.getHighestSpot(level, pos, radius, pos);
            ctx.setOutput("drained", IAuraChunk.getAuraChunk(level, spot).drainAura(spot, amount));
        }
    }

    /** Put aura into an area, at its most depleted spot. @see Drain */
    @NodeAttribute(name = "mbd2_aura_store", group = GROUP, modID = MOD,
            graphTypes = MachineBlueprintGraph.class)
    public static class Store extends MachineActionNode {
        @InputPort public Level level;
        @InputPort public BlockPos pos;
        @InputPort public int radius = 30;
        @InputPort public int amount = 100;
        @OutputPort public int stored;

        @Override
        protected void run(ExecContext ctx, MachineEnvironment env) {
            var machine = env.getMachine();
            var level = resolveLevel(ctx.getInput("level", Level.class, null), machine);
            var pos = resolvePos(ctx.getInput("pos", BlockPos.class, null), machine);
            int amount = ctx.getInput("amount", Integer.class, 0);
            if (level == null || level.isClientSide || pos == null || amount <= 0) return;
            int radius = Math.max(1, ctx.getInput("radius", Integer.class, 30));
            var spot = IAuraChunk.getLowestSpot(level, pos, radius, pos);
            ctx.setOutput("stored", IAuraChunk.getAuraChunk(level, spot).storeAura(spot, amount));
        }
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
