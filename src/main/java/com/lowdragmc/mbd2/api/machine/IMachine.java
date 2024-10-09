package com.lowdragmc.mbd2.api.machine;

import com.lowdragmc.mbd2.api.capability.MBDCapabilities;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeCapabilityHolder;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.api.recipe.content.ContentModifier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public interface IMachine extends IRecipeCapabilityHolder {

    static Optional<IMachine> ofMachine(@Nullable BlockEntity blockEntity) {
        return blockEntity == null ? Optional.empty() : blockEntity.getCapability(MBDCapabilities.CAPABILITY_MACHINE).resolve();
    }

    static Optional<IMachine> ofMachine(@Nonnull BlockGetter level, @Nonnull BlockPos pos) {
        return ofMachine(level.getBlockEntity(pos));
    }

    /**
     * Get the block entity holder.
     */
    BlockEntity getHolder();

    /**
     * Get the level.
     */
    default Level getLevel() {
        return getHolder().getLevel();
    }

    /**
     * Get machine position.
     */
    default BlockPos getPos() {
        return getHolder().getBlockPos();
    }

    /**
     * Get the block state.
     */
    default BlockState getBlockState() {
        return getHolder().getBlockState();
    }

    /**
     * Get the random offset.
     */
    long getOffset();

    /**
     * Get the offset timer.
     */
    default long getOffsetTimer() {
        var level = getLevel();
        return level == null ? getOffset() : (level.getGameTime() + getOffset());
    }

    /**
     * Mark the machine as dirty.
     */
    default void markDirty() {
        var level = getLevel();
        // make sure mark dirty in the server thread.
        if (level != null && !level.isClientSide && level.getServer() != null) {
            level.getServer().execute(() -> getHolder().setChanged());
        }
    }

    /**
     * Is the machine still valid.
     */
    default boolean isInValid() {
        return getHolder().isRemoved();
    }

    /**
     * Use for data not able to be saved with the SyncData system, like optional mod compatiblity in internal machines.
     * @param tag the CompoundTag to load data from
     * @param forDrop if the save is done for dropping the machine as an item.
     */
    default void saveCustomPersistedData(CompoundTag tag, boolean forDrop) {

    }

    /**
     * Use for data not able to be saved with the SyncData system, like optional mod compatiblity in internal machines.
     * @param tag the CompoundTag to load data from
     */
    default void loadCustomPersistedData(CompoundTag tag) {

    }

    /**
     * Get the front facing of the machine.
     */
    Optional<Direction> getFrontFacing();

    /**
     * Whether it has front face.
     * @return false: structure of all sides are available.
     */
    default boolean hasFrontFacing() {
        return getFrontFacing().isPresent();
    }

    /**
     * Is the facing valid for setup.
     */
    boolean isFacingValid(Direction facing);

    /**
     * Set the front facing of the machine.
     */
    void setFrontFacing(Direction facing);

    /**
     * Called when the machine is rotated.
     * <br>
     * It has to be triggered somewhere yourself.
     */
    default void onRotated(Direction oldFacing, Direction newFacing) {

    }

    /**
     * re-render the chunk.
     */
    default void scheduleRenderUpdate() {
        var pos = getPos();
        var level = getLevel();
        if (level != null) {
            var state = level.getBlockState(pos);
            if (level.isClientSide) {
                level.sendBlockUpdated(pos, state, state, 1 << 3);
            }
        }
    }

    /**
     * Notify the block update.
     */
    default void notifyBlockUpdate() {
        var pos = getPos();
        var level = getLevel();
        if (level != null) {
            level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock());
        }
    }

    /**
     * on machine invalid in the chunk.
     * <br>
     * You should call it in yourselves {@link BlockEntity#setRemoved()}.
     */
    default void onUnload() {

    }

    /**
     * on machine valid in the chunk.
     * <br>
     * You should call it in yourselves {@link BlockEntity#clearRemoved()}.
     */
    default void onLoad() {
        getRecipeLogic().inValid();
    }

    //////////////////////////////////////
    //********   RECIPE LOGIC  *********//
    //////////////////////////////////////

    /**
     * Get the recipe type.
     */
    @Nonnull
    MBDRecipeType getRecipeType();

    /**
     * Called when recipe logic status changed
     */
    default void notifyRecipeStatusChanged(RecipeLogic.Status oldStatus, RecipeLogic.Status newStatus) {
    }

    /**
     * Recipe logic
     */
    @Nonnull
    RecipeLogic getRecipeLogic();

    /**
     * do not override it.
     * <br> implement {@link #getModifiedRecipe(MBDRecipe)} for recipe modification.
     * <br> implement {@link #getMaxParallel(MBDRecipe)} for parallel recipe.
     */
    @Nullable
    default MBDRecipe doModifyRecipe(@Nonnull MBDRecipe recipe) {
        recipe = getModifiedRecipe(recipe);
        if (recipe == null) {
            return null;
        }
        return applyParallel(recipe, getMaxParallel(recipe).apply(1).intValue());
    }

    /**
     * Override it to modify recipe on the fly e.g. applying overclock, change chance, etc
     * @param recipe recipe from detected from MBDRecipe
     * @return modified recipe.
     *         null -- this recipe is unavailable
     */
    @Nullable
    default MBDRecipe getModifiedRecipe(@Nonnull MBDRecipe recipe) {
        return recipe;
    }

    /**
     * Get the max parallel.
     */
    default ContentModifier getMaxParallel(@Nonnull MBDRecipe recipe) {
        return ContentModifier.IDENTITY;
    }

    /**
     * Apply parallel to the recipe.
     * @param recipe the recipe to apply parallel
     * @param maxParallel the max parallel
     * @return the modified recipe
     */
    @Nonnull
    default MBDRecipe applyParallel(@Nonnull MBDRecipe recipe, int maxParallel) {
        // apply parallel here
        if (maxParallel > 1) {
            var result = MBDRecipe.accurateParallel(this, recipe, maxParallel, false);
            return result.getFirst();
        }
        return recipe;
    }

    /**
     * Called in {@link RecipeLogic#setupRecipe(MBDRecipe)} ()}
     * @return whether interrupt the recipe setup.
     */
    default boolean beforeWorking(MBDRecipe recipe) {
        return false;
    }

    /**
     * Called per tick in {@link RecipeLogic#handleRecipeWorking()}
     * @return whether interrupt the recipe working.
     */
    default boolean onWorking() {
        return false;
    }

    /**
     * Called per tick in {@link RecipeLogic#handleRecipeWorking()}
     */
    default void onWaiting() {

    }

    /**
     * Called in {@link RecipeLogic#onRecipeFinish()} before outputs are produced
     */
    default void afterWorking() {

    }

    /**
     * Whether progress decrease when machine is waiting for pertick ingredients. (e.g. lack of EU)
     */
    default boolean dampingWhenWaiting() {
        return true;
    }

    /**
     * Always try {@link #doModifyRecipe(MBDRecipe)} before setting up recipe.
     * @return true - will map {@link RecipeLogic#getLastOriginRecipe()} to the latest recipe for next round when finishing.
     * false - keep using the {@link RecipeLogic#getLastRecipe()}, which is already modified.
     */
    default boolean alwaysTryModifyRecipe() {
        return false;
    }

    /**
     * if the recipe handling is waiting, damping value is the decreased ticks of the current progress.
     * @return damping value in tick.
     */
    default int getRecipeDampingValue() {
        return 2;
    }

    /**
     * Get the machine level. it will be used for recipe condition {@link com.lowdragmc.mbd2.common.recipe.MachineLevelCondition} an so on.
     */
    default int getMachineLevel() {
        return 0;
    }

    @Override
    default int getChanceTier() {
        return getMachineLevel();
    }
}
