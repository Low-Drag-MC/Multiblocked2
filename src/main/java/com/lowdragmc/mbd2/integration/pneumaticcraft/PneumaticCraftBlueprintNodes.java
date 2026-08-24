package com.lowdragmc.mbd2.integration.pneumaticcraft;

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
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.api.tileentity.IAirHandlerMachine;

/**
 * Blueprint nodes for PneumaticCraft's pressure and heat capabilities.
 *
 * <p>Gated by {@code modID = "pneumaticcraft"}; see {@code MekanismBlueprintNodes} for why that makes
 * the PneumaticCraft types in these signatures safe on an install without it.</p>
 */
public final class PneumaticCraftBlueprintNodes {

    private static final String GROUP = "mbd2/trait/pneumaticcraft";
    private static final String MOD = "pneumaticcraft";

    private PneumaticCraftBlueprintNodes() {}

    /** A trait's compressed-air buffer, as PneumaticCraft's {@code IAirHandlerMachine}. */
    @NodeAttribute(name = "mbd2_pnc_air_handler", group = GROUP, modID = MOD,
            graphTypes = MachineBlueprintGraph.class)
    public static class AirHandlerOf extends TraitCapabilityNode<IAirHandlerMachine> {
        @OutputPort public IAirHandlerMachine value;
        @OutputPort public boolean found;

        @Override
        protected Class<IAirHandlerMachine> capabilityClass() {
            return IAirHandlerMachine.class;
        }
    }

    /** Pressure, stored air and the buffer's volume. */
    @NodeAttribute(name = "mbd2_pnc_air_info", group = GROUP, modID = MOD,
            graphTypes = MachineBlueprintGraph.class)
    public static class AirInfo extends AnnotatedNode {
        @InputPort public IAirHandlerMachine handler;
        @OutputPort public float pressure;
        @OutputPort public int air;
        @OutputPort public int volume;

        @Override
        public void evaluate(EvalContext ctx) {
            var handler = ctx.getInput("handler", IAirHandlerMachine.class, null);
            if (handler == null) return;
            ctx.setOutput("pressure", handler.getPressure());
            ctx.setOutput("air", handler.getAir());
            ctx.setOutput("volume", handler.getVolume());
        }
    }

    /** Add air to (or, with a negative value, vent it from) a pressure buffer. */
    @NodeAttribute(name = "mbd2_pnc_add_air", group = GROUP, modID = MOD,
            graphTypes = MachineBlueprintGraph.class)
    public static class AddAir extends MachineActionNode {
        @InputPort public IAirHandlerMachine handler;
        @InputPort public int air;

        @Override
        protected void run(ExecContext ctx, MachineEnvironment env) {
            var handler = ctx.getInput("handler", IAirHandlerMachine.class, null);
            if (handler == null) return;
            handler.addAir(ctx.getInput("air", Integer.class, 0));
        }
    }

    /** A trait's heat exchanger, as PneumaticCraft's {@code IHeatExchangerLogic}. */
    @NodeAttribute(name = "mbd2_pnc_heat_handler", group = GROUP, modID = MOD,
            graphTypes = MachineBlueprintGraph.class)
    public static class HeatExchangerOf extends TraitCapabilityNode<IHeatExchangerLogic> {
        @OutputPort public IHeatExchangerLogic value;
        @OutputPort public boolean found;

        @Override
        protected Class<IHeatExchangerLogic> capabilityClass() {
            return IHeatExchangerLogic.class;
        }
    }

    /** A heat exchanger's temperature, in kelvin. */
    @NodeAttribute(name = "mbd2_pnc_heat_info", group = GROUP, modID = MOD,
            graphTypes = MachineBlueprintGraph.class)
    public static class HeatInfo extends AnnotatedNode {
        @InputPort public IHeatExchangerLogic handler;
        @OutputPort public double temperature;
        @OutputPort public double thermalResistance;

        @Override
        public void evaluate(EvalContext ctx) {
            var handler = ctx.getInput("handler", IHeatExchangerLogic.class, null);
            if (handler == null) return;
            ctx.setOutput("temperature", handler.getTemperature());
            ctx.setOutput("thermalResistance", handler.getThermalResistance());
        }
    }

    /** Add heat to (or, with a negative value, take it from) a heat exchanger. */
    @NodeAttribute(name = "mbd2_pnc_add_heat", group = GROUP, modID = MOD,
            graphTypes = MachineBlueprintGraph.class)
    public static class AddHeat extends MachineActionNode {
        @InputPort public IHeatExchangerLogic handler;
        @InputPort public double heat;

        @Override
        protected void run(ExecContext ctx, MachineEnvironment env) {
            var handler = ctx.getInput("handler", IHeatExchangerLogic.class, null);
            if (handler == null) return;
            handler.addHeat(ctx.getInput("heat", Double.class, 0d));
        }
    }
}
