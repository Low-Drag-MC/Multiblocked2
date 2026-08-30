package com.lowdragmc.mbd2.common.blueprint.builtin;

import com.lowdragmc.kilagraph.blueprint.nodes.convert.ToStringNode;
import com.lowdragmc.kilagraph.blueprint.nodes.exec.BranchNode;
import com.lowdragmc.kilagraph.blueprint.nodes.mc.action.WorldEffectNodes;
import com.lowdragmc.kilagraph.blueprint.nodes.mc.item.ItemStackNodes;
import com.lowdragmc.kilagraph.blueprint.nodes.mc.text.TextNodes;
import com.lowdragmc.kilagraph.blueprint.nodes.string.ConcatNode;
import com.lowdragmc.mbd2.common.blueprint.node.event.SetItemInteractionResultNode;
import com.lowdragmc.mbd2.common.blueprint.node.event.UseItemOnEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineInfoBlocks;
import com.lowdragmc.mbd2.common.blueprint.node.machine.MachineInfoNode;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeLogicInfoBlocks;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeLogicInfoNode;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Right-click a machine with a chosen item to have it tell you what it is doing.
 *
 * <h2>What it does</h2>
 * On a right-click with {@code probeItem}, prints the machine's state, tier and recipe status to the
 * player's chat and swallows the click so the machine's UI does not open on top of the message.
 *
 * <h2>Why this one ships</h2>
 * Every other built-in is something to run; this is something to <em>debug with</em>. When a machine is
 * not doing what its blueprints say it should, the first question is always what the recipe logic
 * actually thinks its status is, and answering it in-game beats reading a log. It is also the shortest
 * example of the two things a player-facing blueprint needs: reacting to an interaction, and reporting
 * back through {@code Set Item Interaction Result} so vanilla does not also handle the click.
 */
final class DebugProbeBlueprint {

    private DebugProbeBlueprint() {}

    static BlueprintBuilder build() {
        var b = BlueprintBuilder.create();

        b.header("""
                DEBUG PROBE

                Right-click the machine holding probeItem and it
                reports its state in chat.

                probeItem  what to hold, a stick by default

                Reports machine state, tier and recipe status. To
                report something else, add a block to one of the
                Info nodes and append it to the message - every
                read in the node list works the same way.

                The click is consumed, so the machine's UI stays
                shut while you are holding the probe.""");

        // ---- is it the probe? ----------------------------------------------------------------
        b.add("use", UseItemOnEventNode.class, 0, 0)
                .add("isProbe", ItemStackNodes.SameItem.class, 190, 150)
                .title("isProbe", "holding the probe?")
                .parameter("probeItem", ItemStack.class, new ItemStack(Items.STICK), 190, 280);

        b.wire("isProbe.a", "use.heldItem")
                .wire("isProbe.b", "probeItem");

        b.group("Is it the probe?", 0, 0, 350, 330, BuiltinNotes.READ_GROUP);

        // ---- what to say ---------------------------------------------------------------------
        b.add("machine", MachineInfoNode.class, 0, 420)
                .block("machine", "state", MachineInfoBlocks.MachineStateName.class)
                .block("machine", "tier", MachineInfoBlocks.MachineTier.class)
                .add("logic", RecipeLogicInfoNode.class, 0, 660)
                .block("logic", "status", RecipeLogicInfoBlocks.Status.class);

        // Numbers and enums reach the message as strings, which is what Concat wants: Text Append
        // joins Components, and turning three reads into Components first would be three more nodes
        // for no more meaning.
        b.add("tierText", ToStringNode.class, 190, 480)
                .add("statusText", ToStringNode.class, 190, 670)
                .add("line", ConcatNode.class, 360, 480)
                .option("line", "inputs", 6)
                .title("line", "the report")
                .constant("line.in1", "state=")
                .constant("line.in3", "  tier=")
                .constant("line.in5", "  recipe=")
                .add("message", TextNodes.Literal.class, 560, 480)
                .title("message", "as chat text");

        b.wire("tierText.in", "tier.value")
                .wire("statusText.in", "status.value")
                .wire("line.in2", "state.value")
                .wire("line.in4", "tierText.out")
                .wire("line.in6", "statusText.out")
                .wire("message.text", "line.out");

        b.group("What to say", 0, 420, 700, 400, BuiltinNotes.DECIDE_GROUP);

        // ---- say it --------------------------------------------------------------------------
        b.add("gate", BranchNode.class, 460, 0)
                .title("gate", "probe in hand?")
                .add("say", WorldEffectNodes.SendMessage.class, 650, 0)
                .title("say", "tell the player")
                .add("consume", SetItemInteractionResultNode.class, 850, 0)
                .title("consume", "don't open the UI")
                .constant("consume.result", ItemInteractionResult.SUCCESS);

        b.wire("gate.cond", "isProbe.out")
                .wire("say.player", "use.player")
                .wire("say.message", "message.out");

        b.then("use", "gate");
        b.wire("say.trigger", "gate.trueExec");
        b.then("say", "consume");

        b.group("Say it", 460, 0, 570, 130, BuiltinNotes.ACT_GROUP);

        return b;
    }
}
