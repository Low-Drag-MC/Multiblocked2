package com.lowdragmc.mbd2.common.blueprint.builtin;

import com.lowdragmc.kilagraph.blueprint.nodes.compare.GreaterEqualNode;
import com.lowdragmc.kilagraph.blueprint.nodes.logic.NotNode;
import com.lowdragmc.kilagraph.blueprint.nodes.logic.XorNode;
import com.lowdragmc.kilagraph.blueprint.nodes.mc.redstone.RedstoneNodes;
import com.lowdragmc.mbd2.common.blueprint.node.event.TickEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineInfoBlocks;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineInfoNode;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeLogicActionNodes;

/**
 * Switching a machine on and off with redstone.
 *
 * <h2>What it does</h2>
 * Every tick, reads the strongest redstone signal reaching the machine and sets the recipe logic's
 * working-enabled flag from it. Whether that means "runs while powered" or "stops while powered" is the
 * {@code requiresSignal} parameter, because packs disagree and both are one boolean apart.
 *
 * <h2>Why it is the first example</h2>
 * It is the shortest complete blueprint there is — an event, a read, a decision, a write — so it is the
 * one to read first. The pattern it demonstrates (a machine's own position and level feeding a vanilla
 * {@code mc.*} node) is how a blueprint reaches anything outside its machine at all.
 */
final class RedstoneControlBlueprint {

    private RedstoneControlBlueprint() {}

    static BlueprintBuilder build() {
        var b = BlueprintBuilder.create();

        b.header("""
                REDSTONE CONTROL

                Starts and stops the machine with a redstone signal.

                requiresSignal
                  off - the machine runs until it is powered
                  on  - the machine only runs while powered
                threshold
                  how strong the signal has to be, 1-15.

                Reads the strongest signal from ANY side, which is
                what a redstone lamp does. For one specific side,
                swap Redstone Power for Redstone Signal and give
                it a side.""");

        // ---- read the world ------------------------------------------------------------------
        b.add("tick", TickEventNode.class, 0, 0)
                .add("info", MachineInfoNode.class, 0, 130)
                .block("info", "level", MachineInfoBlocks.MachineLevel.class)
                .block("info", "pos", MachineInfoBlocks.Position.class)
                .add("power", RedstoneNodes.Power.class, 160, 150)
                .title("power", "Signal reaching the machine");

        b.wire("power.level", "level.value")
                .wire("power.pos", "pos.value");

        b.group("Read", 0, 0, 300, 320, BuiltinNotes.READ_GROUP);

        // ---- decide --------------------------------------------------------------------------
        b.add("strongEnough", GreaterEqualNode.class, 400, 150)
                .title("strongEnough", "power >= threshold")
                .parameter("threshold", int.class, 1, 400, 270)
                // XOR then NOT reads as "does the power state match what we asked for" — true when both
                // are on (wants a signal, has one) and when both are off (wants none, has none).
                .add("mismatch", XorNode.class, 570, 150)
                .title("mismatch", "state != wanted")
                .parameter("requiresSignal", boolean.class, false, 570, 270)
                .add("enabled", NotNode.class, 740, 150)
                .title("enabled", "should be running");

        b.wire("strongEnough.a", "power.power")
                .wire("strongEnough.b", "threshold")
                // XOR is variadic, so its pins are in1/in2 rather than a/b.
                .wire("mismatch.in1", "strongEnough.out")
                .wire("mismatch.in2", "requiresSignal")
                .wire("enabled.in", "mismatch.out");

        b.note(400, 330, 400, """
                XOR then NOT is 'the signal matches what we asked
                for': both on, or both off.

                Wiring the machine straight to XOR without the NOT
                gives you the opposite behaviour, which is a good
                first thing to try.""");

        b.group("Decide", 400, 0, 450, 480, BuiltinNotes.DECIDE_GROUP);

        // ---- act -----------------------------------------------------------------------------
        b.add("setEnabled", RecipeLogicActionNodes.SetWorkingEnabled.class, 950, 0)
                .title("setEnabled", "Run / stop the machine");
        b.wire("setEnabled.enabled", "enabled.out");
        b.then("tick", "setEnabled");

        b.group("Act", 950, 0, 180, 90, BuiltinNotes.ACT_GROUP);

        return b;
    }
}
