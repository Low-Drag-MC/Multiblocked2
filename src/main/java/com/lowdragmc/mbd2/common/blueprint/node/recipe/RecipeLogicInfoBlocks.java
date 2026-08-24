package com.lowdragmc.mbd2.common.blueprint.node.recipe;

import com.lowdragmc.kilagraph.graph.core.OutputPort;
import com.lowdragmc.kilagraph.graph.exec.EvalContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.UseWithContext;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.blueprint.MachineBlueprintGraph;
import com.lowdragmc.mbd2.common.blueprint.node.MachineInfoBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The properties of a {@link RecipeLogic}, one block each.
 *
 * <p>The four status predicates are separate blocks rather than one enum output plus a comparison,
 * because that is how graphs actually read them — {@code Branch(isWorking)} is one wire where
 * {@code Equals(status, WORKING)} is three nodes. {@link Status} is still there for the cases that
 * want the value itself.</p>
 */
public final class RecipeLogicInfoBlocks {

    private static final String GROUP = "mbd2/recipe/logic";

    private RecipeLogicInfoBlocks() {}

    private abstract static class LogicBlock extends MachineInfoBlock<RecipeLogic> {
        @Override
        protected final Class<RecipeLogic> targetClass() {
            return RecipeLogic.class;
        }
    }

    // ---- status ------------------------------------------------------------------------------

