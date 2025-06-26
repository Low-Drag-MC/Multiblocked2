package com.lowdragmc.mbd2.api.machine;

import com.lowdragmc.mbd2.api.capability.MBDCapabilities;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeCapabilityHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.content.ContentModifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public interface IMultiPartMachine extends IMachine, IMultiPart {

    static Optional<IMultiPartMachine> ofPartMachine(@Nullable BlockEntity blockEntity) {
        return blockEntity == null ? Optional.empty() : blockEntity.getCapability(MBDCapabilities.CAPABILITY_MACHINE).resolve()
                .filter(IMultiPartMachine.class::isInstance)
                .map(IMultiPartMachine.class::cast);
    }

    static Optional<IMultiPartMachine> ofPartMachine(@Nonnull BlockGetter level, @Nonnull BlockPos pos) {
        return ofPartMachine(level.getBlockEntity(pos));
    }


    /**
     * Get all available traits for recipe logic. It is only used for controller recipe logic.
     * <br>
     * For self recipe logic, use {@link IRecipeCapabilityHolder#getRecipeCapabilitiesProxy()} to get recipe handlers.
     */
    List<IRecipeHandlerTrait<?>> getRecipeHandlers();

    /**
     * Called when controller recipe logic status changed
     */
    default void notifyControllerRecipeStatusChanged(IMultiControllerMachine controller, RecipeLogic.Status oldStatus, RecipeLogic.Status newStatus) {
    }

    /**
     * Called per tick in {@link RecipeLogic#handleRecipeWorking()}
     * @return whether it should keep working
     */
    default boolean onControllerWorking(IMultiControllerMachine controller) {
        return false;
    }

    /**
     * Called per tick in {@link RecipeLogic#handleRecipeWorking()}
     */
    default void onControllerWaiting(IMultiControllerMachine controller) {

    }

    /**
     * Called in {@link RecipeLogic#onRecipeFinish()} before outputs are produced
     */
    default void afterControllerWorking(IMultiControllerMachine controller) {

    }

    /**
     * Called in {@link RecipeLogic#setupRecipe(MBDRecipe)} ()}
     * @return whether interrupt the recipe setup.
     */
    default boolean beforeControllerWorking(IMultiControllerMachine controller) {
        return false;
    }

    /**
     * Override it to modify controller recipe on the fly e.g. applying overclock, change chance, etc
     * @param recipe recipe from detected from MBDRecipeType
     * @param controllerRecipeLogic controller recipe logic
     * @return modified recipe.
     *         null -- this recipe is unavailable
     */
    default @Nullable MBDRecipe modifyControllerRecipe(@Nonnull MBDRecipe recipe, RecipeLogic controllerRecipeLogic) {
        return recipe;
    }

    /**
     * Get the max parallel for controller recipe handling.
     */
    default ContentModifier getMaxControllerParallel(@Nonnull MBDRecipe recipe, RecipeLogic controllerRecipeLogic) {
        return ContentModifier.IDENTITY;
    }

    /**
     * Always try controller's {@link #doModifyRecipe(MBDRecipe)} before setting up controller recipe.
     */
    default boolean alwaysTryModifyControllerRecipe() {
        return false;
    }
}
