package com.lowdragmc.mbd2.common.machine;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.RequireRerender;
import com.lowdragmc.lowdraglib2.syncdata.annotation.UpdateListener;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.capability.recipe.*;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.api.machine.IMultiController;
import com.lowdragmc.mbd2.api.machine.IMultiPart;
import com.lowdragmc.mbd2.api.pattern.predicates.PatternPredicate;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.api.recipe.content.ContentModifier;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigPartSettings;
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState;
import com.lowdragmc.mbd2.common.machine.definition.config.StateMachine;
import com.lowdragmc.mbd2.common.runtime.RuntimeValue;
import com.lowdragmc.mbd2.common.trait.IProxyAutoIOTrait;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.IUIProviderTrait;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.*;

public class MBDPartMachine extends MBDMachine implements IMultiPart {
    /** Per-machine override of whether this part may be shared between multiblocks. */
    public final RuntimeValue<Boolean> canShare = runtimeValues
            .ofBool("part.can_share",
                    () -> Optional.ofNullable(getDefinition().partSettings()).map(ConfigPartSettings::canShare).orElse(true))
            .onChanged(() -> {
                invalidateCapabilities();
                notifyBlockUpdate();
            });

    @DescSynced
    @RequireRerender
    protected final Set<BlockPos> controllerPositions  = new HashSet<>();
    @Getter
    @DescSynced
    @RequireRerender
    protected boolean disableRendering = false;
    @DescSynced
    @RequireRerender
    @UpdateListener(methodName = "onProxyWhileFormedDataUpdated")
    protected CompoundTag proxyWhileFormedData = new CompoundTag();
    protected final Map<BlockPos, Integer> proxyWhileFormedPredicateIds = new HashMap<>();

    public MBDPartMachine(IMachineBlockEntity machineHolder, MBDMachineDefinition definition, Object... args) {
        super(machineHolder, definition, args);
    }

    /**
     * Whether it belongs to the specified controller.
     */
    @Override
    public boolean hasController(BlockPos controllerPos) {
        return controllerPositions.contains(controllerPos);
    }

    /**
     * Whether it belongs to a formed Multiblock.
     */
    @Override
    public boolean isFormed() {
        return !controllerPositions.isEmpty();
    }

    /**
     * Get all attached controllers
     */
    @Override
    public List<IMultiController> getControllers() {
        List<IMultiController> result = new ArrayList<>();
        for (var blockPos : controllerPositions) {
            IMultiController.ofController(getLevel(), blockPos).ifPresent(result::add);
        }
        return result;
    }

    /**
     * Get all available traits for recipe logic. It is only used for controller recipe logic.
     * <br>
     * For self recipe logic, use {@link IRecipeCapabilityHolder#getRecipeCapabilitiesProxy()} to get recipe handlers.
     */
    @Override
    public List<IRecipeHandlerTrait<?>> getRecipeHandlers() {
        var handlers = new ArrayList<IRecipeHandlerTrait<?>>();
        for (ITrait additionalTrait : getAdditionalTraits()) {
            handlers.addAll(additionalTrait.getRecipeHandlerTraits());
        }
        return handlers;
    }

    /**
     * on machine invalid in the chunk.
     * <br>
     * You should call it in yourselves {@link BlockEntity#setRemoved()}.
     */
    @Override
    public void onUnload() {
        super.onUnload();
        var level = getLevel();
        for (BlockPos pos : controllerPositions) {
            if (level instanceof ServerLevel && level.isLoaded(pos)) {
                IMultiController.ofController(getLevel(), pos).ifPresent(IMultiController::onPartUnload);
            }
        }
        controllerPositions.clear();
        proxyWhileFormedPredicateIds.clear();
        proxyWhileFormedData = new CompoundTag();
    }

    /**
     * Called when it was added to a multiblock.
     */
    @Override
    public void removedFromController(IMultiController controller) {
        controllerPositions.remove(controller.getPos());
        clearProxyWhileFormedState(controller.getPos());
        checkDisabledRendering();
        if (!isFormed()) {
            setMachineState("base");
        }
        notifyBlockUpdate();
        // the part no longer proxies the controller's capabilities: invalidate so adjacent pipes re-resolve.
        invalidateCapabilities();
    }

    @Override
    public void addedToController(IMultiController controller) {
        controllerPositions.add(controller.getPos());
        checkDisabledRendering();
        if (isFormed()) {
            setMachineState("formed");
        }
        notifyBlockUpdate();
        // the part now proxies the controller's capabilities: invalidate so adjacent pipes re-resolve.
        invalidateCapabilities();
    }

    /**
     * check if there is any controller ask the part to disable rendering.
     */
    public void checkDisabledRendering() {
        disableRendering = false;
    }

