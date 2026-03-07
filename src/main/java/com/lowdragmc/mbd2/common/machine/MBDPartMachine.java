package com.lowdragmc.mbd2.common.machine;

import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.RequireRerender;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.capability.recipe.*;
import com.lowdragmc.mbd2.api.machine.IMultiController;
import com.lowdragmc.mbd2.api.machine.IMultiPart;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.api.recipe.content.ContentModifier;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigPartSettings;
import com.lowdragmc.mbd2.common.trait.IProxyAutoIOTrait;
import com.lowdragmc.mbd2.common.trait.ITrait;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
    }

    /**
     * Called when it was added to a multiblock.
     */
    @Override
    public void removedFromController(IMultiController controller) {
        controllerPositions.remove(controller.getPos());
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
        var result = false;
        for (var controller : getControllers()) {
            if (controller instanceof MBDMultiblockMachine machine) {
                if (machine.getRenderingDisabledPositions().contains(getPos())) {
                    result = true;
                    break;
                }
            }
        }
        disableRendering = result;
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
}
