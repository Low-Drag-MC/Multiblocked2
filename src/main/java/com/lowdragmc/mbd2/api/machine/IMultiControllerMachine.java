package com.lowdragmc.mbd2.api.machine;

import com.lowdragmc.mbd2.api.capability.MBDCapabilities;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.api.recipe.content.ContentModifier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public interface IMultiControllerMachine extends IMachine, IMultiController {

    static Optional<IMultiControllerMachine> ofControllerMachine(@Nullable BlockEntity blockEntity) {
        return blockEntity == null ? Optional.empty() : blockEntity.getCapability(MBDCapabilities.CAPABILITY_MACHINE).resolve()
                .filter(IMultiControllerMachine.class::isInstance)
                .map(IMultiControllerMachine.class::cast);
    }

    static Optional<IMultiControllerMachine> ofControllerMachine(@Nonnull BlockGetter level, @Nonnull BlockPos pos) {
        return ofControllerMachine(level.getBlockEntity(pos));
    }

    /**
     * Whether it has front face.
     * @return false: structure of all sides are available.
     */
    default boolean hasFrontFacing() {
        return getFrontFacing().isPresent();
    }

    /**
     * Get all parts
     */
    List<IMultiPartMachine> getPartMachines();

    /**
     * Override it to modify recipe on the fly e.g. applying overclock, change chance, etc
     * <br>
     * Parts can modify recipe by calling {@link IMultiPartMachine#modifyControllerRecipe(MBDRecipe, RecipeLogic)}
     * @param recipe recipe from detected from MBDRecipe
     * @return modified recipe.
     *         null -- this recipe is unavailable
     */
    @Nullable
    default MBDRecipe getModifiedRecipe(@Nonnull MBDRecipe recipe) {
        for (var part : getPartMachines()) {
            recipe = part.modifyControllerRecipe(recipe, getRecipeLogic());
            if (recipe == null) return null;
        }
        return recipe;
    }

    @Override
    default ContentModifier getMaxParallel(@NotNull MBDRecipe recipe) {
        var maxParallel = IMachine.super.getMaxParallel(recipe);
        for (var part : getPartMachines()) {
            maxParallel = maxParallel.merge(part.getMaxControllerParallel(recipe, getRecipeLogic()));
        }
        return maxParallel;
    }

    @Override
    default boolean alwaysTryModifyRecipe() {
        for (var part : getPartMachines()) {
            if (part.alwaysTryModifyControllerRecipe()) {
                return true;
            }
        }
        return false;
    }

    /**
     * get parts' Appearance. same as IForgeBlock.getAppearance() / IFabricBlock.getAppearance()
     */
    @Nullable
    default BlockState getPartAppearance(IMultiPartMachine part, Direction side, BlockState sourceState, BlockPos sourcePos) {
        return null;
    }

    /**
     * Called when recipe logic status changed
     */
    default void notifyRecipeStatusChanged(RecipeLogic.Status oldStatus, RecipeLogic.Status newStatus) {
        for (IMultiPartMachine part : getPartMachines()) {
            part.notifyControllerRecipeStatusChanged(this, oldStatus, newStatus);
        }
    }

    @Override
    default boolean beforeWorking(MBDRecipe recipe) {
        IMachine.super.beforeWorking(recipe);
        for (IMultiPartMachine part : getPartMachines()) {
            if (part.beforeControllerWorking(this)) {
                return true;
            }
        }
        return false;
    }

    @Override
    default boolean onWorking() {
        IMachine.super.onWorking();
        for (IMultiPartMachine part : getPartMachines()) {
            if (part.onControllerWorking(this)) {
                return true;
            }
        }
        return false;
    }

    @Override
    default void onWaiting() {
        IMachine.super.onWaiting();
        for (IMultiPartMachine part : getPartMachines()) {
            part.onControllerWaiting(this);
        }
    }

    @Override
    default void afterWorking() {
        IMachine.super.afterWorking();
        for (IMultiPartMachine part : getPartMachines()) {
            part.afterControllerWorking(this);
        }
    }
}
