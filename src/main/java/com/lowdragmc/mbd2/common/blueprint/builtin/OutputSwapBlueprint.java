package com.lowdragmc.mbd2.common.blueprint.builtin;

import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.blueprint.node.event.RecipeModifyBeforeEventNode;
import com.lowdragmc.mbd2.common.blueprint.node.event.SetEventRecipeNode;
import com.lowdragmc.mbd2.common.blueprint.node.recipe.RecipeContentNodes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A machine that produces something other than what its recipes say.
 *
 * <h2>What it does</h2>
 * Before every recipe, throws away the item outputs and puts {@code product} there instead. An
 * enrichment tier that turns every ore recipe into raw ore, a broken machine that only makes slag, a
 * dimension where the same process yields something else.
 *
 * <h2>Why this one exists</h2>
 * Every other recipe-modifying built-in — overclock, upgrade slots, part bonus, heat — moves the same
 * two numbers: duration and amount. This is the one that changes <em>what</em>, which until the
 * {@code Clear Recipe Contents} / {@code Add Recipe Content} nodes existed was not expressible in a
 * blueprint at all and meant dropping to KubeJS.
 *
 * <h2>Clear then add</h2>
 * Two nodes rather than one "replace", because "also produce this" is a different and equally common
 * intent — drop the Clear and the machine keeps its normal output and gains a second one. That is a
 * one-wire edit, which is the point of shipping it as a graph.
 *
 * <h2>What it cannot do</h2>
 * XEI still lists the recipe as authored, so a player reading JEI sees the original product. That is
 * true of every runtime recipe modification, but a swapped output is far more visible than a shorter
 * duration — say so in the machine's tooltip if a pack leans on this.
 */
final class OutputSwapBlueprint {

    private OutputSwapBlueprint() {}

    static BlueprintBuilder build() {
        var b = BlueprintBuilder.create();

        b.header("""
                OUTPUT SWAP

                The machine produces something other than what its
                recipes say.

                product  what to make instead

                Every item output of the recipe is dropped and
                replaced with this one. Fluid outputs are left
                alone.

                Remove the Clear node and it ADDS the product
                instead of replacing - the machine then makes its
                normal output plus this one.

                The recipe still has to match its normal INPUTS:
                only the output side is touched here. Clear and add
                on the IN side to change what it consumes.""");

        // ---- read ----------------------------------------------------------------------------
        b.add("event", RecipeModifyBeforeEventNode.class, 0, 0)
                .parameter("product", ItemStack.class, new ItemStack(Items.RAW_IRON), 0, 180);

        b.group("The event", 0, 0, 200, 300, BuiltinNotes.READ_GROUP);

        // ---- act -----------------------------------------------------------------------------
        b.add("clear", RecipeContentNodes.ClearContents.class, 300, 0)
                .constant("clear.io", IO.OUT)
                .title("clear", "drop the old outputs")
                .add("made", RecipeContentNodes.ContentOfItem.class, 300, 200)
                .title("made", "the new product")
                .add("add", RecipeContentNodes.AddContent.class, 560, 0)
                .constant("add.io", IO.OUT)
                .title("add", "produce this instead")
                .add("apply", SetEventRecipeNode.class, 820, 0)
                .title("apply", "use this recipe");

        b.wire("clear.recipe", "event.recipe")
                .wire("made.item", "product")
                .wire("add.recipe", "clear.result")
                .wire("add.content", "made.content")
                .wire("apply.recipe", "add.result");
        b.then("event", "apply");

        b.note(300, 420, 520, """
                Clear and Add both return a COPY. The recipe the
                event hands you is the one the recipe manager holds
                for every machine in the world - editing it in place
                would change that recipe globally until the next
                reload.""");

        b.group("Rewrite the outputs", 300, 0, 720, 560, BuiltinNotes.ACT_GROUP);

        return b;
    }
}
