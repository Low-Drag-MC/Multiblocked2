package com.lowdragmc.mbd2.common.blueprint.builtin;

import com.lowdragmc.kilagraph.blueprint.nodes.convert.ToIntNode;
import com.lowdragmc.kilagraph.blueprint.nodes.math.AddNode;
import com.lowdragmc.kilagraph.blueprint.nodes.math.DivideNode;
import com.lowdragmc.kilagraph.blueprint.nodes.math.MaxNode;
import com.lowdragmc.kilagraph.blueprint.nodes.math.MinNode;
import com.lowdragmc.kilagraph.blueprint.nodes.math.MultiplyNode;
import com.lowdragmc.mbd2.common.blueprint.node.event.RecipeModifyBeforeEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.event.SetEventRecipeNode;
import com.lowdragmc.mbd2.common.blueprint.node.multiblock.MultiblockNodes;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeBuildNodes;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeInfoBlocks;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeInfoNode;

/**
 * A multiblock that runs faster the bigger it is built.
 *
 * <h2>What it does</h2>
 * Before every recipe, counts the parts in the formed structure and divides the duration by
 * {@code 1 + count * speedPerPart}, capped at {@code maxSpeedup}. On a machine that is not a formed
 * multiblock the count is zero, the factor is one, and nothing changes — so binding this to a single
 * block is harmless rather than a crash.
 *
 * <h2>Why it counts every part</h2>
 * "One bonus per coil, none per casing" needs a loop over the parts, a definition-id test and a counter,
 * which is four times the graph and teaches the loop rather than the idea. Counting all of them is the
 * honest simple version, and the note on the canvas says where to put the filter — the parts list is
 * already on a pin, so a {@code For Each} over it is the natural next edit.
 */
final class PartCountBonusBlueprint {

    private PartCountBonusBlueprint() {}

    static BlueprintBuilder build() {
        var b = BlueprintBuilder.create();

        b.header("""
                PART COUNT BONUS

                A multiblock gets faster the more parts it has.

                speedPerPart  bonus each part contributes
                maxSpeedup    cap on the total factor

                At the defaults, 10 parts run 1.5x faster and
                anything past 40 parts stops helping.

                Every part counts the same. To count only some of
                them, put a For Each over the 'parts' pin, test each
                one's Definition Id and add up the matches - the
                list is already wired out.""");

        // ---- read ----------------------------------------------------------------------------
        b.add("event", RecipeModifyBeforeEventNode.class, 0, 0)
                .add("parts", MultiblockNodes.Parts.class, 0, 150)
                .title("parts", "Parts in the structure")
                .add("recipe", RecipeInfoNode.class, 0, 320)
                .block("recipe", "duration", RecipeInfoBlocks.Duration.class);

        b.wire("parts.machine", "event.machine")
                .wire("recipe.target", "event.recipe");

        b.group("Read", 0, 0, 180, 480, BuiltinNotes.READ_GROUP);

        // ---- decide --------------------------------------------------------------------------
        b.add("bonus", MultiplyNode.class, 280, 150)
                .title("bonus", "count x per-part")
                .parameter("speedPerPart", float.class, 0.05f, 280, 290)
                .add("factor", AddNode.class, 450, 150)
                .title("factor", "1 + bonus")
                .constant("factor.in1", 1f)
                .add("capped", MinNode.class, 620, 150)
                .title("capped", "speed factor")
                .parameter("maxSpeedup", float.class, 3f, 620, 290);

        b.wire("bonus.in1", "parts.count")
                .wire("bonus.in2", "speedPerPart")
                .wire("factor.in2", "bonus.out")
                .wire("capped.in1", "factor.out")
                .wire("capped.in2", "maxSpeedup");

        b.add("newDuration", DivideNode.class, 800, 320)
                .title("newDuration", "duration / factor")
                .add("atLeastOne", MaxNode.class, 960, 320)
                .constant("atLeastOne.in2", 1f)
                .title("atLeastOne", "never below 1 tick")
                .add("ticks", ToIntNode.class, 1130, 320)
                .option("ticks", "op", ToIntNode.Op.ROUND)
                .title("ticks", "whole ticks");

        b.wire("newDuration.a", "duration.value")
                .wire("newDuration.b", "capped.out")
                .wire("atLeastOne.in1", "newDuration.out")
                .wire("ticks.in", "atLeastOne.out");

        b.group("Speed factor", 280, 0, 1000, 450, BuiltinNotes.DECIDE_GROUP);

        // ---- act -----------------------------------------------------------------------------
        b.add("faster", RecipeBuildNodes.SetDuration.class, 1380, 0)
                .title("faster", "duration = ticks")
                .add("apply", SetEventRecipeNode.class, 1560, 0)
                .title("apply", "use this recipe");

        b.wire("faster.recipe", "event.recipe")
                .wire("faster.duration", "ticks.out")
                .wire("apply.recipe", "faster.result");

        b.then("event", "apply");

        b.group("Rewrite the recipe", 1380, 0, 320, 120, BuiltinNotes.ACT_GROUP);

        return b;
    }
}
