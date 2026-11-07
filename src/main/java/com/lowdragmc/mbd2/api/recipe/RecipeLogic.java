package com.lowdragmc.mbd2.api.recipe;

import com.google.common.collect.Table;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.RequireRerender;
import com.lowdragmc.lowdraglib2.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.IBlockEntityManaged;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeCapabilityHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandler;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeHandlerSlotsProxy;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.config.ConfigHolder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.VisibleForTesting;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Runs recipes for a machine: searches, sets up, works and finishes them, one at a time.
 *
 * <p>A machine may own several, see {@link IMachine#getRecipeLogics()}. Each has a {@link #getName() name} and a
 * {@link #getThreadIndex() thread index} within its group, and is itself an {@link IRecipeCapabilityHolder} of the
 * recipe handlers this logic may use.</p>
 */
public class RecipeLogic implements IBlockEntityManaged, IRecipeCapabilityHolder {
    public static final String PRIMARY_NAME = "main";

    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(RecipeLogic.class);
    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public BlockEntity asBlockEntity() {
        return machine.getHolder();
    }

    public enum Status {
        IDLE, WORKING, WAITING, SUSPEND
    }
    @Getter
    public final IMachine machine;
    @Getter
    private final String name;
    @Getter
    private final int threadIndex;
    public List<RecipeHolder<MBDRecipe>> lastFailedMatches;

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
    protected ResourceLocation lastOriginRecipe;
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
    protected CompletableFuture<List<RecipeHolder<MBDRecipe>>> completableFuture = null;
    @Getter
    protected boolean isValid = false;
    /** The recipe {@link #handlersTold} were told {@code preWorking} for. Not saved, see {@link #syncHandlers()}. */
    @Nullable
    private MBDRecipe handlersWorkingOn;
    private List<MBDRecipe.WorkingHandler> handlersTold = List.of();
    private boolean postingAfterWorking;

    /** Idle polls without a reported change before a logic searches anyway. */
    public static final int FALLBACK_POLLS = 20;
    private volatile boolean searchDirty = true;
    private final List<ISubscription> handlerSubscriptions = new ArrayList<>();
    private boolean subscribed;
    private int subscribedFingerprint;
    private boolean allHandlersTracked;
    private int idlePolls;
    private int searchedGeneration = -1;
    @Getter(onMethod_ = @VisibleForTesting)
    private int searchCount;

    /** The primary logic: {@link #PRIMARY_NAME}, thread 0. */
    public RecipeLogic(IMachine machine) {
        this(machine, PRIMARY_NAME, 0);
    }

    public RecipeLogic(IMachine machine, String name, int threadIndex) {
        this.machine = machine;
        this.name = name;
        this.threadIndex = threadIndex;
    }

    public boolean isPrimary() {
        return machine.getRecipeLogic() == this;
    }

    /** {@link #getName()} for thread 0, {@code name#thread} for the others. */
    public String getAddress() {
        return threadIndex == 0 ? name : name + "#" + threadIndex;
    }

    /** Whether {@code address} names this logic: {@code name#thread}, or for thread 0 also the bare name. */
    public boolean matchesAddress(String address) {
        return address.equals(getAddress()) || address.equals(name + "#" + threadIndex);
    }

    /**
     * Whether this logic is in {@code scope}: logic names, each meaning every thread of that logic, and
     * {@code name#thread} addresses. An empty scope holds every logic.
     */
    public boolean isInScope(Collection<String> scope) {
        return scope.isEmpty() || scope.contains(name) || scope.contains(name + "#" + threadIndex);
    }

    /**
     * The status a machine shows for its logics together: working if any works, else waiting if any waits, else
     * suspended if all are, else idle. Each logic counts with the status {@code statusOf} gives it.
     */
    public static Status aggregate(Iterable<RecipeLogic> logics, Function<RecipeLogic, Status> statusOf) {
        boolean any = false;
        boolean anyWaiting = false;
        boolean allSuspended = true;
        for (var logic : logics) {
            var status = statusOf.apply(logic);
            any = true;
            if (status == Status.WORKING) return Status.WORKING;
            if (status == Status.WAITING) anyWaiting = true;
            if (status != Status.SUSPEND) allSuspended = false;
        }
        if (anyWaiting) return Status.WAITING;
        return any && allSuspended ? Status.SUSPEND : Status.IDLE;
    }

    public MBDRecipeType getRecipeType() {
        return machine.getRecipeType(this);
    }

    /** The recipe handlers this logic may use, a subset of the machine's. */
    @Nonnull
    @Override
    public Table<IO, RecipeCapability<?>, List<IRecipeHandler<?>>> getRecipeCapabilitiesProxy() {
        return machine.getRecipeCapabilitiesProxy(this);
    }

    @Override
    public int getChanceTier() {
        return machine.getChanceTier();
    }

    @Override
    public String toString() {
        return "RecipeLogic{" + getAddress() + " of " + machine + "}";
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

    public float getProgressPercent() {
        return duration == 0 ? 0f : progress / (duration * 1f);
    }

    public float getFuelProgressPercent() {
        return fuelMaxTime == 0 ? 0f : fuelTime / (fuelMaxTime * 1f);
    }

    public boolean needFuel() {
        if (getRecipeType().isRequireFuelForWorking()){
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
        if (!isValid) {
            return;
        }
        // a logic loaded mid-recipe has not told its handlers yet
        syncHandlers();
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
                if (needsSearch()) {
                    findAndHandleRecipe();
                } else {
                    retryFailedMatches();
                }
            }
        }
        if (fuelTime > 0) {
            fuelTime--;
            if (fuelTime == 0) {
                machine.onFuelBurningFinish(this, lastFuelRecipe);
            }
        }
        if (isSuspend() && completableFuture != null) {
            completableFuture.cancel(true);
            completableFuture = null;
        }
    }

    /**
     * Keep the handlers working on {@link #getLastRecipe()} exactly while this logic is valid and
     * {@link Status#WORKING}. Every {@code preWorking} gets one {@code postWorking}.
     */
    protected void syncHandlers() {
        var want = isValid && status == Status.WORKING ? lastRecipe : null;
        if (handlersWorkingOn == want) return;
        stopHandlers();
        if (want != null) {
            handlersWorkingOn = want;
            handlersTold = want.preWorking(this);
        }
    }

    protected void stopHandlers() {
        var working = handlersWorkingOn;
        if (working != null) {
            var told = handlersTold;
            handlersWorkingOn = null;
            handlersTold = List.of();
            working.postWorking(this, told);
        }
    }

    /**
     * Whether an idle poll should search. Every poll for the legacy search or while some handler does not report
     * its changes; otherwise only after a change, a recipe reload, or {@link #FALLBACK_POLLS} quiet polls.
     */
    protected boolean needsSearch() {
        if (usesLegacySearch()) return true;
        refreshSubscriptions();
        if (searchDirty || !allHandlersTracked || searchedGeneration != MBDRecipeType.getLookupGeneration()) {
            return true;
        }
        return ++idlePolls >= FALLBACK_POLLS;
    }

    /** Something this logic may use changed: its next idle poll searches. */
    public void markSearchDirty() {
        this.searchDirty = true;
    }

    /** The handlers this logic may use were replaced: stop listening to the old ones until its next poll. */
    public void releaseHandlers() {
        unsubscribeHandlers();
        searchDirty = true;
    }

    /**
     * Re-check the recipes the last search matched but could not start, as conditions, modifiers or fuel can come
     * right without any handler changing.
     */
    protected void retryFailedMatches() {
        var pending = lastFailedMatches;
        if (pending == null) return;
        lastFailedMatches = null;
        for (var match : pending) {
            if (checkMatchedRecipeAvailable(match)) return;
        }
        lastFailedMatches = pending;
    }

    private void refreshSubscriptions() {
        if (!isValid) return;
        var table = getRecipeCapabilitiesProxy();
        int fingerprint = handlerFingerprint(table);
        if (subscribed && fingerprint == subscribedFingerprint) return;
        unsubscribeHandlers();
        boolean tracked = true;
        var seen = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        for (var handlers : table.values()) {
            for (var handler : handlers) {
                var raw = RecipeHandlerSlotsProxy.unwrap(handler);
                if (raw instanceof IRecipeHandlerTrait<?> trait && trait.notifiesAllChanges()) {
                    if (seen.add(raw)) {
                        handlerSubscriptions.add(trait.addChangedListener(this::markSearchDirty));
                    }
                } else {
                    tracked = false;
                }
            }
        }
        allHandlersTracked = tracked;
        subscribedFingerprint = fingerprint;
        subscribed = true;
        searchDirty = true;
    }

    private void unsubscribeHandlers() {
        for (var subscription : handlerSubscriptions) {
            subscription.unsubscribe();
        }
        handlerSubscriptions.clear();
        subscribed = false;
    }

    private static int handlerFingerprint(Table<IO, RecipeCapability<?>, List<IRecipeHandler<?>>> table) {
        int hash = 1;
        for (var cell : table.cellSet()) {
            hash = 31 * hash + cell.getRowKey().ordinal();
            hash = 31 * hash + System.identityHashCode(cell.getColumnKey());
            for (var handler : cell.getValue()) {
                hash = 31 * hash + System.identityHashCode(handler);
            }
        }
        return hash;
    }

    /** Whether a class overrides {@code base}'s method {@code name(parameters)}. */
    private static ClassValue<Boolean> overrides(Class<?> base, String name, Class<?>... parameters) {
        return new ClassValue<>() {
            @Override
            protected Boolean computeValue(Class<?> type) {
                for (var c = type; c != null && c != base; c = c.getSuperclass()) {
                    try {
                        c.getDeclaredMethod(name, parameters);
                        return true;
                    } catch (NoSuchMethodException ignored) {
                    }
                }
                return false;
            }
        };
    }

    private static final ClassValue<Boolean> OVERRIDES_SEARCH = overrides(RecipeLogic.class, "searchRecipe");
    private static final ClassValue<Boolean> TYPE_OVERRIDES_SEARCH =
            overrides(MBDRecipeType.class, "searchRecipe", RecipeManager.class, IRecipeCapabilityHolder.class);
    private static final ClassValue<Boolean> TYPE_OVERRIDES_FUEL_SEARCH =
            overrides(MBDRecipeType.class, "searchFuelRecipe", RecipeManager.class, IRecipeCapabilityHolder.class);

    /**
     * Whether this logic searches as it did before the recipe index, polling {@link #searchRecipe()}: an addon
     * overrides it, or {@link MBDRecipeType#searchRecipe} of its recipe type.
     */
    protected boolean usesLegacySearch() {
        return OVERRIDES_SEARCH.get(getClass()) || TYPE_OVERRIDES_SEARCH.get(getRecipeType().getClass());
    }

    /**
     * Match the recipes the {@link RecipeLookup} cannot rule out, in priority order, and start the first that can.
     * Those that match but cannot start are kept for {@link #retryFailedMatches()}.
     * <p>
     * A logic that has to poll, and holds inputs the index cannot read, matches on a background thread when
     * {@link ConfigHolder#ASYNC_RECIPE_SEARCHING} is on: the index narrows nothing down for it.
     */
    protected void searchAndStartRecipe() {
        refreshSubscriptions();
        searchDirty = false;
        idlePolls = 0;
        searchCount++;
        searchedGeneration = MBDRecipeType.getLookupGeneration();
        lastFailedMatches = null;
        if (!hasProxies()) return;
        var lookup = getRecipeType().getLookup(getRecipeManager());
        var available = availableLookupKeys(lookup);
        var candidates = lookup.candidates(available);
        if (!allHandlersTracked && available.containsValue(null) && ConfigHolder.ASYNC_RECIPE_SEARCHING.get()) {
            completableFuture = supplyAsyncSearchingTask(() -> matching(candidates));
            return;
        }
        for (var match : candidates) {
            var recipe = match.value();
            if (!recipe.matchRecipe(this).isSuccess() || !recipe.matchTickRecipe(this).isSuccess()) continue;
            if (checkMatchedRecipeAvailable(match)) return;
            if (lastFailedMatches == null) {
                lastFailedMatches = new ArrayList<>();
            }
            lastFailedMatches.add(match);
        }
    }

    private List<RecipeHolder<MBDRecipe>> matching(List<RecipeHolder<MBDRecipe>> candidates) {
        var matches = new ArrayList<RecipeHolder<MBDRecipe>>();
        for (var candidate : candidates) {
            var recipe = candidate.value();
            if (recipe.matchRecipe(this).isSuccess() && recipe.matchTickRecipe(this).isSuccess()) {
                matches.add(candidate);
            }
        }
        return matches;
    }

    /**
     * For each capability {@code lookup} indexes, the keys this logic's inputs hold; {@code null} for a capability
     * one of whose inputs cannot say.
     */
    public Map<RecipeCapability<?>, Collection<?>> availableLookupKeys(RecipeLookup lookup) {
        var table = getRecipeCapabilitiesProxy();
        var available = new HashMap<RecipeCapability<?>, Collection<?>>();
        for (var capability : lookup.getIndexedCapabilities()) {
            var keys = new HashSet<Object>();
            boolean unknown = false;
            for (var io : new IO[]{IO.IN, IO.BOTH}) {
                var handlers = table.get(io, capability);
                if (handlers == null) continue;
                for (var handler : handlers) {
                    var held = handler.getAvailableLookupKeys();
                    if (held == null) {
                        unknown = true;
                        break;
                    }
                    keys.addAll(held);
                }
                if (unknown) break;
            }
            available.put(capability, unknown ? null : keys);
        }
        return available;
    }

    /** The fuel recipes this logic's inputs match, in the order fuel is tried. */
    protected List<RecipeHolder<MBDRecipe>> searchFuelRecipes() {
        var type = getRecipeType();
        if (TYPE_OVERRIDES_FUEL_SEARCH.get(type.getClass())) {
            return type.searchFuelRecipe(getRecipeManager(), machine);
        }
        var fuels = new ArrayList<RecipeHolder<MBDRecipe>>();
        if (!type.isRequireFuelForWorking() || !hasProxies()) return fuels;
        var manager = getRecipeManager();
        for (var id : type.getFuelRecipeConfig().getFuelRecipeTypes()) {
            if (!(BuiltInRegistries.RECIPE_TYPE.get(id) instanceof MBDRecipeType fuelType)) continue;
            var lookup = fuelType.getLookup(manager);
            fuels.addAll(matching(lookup.candidates(availableLookupKeys(lookup))));
        }
        fuels.sort(Comparator.comparingInt(recipe -> recipe.value().priority));
        return fuels;
    }

    @Nullable
    private static Component reasonOf(MBDRecipe.ActionResult result) {
        return result.reason() == null ? null : result.reason().get();
    }

    /**
     * Verifies if a matched recipe is available and valid for processing in the machine.
     * This method modifies the recipe using the machine's modification logic and
     * checks if the conditions, match criteria, and tick requirements for the recipe are met.
     * If valid, the recipe is set up for processing, and its status is updated.
     *
     * @param match the {@code RecipeHolder} containing the recipe to be checked and validated.
     * @return {@code true} if the recipe is valid, matches the machine's requirements,
     *         and can be processed; {@code false} otherwise.
     */
    protected boolean checkMatchedRecipeAvailable(RecipeHolder<MBDRecipe> match) {
        var modified = machine.doModifyRecipe(this, match.value());
        if (modified != null) {
            if (modified.checkConditions(this).isSuccess() &&
                    modified.matchRecipe(this).isSuccess() &&
                    modified.matchTickRecipe(this).isSuccess()) {
                setupRecipe(modified);
            }
            if (lastRecipe != null && getStatus() == Status.WORKING) {
                lastOriginRecipe = match.id();
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
        assert lastRecipe != null;
        if (consumeInputsAfterWorking) {
            if (!lastRecipe.matchRecipe(this).isSuccess()) {
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
                    if (machine.onWorking(this)) {
                        this.interruptRecipe();
                        return;
                    }
                    progress++;
                    totalContinuousRunningTime++;
                } else {
                    setWaiting(reasonOf(result));
                }
            } else {
                setWaiting(Component.translatable("mbd2.recipe_logic.insufficient_fuel"));
            }
        } else {
            setWaiting(reasonOf(result));
        }
        if (isWaiting()) {
            doDamping();
        }
    }

    /**
     * Applies damping to the recipe progress if the machine is in a waiting state and damping is allowed.
     * This method decreases the `{@code progress} field by a specified damping value retrieved from the machine,
     * ensuring that the progress does not fall below zero.
     */
    protected void doDamping() {
        if (progress > 0 && machine.dampingWhenWaiting(this)) {
            this.progress = Math.max(0, progress - machine.getRecipeDampingValue(this));
        }
    }

    /**
     * The search a recipe logic ran before the recipe index, still used when an addon overrides it or
     * {@link MBDRecipeType#searchRecipe}. Hands the type the machine, as it always has; whatever it finds is matched
     * against this logic's own handlers before it starts.
     */
    protected List<RecipeHolder<MBDRecipe>> searchRecipe() {
        return getRecipeType().searchRecipe(getRecipeManager(), machine);
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
                lastRecipe.matchRecipe(this).isSuccess() &&
                lastRecipe.matchTickRecipe(this).isSuccess() &&
                lastRecipe.checkConditions(this).isSuccess()) {
            MBDRecipe recipe = lastRecipe;
            lastRecipe = null;
            lastOriginRecipe = null;
            setupRecipe(recipe);
        } else { // try to find and handle a new recipe
            lastRecipe = null;
            lastOriginRecipe = null;
            if (completableFuture == null) {
                if (!usesLegacySearch()) {
                    searchAndStartRecipe();
                } else if (ConfigHolder.ASYNC_RECIPE_SEARCHING.get()) {
                    // try to search recipe in threads.
                    completableFuture = supplyAsyncSearchingTask(this::searchRecipe);
                } else {
                    handleSearchingRecipes(searchRecipe());
                }
            } else if (completableFuture.isDone()) {
                var lastFuture = this.completableFuture;
                completableFuture = null;
                if (!lastFuture.isCancelled()) {
                    // if searching task is done, try to handle searched recipes.
                    try {
                        var matches = lastFuture.join().stream().filter(match -> match.value().matchRecipe(this).isSuccess()).toList();
                        if (!matches.isEmpty()) {
                            handleSearchingRecipes(matches);
                        }
                    } catch (Throwable throwable) {
                        // if error occurred, schedule a new async task.
                        if (usesLegacySearch()) {
                            completableFuture = supplyAsyncSearchingTask(this::searchRecipe);
                        }
                    }
                } else {
                    handleSearchingRecipes(searchRecipe());
                }
            }
        }
        recipeDirty = false;
    }

    private CompletableFuture<List<RecipeHolder<MBDRecipe>>> supplyAsyncSearchingTask(Supplier<List<RecipeHolder<MBDRecipe>>> search) {
        return CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("Searching recipes", search), Util.backgroundExecutor());
    }

    private void handleSearchingRecipes(List<RecipeHolder<MBDRecipe>> matches) {
        for (var match : matches) {
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
        for (var recipeHolder : searchFuelRecipes()) {
            var recipe = recipeHolder.value();
            recipe = machine.modifyFuelRecipe(this, recipe);
            if (recipe != null && recipe.checkConditions(this).isSuccess() && recipe.handleRecipeIO(IO.IN, this)) {
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
            var result = recipe.matchTickRecipe(this);
            if (result.isSuccess()) {
                recipe.handleTickRecipeIO(IO.IN, this);
                recipe.handleTickRecipeIO(IO.OUT, this);
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
            if (machine.beforeWorking(this, recipe)) {
                setStatus(Status.IDLE);
                progress = 0;
                duration = 0;
                consumeInputsAfterWorking = false;
                return;
            }
            consumeInputsAfterWorking = machine.consumeInputsAfterWorking(this, recipe);
            if (consumeInputsAfterWorking || recipe.handleRecipeIO(IO.IN, this)) {
                recipeDirty = false;
                lastRecipe = recipe;
                setStatus(Status.WORKING);
                progress = 0;
                // a modifier can bring the duration down to 0, which would never finish
                duration = Math.max(1, recipe.duration);
                // a re-run from the finish stays WORKING, so setStatus has not restarted the handlers
                syncHandlers();
            }
        }
    }

    public void setStatus(Status status) {
        if (this.status != status) {
            if (this.status == Status.WORKING) {
                this.totalContinuousRunningTime = 0;
            }
            machine.notifyRecipeStatusChanged(this, this.status, status);
            this.status = status;
            if (this.status != Status.WAITING) {
                waitingReason = null;
            }
            syncHandlers();
        }
    }

    public void setWaiting(@Nullable Component reason) {
        setStatus(Status.WAITING);
        waitingReason = reason;
        machine.onWaiting(this);
    }

    /**
     * mark current handling recipe (if exist) as dirty.
     * do not try it immediately in the next round
     */
    public void markLastRecipeDirty() {
        this.recipeDirty = true;
        this.searchDirty = true;
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
            // taking deferred inputs that are partly gone would take the rest and fail
            if (consumeInputsAfterWorking && !lastRecipe.matchRecipe(false, IO.IN, this, lastRecipe.inputs, false).isSuccess()) {
                interruptRecipe();
                return;
            }
            var room = lastRecipe.matchRecipe(false, IO.OUT, this, lastRecipe.outputs, false);
            if (!room.isSuccess() && consumeInputsAfterWorking) {
                // the deferred inputs are due: taking them may make the room, and they cannot leave while it waits
                consumeInputsAfterWorking = false;
                if (!lastRecipe.handleRecipeIO(IO.IN, this)) {
                    interruptRecipe();
                    return;
                }
                machine.onConsumeInputsAfterWorking(this);
                room = lastRecipe.matchRecipe(false, IO.OUT, this, lastRecipe.outputs, false);
            }
            if (!room.isSuccess()) {
                // the output slots filled since the recipe started: wait rather than void what does not fit
                setWaiting(reasonOf(room));
                return;
            }
            afterWorking();
            // a handler interrupted or reset the recipe
            if (isIdle()) return;
            if (consumeInputsAfterWorking) {
                consumeInputsAfterWorking = false;
                if (!lastRecipe.handleRecipeIO(IO.IN, this)) {
                    stopHandlers();
                    setStatus(Status.IDLE);
                    progress = 0;
                    duration = 0;
                    return;
                }
                machine.onConsumeInputsAfterWorking(this);
            }
            stopHandlers();
            lastRecipe.handleRecipeIO(IO.OUT, this);
            machine.onRecipeFinish(this);
            if (machine.alwaysReSearchRecipe(this)) {
                markLastRecipeDirty();
            }
            if (!recipeDirty && machine.alwaysTryModifyRecipe(this)) {
                if (lastOriginRecipe != null && getRecipeManager()
                        .byKey(lastOriginRecipe)
                        .map(RecipeHolder::value)
                        .orElse(null) instanceof MBDRecipe recipe) {
                    var modified = machine.doModifyRecipe(this, recipe);
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
                    lastRecipe.matchRecipe(this).isSuccess() &&
                    lastRecipe.matchTickRecipe(this).isSuccess() &&
                    lastRecipe.checkConditions(this).isSuccess()) {
                setupRecipe(lastRecipe);
            }
            // not re-run, or the re-run did not take: at full progress it would finish again next tick for free
            if (status != Status.IDLE && progress >= duration) {
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
                afterWorking();
            }
            stopHandlers();
            setStatus(Status.IDLE);
            progress = 0;
            duration = 0;
            consumeInputsAfterWorking = false;
        }
    }

    /** A handler interrupting or resetting the recipe from {@code afterWorking} must not get it again. */
    private void afterWorking() {
        if (postingAfterWorking) return;
        postingAfterWorking = true;
        try {
            machine.afterWorking(this);
        } finally {
            postingAfterWorking = false;
        }
    }

    public void inValid() {
        if (isValid) {
            stopHandlers();
            unsubscribeHandlers();
            if (completableFuture != null) {
                completableFuture.cancel(true);
                completableFuture = null;
            }
            isValid = false;
        }
    }

    public void valid() {
        if (!isValid) {
            isValid = true;
        }
    }

}