    public void setProxyWhileFormedState(BlockPos controllerPos, StateMachine<MachineState> stateMachine) {
        setProxyWhileFormedState(controllerPos, stateMachine, Collections.emptyList());
    }

    public void setProxyWhileFormedState(BlockPos controllerPos, StateMachine<MachineState> stateMachine, List<ConfigPartSettings.ProxyCapability> proxyCapabilities) {
        setProxyWhileFormedPredicate(controllerPos, -1);
    }

    public void setProxyWhileFormedPredicate(BlockPos controllerPos, int predicateId) {
        proxyWhileFormedPredicateIds.put(controllerPos.immutable(), predicateId);
        rebuildProxyWhileFormedData();
        notifyBlockUpdate();
        // proxied capabilities may have appeared: invalidate so adjacent pipes re-resolve.
        invalidateCapabilities();
    }

    public void clearProxyWhileFormedState(BlockPos controllerPos) {
        boolean removed = proxyWhileFormedPredicateIds.remove(controllerPos) != null;
        if (removed) {
            rebuildProxyWhileFormedData();
            notifyBlockUpdate();
            // proxied capabilities are gone: invalidate so adjacent pipes re-resolve.
            invalidateCapabilities();
        }
    }

    public List<ConfigPartSettings.ProxyCapability> getPredicateProxyCapabilities(BlockPos controllerPos) {
        return resolveProxyWhileFormed(controllerPos)
                .map(PatternPredicate.ProxyWhileFormed::getProxyCapabilities)
                .orElse(Collections.emptyList());
    }

    @Override
    public MachineState getMachineState() {
        loadProxyWhileFormedPredicateIdsFromData();
        var ownState = super.getMachineState();
        if (proxyWhileFormedPredicateIds.isEmpty()) {
            return ownState;
        }
        var level = getLevel();
        if (level == null) {
            return ownState;
        }
        return proxyWhileFormedPredicateIds.entrySet().stream()
                .sorted(Comparator.comparingLong(entry -> entry.getKey().asLong()))
                .map(entry -> resolveProxyWhileFormed(entry.getKey())
                        .map(controller -> {
                            var proxyStateMachine = controller.getStateMachine();
                            var controllerState = IMachine.ofMachine(level, entry.getKey())
                                    .filter(MBDMachine.class::isInstance)
                                    .map(MBDMachine.class::cast)
                                    .map(MBDMachine::getMachineStateName)
                                    .orElse(null);
                            if (controllerState == null) return null;
                            return proxyStateMachine.hasState(controllerState) ? proxyStateMachine.getState(controllerState) : null;
                        })
                        .orElse(null))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(ownState);
    }

    @SuppressWarnings("unused")
    protected void onProxyWhileFormedDataUpdated(CompoundTag newValue, CompoundTag oldValue) {
        loadProxyWhileFormedPredicateIdsFromData(newValue);
        notifyBlockUpdate();
        // the synced proxy state changed: invalidate so adjacent pipes re-resolve the proxied capabilities.
        invalidateCapabilities();
    }

    private void rebuildProxyWhileFormedData() {
        var level = getLevel();
        if (level == null) {
            return;
        }
        var tag = new CompoundTag();
        for (var entry : proxyWhileFormedPredicateIds.entrySet()) {
            tag.putInt(Long.toString(entry.getKey().asLong()), entry.getValue());
        }
        proxyWhileFormedData = tag;
    }

    private void loadProxyWhileFormedPredicateIdsFromData() {
        if (proxyWhileFormedPredicateIds.isEmpty() && !proxyWhileFormedData.isEmpty()) {
            loadProxyWhileFormedPredicateIdsFromData(proxyWhileFormedData);
        }
    }

    private void loadProxyWhileFormedPredicateIdsFromData(CompoundTag data) {
        var level = getLevel();
        if (level == null) {
            return;
        }
        proxyWhileFormedPredicateIds.clear();
        for (var key : data.getAllKeys()) {
            if (data.contains(key, net.minecraft.nbt.Tag.TAG_INT)) {
                try {
                    proxyWhileFormedPredicateIds.put(BlockPos.of(Long.parseLong(key)), data.getInt(key));
                } catch (NumberFormatException ignored) {
                    // Ignore malformed synced data from older saves or manual edits.
                }
            }
        }
    }

    private Optional<PatternPredicate.ProxyWhileFormed> resolveProxyWhileFormed(BlockPos controllerPos) {
        var level = getLevel();
        if (level == null) return Optional.empty();
        int predicateId = proxyWhileFormedPredicateIds.getOrDefault(controllerPos, -1);
        if (predicateId < 0) return Optional.empty();
        return IMachine.ofMachine(level, controllerPos)
                .filter(MBDMultiblockMachine.class::isInstance)
                .map(MBDMultiblockMachine.class::cast)
                .map(MBDMultiblockMachine::getPattern)
                .map(pattern -> pattern.getPredicate(predicateId))
                .map(predicate -> predicate.proxyWhileFormed)
                .filter(PatternPredicate.ProxyWhileFormed::isEnable);
    }

