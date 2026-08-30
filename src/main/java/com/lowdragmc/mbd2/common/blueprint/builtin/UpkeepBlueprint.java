package com.lowdragmc.mbd2.common.blueprint.builtin;

import com.lowdragmc.kilagraph.blueprint.nodes.exec.BranchNode;
import com.lowdragmc.kilagraph.blueprint.nodes.logic.NotNode;
import com.lowdragmc.kilagraph.blueprint.nodes.mc.container.FluidContainerNodes;
import com.lowdragmc.kilagraph.blueprint.nodes.mc.text.TextNodes;
import com.lowdragmc.mbd2.common.blueprint.node.event.OnRecipeWorkingEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeLogicActionNodes;
import com.lowdragmc.mbd2.common.blueprint.node.trait.TraitCapabilityNodes;

/**
 * A machine that burns coolant while it runs, and stalls without it.
 *
 * <h2>What it does</h2>
 * Every tick the machine is working, drains {@code amountPerTick} from a named fluid trait. If the
 * drain comes up short the recipe is put into <em>waiting</em> with a reason the UI shows on hover,
 * so the machine stops making progress but keeps what it has and resumes the moment coolant returns.
 *
 * <h2>Waiting, not cancelling</h2>
 * Cancelling the tick would also stop progress, and would be wrong: a cancelled tick is silent, so the
 * machine sits at 40% with no indication why. {@code Set Waiting} is the state the recipe logic has for
 * exactly this — it shows the reason, and {@code dampingWhenWaiting} decides whether progress decays.
 * Any blueprint that gates an already-running recipe on a resource wants this shape.
 *
 * <h2>The drain is real, and that is the point</h2>
 * A simulated drain would tell you whether the coolant is <em>there</em> without consuming it, which is
 * a machine that reports a cost it never pays. The drain happens; {@code ok} says whether it was met.
 */
final class UpkeepBlueprint {

    private UpkeepBlueprint() {}

    static BlueprintBuilder build() {
        var b = BlueprintBuilder.create();

        b.header("""
                UPKEEP

                The machine burns a fluid while it works, and
                stalls when it runs out.

                traitName       which fluid trait to drain, named
                                as it appears in the trait list
                amountPerTick   mB drained per tick of progress
                reason          what the UI says while stalled

                Draining stops progress rather than cancelling the
                recipe: the machine keeps what it has done and
                carries on once the tank is refilled.

                To charge energy instead of fluid, swap the two
                trait/drain nodes for Trait Energy Storage and
                Extract Energy - the rest is unchanged.""");

        // ---- read ----------------------------------------------------------------------------
        b.add("working", OnRecipeWorkingEventNode.class, 0, 0)
                .add("tank", TraitCapabilityNodes.FluidHandlerOf.class, 0, 160)
                .title("tank", "the coolant tank")
                .parameter("traitName", String.class, "fluid_tank", 0, 330);

        b.wire("tank.traitName", "traitName");

        b.group("The tank", 0, 0, 210, 410, BuiltinNotes.READ_GROUP);

        // ---- act -----------------------------------------------------------------------------
        b.parameter("amountPerTick", int.class, 10, 310, 330)
                .add("burn", FluidContainerNodes.Drain.class, 310, 0)
                .title("burn", "burn the coolant")
                .add("ranDry", NotNode.class, 540, 160)
                .title("ranDry", "not enough?")
                .add("gate", BranchNode.class, 540, 0)
                .title("gate", "did it drain?");

        b.wire("burn.container", "tank.value")
                .wire("burn.amount", "amountPerTick")
                .wire("ranDry.in", "burn.ok")
                .wire("gate.cond", "ranDry.out");

        b.wire("burn.trigger", "working.next");
        b.wire("gate.in", "burn.next");

        b.parameter("reason", String.class, "Out of coolant", 720, 330)
                .add("message", TextNodes.Literal.class, 720, 160)
                .title("message", "as hover text")
                .add("stall", RecipeLogicActionNodes.SetWaiting.class, 900, 0)
                .title("stall", "stop making progress");

        b.wire("message.text", "reason")
                .wire("stall.reason", "message.out");
        b.wire("stall.in", "gate.trueExec");

        b.note(310, 430, 480, """
                The drain is not simulated. A machine that checks
                whether it could pay and then does not is a machine
                whose running cost is a lie - 'ok' reports whether
                the fluid that just left covered the tick.""");

        b.group("Burn it, or stall", 310, 0, 800, 410, BuiltinNotes.ACT_GROUP);

        return b;
    }
}
