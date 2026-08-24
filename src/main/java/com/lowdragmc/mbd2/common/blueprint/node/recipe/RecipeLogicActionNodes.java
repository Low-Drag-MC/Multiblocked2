package com.lowdragmc.mbd2.common.blueprint.node.recipe;

import com.lowdragmc.kilagraph.graph.core.InputPort;
import com.lowdragmc.kilagraph.graph.exec.ExecContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.MachineEnvironment;
import com.lowdragmc.mbd2.common.blueprint.node.MachineActionNode;
import com.lowdragmc.mbd2.common.blueprint.node.MachineNodes;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The things a blueprint can do <em>to</em> a machine's recipe logic.
 *
 * <p>Server-side only, and enforced in the shared base rather than per node: recipe state is server
 * state, and a client-side event reaching one of these would desync silently.</p>
 */
public final class RecipeLogicActionNodes {

    private static final String GROUP = "mbd2/recipe/logic";

    private RecipeLogicActionNodes() {}

    /** Base: resolves the recipe logic, falling back to the blueprint's own machine's. */
    public abstract static class LogicAction extends MachineActionNode {
        @InputPort
        public RecipeLogic recipeLogic;

        protected abstract void apply(ExecContext ctx, RecipeLogic logic);

        @Override
        protected final void run(ExecContext ctx, MachineEnvironment env) {
            RecipeLogic logic = resolve(ctx);
            if (logic == null) return;
            // Asked of the level rather than the machine: RecipeLogic holds an IMachine, which has no
            // side accessor of its own, and a machine still being constructed has no level yet.
            var level = logic.machine.getLevel();
            if (level == null || level.isClientSide) return;
            apply(ctx, logic);
        }

        @Nullable
        private RecipeLogic resolve(ExecContext ctx) {
            var explicit = ctx.getInput("recipeLogic", RecipeLogic.class, null);
            if (explicit != null) return explicit;
            var machine = MachineNodes.ownMachine(ctx.getExecutor());
            return machine == null ? null : machine.getRecipeLogic();
        }
    }

    /**
     * Force the recipe logic into a status.
     *
     * <p>Blunt: it does not check that the status makes sense for what the machine is holding. Prefer
     * {@code Set Working Enabled}, {@code Interrupt Recipe} or {@code Set Waiting} — each of those puts
     * the logic into a consistent state.</p>
     */
    @NodeAttribute(name = "mbd2_recipe_logic_set_status", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SetStatus extends LogicAction {
        @InputPort public RecipeLogic.Status status = RecipeLogic.Status.IDLE;

        @Override
        protected void apply(ExecContext ctx, RecipeLogic logic) {
            var status = ctx.getInput("status", RecipeLogic.Status.class, null);
            if (status != null) logic.setStatus(status);
        }
    }

    /**
     * Switch the machine on or off.
     *
     * <p>Off suspends it, keeping the recipe and its progress; on resumes that recipe if there is one,
     * and goes idle otherwise. This is the node a redstone-controlled machine wants.</p>
     */
    @NodeAttribute(name = "mbd2_recipe_logic_set_working_enabled", group = GROUP,
            graphTypes = MachineBlueprintGraph.class)
    public static class SetWorkingEnabled extends LogicAction {
        @InputPort public boolean enabled = true;

        @Override
        protected void apply(ExecContext ctx, RecipeLogic logic) {
            logic.setWorkingEnabled(ctx.getInput("enabled", Boolean.class, true));
        }
    }

    /** Put the logic into waiting with a reason, which the UI shows as the machine's hover text. */
    @NodeAttribute(name = "mbd2_recipe_logic_set_waiting", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SetWaiting extends LogicAction {
        @InputPort public Component reason;

        @Override
        protected void apply(ExecContext ctx, RecipeLogic logic) {
            logic.setWaiting(ctx.getInput("reason", Component.class, null));
        }
    }

    /** Stop the running recipe, keeping its progress so it can resume. */
    @NodeAttribute(name = "mbd2_recipe_logic_interrupt", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class Interrupt extends LogicAction {
        @Override
        protected void apply(ExecContext ctx, RecipeLogic logic) {
            logic.interruptRecipe();
        }
    }

    /** Throw away the running recipe and its progress, and go idle. */
    @NodeAttribute(name = "mbd2_recipe_logic_reset", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class Reset extends LogicAction {
        @Override
        protected void apply(ExecContext ctx, RecipeLogic logic) {
            logic.resetRecipeLogic();
        }
    }

    /**
     * Make the logic re-run its modifiers against the current recipe on the next tick.
     *
     * <p>What to call after a blueprint changes something a {@code Recipe Modify} hook reads — the
     * machine caches the modified recipe, so without this the change only takes effect on the next
     * recipe.</p>
     */
    @NodeAttribute(name = "mbd2_recipe_logic_mark_dirty", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class MarkRecipeDirty extends LogicAction {
        @Override
        protected void apply(ExecContext ctx, RecipeLogic logic) {
            logic.markLastRecipeDirty();
        }
    }

    /** Set how many ticks the running recipe still needs. */
    @NodeAttribute(name = "mbd2_recipe_logic_set_progress", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SetProgress extends LogicAction {
        @InputPort public int progress;

        @Override
        protected void apply(ExecContext ctx, RecipeLogic logic) {
            logic.setProgress(Math.max(0, ctx.getInput("progress", Integer.class, 0)));
        }
    }

    /** Set the running recipe's total duration in ticks. */
    @NodeAttribute(name = "mbd2_recipe_logic_set_duration", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    public static class SetDuration extends LogicAction {
        @InputPort public int duration;

        @Override
        protected void apply(ExecContext ctx, RecipeLogic logic) {
            logic.setDuration(Math.max(0, ctx.getInput("duration", Integer.class, 0)));
        }
    }
}
