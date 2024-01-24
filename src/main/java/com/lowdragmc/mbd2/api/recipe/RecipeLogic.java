package com.lowdragmc.mbd2.api.recipe;

import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.syncdata.IEnhancedManaged;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.annotation.UpdateListener;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.config.ConfigHolder;
import lombok.Getter;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

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

    @Getter
    @Persisted
    @DescSynced
    @UpdateListener(methodName = "onStatusSynced")
    private Status status = Status.IDLE;

    @Nullable
    @Persisted
    @DescSynced
    private Component waitingReason = null;
    /**
     * unsafe, it may not be found from {@link RecipeManager}. Do not index it.
     */
    @Nullable
    @Getter
    @Persisted
    protected MBDRecipe lastRecipe;
    /**
     * safe, it is the origin recipe before {@link IMachine#doModifyRecipe(MBDRecipe)}' which can be found from {@link RecipeManager}.
     */
    @Nullable
    @Getter
    @Persisted
    protected MBDRecipe lastOriginRecipe;
    @Persisted
    @Getter
    protected int progress;
    @Getter
    @Persisted
    protected int duration;
    @Getter
    @Persisted
    protected int fuelTime;
    @Getter
    @Persisted
    protected int fuelMaxTime;
    @Getter(onMethod_ = @VisibleForTesting)
    protected boolean recipeDirty;
    @Persisted
    @Getter
    protected long totalContinuousRunningTime;
    @Nullable
    protected CompletableFuture<List<MBDRecipe>> completableFuture = null;
    // if storage is dirty while async searching recipe, it will be set to true.
    protected boolean dirtySearching = false;

    public RecipeLogic(IMachine machine) {
        this.machine = machine;
    }

    @SuppressWarnings("unused")
    @OnlyIn(Dist.CLIENT)
    protected void onStatusSynced(Status newValue, Status oldValue) {
        getMachine().scheduleRenderUpdate();
    }

    /**
     * Call it to abort current recipe and reset the first state.
     */
    public void resetRecipeLogic() {
        recipeDirty = false;
        lastRecipe = null;
        lastOriginRecipe = null;
        progress = 0;
        duration = 0;
        fuelTime = 0;
        lastFailedMatches = null;
        status = Status.IDLE;
    }

    public double getProgressPercent() {
        return duration == 0 ? 0.0 : progress / (duration * 1.0);
    }

    public boolean needFuel() {
        if (machine.getRecipeType().isFuelRecipeType()) {
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
                if (progress >= duration) {
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
        } else {
            if (isSuspend()) {
                if (completableFuture != null) {
                    completableFuture.cancel(true);
                    completableFuture = null;
                }
            }
        }
    }

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

    public void handleRecipeWorking() {
        Status last = this.status;
        assert lastRecipe != null;
        var result = lastRecipe.checkConditions(this, true);
        if (result.isSuccess()) {
            if (handleFuelRecipe()) {
                result = handleTickRecipe(lastRecipe);
                if (result.isSuccess()) {
                    setStatus(Status.WORKING);
                    machine.onWorking();
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

    protected void doDamping() {
        if (progress > 0 && machine.dampingWhenWaiting()) {
            this.progress = Math.max(0, progress - ConfigHolder.INSTANCE.recipeDampingValue);
        }
    }

    protected List<MBDRecipe> searchRecipe() {
        return machine.getRecipeType().searchRecipe(getRecipeManager(), this.machine);
    }

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
                if (ConfigHolder.INSTANCE.asyncRecipeSearching) {
                    completableFuture = supplyAsyncSearchingTask();
                } else {
                    handleSearchingRecipes(searchRecipe());
                }
                dirtySearching = false;
            } else if (completableFuture.isDone()) {
                var lastFuture = this.completableFuture;
                completableFuture = null;
                if (!lastFuture.isCancelled()) {
                    // if searching task is done, try to handle searched recipes.
                    try {
                        var matches = lastFuture.join().stream().filter(match -> match.matchRecipe(machine).isSuccess()).toList();
                        if (!matches.isEmpty()) {
                            handleSearchingRecipes(matches);
                        } else if (dirtySearching) {
                            completableFuture = supplyAsyncSearchingTask();
                        }
                    } catch (Throwable throwable) {
                        // if error occurred, schedule a new async task.
                        completableFuture = supplyAsyncSearchingTask();
                    }
                } else {
                    handleSearchingRecipes(searchRecipe());
                }
                dirtySearching = false;
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
        for (MBDRecipe recipe : machine.getRecipeType().searchFuelRecipe(getRecipeManager(), machine)) {
            if (recipe.checkConditions(this, true).isSuccess() && recipe.handleRecipeIO(IO.IN, this.machine)) {
                fuelMaxTime = recipe.duration;
                fuelTime = fuelMaxTime;
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

    public void setupRecipe(MBDRecipe recipe) {
        if (handleFuelRecipe()) {
            machine.beforeWorking();
            recipe.preWorking(this.machine);
            if (recipe.handleRecipeIO(IO.IN, this.machine)) {
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
            machine.notifyStatusChanged(this.status, status);
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
     *
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
        machine.afterWorking();
        if (lastRecipe != null) {
            lastRecipe.postWorking(this.machine);
            lastRecipe.handleRecipeIO(IO.OUT, this.machine);
            if (machine.alwaysTryModifyRecipe()) {
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
    public void interruptRecipe() {
        machine.afterWorking();
        if (lastRecipe != null) {
            lastRecipe.postWorking(this.machine);
            setStatus(Status.IDLE);
            progress = 0;
            duration = 0;
        }
    }

    public void inValid() {
        if (lastRecipe != null && isWorking()) {
            lastRecipe.postWorking(machine);
        }
    }

}
