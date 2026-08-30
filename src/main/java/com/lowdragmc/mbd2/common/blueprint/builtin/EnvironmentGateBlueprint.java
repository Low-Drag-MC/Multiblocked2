package com.lowdragmc.mbd2.common.blueprint.builtin;

import com.lowdragmc.kilagraph.blueprint.nodes.exec.BranchNode;
import com.lowdragmc.kilagraph.blueprint.nodes.logic.XorNode;
import com.lowdragmc.kilagraph.blueprint.nodes.mc.world.LevelInfoBlocks;
import com.lowdragmc.kilagraph.blueprint.nodes.mc.world.LevelInfoNode;
import com.lowdragmc.mbd2.common.blueprint.node.event.BeforeRecipeWorkingEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.event.CancelEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineInfoBlocks;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineInfoNode;

/**
 * A machine that only runs in the right weather.
 *
 * <h2>What it does</h2>
 * Before a recipe starts, checks whether it is raining and refuses to start if that is not what
 * {@code needsRain} asked for. A solar furnace that stops under cloud, or a condenser that only runs
 * when it does.
 *
 * <h2>Why it hooks Before Recipe Working</h2>
 * This is the event that can still say no. Cancelling here means the recipe never starts, so the
 * machine simply stays idle; cancelling a tick mid-recipe would freeze one that had already begun,
 * which is a different behaviour and usually the wrong one. A blueprint that gates <em>whether</em>
 * something happens belongs here, and one that gates <em>while</em> it happens belongs on
 * {@code On Recipe Working} — see the Upkeep built-in.
 *
 * <h2>Reaching the world at all</h2>
 * A machine node cannot ask about the weather; a vanilla {@code mc.*} node can, given a level. The
 * bridge is the machine's own {@code Level} property, which is the same shape every "read something
 * about the world around the machine" graph takes — biome, time, light, nearby blocks.
 */
final class EnvironmentGateBlueprint {

    private EnvironmentGateBlueprint() {}

    static BlueprintBuilder build() {
        var b = BlueprintBuilder.create();

        b.header("""
                ENVIRONMENT GATE

                The machine only starts a recipe in the weather
                you asked for.

                needsRain
                  off - the machine stops while it is raining
                  on  - the machine only runs while it is raining

                Checked when a recipe STARTS, so a recipe already
                running is left alone to finish.

                Level Info carries more than weather - day/night,
                game time, difficulty. Swap the Weather block for
                another one and the rest of the graph is the same
                shape.""");

        // ---- read ----------------------------------------------------------------------------
        b.add("before", BeforeRecipeWorkingEventNode.class, 0, 0)
                .add("machine", MachineInfoNode.class, 0, 160)
                .block("machine", "level", MachineInfoBlocks.MachineLevel.class)
                .add("world", LevelInfoNode.class, 230, 160)
                .block("world", "weather", LevelInfoBlocks.Weather.class)
                .title("world", "the world around it");

        b.wire("world.target", "level.value");

        b.group("Read the world", 0, 0, 380, 300, BuiltinNotes.READ_GROUP);

        // ---- decide --------------------------------------------------------------------------
        // Same XOR idiom as Redstone Control: "is what we found what we asked for". Here the mismatch
        // is what cancels, so no NOT is needed — the true side of the branch is the refusal.
        b.add("mismatch", XorNode.class, 480, 160)
                .title("mismatch", "weather != wanted")
                .parameter("needsRain", boolean.class, false, 480, 300)
                .add("gate", BranchNode.class, 660, 0)
                .title("gate", "wrong weather?");

        b.wire("mismatch.in1", "weather.raining")
                .wire("mismatch.in2", "needsRain")
                .wire("gate.cond", "mismatch.out");
        b.then("before", "gate");

        b.group("Is it the right weather?", 480, 0, 320, 370, BuiltinNotes.DECIDE_GROUP);

        // ---- act -----------------------------------------------------------------------------
        b.add("refuse", CancelEventNode.class, 880, 0)
                .title("refuse", "do not start");
        b.wire("refuse.in", "gate.trueExec");

        b.note(880, 160, 430, """
                Cancelling Before Recipe Working stops the recipe
                from starting at all. The machine goes idle rather
                than stalling, which is what you want when the
                condition may hold for a long time.""");

        b.group("Refuse", 880, 0, 200, 110, BuiltinNotes.ACT_GROUP);

        return b;
    }
}
