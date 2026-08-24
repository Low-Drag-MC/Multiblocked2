package com.lowdragmc.mbd2.integration.mekanism;

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
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.heat.IHeatHandler;

/**
 * Blueprint nodes for Mekanism's chemical tanks and heat capabilities.
 *
 * <p>Every class here is gated by {@code modID = "mekanism"}, which LDLib2's registry checks against the
 * ASM scan data <em>before</em> loading the class — so on an install without Mekanism these are never
 * touched, and the Mekanism types in their signatures never have to resolve.</p>
 */
public final class MekanismBlueprintNodes {

    private static final String GROUP = "mbd2/trait/mekanism";
    private static final String MOD = "mekanism";

    private MekanismBlueprintNodes() {}

    /** A trait's chemical tanks, as Mekanism's {@code IChemicalHandler}. */
    @NodeAttribute(name = "mbd2_mek_chemical_handler", group = GROUP, modID = MOD,
            graphTypes = MachineBlueprintGraph.class)
    public static class ChemicalHandlerOf extends TraitCapabilityNode<IChemicalHandler> {
        @OutputPort public IChemicalHandler value;
        @OutputPort public boolean found;

        @Override
        protected Class<IChemicalHandler> capabilityClass() {
            return IChemicalHandler.class;
        }
    }

    /** What one chemical tank holds, and how much it can. */
    @NodeAttribute(name = "mbd2_mek_chemical_tank", group = GROUP, modID = MOD,
            graphTypes = MachineBlueprintGraph.class)
    public static class TankContents extends AnnotatedNode {
        @InputPort public IChemicalHandler handler;
        @InputPort public int tank;
        @OutputPort public ChemicalStack stack;
        @OutputPort public long amount;
        @OutputPort public long capacity;
        @OutputPort public boolean empty;

        @Override
        public void evaluate(EvalContext ctx) {
            var handler = ctx.getInput("handler", IChemicalHandler.class, null);
            int tank = ctx.getInput("tank", Integer.class, 0);
            if (handler == null || tank < 0 || tank >= handler.getChemicalTanks()) {
                ctx.setOutput("empty", true);
                return;
            }
            var stack = handler.getChemicalInTank(tank);
            ctx.setOutput("stack", stack);
            ctx.setOutput("amount", stack.getAmount());
            ctx.setOutput("capacity", handler.getChemicalTankCapacity(tank));
            ctx.setOutput("empty", stack.isEmpty());
        }
    }

    /** How many chemical tanks a handler has. */
    @NodeAttribute(name = "mbd2_mek_chemical_tanks", group = GROUP, modID = MOD,
            graphTypes = MachineBlueprintGraph.class)
    public static class TankCount extends AnnotatedNode {
        @InputPort public IChemicalHandler handler;
        @OutputPort public int count;

        @Override
        public void evaluate(EvalContext ctx) {
            var handler = ctx.getInput("handler", IChemicalHandler.class, null);
            ctx.setOutput("count", handler == null ? 0 : handler.getChemicalTanks());
        }
    }

    /** Push chemical in. {@code remainder} is what did not fit. */
    @NodeAttribute(name = "mbd2_mek_chemical_insert", group = GROUP, modID = MOD,
            graphTypes = MachineBlueprintGraph.class)
    public static class Insert extends MachineActionNode {
        @InputPort public IChemicalHandler handler;
        @InputPort public ChemicalStack stack;
        @InputPort public boolean simulate = false;
        @OutputPort public ChemicalStack remainder;

        @Override
        protected void run(ExecContext ctx, MachineEnvironment env) {
            var handler = ctx.getInput("handler", IChemicalHandler.class, null);
            var stack = ctx.getInput("stack", ChemicalStack.class, null);
            if (handler == null || stack == null || stack.isEmpty()) {
                ctx.setOutput("remainder", stack);
                return;
            }
            var action = ctx.getInput("simulate", Boolean.class, false) ? Action.SIMULATE : Action.EXECUTE;
            ctx.setOutput("remainder", handler.insertChemical(stack, action));
        }
    }

    /** Pull chemical out of a tank, up to {@code amount}. */
    @NodeAttribute(name = "mbd2_mek_chemical_extract", group = GROUP, modID = MOD,
            graphTypes = MachineBlueprintGraph.class)
    public static class Extract extends MachineActionNode {
        @InputPort public IChemicalHandler handler;
        @InputPort public int tank;
        @InputPort public long amount = 1000;
        @InputPort public boolean simulate = false;
        @OutputPort public ChemicalStack extracted;

        @Override
        protected void run(ExecContext ctx, MachineEnvironment env) {
            var handler = ctx.getInput("handler", IChemicalHandler.class, null);
            int tank = ctx.getInput("tank", Integer.class, 0);
            long amount = ctx.getInput("amount", Long.class, 0L);
            if (handler == null || tank < 0 || tank >= handler.getChemicalTanks() || amount <= 0) return;
            var action = ctx.getInput("simulate", Boolean.class, false) ? Action.SIMULATE : Action.EXECUTE;
            ctx.setOutput("extracted", handler.extractChemical(tank, amount, action));
        }
    }

    /** A trait's heat capability, as Mekanism's {@code IHeatHandler}. */
    @NodeAttribute(name = "mbd2_mek_heat_handler", group = GROUP, modID = MOD,
            graphTypes = MachineBlueprintGraph.class)
    public static class HeatHandlerOf extends TraitCapabilityNode<IHeatHandler> {
        @OutputPort public IHeatHandler value;
        @OutputPort public boolean found;

        @Override
        protected Class<IHeatHandler> capabilityClass() {
            return IHeatHandler.class;
        }
    }

    /** A heat capability's temperature and how much heat it takes to move it. */
    @NodeAttribute(name = "mbd2_mek_heat_info", group = GROUP, modID = MOD,
            graphTypes = MachineBlueprintGraph.class)
    public static class HeatInfo extends AnnotatedNode {
        @InputPort public IHeatHandler handler;
        @OutputPort public double temperature;
        @OutputPort public double heatCapacity;

        @Override
        public void evaluate(EvalContext ctx) {
            var handler = ctx.getInput("handler", IHeatHandler.class, null);
            if (handler == null) return;
            ctx.setOutput("temperature", handler.getTotalTemperature());
            ctx.setOutput("heatCapacity", handler.getTotalHeatCapacity());
        }
    }

    /** Add heat to (or, with a negative value, take it from) a heat capability. */
    @NodeAttribute(name = "mbd2_mek_heat_add", group = GROUP, modID = MOD,
            graphTypes = MachineBlueprintGraph.class)
    public static class AddHeat extends MachineActionNode {
        @InputPort public IHeatHandler handler;
        @InputPort public double heat;

        @Override
        protected void run(ExecContext ctx, MachineEnvironment env) {
            var handler = ctx.getInput("handler", IHeatHandler.class, null);
            if (handler == null) return;
            handler.handleHeat(ctx.getInput("heat", Double.class, 0d));
        }
    }
}