    /** Idle, working, waiting or suspend. */
    @NodeAttribute(name = "mbd2_recipe_logic_status", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(RecipeLogicInfoNode.class)
    public static class Status extends LogicBlock {
        @OutputPort public RecipeLogic.Status value;

        @Override
        protected void read(RecipeLogic logic, EvalContext ctx) {
            ctx.setOutput("value", logic.getStatus());
        }
    }

    /** Running a recipe right now. */
    @NodeAttribute(name = "mbd2_recipe_logic_is_working", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(RecipeLogicInfoNode.class)
    public static class IsWorking extends LogicBlock {
        @OutputPort public boolean value;

        @Override
        protected void read(RecipeLogic logic, EvalContext ctx) {
            ctx.setOutput("value", logic.isWorking());
        }
    }

    /** No recipe matched. */
    @NodeAttribute(name = "mbd2_recipe_logic_is_idle", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(RecipeLogicInfoNode.class)
    public static class IsIdle extends LogicBlock {
        @OutputPort public boolean value;

        @Override
        protected void read(RecipeLogic logic, EvalContext ctx) {
            ctx.setOutput("value", logic.isIdle());
        }
    }

    /** A recipe matched but cannot proceed — see {@link WaitingReason}. */
    @NodeAttribute(name = "mbd2_recipe_logic_is_waiting", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(RecipeLogicInfoNode.class)
    public static class IsWaiting extends LogicBlock {
        @OutputPort public boolean value;

        @Override
        protected void read(RecipeLogic logic, EvalContext ctx) {
            ctx.setOutput("value", logic.isWaiting());
        }
    }

    /** Switched off, by {@code Set Working Enabled} or by something else. */
    @NodeAttribute(name = "mbd2_recipe_logic_is_suspend", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(RecipeLogicInfoNode.class)
    public static class IsSuspend extends LogicBlock {
        @OutputPort public boolean value;

        @Override
        protected void read(RecipeLogic logic, EvalContext ctx) {
            ctx.setOutput("value", logic.isSuspend());
        }
    }

    /** Working, waiting, or suspended mid-recipe — i.e. the machine has something in progress. */
    @NodeAttribute(name = "mbd2_recipe_logic_is_active", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(RecipeLogicInfoNode.class)
    public static class IsActive extends LogicBlock {
        @OutputPort public boolean value;

        @Override
        protected void read(RecipeLogic logic, EvalContext ctx) {
            ctx.setOutput("value", logic.isActive());
        }
    }

    /** Why the recipe cannot proceed, when waiting. Absent otherwise. */
    @NodeAttribute(name = "mbd2_recipe_logic_waiting_reason", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(RecipeLogicInfoNode.class)
    public static class WaitingReason extends LogicBlock {
        @OutputPort public Component value;

        @Override
        protected void read(RecipeLogic logic, EvalContext ctx) {
            ctx.setOutput("value", logic.getWaitingReason());
        }
    }

    // ---- progress ----------------------------------------------------------------------------

    /** Ticks of progress into the current recipe. */
    @NodeAttribute(name = "mbd2_recipe_logic_progress", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(RecipeLogicInfoNode.class)
    public static class Progress extends LogicBlock {
        @OutputPort public int value;

        @Override
        protected void read(RecipeLogic logic, EvalContext ctx) {
            ctx.setOutput("value", logic.getProgress());
        }
    }

    /** How many ticks the current recipe takes. Zero when nothing is running. */
    @NodeAttribute(name = "mbd2_recipe_logic_max_progress", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(RecipeLogicInfoNode.class)
    public static class MaxProgress extends LogicBlock {
        @OutputPort public int value;

        @Override
        protected void read(RecipeLogic logic, EvalContext ctx) {
            ctx.setOutput("value", logic.getMaxProgress());
        }
    }

    /** Progress as 0..1, ready to drive a bar. */
    @NodeAttribute(name = "mbd2_recipe_logic_progress_percent", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(RecipeLogicInfoNode.class)
    public static class ProgressPercent extends LogicBlock {
        @OutputPort public float value;

        @Override
        protected void read(RecipeLogic logic, EvalContext ctx) {
            ctx.setOutput("value", logic.getProgressPercent());
        }
    }

    /** How long the machine has been running without going idle. Resets when it stops. */
    @NodeAttribute(name = "mbd2_recipe_logic_running_time", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(RecipeLogicInfoNode.class)
    public static class ContinuousRunningTime extends LogicBlock {
        @OutputPort public long value;

        @Override
        protected void read(RecipeLogic logic, EvalContext ctx) {
            ctx.setOutput("value", logic.getTotalContinuousRunningTime());
        }
    }

    // ---- the recipe --------------------------------------------------------------------------

    /** The recipe being run, after modifiers. Absent when idle. */
    @NodeAttribute(name = "mbd2_recipe_logic_last_recipe", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(RecipeLogicInfoNode.class)
    public static class LastRecipe extends LogicBlock {
        @OutputPort public MBDRecipe value;

        @Override
        protected void read(RecipeLogic logic, EvalContext ctx) {
            ctx.setOutput("value", logic.getLastRecipe());
        }
    }

    /**
     * The id of the recipe as it was matched, before modifiers.
     *
     * <p>Unlike {@link LastRecipe} this one is safe to compare against a datapack recipe id — a
     * modified recipe is a fresh object that the recipe manager has never heard of.</p>
     */
    @NodeAttribute(name = "mbd2_recipe_logic_origin_recipe_id", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(RecipeLogicInfoNode.class)
    public static class OriginRecipeId extends LogicBlock {
        @OutputPort public ResourceLocation value;

        @Override
        protected void read(RecipeLogic logic, EvalContext ctx) {
            ctx.setOutput("value", logic.getLastOriginRecipe());
        }
    }

    // ---- fuel --------------------------------------------------------------------------------

    /** Whether this machine's recipe type burns fuel. */
    @NodeAttribute(name = "mbd2_recipe_logic_need_fuel", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(RecipeLogicInfoNode.class)
    public static class NeedFuel extends LogicBlock {
        @OutputPort public boolean value;

        @Override
        protected void read(RecipeLogic logic, EvalContext ctx) {
            ctx.setOutput("value", logic.needFuel());
        }
    }

    /** Ticks of fuel left. */
    @NodeAttribute(name = "mbd2_recipe_logic_fuel_time", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(RecipeLogicInfoNode.class)
    public static class FuelTime extends LogicBlock {
        @OutputPort public int value;

        @Override
        protected void read(RecipeLogic logic, EvalContext ctx) {
            ctx.setOutput("value", logic.getFuelTime());
        }
    }

    /** Fuel remaining as 0..1, ready to drive a bar. */
    @NodeAttribute(name = "mbd2_recipe_logic_fuel_percent", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(RecipeLogicInfoNode.class)
    public static class FuelPercent extends LogicBlock {
        @OutputPort public float value;

        @Override
        protected void read(RecipeLogic logic, EvalContext ctx) {
            ctx.setOutput("value", logic.getFuelProgressPercent());
        }
    }

    /** The fuel recipe currently burning. Absent when nothing is. */
    @NodeAttribute(name = "mbd2_recipe_logic_last_fuel_recipe", group = GROUP, graphTypes = MachineBlueprintGraph.class)
    @UseWithContext(RecipeLogicInfoNode.class)
    public static class LastFuelRecipe extends LogicBlock {
        @OutputPort public MBDRecipe value;

        @Override
        protected void read(RecipeLogic logic, EvalContext ctx) {
            ctx.setOutput("value", logic.getLastFuelRecipe());
        }
    }
}
