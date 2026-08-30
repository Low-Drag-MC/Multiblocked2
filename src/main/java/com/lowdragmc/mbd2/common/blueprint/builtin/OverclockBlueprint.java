package com.lowdragmc.mbd2.common.blueprint.builtin;

import com.lowdragmc.kilagraph.blueprint.nodes.convert.ToIntNode;
import com.lowdragmc.kilagraph.blueprint.nodes.math.ClampNode;
import com.lowdragmc.kilagraph.blueprint.nodes.math.DivideNode;
import com.lowdragmc.kilagraph.blueprint.nodes.math.MaxNode;
import com.lowdragmc.kilagraph.blueprint.nodes.math.PowNode;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.blueprint.node.event.RecipeModifyBeforeEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.event.SetEventRecipeNode;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineInfoBlocks;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineInfoNode;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeBuildNodes;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeInfoBlocks;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeInfoNode;

/**
 * Trading energy for speed, by machine tier.
 *
 * <h2>What it does</h2>
 * Before every recipe starts, takes the machine's tier as a number of overclocks, divides the duration
 * by {@code speedPerTier} that many times and multiplies the inputs by {@code costPerTier} that many
 * times. Tier 0 changes nothing, so a machine that never sets its tier is unaffected by having this
 * bound.
 *
 * <h2>Why tier and not stored energy</h2>
 * Overclocking off the energy currently in the buffer makes a recipe's cost depend on when it happened
 * to start, which is invisible in the UI and impossible to plan around. Tier is a property of the
 * machine, so the same machine always runs the same recipe the same way — and {@code Set Machine Tier}
 * is a node, so a blueprint that wants the buffer to decide can set the tier from it and still get a
 * machine whose behaviour is a stated number.
 *
 * <h2>The two-step edit</h2>
 * Scaling the inputs and setting the duration are separate nodes on purpose: {@code Scale Recipe} can
 * only multiply the duration by the same factor it multiplies contents by, and an overclock needs them
 * to move in opposite directions. So the inputs go through the modifier and the duration is written
 * afterwards — which is the shape any asymmetric recipe edit takes.
 */
final class OverclockBlueprint {

    private OverclockBlueprint() {}

    static BlueprintBuilder build() {
        var b = BlueprintBuilder.create();

        b.header("""
                OVERCLOCK

                Faster recipes that cost more, once per machine tier.

                speedPerTier  duration is divided by this per tier
                costPerTier   inputs are multiplied by this per tier
                maxOverclocks cap, however high the tier goes

                At the defaults, tier 2 runs 4x faster for 16x the
                inputs. Tier 0 leaves the recipe exactly as it was.

                Set the tier with Set Machine Tier, or from the
                machine's own config. Bind this AFTER any blueprint
                that decides which recipe runs, and BEFORE one that
                reads the final duration - blueprints run in the
                order they are listed on the machine.""");

        // ---- read ----------------------------------------------------------------------------
        b.add("event", RecipeModifyBeforeEventNode.class, 0, 0)
                .add("machine", MachineInfoNode.class, 0, 150)
                .block("machine", "tier", MachineInfoBlocks.MachineTier.class)
                .add("recipe", RecipeInfoNode.class, 0, 340)
                .block("recipe", "duration", RecipeInfoBlocks.Duration.class);

        b.wire("recipe.target", "event.recipe");

        b.group("Read", 0, 0, 170, 500, BuiltinNotes.READ_GROUP);

        // ---- how many overclocks -------------------------------------------------------------
        b.add("clamp", ClampNode.class, 270, 150)
                .title("clamp", "overclocks")
                .constant("clamp.min", 0f)
                .parameter("maxOverclocks", int.class, 4, 270, 260)
                .add("overclocks", ToIntNode.class, 450, 150)
                .option("overclocks", "op", ToIntNode.Op.FLOOR)
                .title("overclocks", "whole steps");

        b.wire("clamp.in", "tier.value")
                .wire("clamp.max", "maxOverclocks")
                .wire("overclocks.in", "clamp.out");

        // speedPerTier ^ overclocks — the classic doubling, expressed as one exponent rather than a
        // loop, so the whole overclock is a data graph with no exec flow in it at all.
        b.add("speedup", PowNode.class, 620, 150)
                .title("speedup", "speed factor")
                .parameter("speedPerTier", float.class, 2f, 620, 260)
                .add("costup", PowNode.class, 620, 460)
                .title("costup", "input factor")
                .parameter("costPerTier", float.class, 4f, 620, 570);

        b.wire("speedup.base", "speedPerTier")
                .wire("speedup.exp", "overclocks.out")
                .wire("costup.base", "costPerTier")
                .wire("costup.exp", "overclocks.out");

        // ---- the new duration ----------------------------------------------------------------
        b.add("newDuration", DivideNode.class, 790, 320)
                .title("newDuration", "duration / speed")
                // A recipe of zero ticks never finishes, so the floor is one tick, not the natural zero.
                .add("atLeastOne", MaxNode.class, 950, 320)
                .constant("atLeastOne.in2", 1f)
                .title("atLeastOne", "never below 1 tick")
                .add("ticks", ToIntNode.class, 1120, 320)
                .option("ticks", "op", ToIntNode.Op.ROUND)
                .title("ticks", "whole ticks");

        b.wire("newDuration.a", "duration.value")
                .wire("newDuration.b", "speedup.out")
                .wire("atLeastOne.in1", "newDuration.out")
                .wire("ticks.in", "atLeastOne.out");

        b.add("costModifier", RecipeBuildNodes.Modifier.class, 950, 460)
                .title("costModifier", "x cost factor");
        b.wire("costModifier.multiplier", "costup.out");

        b.note(790, 620, 420, """
                The 1-tick floor is not decoration: a recipe whose
                duration rounds to 0 never reaches its own end and
                the machine sits at 'working' forever. Any blueprint
                that divides a duration needs it.""");

        b.group("Overclocks & factors", 270, 0, 1010, 720, BuiltinNotes.DECIDE_GROUP);

        // ---- act -----------------------------------------------------------------------------
        b.add("costlier", RecipeBuildNodes.Scale.class, 1380, 0)
                .title("costlier", "inputs x cost factor")
                .constant("costlier.target", IO.IN)
                .constant("costlier.modifyDuration", false)
                .add("faster", RecipeBuildNodes.SetDuration.class, 1600, 0)
                .title("faster", "duration = ticks")
                .add("apply", SetEventRecipeNode.class, 1780, 0)
                .title("apply", "use this recipe");

        b.wire("costlier.recipe", "event.recipe")
                .wire("costlier.modifier", "costModifier.modifier")
                .wire("faster.recipe", "costlier.result")
                .wire("faster.duration", "ticks.out")
                .wire("apply.recipe", "faster.result");

        b.then("event", "apply");

        b.group("Rewrite the recipe", 1380, 0, 540, 120, BuiltinNotes.ACT_GROUP);

        return b;
    }
}
