package com.lowdragmc.mbd2.test.tests.recipe.logic;

import com.google.common.collect.Table;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandler;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.machine.IMultiController;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.api.recipe.content.ContentModifier;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import com.lowdragmc.mbd2.common.machine.MBDPartMachine;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.MultiblockMachineDefinition;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Machines that override the recipe hooks the way an addon written against the single-logic API does: the
 * pre-multi-logic signatures, each counting its calls and then deferring to {@code super}.
 *
 * <p>They pin two things the multi-logic work must not break: every legacy override still runs, exactly as often
 * as it did, and calling {@code super} still reaches MBD2's own behaviour (events, part fan-out).</p>
 */
public final class LegacyHookMachines {
    private LegacyHookMachines() {}

    /** Call counts by hook name, sorted so a failure message reads the same every run. */
    public static final class Counters {
        private final Map<String, Integer> counts = new TreeMap<>();
        private final Map<String, Integer> contexts = new TreeMap<>();

        /** @param context the logic the hook saw as {@code getContextRecipeLogic()}, or null where there is none */
        void hit(String hook, @Nullable RecipeLogic context) {
            counts.merge(hook, 1, Integer::sum);
            contexts.merge(context == null ? "<none>" : context.getAddress(), 1, Integer::sum);
        }

        /** How many hook calls saw each logic (by address) as their context. */
        public Map<String, Integer> contexts() {
            return new TreeMap<>(contexts);
        }

        public int get(String hook) {
            return counts.getOrDefault(hook, 0);
        }

        public Map<String, Integer> snapshot() {
            return new TreeMap<>(counts);
        }

        public void reset() {
            counts.clear();
            contexts.clear();
        }
    }

    public interface Counted {
        Counters counters();
    }

    /** A simple machine. Every simple definition builds an {@link MBDPartMachine}, so this extends it too. */
    public static class SimpleMachine extends MBDPartMachine implements Counted {
        private final Counters counters = new Counters();

        public SimpleMachine(IMachineBlockEntity holder, MBDMachineDefinition definition) {
            super(holder, definition);
        }

        @Override
        public Counters counters() {
            return counters;
        }

        @Override
        public boolean beforeWorking(MBDRecipe recipe) {
            counters.hit("beforeWorking", getContextRecipeLogic());
            return super.beforeWorking(recipe);
        }

        @Override
        public boolean onWorking() {
            counters.hit("onWorking", getContextRecipeLogic());
            return super.onWorking();
        }

        @Override
        public void onWaiting() {
            counters.hit("onWaiting", getContextRecipeLogic());
            super.onWaiting();
        }

        @Override
        public void afterWorking() {
            counters.hit("afterWorking", getContextRecipeLogic());
            super.afterWorking();
        }

        @Override
        public void onRecipeFinish() {
            counters.hit("onRecipeFinish", getContextRecipeLogic());
            super.onRecipeFinish();
        }

        @Override
        public void onConsumeInputsAfterWorking() {
            counters.hit("onConsumeInputsAfterWorking", getContextRecipeLogic());
            super.onConsumeInputsAfterWorking();
        }

        @Override
        public void notifyRecipeStatusChanged(RecipeLogic.Status oldStatus, RecipeLogic.Status newStatus) {
            counters.hit("notifyRecipeStatusChanged(" + oldStatus + "->" + newStatus + ")", getContextRecipeLogic());
            super.notifyRecipeStatusChanged(oldStatus, newStatus);
        }

        @Nullable
        @Override
        public MBDRecipe doModifyRecipe(MBDRecipe recipe) {
            counters.hit("doModifyRecipe", getContextRecipeLogic());
            return super.doModifyRecipe(recipe);
        }

        @Nullable
        @Override
        public MBDRecipe getModifiedRecipe(MBDRecipe recipe) {
            counters.hit("getModifiedRecipe", getContextRecipeLogic());
            return super.getModifiedRecipe(recipe);
        }

