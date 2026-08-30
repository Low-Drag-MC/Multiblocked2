package com.lowdragmc.mbd2.integration.pneumaticcraft;

import com.lowdragmc.kilagraph.graph.core.AnnotatedNode;
import com.lowdragmc.kilagraph.graph.core.InputPort;
import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.EvalContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;

/**
 * Reading and building the payload behind PneumaticCraft's pressure/air recipe capability.
 *
 * <p>The recipe-content nodes are generic; what each capability has to supply is the pair that turns
 * its payload into graph values and back. {@link PressureAir} carries two of them, and which one
 * {@code value} means depends on the other — see {@link PressureAirInfo}.</p>
 *
 * <h2>Absent PneumaticCraft</h2>
 * {@code modID = "pneumaticcraft"} makes LDLib2's registry skip these from the ASM scan data before
 * the class is loaded. {@link PressureAir} is MBD2's own record and would resolve regardless, but the
 * capability is gated the same way, so ungated nodes would only be dead palette entries.
 */
public final class PNCRecipeContentNodes {

    private static final String GROUP = "mbd2/recipe/pneumaticcraft";
    private static final String MOD = "pneumaticcraft";

    private PNCRecipeContentNodes() {}

    /**
     * What a pressure/air payload asks for.
     *
     * <p>{@code isAir} is not a detail: it decides whether {@code value} is a quantity of air or a
     * pressure, which are different units on different scales. Read it before comparing.</p>
     */
    @NodeAttribute(name = "mbd2_pnc_pressure_air_info", group = GROUP, modID = MOD,
            graphTypes = MachineBlueprintGraph.class)
    public static class PressureAirInfo extends AnnotatedNode {
        @InputPort public PressureAir pressureAir;
        @OutputPort public boolean isAir;
        @OutputPort public float value;

        @Override
        public void evaluate(EvalContext ctx) {
            var pressureAir = ctx.getInput("pressureAir", PressureAir.class, null);
            ctx.setOutput("isAir", pressureAir != null && pressureAir.isAir());
            ctx.setOutput("value", pressureAir == null ? 0f : pressureAir.value());
        }
    }

    /**
     * A pressure/air payload, to feed {@code Content Of}.
     *
     * <p>{@code Content Of} can already coerce a bare number, but only ever as a pressure — this is
     * the one that can ask for air.</p>
     */
    @NodeAttribute(name = "mbd2_pnc_pressure_air_of", group = GROUP, modID = MOD,
            graphTypes = MachineBlueprintGraph.class)
    public static class PressureAirOf extends AnnotatedNode {
        @InputPort public boolean isAir = false;
        @InputPort public float value;
        @OutputPort public PressureAir pressureAir;

        @Override
        public void evaluate(EvalContext ctx) {
            ctx.setOutput("pressureAir", new PressureAir(
                    ctx.getInput("isAir", Boolean.class, false),
                    ctx.getInput("value", Float.class, 0f)));
        }
    }
}
