package com.lowdragmc.mbd2.api.recipe;

import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.syncdata.IEnhancedManaged;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.annotation.RequireRerender;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.config.ConfigHolder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.VisibleForTesting;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RecipeLogic implements IEnhancedManaged {
    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(RecipeLogic.class);
    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onChanged() {
        machine.markDirty();
    }

    @Override
    public void scheduleRenderUpdate() {
        machine.scheduleRenderUpdate();
    }

    public enum Status {
        IDLE, WORKING, WAITING, SUSPEND
    }
    @Getter
    public final IMachine machine;
    public List<MBDRecipe> lastFailedMatches;

    @Getter @Persisted @DescSynced @RequireRerender
    private Status status = Status.IDLE;

    @Nullable
    @Persisted @DescSynced
    @Getter
    private Component waitingReason = null;
    /**
     * unsafe, it may not be found from {@link RecipeManager}. Do not index it.
     */
    @Nullable @Getter @Persisted @Setter
    protected MBDRecipe lastRecipe;
    /**
     * safe, it is the origin recipe before {@link IMachine#doModifyRecipe(MBDRecipe)}' which can be found from {@link RecipeManager}.
     */
    @Nullable @Getter @Persisted
    protected MBDRecipe lastOriginRecipe;
    @Getter @Persisted @Setter
    protected boolean consumeInputsAfterWorking;
    @Persisted
    @Getter @Setter
    protected int progress;
    @Getter @Persisted @Setter
    protected int duration;
    @Getter @Persisted @Setter
    protected int fuelTime;
    @Nullable @Getter @Persisted @Setter
    protected MBDRecipe lastFuelRecipe;
    @Getter @Persisted @Setter
    protected int fuelMaxTime;
    @Getter(onMethod_ = @VisibleForTesting)
    protected boolean recipeDirty;
    @Persisted
    @Getter @Setter
    protected long totalContinuousRunningTime;
    @Nullable
    protected CompletableFuture<List<MBDRecipe>> completableFuture = null;

    public RecipeLogic(IMachine machine) {
        this.machine = machine;
    }

    /**
     * Call it to abort current recipe and reset the first state.
     */
    public void resetRecipeLogic() {
        interruptRecipe();
        recipeDirty = false;
        lastRecipe = null;
        lastOriginRecipe = null;
        progress = 0;
        duration = 0;
        fuelTime = 0;
        lastFailedMatches = null;
        consumeInputsAfterWorking = false;
        setStatus(Status.IDLE);
    }

    public double getProgressPercent() {
        return duration == 0 ? 0.0 : progress / (duration * 1.0);
    }

    public double getFuelProgressPercent() {
        return fuelMaxTime == 0 ? 0.0 : fuelTime / (fuelMaxTime * 1.0);
    }

    public boolean needFuel() {
        if (machine.getRecipeType().isRequireFuelForWorking()){
            return true;
        }
        return false;
    }

    /**
     * it should be called on the server side restrictively.
     */
    public RecipeManager getRecipeManager() {
        return Platform.getMinecraftServer().getRecipeManager();
    }

    public void serverTick() {
        if (!isSuspend()) {
            if (!isIdle() && lastRecipe != null) {
                if (progress < duration) {
                    handleRecipeWorking();
                }
                if (isIdle() || duration == 0) {
                    // interrupt recipe
                } else if (progress >= duration) {
                    onRecipeFinish();
                }
            } else if (lastRecipe != null) {
                findAndHandleRecipe();
            } else if (getMachine().getOffsetTimer() % 5 == 0) {
                findAndHandleRecipe();
                if (lastFailedMatches != null) {
                    for (MBDRecipe match : lastFailedMatches) {
                        if (checkMatchedRecipeAvailable(match)) break;
                    }
                }
            }
        }
        if (fuelTime > 0) {
            fuelTime--;
            if (fuelTime == 0) {
                getMachine().onFuelBurningFinish(lastFuelRecipe);
            }
        } else {
            if (isSuspend()) {
                if (completableFuture != null) {
                    completableFuture.cancel(true);
                    completableFuture = null;
                }
            }
        }
    }

    /**
     * Checks if a matched recipe is available, processes the recipe based on the machine's current state,
     * and updates the recipe logic if the conditions are met.
     *
     * @param match the MBDRecipe object representing the matched recipe to be checked and potentially set up.
     * @return true if the matched recipe is successfully processed and set up in the machine, false otherwise.
     */
    protected boolean checkMatchedRecipeAvailable(MBDRecipe match) {
        var modified = machine.doModifyRecipe(match);
        if (modified != null) {
            if (modified.checkConditions(this).isSuccess() &&
                    modified.matchRecipe(machine).isSuccess() &&
                    modified.matchTickRecipe(machine).isSuccess()) {
                setupRecipe(modified);
            }
            if (lastRecipe != null && getStatus() == Status.WORKING) {
                lastOriginRecipe = match;
                lastFailedMatches = null;
                return true;
            }
        }
        return false;
    }

    /**
     * Handles the execution of the current recipe in the machine, managing its progression, fuel requirements,
     * status transitions, and error conditions.
     * <br>
     * The method checks if the conditions for the currently assigned recipe are met. If successful, it ensures
     * that sufficient fuel is available for the recipe. It then processes the recipe logic for the current tick,
     * updates the machine's status to `WORKING`, and increments the progress. If the machine requests interruption
     * during processing, the recipe will be interrupted immediately.
     * <br>
     * If the conditions check or fuel availability fails, the recipe status is set to {@code WAITING} with an appropriate
     * explanation, and progress damping may occur. Transitions in machine status trigger pre or post working
     * operations on the current recipe.
     * <br>
     * The method ensures consistent handling of machine state, fuel usage, and recipe progress while accommodating
     * potential interruptions and waiting states.
     */
    public void handleRecipeWorking() {
        Status last = this.status;
        assert lastRecipe != null;
        if (consumeInputsAfterWorking) {
            if (!lastRecipe.matchRecipe(machine).isSuccess()) {
                interruptRecipe();
                return;
            }
        }
        var result = lastRecipe.checkConditions(this);
        if (result.isSuccess()) {
            if (handleFuelRecipe()) {
                result = handleTickRecipe(lastRecipe);
                if (result.isSuccess()) {
                    setStatus(Status.WORKING);
                    if (machine.onWorking()) {
                        this.interruptRecipe();
                        return;
                    }
                    progress++;
                    totalContinuousRunningTime++;
                } else {
                    setWaiting(result.reason().get());
                }
            } else {
                setWaiting(Component.translatable("mbd2.recipe_logic.insufficient_fuel"));
            }
        } else {
            setWaiting(result.reason().get());
        }
        if (isWaiting()) {
            doDamping();
        }
        if (last == Status.WORKING && getStatus() != Status.WORKING) {
            lastRecipe.postWorking(machine);
        } else if (last != Status.WORKING && getStatus() == Status.WORKING) {
            lastRecipe.preWorking(machine);
        }
    }

    /**
     * Applies damping to the recipe progress if the machine is in a waiting state and damping is allowed.
     * This method decreases the `{@code progress} field by a specified damping value retrieved from the machine,
     * ensuring that the progress does not fall below zero.
     */
    protected void doDamping() {
        if (progress > 0 && machine.dampingWhenWaiting()) {
            this.progress = Math.max(0, progress - getMachine().getRecipeDampingValue());
        }
    }

    protected List<MBDRecipe> searchRecipe() {
        return machine.getRecipeType().searchRecipe(getRecipeManager(), this.machine);
    }


    /**
     * Finds and handles an appropriate recipe to process in the machine. The method determines the current state
     * of recipe processing and either continues handling the last valid recipe or searches for a new recipe.
     * <br>
     * The following steps are performed:
     * <lu>
     * <li>If the {@code recipeDirty} flag is not set and a valid {@code lastRecipe} exists, checks the recipe's validity
     * and conditions. If all checks succeed, the recipe is set up for processing.</li>
     * <li>If no valid {@code lastRecipe} exists or the {@code recipeDirty} flag is set, a new recipe is searched for.
     * This can involve starting an asynchronous task (if allowed by configuration) or performing a synchronous search.</li>
     * <li>If an asynchronous task is already in progress, the method checks for its completion:
     *  <ul>
     *   <li>If the task is complete and not canceled, filters the resulting recipes for valid matches and processes them.</li>
     *   <li>If the task encounters errors or is canceled, a new asynchronous task is initiated or a synchronous search is performed.</li>
     *  </ul>
     * </li>
     * <li>The {@code recipeDirty} flag is reset at the end of the method.</li>
     * </lu>
     * <br>
     * This method ensures efficient handling of recipe logic while accommodating async or sync recipe searches
     * and error handling.
     */
    public void findAndHandleRecipe() {
        lastFailedMatches = null;
        // try to execute last recipe if possible
        if (!recipeDirty && lastRecipe != null &&
                lastRecipe.matchRecipe(this.machine).isSuccess() &&
                lastRecipe.matchTickRecipe(this.machine).isSuccess() &&
                lastRecipe.checkConditions(this).isSuccess()) {
            MBDRecipe recipe = lastRecipe;
            lastRecipe = null;
            lastOriginRecipe = null;
            setupRecipe(recipe);
        } else { // try to find and handle a new recipe
            lastRecipe = null;
            lastOriginRecipe = null;
            if (completableFuture == null) {
                // try to search recipe in threads.
                if (ConfigHolder.asyncRecipeSearching) {
                    completableFuture = supplyAsyncSearchingTask();
                } else {
                    handleSearchingRecipes(searchRecipe());
                }
            } else if (completableFuture.isDone()) {
                var lastFuture = this.completableFuture;
                completableFuture = null;
                if (!lastFuture.isCancelled()) {
                    // if searching task is done, try to handle searched recipes.
                    try {
                        var matches = lastFuture.join().stream().filter(match -> match.matchRecipe(machine).isSuccess()).toList();
                        if (!matches.isEmpty()) {
                            handleSearchingRecipes(matches);
                        }
                    } catch (Throwable throwable) {
                        // if error occurred, schedule a new async task.
                        completableFuture = supplyAsyncSearchingTask();
                    }
                } else {
                    handleSearchingRecipes(searchRecipe());
                }
            }
        }
        recipeDirty = false;
    }

    private CompletableFuture<List<MBDRecipe>> supplyAsyncSearchingTask() {
        return CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("Searching recipes", this::searchRecipe), Util.backgroundExecutor());
    }

    private void handleSearchingRecipes(List<MBDRecipe> matches) {
        for (MBDRecipe match : matches) {
            // try to modify recipe by machine, such as overclock, tier checking.
            if (checkMatchedRecipeAvailable(match)) break;
            // cache matching recipes.
            if (lastFailedMatches == null) {
                lastFailedMatches = new ArrayList<>();
            }
            lastFailedMatches.add(match);
        }
    }

    public boolean handleFuelRecipe() {
        if (!needFuel() || fuelTime > 0) return true;
        lastFuelRecipe = null;
        for (MBDRecipe recipe : machine.getRecipeType().searchFuelRecipe(getRecipeManager(), machine)) {
            recipe = getMachine().modifyFuelRecipe(recipe);
            if (recipe.checkConditions(this).isSuccess() && recipe.handleRecipeIO(IO.IN, this.machine)) {
                fuelMaxTime = recipe.duration;
                fuelTime = fuelMaxTime;
                lastFuelRecipe = recipe;
            }
            if (fuelTime > 0) return true;
        }
        return false;
    }

    public MBDRecipe.ActionResult handleTickRecipe(MBDRecipe recipe) {
        if (recipe.hasTick()) {
            var result = recipe.matchTickRecipe(this.machine);
            if (result.isSuccess()) {
                recipe.handleTickRecipeIO(IO.IN, this.machine);
                recipe.handleTickRecipeIO(IO.OUT, this.machine);
            } else {
                return result;
            }
        }
        return MBDRecipe.ActionResult.SUCCESS;
    }

    /**
     * Set up a recipe for processing. you can schedule it actively if you want.
     */
    public void setupRecipe(MBDRecipe recipe) {
        if (handleFuelRecipe()) {
            if (machine.beforeWorking(recipe)) {
                setStatus(Status.IDLE);
                progress = 0;
                duration = 0;
                consumeInputsAfterWorking = false;
                return;
            }
            recipe.preWorking(this.machine);
            consumeInputsAfterWorking = machine.consumeInputsAfterWorking(recipe);
            if (consumeInputsAfterWorking || recipe.handleRecipeIO(IO.IN, this.machine)) {
                recipeDirty = false;
                lastRecipe = recipe;
                setStatus(Status.WORKING);
                progress = 0;
                duration = recipe.duration;
            }
        }
    }

    public void setStatus(Status status) {
        if (this.status != status) {
            if (this.status == Status.WORKING) {
                this.totalContinuousRunningTime = 0;
            }
            machine.notifyRecipeStatusChanged(this.status, status);
            this.status = status;
            if (this.status != Status.WAITING) {
                waitingReason = null;
            }
        }
    }

    public void setWaiting(@Nullable Component reason) {
        setStatus(Status.WAITING);
        waitingReason = reason;
        machine.onWaiting();
    }

    /**
     * mark current handling recipe (if exist) as dirty.
     * do not try it immediately in the next round
     */
    public void markLastRecipeDirty() {
        this.recipeDirty = true;
    }

    public boolean isWorking() {
        return status == Status.WORKING;
    }

    public boolean isIdle() {
        return status == Status.IDLE;
    }

    public boolean isWaiting() {
        return status == Status.WAITING;
    }

    public boolean isSuspend() {
        return status == Status.SUSPEND;
    }

    /**
     * Toggle working enabled. If machine is not allowed to work, the working machine will be suspended.
     * @param isWorkingAllowed whether machine is allowed to work.
     */
    public void setWorkingEnabled(boolean isWorkingAllowed) {
        if (!isWorkingAllowed) {
            setStatus(Status.SUSPEND);
        } else {
            if (lastRecipe != null && duration > 0) {
                setStatus(Status.WORKING);
            } else {
                setStatus(Status.IDLE);
            }
        }
    }

    public int getMaxProgress() {
        return duration;
    }

    /**
     * @return whether machine is active, i.e. working, waiting or suspend.
     */
    public boolean isActive() {
        return isWorking() || isWaiting() || (isSuspend() && lastRecipe != null && duration > 0);
    }

    @Deprecated
    public boolean isHasNotEnoughEnergy() {
        return isWaiting();
    }

    public void onRecipeFinish() {
        if (lastRecipe != null) {
            machine.afterWorking();
            if (consumeInputsAfterWorking) {
                lastRecipe.handleRecipeIO(IO.IN, this.machine);
                consumeInputsAfterWorking = false;
                machine.onConsumeInputsAfterWorking();
            }
            lastRecipe.postWorking(this.machine);
            lastRecipe.handleRecipeIO(IO.OUT, this.machine);
            machine.onRecipeFinish();
            if (machine.alwaysReSearchRecipe()) {
                markLastRecipeDirty();
            }
            if (!recipeDirty && machine.alwaysTryModifyRecipe()) {
                if (lastOriginRecipe != null) {
                    var modified = machine.doModifyRecipe(lastOriginRecipe);
                    if (modified == null) {
                        markLastRecipeDirty();
                    } else {
                        lastRecipe = modified;
                    }
                } else {
                    markLastRecipeDirty();
                }
            }
            // try it again
            if (!recipeDirty &&
                    lastRecipe.matchRecipe(this.machine).isSuccess() &&
                    lastRecipe.matchTickRecipe(this.machine).isSuccess() &&
                    lastRecipe.checkConditions(this).isSuccess()) {
                setupRecipe(lastRecipe);
            } else {
                setStatus(Status.IDLE);
                progress = 0;
                duration = 0;
            }
        }
    }

    /**
     * Interrupt current recipe without io.
     */
    public void interruptRecipe(){
        if (lastRecipe != null) {
            if (!isIdle()) {
                machine.afterWorking();
            }
            lastRecipe.postWorking(this.machine);
            setStatus(Status.IDLE);
            progress = 0;
            duration = 0;
            consumeInputsAfterWorking = false;
        }
    }

    public void inValid() {
        if (lastRecipe != null && isWorking()) {
            lastRecipe.postWorking(machine);
        }
    }

}
