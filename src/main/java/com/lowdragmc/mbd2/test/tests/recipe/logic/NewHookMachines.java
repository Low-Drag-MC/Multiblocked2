package com.lowdragmc.mbd2.test.tests.recipe.logic;

import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.machine.IMultiController;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.api.recipe.content.ContentModifier;
import com.lowdragmc.mbd2.common.machine.MBDPartMachine;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A machine overriding only the per-logic {@code (RecipeLogic, ...)} hooks, each counting its calls and the logic it
 * was handed, then deferring to {@code super}. Its counts must equal what {@link LegacyHookMachines} counts on the
 * legacy hooks for the same run: every hook a logic triggers goes through its per-logic form exactly once.
 */
public class NewHookMachines {
    private NewHookMachines() {}

    public static class SimpleMachine extends MBDPartMachine implements LegacyHookMachines.Counted {
        private final LegacyHookMachines.Counters counters = new LegacyHookMachines.Counters();
        /** Calls per logic address, or {@code foreign} for a logic of another machine. */
        public final Map<String, Integer> logics = new TreeMap<>();

        public SimpleMachine(IMachineBlockEntity holder, MBDMachineDefinition definition) {
            super(holder, definition);
        }

        @Override
        public LegacyHookMachines.Counters counters() {
            return counters;
        }

        private void hit(String hook, RecipeLogic logic) {
            counters.hit(hook, logic);
            logics.merge(logic.getMachine() == this ? logic.getAddress() : "foreign", 1, Integer::sum);
        }

        @Override
        public void notifyRecipeStatusChanged(RecipeLogic logic, RecipeLogic.Status oldStatus, RecipeLogic.Status newStatus) {
            hit("notifyRecipeStatusChanged(" + oldStatus + "->" + newStatus + ")", logic);
            super.notifyRecipeStatusChanged(logic, oldStatus, newStatus);
        }

        @Override
        public @Nullable MBDRecipe modifyFuelRecipe(RecipeLogic logic, MBDRecipe recipe) {
            hit("modifyFuelRecipe", logic);
            return super.modifyFuelRecipe(logic, recipe);
        }

        @Override
        public void onFuelBurningFinish(RecipeLogic logic, @Nullable MBDRecipe recipe) {
            hit("onFuelBurningFinish", logic);
            super.onFuelBurningFinish(logic, recipe);
        }

        @Override
        public @Nullable MBDRecipe doModifyRecipe(RecipeLogic logic, @NotNull MBDRecipe recipe) {
            hit("doModifyRecipe", logic);
            return super.doModifyRecipe(logic, recipe);
        }

        @Override
        public @Nullable MBDRecipe getModifiedRecipe(RecipeLogic logic, @NotNull MBDRecipe recipe) {
            hit("getModifiedRecipe", logic);
            return super.getModifiedRecipe(logic, recipe);
        }

        @Override
        public ContentModifier getMaxParallel(RecipeLogic logic, @NotNull MBDRecipe recipe) {
            hit("getMaxParallel", logic);
            return super.getMaxParallel(logic, recipe);
        }

        @Override
        public @NotNull MBDRecipe applyParallel(RecipeLogic logic, @NotNull MBDRecipe recipe, int maxParallel) {
            hit("applyParallel", logic);
            return super.applyParallel(logic, recipe, maxParallel);
        }

        @Override
        public boolean beforeWorking(RecipeLogic logic, MBDRecipe recipe) {
            hit("beforeWorking", logic);
            return super.beforeWorking(logic, recipe);
        }

        @Override
        public boolean onWorking(RecipeLogic logic) {
            hit("onWorking", logic);
            return super.onWorking(logic);
        }

        @Override
        public void onWaiting(RecipeLogic logic) {
            hit("onWaiting", logic);
            super.onWaiting(logic);
        }

        @Override
        public void afterWorking(RecipeLogic logic) {
            hit("afterWorking", logic);
            super.afterWorking(logic);
        }

        @Override
        public void onConsumeInputsAfterWorking(RecipeLogic logic) {
            hit("onConsumeInputsAfterWorking", logic);
            super.onConsumeInputsAfterWorking(logic);
        }

        @Override
        public void onRecipeFinish(RecipeLogic logic) {
            hit("onRecipeFinish", logic);
            super.onRecipeFinish(logic);
        }

        @Override
        public boolean dampingWhenWaiting(RecipeLogic logic) {
            hit("dampingWhenWaiting", logic);
            return super.dampingWhenWaiting(logic);
        }

        @Override
        public boolean alwaysTryModifyRecipe(RecipeLogic logic) {
            hit("alwaysTryModifyRecipe", logic);
            return super.alwaysTryModifyRecipe(logic);
        }

        @Override
        public boolean alwaysReSearchRecipe(RecipeLogic logic) {
            hit("alwaysReSearchRecipe", logic);
            return super.alwaysReSearchRecipe(logic);
        }

        @Override
        public int getRecipeDampingValue(RecipeLogic logic) {
            hit("getRecipeDampingValue", logic);
            return super.getRecipeDampingValue(logic);
        }

        @Override
        public boolean consumeInputsAfterWorking(RecipeLogic logic, MBDRecipe recipe) {
            hit("consumeInputsAfterWorking", logic);
            return super.consumeInputsAfterWorking(logic, recipe);
        }
    }

    /** A part writing down the status changes its controller tells it: each logic's own, and the machine-wide ones. */
    public static class Part extends MBDPartMachine {
        public final List<String> perLogic = new ArrayList<>();
        public final List<String> machineWide = new ArrayList<>();

        public Part(IMachineBlockEntity holder, MBDMachineDefinition definition) {
            super(holder, definition);
        }

        @Override
        public void notifyControllerRecipeStatusChanged(IMultiController controller, RecipeLogic logic,
                                                        RecipeLogic.Status oldStatus, RecipeLogic.Status newStatus) {
            perLogic.add(logic.getAddress() + ":" + oldStatus + "->" + newStatus);
            super.notifyControllerRecipeStatusChanged(controller, logic, oldStatus, newStatus);
        }

        @Override
        public void notifyControllerRecipeStatusChanged(IMultiController controller, RecipeLogic.Status oldStatus,
                                                        RecipeLogic.Status newStatus) {
            machineWide.add(oldStatus + "->" + newStatus);
            super.notifyControllerRecipeStatusChanged(controller, oldStatus, newStatus);
        }
    }
}
