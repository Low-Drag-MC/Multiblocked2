package com.lowdragmc.mbd2.common.blueprint.node.recipe;

import com.lowdragmc.kilagraph.graph.core.AnnotatedNode;
import com.lowdragmc.kilagraph.graph.core.InputPort;
import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.EvalContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.content.ContentModifier;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import net.minecraft.nbt.CompoundTag;

/**
 * Building a changed recipe from an existing one.
 *
 * <p>Every node here <b>copies</b>. A recipe that reached a {@code Recipe Modify} hook may be the shared
 * instance the recipe manager holds, so mutating it in place would change that recipe for every machine
 * in the world — the bug this whole group exists to make unavailable. Feed the copy to
 * {@code Set Event Recipe}.</p>
 */
public final class RecipeBuildNodes {

    private static final String GROUP = "mbd2/recipe";

    private RecipeBuildNodes() {}

    /**
     * A modifier: {@code value * multiplier + addition}.
     *
     * <p>The unit is {@code multiplier = 1, addition = 0}, which every {@code Scale} node treats as
     * "leave it alone".</p>
     */
    @NodeAttribute(name = "mbd2_content_modifier", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class Modifier extends AnnotatedNode {
        @InputPort public double multiplier = 1;
        @InputPort public double addition = 0;
        @OutputPort public ContentModifier modifier;

        @Override
        public void evaluate(EvalContext ctx) {
            ctx.setOutput("modifier", ContentModifier.of(
                    ctx.getInput("multiplier", Double.class, 1d),
                    ctx.getInput("addition", Double.class, 0d)));
        }
    }

    /**
     * Compose two modifiers into one.
     *
     * <p>Composition is not commutative — {@code a} is applied first — so swapping the inputs of a
     * {@code x2 then +1} gives {@code +1 then x2}.</p>
     */
    @NodeAttribute(name = "mbd2_content_modifier_merge", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class MergeModifier extends AnnotatedNode {
        @InputPort public ContentModifier a;
        @InputPort public ContentModifier b;
        @OutputPort public ContentModifier modifier;

        @Override
        public void evaluate(EvalContext ctx) {
            var first = ctx.getInput("a", ContentModifier.class, ContentModifier.IDENTITY);
            var second = ctx.getInput("b", ContentModifier.class, ContentModifier.IDENTITY);
            ctx.setOutput("modifier", first.merge(second));
        }
    }

    /** Apply a modifier to a plain number, for arithmetic that should follow the same rule as the contents. */
    @NodeAttribute(name = "mbd2_content_modifier_apply", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class ApplyModifier extends AnnotatedNode {
        @InputPort public ContentModifier modifier;
        @InputPort public double value;
        @OutputPort public double result;

        @Override
        public void evaluate(EvalContext ctx) {
            var modifier = ctx.getInput("modifier", ContentModifier.class, ContentModifier.IDENTITY);
            ctx.setOutput("result", modifier.apply(ctx.getInput("value", Double.class, 0d)).doubleValue());
        }
    }

    /** A shallow copy of a recipe, safe to change. */
    @NodeAttribute(name = "mbd2_recipe_copy", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class Copy extends AnnotatedNode {
        @InputPort public MBDRecipe recipe;
        @OutputPort public MBDRecipe result;

        @Override
        public void evaluate(EvalContext ctx) {
            var recipe = ctx.getInput("recipe", MBDRecipe.class, null);
            ctx.setOutput("result", recipe == null ? null : recipe.copy());
        }
    }

    /**
     * A copy with every content scaled by a modifier — the usual way to make a machine cheaper, faster
     * or more productive.
     *
     * <p>{@code target} picks which side is scaled: {@code IN} for costs, {@code OUT} for products,
     * {@code BOTH} for a straight parallel. {@code modifyDuration} additionally scales the recipe's
     * duration by the same modifier.</p>
     */
    @NodeAttribute(name = "mbd2_recipe_scale", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class Scale extends AnnotatedNode {
        @InputPort public MBDRecipe recipe;
        @InputPort public ContentModifier modifier;
        @InputPort public IO target = IO.BOTH;
        @InputPort public boolean modifyDuration = false;
        @OutputPort public MBDRecipe result;

        @Override
        public void evaluate(EvalContext ctx) {
            var recipe = ctx.getInput("recipe", MBDRecipe.class, null);
            if (recipe == null) return;
            var modifier = ctx.getInput("modifier", ContentModifier.class, ContentModifier.IDENTITY);
            var target = ctx.getInput("target", IO.class, IO.BOTH);
            var duration = ctx.getInput("modifyDuration", Boolean.class, false);
            ctx.setOutput("result", recipe.copy(modifier, duration, target));
        }
    }

    /** A copy whose duration is set outright, in ticks. Clamped to at least one tick. */
    @NodeAttribute(name = "mbd2_recipe_set_duration", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SetDuration extends AnnotatedNode {
        @InputPort public MBDRecipe recipe;
        @InputPort public int duration = 20;
        @OutputPort public MBDRecipe result;

        @Override
        public void evaluate(EvalContext ctx) {
            var recipe = ctx.getInput("recipe", MBDRecipe.class, null);
            if (recipe == null) return;
            var copy = recipe.copy();
            copy.duration = Math.max(1, ctx.getInput("duration", Integer.class, 20));
            ctx.setOutput("result", copy);
        }
    }

    /** A copy with a different priority, for a blueprint that wants its recipe to win a tie. */
    @NodeAttribute(name = "mbd2_recipe_set_priority", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SetPriority extends AnnotatedNode {
        @InputPort public MBDRecipe recipe;
        @InputPort public int priority = 0;
        @OutputPort public MBDRecipe result;

        @Override
        public void evaluate(EvalContext ctx) {
            var recipe = ctx.getInput("recipe", MBDRecipe.class, null);
            if (recipe == null) return;
            var copy = recipe.copy();
            copy.priority = ctx.getInput("priority", Integer.class, 0);
            ctx.setOutput("result", copy);
        }
    }

    /**
     * A copy with a replaced data tag.
     *
     * <p>The recipe's data is what a blueprint and a recipe author use to talk to each other — put a
     * value on the recipe in the recipe editor, read it here with {@code Recipe Data}.</p>
     */
    @NodeAttribute(name = "mbd2_recipe_set_data", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SetData extends AnnotatedNode {
        @InputPort public MBDRecipe recipe;
        @InputPort public CompoundTag data;
        @OutputPort public MBDRecipe result;

        @Override
        public void evaluate(EvalContext ctx) {
            var recipe = ctx.getInput("recipe", MBDRecipe.class, null);
            if (recipe == null) return;
            var data = ctx.getInput("data", CompoundTag.class, null);
            var copy = recipe.copy();
            copy.data = data == null ? new CompoundTag() : data.copy();
            ctx.setOutput("result", copy);
        }
    }

    /**
     * A deep copy — the contents are cloned too, not shared with the original.
     *
     * <p>Only needed when something downstream mutates a content in place. The plain {@code Copy} is
     * enough for scaling and for replacing whole contents, and is much cheaper.</p>
     */
    @NodeAttribute(name = "mbd2_recipe_deep_copy", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class DeepCopy extends AnnotatedNode {
        @InputPort public MBDRecipe recipe;
        @OutputPort public MBDRecipe result;

        @Override
        public void evaluate(EvalContext ctx) {
            var recipe = ctx.getInput("recipe", MBDRecipe.class, null);
            ctx.setOutput("result", recipe == null ? null : recipe.deepCopied(recipe.id));
        }
    }
}
