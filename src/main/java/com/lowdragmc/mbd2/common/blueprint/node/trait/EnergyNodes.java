package com.lowdragmc.mbd2.common.blueprint.node.trait;

import com.lowdragmc.kilagraph.graph.core.AnnotatedNode;
import com.lowdragmc.kilagraph.graph.core.InputPort;
import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.EvalContext;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.MachineEnvironment;
import com.lowdragmc.mbd2.common.blueprint.node.MachineActionNode;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Reading and moving Forge Energy.
 *
 * <p>Nothing here is MBD2-specific — an {@code IEnergyStorage} is an {@code IEnergyStorage} whether it
 * came from a machine trait or a modded generator — but no upstream graph library carries one, so this
 * is where the nodes live. Get a machine's with {@code Trait Energy Storage}.</p>
 */
public final class EnergyNodes {

    private static final String GROUP = "mbd2/trait/energy";

    private EnergyNodes() {}

    /** Reads: stored, capacity, and the 0..1 ratio a bar wants. */
    @NodeAttribute(name = "mbd2_energy_info", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class Info extends AnnotatedNode {
        @InputPort public IEnergyStorage storage;
        @OutputPort public int stored;
        @OutputPort public int capacity;
        @OutputPort public float fillRatio;
        @OutputPort public boolean canReceive;
        @OutputPort public boolean canExtract;

        @Override
        public void evaluate(EvalContext ctx) {
            var storage = ctx.getInput("storage", IEnergyStorage.class, null);
            if (storage == null) return;
            int stored = storage.getEnergyStored();
            int capacity = storage.getMaxEnergyStored();
            ctx.setOutput("stored", stored);
            ctx.setOutput("capacity", capacity);
            ctx.setOutput("fillRatio", capacity <= 0 ? 0f : (float) stored / capacity);
            ctx.setOutput("canReceive", storage.canReceive());
            ctx.setOutput("canExtract", storage.canExtract());
        }
    }

    /** Push energy in. {@code accepted} is how much actually fit. */
    @NodeAttribute(name = "mbd2_energy_receive", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class Receive extends MachineActionNode {
        @InputPort public IEnergyStorage storage;
        @InputPort public int amount;
        @InputPort public boolean simulate = false;
        @OutputPort public int accepted;

        @Override
        protected void run(ExecContext ctx, MachineEnvironment env) {
            var storage = ctx.getInput("storage", IEnergyStorage.class, null);
            int amount = ctx.getInput("amount", Integer.class, 0);
            if (storage == null || amount <= 0) {
                ctx.setOutput("accepted", 0);
                return;
            }
            ctx.setOutput("accepted",
                    storage.receiveEnergy(amount, ctx.getInput("simulate", Boolean.class, false)));
        }
    }

    /** Pull energy out. {@code extracted} is how much was actually available. */
    @NodeAttribute(name = "mbd2_energy_extract", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class Extract extends MachineActionNode {
        @InputPort public IEnergyStorage storage;
        @InputPort public int amount;
        @InputPort public boolean simulate = false;
        @OutputPort public int extracted;

        @Override
        protected void run(ExecContext ctx, MachineEnvironment env) {
            var storage = ctx.getInput("storage", IEnergyStorage.class, null);
            int amount = ctx.getInput("amount", Integer.class, 0);
            if (storage == null || amount <= 0) {
                ctx.setOutput("extracted", 0);
                return;
            }
            ctx.setOutput("extracted",
                    storage.extractEnergy(amount, ctx.getInput("simulate", Boolean.class, false)));
        }
    }
}