    /**
     * Can it be shared among multi multiblock.
     */
    @Override
    public boolean canShared() {
        return canShare.get();
    }

    /**
     * Called when controller recipe logic status changed
     */
    @Override
    public void notifyControllerRecipeStatusChanged(IMultiController controller, RecipeLogic.Status oldStatus, RecipeLogic.Status newStatus) {
        IMultiPart.super.notifyControllerRecipeStatusChanged(controller, oldStatus, newStatus);
        if (isFormed()) {
            switch (newStatus) {
                case WORKING -> setMachineState("working");
                case IDLE -> {
                    if (getDefinition().stateMachine().hasState("formed")) {
                        setMachineState("formed");
                    } else {
                        setMachineState("base");
                    }
                }
                case WAITING -> setMachineState("waiting");
                case SUSPEND -> setMachineState("suspend");
            }
        } else {
            setMachineState("base");
        }
    }

    /**
     * Override it to modify controller recipe on the fly e.g. applying overclock, change chance, etc
     * <br>
     * We will apply part recipe modifiers here. see {@link ConfigPartSettings#recipeModifiers()}.
     * @param recipe recipe from detected from MBDRecipeType
     * @param controllerRecipeLogic controller recipe logic
     * @return modified recipe.
     *         null -- this recipe is unavailable
     */
    @Override
    public MBDRecipe modifyControllerRecipe(@Nonnull MBDRecipe recipe, RecipeLogic controllerRecipeLogic) {
        if (getDefinition().partSettings() != null) {
            return getDefinition().partSettings().recipeModifiers().applyModifiers(controllerRecipeLogic, recipe);
        }
        return recipe;
    }

    @Override
    public ContentModifier getMaxControllerParallel(@NotNull MBDRecipe recipe, RecipeLogic controllerRecipeLogic) {
        if (getDefinition().partSettings() != null) {
            return getDefinition().partSettings().recipeModifiers().getMaxParallel(controllerRecipeLogic, recipe);
        }
        return ContentModifier.IDENTITY;
    }

    @Override
    public boolean alwaysTryModifyControllerRecipe() {
        if (getDefinition().partSettings() != null) {
            return !getDefinition().partSettings().recipeModifiers().recipeModifiers.isEmpty();
        }
        return false;
    }

    @Override
    public void internalServerTick() {
        super.internalServerTick();
        var partSettings = getDefinition().partSettings();
        var staticProxies = partSettings == null
                ? Collections.<ConfigPartSettings.ProxyCapability>emptyList()
                : partSettings.proxyControllerCapabilities();
        // Resolve the synced ids before the early-out, or a part that only proxies via a predicate
        // would bail out here forever and never auto IO.
        loadProxyWhileFormedPredicateIdsFromData();
        if (staticProxies.isEmpty() && proxyWhileFormedPredicateIds.isEmpty()) return;
        var front = getFrontFacing().orElse(Direction.NORTH);
        var pos = getPos();
        var timer = getOffsetTimer();
        for (var controller : getControllers()) {
            if (!(controller instanceof MBDMultiblockMachine proxyController)) continue;
            IProxyAutoIOTrait.handleProxyAutoIO(proxyController, staticProxies, pos, front, timer);
            // the predicate that matched this part can carry proxy capabilities of its own; they forward
            // the controller's traits exactly like partSettings does, so they have to auto IO too (#237).
            IProxyAutoIOTrait.handleProxyAutoIO(proxyController,
                    getPredicateProxyCapabilities(proxyController.getPos()), pos, front, timer);
        }
    }

    @Override
    protected void bindMachineUI(UI ui) {
        super.bindMachineUI(ui);

        // proxy controller ui
        if (getDefinition().partSettings() != null && getDefinition().partSettings().isEnable()) {
            var prefix = "controller:";
            var midTag = "@ui:";
            ui.selectRegex("controller:.*?@ui:").forEach(element -> {
                var id = element.getId();
                if (id.startsWith(prefix)) {
                    int atIndex = id.indexOf(midTag);
                    if (atIndex != -1) {
                        var traitName = id.substring(prefix.length(), atIndex);
                        var uiName = "ui:" + id.substring(atIndex + midTag.length());
                        for (var controller : getControllers()) {
                            if (controller instanceof MBDMachine mbdMachine) {
                                var trait = mbdMachine.getTraitByName(traitName);
                                if (trait != null &&
                                        trait.getDefinition() instanceof IUIProviderTrait provider &&
                                        uiName.startsWith(provider.uiId())) {
                                    element.setId(uiName);
                                    provider.initTraitUI(trait, ui);
                                }
                            }
                        }
                    }
                }
            });
        }
    }
}
