package com.lowdragmc.mbd2.common.blueprint.builtin;

import com.lowdragmc.kilagraph.blueprint.nodes.convert.ToIntNode;
import com.lowdragmc.kilagraph.blueprint.nodes.exec.BranchNode;
import com.lowdragmc.kilagraph.blueprint.nodes.logic.AndNode;
import com.lowdragmc.kilagraph.blueprint.nodes.math.AddNode;
import com.lowdragmc.kilagraph.blueprint.nodes.math.DivideNode;
import com.lowdragmc.kilagraph.blueprint.nodes.math.MaxNode;
import com.lowdragmc.kilagraph.blueprint.nodes.math.MinNode;
import com.lowdragmc.kilagraph.blueprint.nodes.math.MultiplyNode;
import com.lowdragmc.kilagraph.blueprint.nodes.mc.item.ItemStackNodes;
import com.lowdragmc.mbd2.common.blueprint.node.event.RecipeModifyBeforeEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.event.SetEventRecipeNode;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeBuildNodes;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeInfoBlocks;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeInfoNode;
import com.lowdragmc.mbd2.common.blueprint.node.trait.ItemHandlerNodes;
import com.lowdragmc.mbd2.common.blueprint.node.trait.TraitCapabilityNodes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Speed upgrades you put in a slot.
 *
 * <h2>What it does</h2>
 * Before every recipe, looks in one slot of a named item trait, and if it holds the upgrade item
 * divides the duration by {@code 1 + count * speedPerUpgrade}. Put four in and the machine runs
 * faster; take them out and it goes back to normal. The Mekanism/Thermal shape, which is the one most
 * people picture when they say "upgrade".
 *
 * <h2>Reading a slot without emptying it</h2>
 * There is no "peek at a slot" node, and there does not need to be: {@code Extract From Slot} with
 * {@code simulate} on is exactly that, and it is the idiom for every "what is in there" question. The
 * simulate flag is the whole difference between reading a container and looting it — a graph that
 * forgets it will quietly eat the player's upgrades.
 *
 * <h2>Why it checks the item</h2>
 * The slot is a normal item trait, so anything can be put in it. Testing what is actually there means
 * a stack of cobblestone in the upgrade slot does nothing, rather than making the machine four times
 * faster — which is what counting the stack size alone would do.
 */
final class UpgradeSlotsBlueprint {

    private UpgradeSlotsBlueprint() {}

