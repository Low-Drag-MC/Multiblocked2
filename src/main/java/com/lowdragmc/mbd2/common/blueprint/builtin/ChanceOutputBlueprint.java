package com.lowdragmc.mbd2.common.blueprint.builtin;

import com.lowdragmc.kilagraph.blueprint.nodes.compare.LessThanNode;
import com.lowdragmc.kilagraph.blueprint.nodes.exec.BranchNode;
import com.lowdragmc.kilagraph.blueprint.nodes.math.RandomNode;
import com.lowdragmc.mbd2.common.blueprint.node.event.OnRecipeFinishEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.trait.ItemHandlerNodes;
import com.lowdragmc.mbd2.common.blueprint.node.trait.TraitCapabilityNodes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A chance of an extra product every time a recipe finishes.
 *
 * <h2>What it does</h2>
 * When a recipe completes, rolls once and — if the roll comes in under {@code chance} — inserts
 * {@code bonusItem} into the named item trait. The insert is a normal one, so a full output slot simply
 * means the bonus is lost, exactly as it would be for a recipe's own output.
 *
 * <h2>Why the trait is a parameter and not the recipe's output side</h2>
 * A recipe's outputs are fixed by the recipe; this hangs off the machine instead, so one setting gives
 * every recipe the machine runs the same bonus. Which slot that is has to be named because a machine can
 * have several item traits and there is no "the output one" — the name is the one shown in the machine
 * editor's trait list.
 */
final class ChanceOutputBlueprint {

    private ChanceOutputBlueprint() {}

    static BlueprintBuilder build() {
        var b = BlueprintBuilder.create();

        b.header("""
                CHANCE OUTPUT

                A chance of one extra item each time a recipe
                finishes.

                bonusItem  what to give
                chance     0..1, so 0.1 is a one-in-ten roll
                traitName  which item trait to put it in, named as
                           it appears in the trait list

                The roll happens once per completed recipe, on the
                server. A full slot drops the bonus, the same as it
                would a normal output.""");

        // ---- roll ----------------------------------------------------------------------------
        b.add("finish", OnRecipeFinishEventNode.class, 0, 0)
                .add("roll", RandomNode.class, 0, 150)
                .title("roll", "roll 0..1")
                .constant("roll.min", 0f)
                .constant("roll.max", 1f)
                .add("won", LessThanNode.class, 190, 150)
                .title("won", "roll < chance")
                .parameter("chance", float.class, 0.1f, 190, 270);

        b.wire("won.a", "roll.out")
                .wire("won.b", "chance");

        b.note(0, 330, 420, """
                'roll < chance' and not '<=': at chance = 0 the roll
                can be exactly 0, and a 0 chance that sometimes pays
                out is the bug this avoids.""");

        b.group("Roll", 0, 0, 400, 440, BuiltinNotes.DECIDE_GROUP);

        // ---- act -----------------------------------------------------------------------------
        b.add("gate", BranchNode.class, 520, 0)
                .title("gate", "lucky?")
                .add("slot", TraitCapabilityNodes.ItemHandlerOf.class, 520, 150)
                .title("slot", "the output trait")
                .parameter("traitName", String.class, "item_slot", 520, 300)
                .add("give", ItemHandlerNodes.Insert.class, 720, 0)
                .title("give", "give the bonus")
                .parameter("bonusItem", ItemStack.class, new ItemStack(Items.GOLD_NUGGET), 720, 300);

        b.wire("gate.cond", "won.out")
                .wire("slot.traitName", "traitName")
                .wire("give.handler", "slot.value")
                .wire("give.stack", "bonusItem");

        b.then("finish", "gate");
        b.wire("give.in", "gate.trueExec");

        b.group("Give", 520, 0, 380, 350, BuiltinNotes.ACT_GROUP);

        return b;
    }
}
