package com.lowdragmc.mbd2.integration.create;

import com.lowdragmc.kilagraph.graph.core.AnnotatedNode;
import com.lowdragmc.kilagraph.graph.core.InputPort;
import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.EvalContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleFloat;

/**
 * Reading and building the payload behind Create's rotation recipe capability.
 *
 * <p>{@code Content Value} on {@code create_rotation} hands back a {@link CreateRotation}, and until
 * these existed nothing could open one or make one — a blueprint could move a rotation recipe's
 * numbers around only by going through NBT. The recipe-content nodes themselves are generic; what a
 * capability has to supply is the pair that turns its payload into graph values and back.</p>
 *
 * <h2>Absent Create</h2>
 * {@code modID = "create"} means LDLib2's registry reads the ASM scan data and skips these before the
 * class is ever loaded. That guard costs nothing here — {@link CreateRotation} and {@link ToggleFloat}
 * are MBD2's own types and would resolve without Create installed — but the capability they describe
 * is gated the same way, so ungated nodes would only put dead entries in the palette.
 */
public final class CreateRecipeContentNodes {

    private static final String GROUP = "mbd2/recipe/create";
    private static final String MOD = "create";

    private CreateRecipeContentNodes() {}

    /**
     * What a rotation payload asks for.
     *
     * <p>{@code mode} is the part worth reading before doing arithmetic on {@code value}: the same
     * number means stress units or RPM depending on it, and a blueprint that doubles one having
     * assumed the other is wrong in a way nothing else will report.</p>
     */
    @NodeAttribute(name = "mbd2_create_rotation_info", group = GROUP, modID = MOD,
            graphTypes = MachineBlueprintGraph.class)
    public static class RotationInfo extends AnnotatedNode {
        @InputPort public CreateRotation rotation;
        @OutputPort public float value;
        @OutputPort public CreateRotation.Mode mode;
        @OutputPort public boolean overridesTorque;
        @OutputPort public float torque;

        @Override
        public void evaluate(EvalContext ctx) {
            var rotation = ctx.getInput("rotation", CreateRotation.class, null);
            if (rotation == null) {
                ctx.setOutput("value", 0f);
                ctx.setOutput("mode", CreateRotation.Mode.STRESS);
                ctx.setOutput("overridesTorque", false);
                ctx.setOutput("torque", 0f);
                return;
            }
            var override = rotation.torqueOverride;
            ctx.setOutput("value", rotation.value);
            ctx.setOutput("mode", rotation.mode);
            ctx.setOutput("overridesTorque", override != null && override.isEnable());
            ctx.setOutput("torque", override == null ? 0f : override.getValue());
        }
    }

    /**
     * A rotation payload, to feed {@code Content Of}.
     *
     * <p>{@code Content Of} can already coerce a bare number, but only ever as stress and never with
     * a torque override — the two things a rotation recipe is actually configured with.</p>
     */
    @NodeAttribute(name = "mbd2_create_rotation_of", group = GROUP, modID = MOD,
            graphTypes = MachineBlueprintGraph.class)
    public static class RotationOf extends AnnotatedNode {
        @InputPort public float value;
        @InputPort public CreateRotation.Mode mode = CreateRotation.Mode.STRESS;
        @InputPort public boolean overrideTorque = false;
        @InputPort public float torque;
        @OutputPort public CreateRotation rotation;

        @Override
        public void evaluate(EvalContext ctx) {
            var override = ctx.getInput("overrideTorque", Boolean.class, false)
                    ? ToggleFloat.of(true, ctx.getInput("torque", Float.class, 0f))
                    : ToggleFloat.ofDisabled();
            ctx.setOutput("rotation", new CreateRotation(
                    ctx.getInput("value", Float.class, 0f),
                    ctx.getInput("mode", CreateRotation.Mode.class, CreateRotation.Mode.STRESS),
                    override));
        }
    }
}
