package com.lowdragmc.mbd2.common.blueprint.builtin;

import com.lowdragmc.kilagraph.blueprint.nodes.convert.ToIntNode;
import com.lowdragmc.kilagraph.blueprint.nodes.flow.SelectNode;
import com.lowdragmc.kilagraph.blueprint.nodes.math.RemapNode;
import com.lowdragmc.kilagraph.blueprint.nodes.math.SubtractNode;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.mbd2.common.blueprint.node.event.TickEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineRedstoneNodes;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeLogicInfoBlocks;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeLogicInfoNode;

/**
 * Reporting recipe progress to a comparator.
 *
 * <h2>What it does</h2>
 * Every tick, maps the recipe logic's progress (0..1) onto the 0-15 a comparator reads and writes it to
 * the machine's analog signal. {@code invert} flips it, so an automation line can be driven by "nearly
 * done" as easily as by "just started".
 *
 * <h2>Why 0-15 is not just a multiply</h2>
 * A comparator carries fifteen levels, so a raw {@code progress * 15} spends its whole first level on
 * "hasn't started". This maps 0..1 onto 0..15 and rounds, which is the same shape vanilla uses for a
 * container's fullness — and the reason the graph has a Remap and a To Int rather than one multiply.
 */
final class ComparatorProgressBlueprint {

    private ComparatorProgressBlueprint() {}

    static BlueprintBuilder build() {
        var b = BlueprintBuilder.create();

        b.header("""
                COMPARATOR PROGRESS

                Puts the machine's recipe progress on a comparator
                as a 0-15 signal.

                invert
                  off - 0 when idle, 15 when nearly done
                  on  - 15 when idle, 0 when nearly done

                Put a comparator against the machine to read it.
                To report something else instead - a tank's
                fullness, a slot's fullness - replace Progress
                Percent with the read you want and keep the
                Remap -> To Int -> Set Analog chain.""");

        // ---- read ----------------------------------------------------------------------------
        b.add("tick", TickEventNode.class, 0, 0)
                .add("logic", RecipeLogicInfoNode.class, 0, 130)
                .block("logic", "percent", RecipeLogicInfoBlocks.ProgressPercent.class);

        b.group("Read", 0, 0, 160, 300, BuiltinNotes.READ_GROUP);

        // ---- decide --------------------------------------------------------------------------
        // Remap carries five inputs, so it is nearly twice the width of the nodes around it and the
        // column after it has to start further right than the usual pitch.
        b.add("scale", RemapNode.class, 260, 130)
                .title("scale", "0..1 -> 0..15")
                .constant("scale.fromMin", 0f)
                .constant("scale.fromMax", 1f)
                .constant("scale.toMin", 0f)
                .constant("scale.toMax", 15f)
                .add("level", ToIntNode.class, 490, 130)
                .option("level", "op", ToIntNode.Op.ROUND)
                .title("level", "nearest whole level");

        b.wire("scale.in", "percent.value")
                .wire("level.in", "scale.out");

        // 15 - level rather than a second Remap: one subtraction is easier to read than a mapping
        // whose only difference from the one above it is that two numbers are the other way round.
        b.add("flip", SubtractNode.class, 660, 250)
                .title("flip", "15 - level")
                .constant("flip.a", 15f)
                .parameter("invert", boolean.class, false, 660, 370)
                .add("pick", SelectNode.class, 850, 130)
                // Select's value ports are typed by its `type` option; left at UNKNOWN they would take
                // anything, which is one more thing a reader has to work out from the wires.
                .option("pick", "type", TypeHandles.INT.getIdentification())
                .title("pick", "inverted?");

        b.wire("flip.b", "level.out")
                .wire("pick.cond", "invert")
                .wire("pick.ifTrue", "flip.out")
                .wire("pick.ifFalse", "level.out");

        b.group("Decide", 260, 0, 760, 420, BuiltinNotes.DECIDE_GROUP);

        // ---- act -----------------------------------------------------------------------------
        b.add("emit", MachineRedstoneNodes.SetAnalogSignal.class, 1120, 0)
                .title("emit", "Comparator reads this");
        b.wire("emit.signal", "pick.out");
        b.then("tick", "emit");

        b.group("Act", 1120, 0, 180, 90, BuiltinNotes.ACT_GROUP);

        return b;
    }
}