        @Override
        public ContentModifier getMaxParallel(MBDRecipe recipe) {
            counters.hit("getMaxParallel", getContextRecipeLogic());
            return super.getMaxParallel(recipe);
        }

        @Override
        public MBDRecipe applyParallel(MBDRecipe recipe, int maxParallel) {
            counters.hit("applyParallel", getContextRecipeLogic());
            return super.applyParallel(recipe, maxParallel);
        }

        @Override
        public boolean alwaysTryModifyRecipe() {
            counters.hit("alwaysTryModifyRecipe", getContextRecipeLogic());
            return super.alwaysTryModifyRecipe();
        }

        @Override
        public boolean alwaysReSearchRecipe() {
            counters.hit("alwaysReSearchRecipe", getContextRecipeLogic());
            return super.alwaysReSearchRecipe();
        }

        @Override
        public boolean consumeInputsAfterWorking(MBDRecipe recipe) {
            counters.hit("consumeInputsAfterWorking", getContextRecipeLogic());
            return super.consumeInputsAfterWorking(recipe);
        }

        @Override
        public boolean dampingWhenWaiting() {
            counters.hit("dampingWhenWaiting", getContextRecipeLogic());
            return super.dampingWhenWaiting();
        }

        @Override
        public int getRecipeDampingValue() {
            counters.hit("getRecipeDampingValue", getContextRecipeLogic());
            return super.getRecipeDampingValue();
        }

        @Nullable
        @Override
        public MBDRecipe modifyFuelRecipe(MBDRecipe recipe) {
            counters.hit("modifyFuelRecipe", getContextRecipeLogic());
            return super.modifyFuelRecipe(recipe);
        }

        @Override
        public void onFuelBurningFinish(@Nullable MBDRecipe recipe) {
            counters.hit("onFuelBurningFinish", getContextRecipeLogic());
            super.onFuelBurningFinish(recipe);
        }
    }

    /** A multiblock controller overriding the same hooks. */
    public static class Controller extends MBDMultiblockMachine implements Counted {
        private final Counters counters = new Counters();

        public Controller(IMachineBlockEntity holder, MBDMachineDefinition definition) {
            super(holder, (MultiblockMachineDefinition) definition);
        }

        @Override
        public Counters counters() {
            return counters;
        }

        @Override
        public boolean beforeWorking(MBDRecipe recipe) {
            counters.hit("beforeWorking", getContextRecipeLogic());
            return super.beforeWorking(recipe);
        }

        @Override
        public boolean onWorking() {
            counters.hit("onWorking", getContextRecipeLogic());
            return super.onWorking();
        }

        @Override
        public void onWaiting() {
            counters.hit("onWaiting", getContextRecipeLogic());
            super.onWaiting();
        }

        @Override
        public void afterWorking() {
            counters.hit("afterWorking", getContextRecipeLogic());
            super.afterWorking();
        }

        @Override
        public void onRecipeFinish() {
            counters.hit("onRecipeFinish", getContextRecipeLogic());
            super.onRecipeFinish();
        }

        @Override
        public void notifyRecipeStatusChanged(RecipeLogic.Status oldStatus, RecipeLogic.Status newStatus) {
            counters.hit("notifyRecipeStatusChanged(" + oldStatus + "->" + newStatus + ")", getContextRecipeLogic());
            super.notifyRecipeStatusChanged(oldStatus, newStatus);
        }

        @Nullable
        @Override
        public MBDRecipe doModifyRecipe(MBDRecipe recipe) {
            counters.hit("doModifyRecipe", getContextRecipeLogic());
            return super.doModifyRecipe(recipe);
        }

        @Nullable
        @Override
        public MBDRecipe getModifiedRecipe(MBDRecipe recipe) {
            counters.hit("getModifiedRecipe", getContextRecipeLogic());
            return super.getModifiedRecipe(recipe);
        }