    static BlueprintBuilder build() {
        var b = BlueprintBuilder.create();

        b.header("""
                UPGRADE SLOTS

                Speed upgrades the player puts in a slot.

                traitName        which item trait holds them,
                                 named as in the trait list
                slot             which slot of it, from zero
                upgradeItem      what counts as an upgrade
                speedPerUpgrade  bonus each one adds
                maxUpgrades      cap on how many count

                At the defaults, four upgrades run the machine
                three times faster.

                Give the machine a small item trait for these and
                keep it off the recipe IO, or the upgrades will be
                treated as recipe inputs.""");

        // ---- read the slot -------------------------------------------------------------------
        b.add("modify", RecipeModifyBeforeEventNode.class, 0, 0)
                .add("recipe", RecipeInfoNode.class, 0, 150)
                .block("recipe", "duration", RecipeInfoBlocks.Duration.class)
                .add("bay", TraitCapabilityNodes.ItemHandlerOf.class, 0, 340)
                .title("bay", "the upgrade trait")
                .parameter("traitName", String.class, "item_slot", 0, 500)
                .parameter("slot", int.class, 0, 0, 620)
                .add("peek", ItemHandlerNodes.ExtractSlot.class, 250, 340)
                .title("peek", "look, do not take")
                // Simulate: without it this empties the player's upgrade slot every time a recipe starts.
                .constant("peek.simulate", true)
                .constant("peek.amount", Integer.MAX_VALUE);

        b.wire("recipe.target", "modify.recipe")
                .wire("bay.traitName", "traitName")
                .wire("peek.handler", "bay.value")
                .wire("peek.slot", "slot");
        b.then("modify", "peek");

        b.group("What is in the slot", 0, 0, 400, 690, BuiltinNotes.READ_GROUP);

        // ---- is it an upgrade, and how many? -------------------------------------------------
        b.parameter("upgradeItem", ItemStack.class, new ItemStack(Items.SUGAR), 500, 500)
                .add("isUpgrade", ItemStackNodes.SameItem.class, 500, 340)
                .title("isUpgrade", "the right item?")
                .add("unpack", ItemStackNodes.Unpack.class, 500, 170)
                .title("unpack", "how many")
                .parameter("maxUpgrades", int.class, 4, 700, 500)
                .add("capped", MinNode.class, 700, 170)
                .title("capped", "capped count");

        b.wire("isUpgrade.a", "peek.extracted")
                .wire("isUpgrade.b", "upgradeItem")
                .wire("unpack.stack", "peek.extracted")
                .wire("capped.in1", "unpack.count")
                .wire("capped.in2", "maxUpgrades");

        // Anything that is not the upgrade item contributes nothing. The AND is what stops a stack of
        // cobblestone in the slot from making the machine faster.
        b.add("counts", AndNode.class, 900, 340)
                .title("counts", "worth a bonus?")
                .add("gate", BranchNode.class, 900, 0)
                .title("gate", "any upgrades?")
                .add("notEmpty", ItemStackNodes.Unpack.class, 700, 340)
                .title("notEmpty", "is there anything");

        b.wire("notEmpty.stack", "peek.extracted");
        // `empty` is true for an empty stack, so the AND wants its opposite — wiring `isUpgrade` alone
        // would be enough today, but SameItem on two empty stacks is true, and an empty slot must not
        // count as an upgrade.
        b.add("present", com.lowdragmc.kilagraph.blueprint.nodes.logic.NotNode.class, 700, 620)
                .title("present", "not empty");
        b.wire("present.in", "notEmpty.empty")
                .wire("counts.in1", "isUpgrade.out")
                .wire("counts.in2", "present.out")
                .wire("gate.cond", "counts.out");
        b.then("peek", "gate");

        b.parameter("speedPerUpgrade", float.class, 0.5f, 1100, 500)
                .add("bonus", MultiplyNode.class, 1100, 170)
                .title("bonus", "count x per-upgrade")
                .add("factor", AddNode.class, 1300, 170)
                .title("factor", "1 + bonus")
                .constant("factor.in1", 1f);

        b.wire("bonus.in1", "capped.out")
                .wire("bonus.in2", "speedPerUpgrade")
                .wire("factor.in2", "bonus.out");

        b.group("How much faster", 500, 0, 1000, 690, BuiltinNotes.DECIDE_GROUP);

        // ---- act -----------------------------------------------------------------------------
        b.add("newDuration", DivideNode.class, 1600, 170)
                .title("newDuration", "duration / factor")
                .add("atLeastOne", MaxNode.class, 1780, 170)
                .constant("atLeastOne.in2", 1f)
                .title("atLeastOne", "never below 1 tick")
                .add("ticks", ToIntNode.class, 1960, 170)
                .option("ticks", "op", ToIntNode.Op.ROUND)
                .title("ticks", "whole ticks")
                .add("faster", RecipeBuildNodes.SetDuration.class, 1600, 0)
                .title("faster", "duration = ticks")
                .add("apply", SetEventRecipeNode.class, 2160, 0)
                .title("apply", "use this recipe");

        b.wire("newDuration.a", "duration.value")
                .wire("newDuration.b", "factor.out")
                .wire("atLeastOne.in1", "newDuration.out")
                .wire("ticks.in", "atLeastOne.out")
                .wire("faster.recipe", "modify.recipe")
                .wire("faster.duration", "ticks.out")
                .wire("apply.recipe", "faster.result");

        // Only the upgraded path rewrites the recipe. With no upgrades the flow simply ends, and the
        // recipe the event already carries is the one that runs.
        //
        // The branch feeds Set Event Recipe, not Set Recipe Duration: the duration node is pure data —
        // it computes a new recipe from an old one and has no exec pins at all. What has to be gated is
        // the write back onto the event, which is the only node here that changes anything.
        b.wire("apply.in", "gate.trueExec");

        b.note(1600, 340, 460, """
                The false side goes nowhere on purpose. An event
                whose recipe is never rewritten keeps the one it
                came with, so 'no upgrades' needs no branch of its
                own to say 'leave it alone'.""");

        b.group("Rewrite the recipe", 1600, 0, 700, 280, BuiltinNotes.ACT_GROUP);

        return b;
    }
}
