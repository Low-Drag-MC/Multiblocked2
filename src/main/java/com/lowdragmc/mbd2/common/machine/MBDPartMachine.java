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
    protected final Map<BlockPos, StateMachine<MachineState>> proxyWhileFormedStateMachines = new HashMap<>();
    protected final Map<BlockPos, List<ConfigPartSettings.ProxyCapability>> predicateProxyCapabilities = new HashMap<>();

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
        proxyWhileFormedStateMachines.clear();
        predicateProxyCapabilities.clear();
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
    }

    @Override
    public void addedToController(IMultiController controller) {
        controllerPositions.add(controller.getPos());
        checkDisabledRendering();
        if (isFormed()) {
            setMachineState("formed");
        }
        notifyBlockUpdate();
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
        proxyWhileFormedStateMachines.put(controllerPos.immutable(),
                stateMachine == null ? PatternPredicate.ProxyWhileFormed.createDefaultStateMachine() : stateMachine);
        if (proxyCapabilities.isEmpty()) {
            predicateProxyCapabilities.remove(controllerPos.immutable());
        } else {
            predicateProxyCapabilities.put(controllerPos.immutable(), List.copyOf(proxyCapabilities));
        }
        rebuildProxyWhileFormedData();
        notifyBlockUpdate();
    }

    public void clearProxyWhileFormedState(BlockPos controllerPos) {
        boolean removedState = proxyWhileFormedStateMachines.remove(controllerPos) != null;
        boolean removedCaps = predicateProxyCapabilities.remove(controllerPos) != null;
        if (removedState || removedCaps) {
            rebuildProxyWhileFormedData();
            notifyBlockUpdate();
        }
    }

    public List<ConfigPartSettings.ProxyCapability> getPredicateProxyCapabilities(BlockPos controllerPos) {
        return predicateProxyCapabilities.getOrDefault(controllerPos, Collections.emptyList());
    }

    @Override
    public MachineState getMachineState() {
        loadProxyWhileFormedStateMachinesFromData();
        var ownState = super.getMachineState();
        if (proxyWhileFormedStateMachines.isEmpty()) {
            return ownState;
        }
        var level = getLevel();
        if (level == null) {
            return ownState;
        }
        return proxyWhileFormedStateMachines.entrySet().stream()
                .sorted(Comparator.comparingLong(entry -> entry.getKey().asLong()))
                .map(entry -> IMachine.ofMachine(level, entry.getKey())
                        .filter(MBDMachine.class::isInstance)
                        .map(MBDMachine.class::cast)
                        .map(controller -> {
                            var proxyStateMachine = entry.getValue();
                            var controllerState = controller.getMachineStateName();
                            return proxyStateMachine.hasState(controllerState) ? proxyStateMachine.getState(controllerState) : null;
                        })
                        .orElse(null))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(ownState);
    }

    @SuppressWarnings("unused")
    protected void onProxyWhileFormedDataUpdated(CompoundTag newValue, CompoundTag oldValue) {
        loadProxyWhileFormedStateMachinesFromData(newValue);
        notifyBlockUpdate();
    }

    private void rebuildProxyWhileFormedData() {
        var level = getLevel();
        if (level == null) {
            return;
        }
        var tag = new CompoundTag();
        var provider = level.registryAccess();
        for (var entry : proxyWhileFormedStateMachines.entrySet()) {
            tag.put(Long.toString(entry.getKey().asLong()), entry.getValue().serializeNBT(provider));
        }
        proxyWhileFormedData = tag;
    }

    private void loadProxyWhileFormedStateMachinesFromData() {
        if (proxyWhileFormedStateMachines.isEmpty() && !proxyWhileFormedData.isEmpty()) {
            loadProxyWhileFormedStateMachinesFromData(proxyWhileFormedData);
        }
    }

    private void loadProxyWhileFormedStateMachinesFromData(CompoundTag data) {
        var level = getLevel();
        if (level == null) {
            return;
        }
        proxyWhileFormedStateMachines.clear();
        var provider = level.registryAccess();
        for (var key : data.getAllKeys()) {
            if (data.get(key) instanceof CompoundTag stateMachineTag) {
                try {
                    var stateMachine = PatternPredicate.ProxyWhileFormed.createDefaultStateMachine();
                    stateMachine.deserializeNBT(provider, stateMachineTag);
                    proxyWhileFormedStateMachines.put(BlockPos.of(Long.parseLong(key)), stateMachine);
                } catch (NumberFormatException ignored) {
                    // Ignore malformed synced data from older saves or manual edits.
                }
            }
        }
    }

    /**
     * Can it be shared among multi multiblock.
     */
    @Override
    public boolean canShared() {
        return Optional.ofNullable(getDefinition().partSettings()).map(ConfigPartSettings::canShare).orElse(true);
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
        for (var proxy : Objects.requireNonNull(getDefinition().partSettings()).proxyControllerCapabilities()) {
            if (proxy.autoIO().isEnable()) {
                var front = getFrontFacing().orElse(Direction.NORTH);
                var pos = getPos();
                for (var controller : getControllers()) {
                    if (controller instanceof MBDMultiblockMachine proxyController) {
                        for (var trait : proxyController.getAdditionalTraits()) {
                            if (trait instanceof IProxyAutoIOTrait autoIOTrait && trait.getDefinition().getName().contains(proxy.traitNameFilter())) {
                                for (var side : Direction.values()) {
                                    var io =  proxy.autoIO().getIO(front, side);
                                    if (io != IO.NONE) {
                                        autoIOTrait.handleAutoIO(pos, side, io);
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