        @Override
        public ContentModifier getMaxParallel(MBDRecipe recipe) {
            counters.hit("getMaxParallel", getContextRecipeLogic());
            return super.getMaxParallel(recipe);
        }

        @Override
        public boolean alwaysTryModifyRecipe() {
            counters.hit("alwaysTryModifyRecipe", getContextRecipeLogic());
            return super.alwaysTryModifyRecipe();
        }

        @Override
        public boolean consumeInputsAfterWorking(MBDRecipe recipe) {
            counters.hit("consumeInputsAfterWorking", getContextRecipeLogic());
            return super.consumeInputsAfterWorking(recipe);
        }
    }

    /**
     * Overrides the legacy getters the way an addon swapping its recipe type at runtime would: it runs
     * {@link MultiRecipeLogicFixtures#TYPE_B} whatever its definition says, and counts the reads of its handler table.
     */
    public static class LegacyGettersMachine extends MBDPartMachine {
        public int tableReads;

        public LegacyGettersMachine(IMachineBlockEntity holder, MBDMachineDefinition definition) {
            super(holder, definition);
        }

        @Override
        public MBDRecipeType getRecipeType() {
            return MBDRegistries.RECIPE_TYPES.get(MultiRecipeLogicFixtures.TYPE_B);
        }

        @Override
        public Table<IO, RecipeCapability<?>, List<IRecipeHandler<?>>> getRecipeCapabilitiesProxy() {
            tableReads++;
            return super.getRecipeCapabilitiesProxy();
        }
    }

    /** A part overriding the controller-facing hooks of {@link com.lowdragmc.mbd2.api.machine.IMultiPart}. */
    public static class Part extends MBDPartMachine implements Counted {
        private final Counters counters = new Counters();

        public Part(IMachineBlockEntity holder, MBDMachineDefinition definition) {
            super(holder, definition);
        }

        @Override
        public Counters counters() {
            return counters;
        }

        @Override
        public void notifyControllerRecipeStatusChanged(IMultiController controller, RecipeLogic.Status oldStatus, RecipeLogic.Status newStatus) {
            counters.hit("notifyControllerRecipeStatusChanged(" + oldStatus + "->" + newStatus + ")", controller.getContextRecipeLogic());
            super.notifyControllerRecipeStatusChanged(controller, oldStatus, newStatus);
        }

        @Override
        public boolean onControllerWorking(IMultiController controller) {
            counters.hit("onControllerWorking", controller.getContextRecipeLogic());
            return super.onControllerWorking(controller);
        }

        @Override
        public void onControllerWaiting(IMultiController controller) {
            counters.hit("onControllerWaiting", controller.getContextRecipeLogic());
            super.onControllerWaiting(controller);
        }

        @Override
        public void afterControllerWorking(IMultiController controller) {
            counters.hit("afterControllerWorking", controller.getContextRecipeLogic());
            super.afterControllerWorking(controller);
        }

        @Override
        public boolean beforeControllerWorking(IMultiController controller) {
            counters.hit("beforeControllerWorking", controller.getContextRecipeLogic());
            return super.beforeControllerWorking(controller);
        }

        @Override
        public MBDRecipe modifyControllerRecipe(MBDRecipe recipe, RecipeLogic controllerRecipeLogic) {
            counters.hit("modifyControllerRecipe", controllerRecipeLogic);
            return super.modifyControllerRecipe(recipe, controllerRecipeLogic);
        }

        @Override
        public ContentModifier getMaxControllerParallel(MBDRecipe recipe, RecipeLogic controllerRecipeLogic) {
            counters.hit("getMaxControllerParallel", controllerRecipeLogic);
            return super.getMaxControllerParallel(recipe, controllerRecipeLogic);
        }

        @Override
        public boolean alwaysTryModifyControllerRecipe() {
            counters.hit("alwaysTryModifyControllerRecipe", null);
            return super.alwaysTryModifyControllerRecipe();
        }
    }
}
