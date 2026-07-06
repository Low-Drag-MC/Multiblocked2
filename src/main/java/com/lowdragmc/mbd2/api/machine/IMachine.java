package com.lowdragmc.mbd2.api.machine;

import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.capability.MBDCapabilities;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeCapabilityHolder;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.api.recipe.content.ContentModifier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public interface IMachine extends IRecipeCapabilityHolder {

    static Optional<IMachine> ofMachine(@Nullable BlockEntity blockEntity) {
        if (blockEntity == null) {
            return Optional.empty();
        }
        if (blockEntity instanceof IMachineBlockEntity machineBlockEntity) {
            return Optional.of(machineBlockEntity.getMetaMachine());
        }
        return blockEntity.getLevel() == null ? Optional.empty() : Optional.ofNullable(MBDCapabilities.CAPABILITY_MACHINE.getCapability(
                blockEntity.getLevel(), blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, null));
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
    default void saveCustomPersistedData(HolderLookup.Provider provider, CompoundTag tag, boolean forDrop) {

    }

    /**
     * Use for data not able to be saved with the SyncData system, like optional mod compatiblity in internal machines.
     * @param tag the CompoundTag to load data from
     */
    default void loadCustomPersistedData(HolderLookup.Provider provider, CompoundTag tag) {

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
     * Notify listeners that the block capabilities exposed at this position may have changed, so cached
     * capability lookups (e.g. from adjacent pipes) get re-resolved. A plain {@link #notifyBlockUpdate()}
     * is not enough for this: NeoForge's {@code BlockCapabilityCache} only re-resolves after
     * {@link net.minecraft.world.level.Level#invalidateCapabilities(BlockPos)} is called.
     * <br>
     * This must be called whenever the set of proxied/exposed capabilities changes, e.g. on formed/unformed.
     */
    default void invalidateCapabilities() {
        var pos = getPos();
        var level = getLevel();
        if (level != null) {
            level.invalidateCapabilities(pos);
        }
    }

    /**
     * on machine chunk unloaded.
     * <br>
     * You should call it in yourselves {@link BlockEntity#onChunkUnloaded()}.
     */
    default void onChunkUnloaded() {
        getRecipeLogic().inValid();
    }

    /**
     * on machine invalid in the chunk.
     * <br>
     * You should call it in yourselves {@link BlockEntity#setRemoved()}.
     */
    default void onUnload() {
        getRecipeLogic().inValid();
    }

    /**
     * on machine valid in the chunk.
     * <br>
     * You should call it in yourselves {@link BlockEntity#clearRemoved()}.
     */
    default void onLoad() {
        getRecipeLogic().valid();
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
     * Shall we run the recipe logic during the server tick?
     * <br>
     * if the machine has no recipe logic or using the {@link MBDRecipeType#DUMMY}, it will return false.
     */
    default boolean runRecipeLogic() {
        return getRecipeType() != MBDRecipeType.DUMMY;
    }

    /**
     * Recipe logic
     */
    @Nonnull
    RecipeLogic getRecipeLogic();

    /**
     * Modify fuel recipe for actual handling.
     * @return return null to skip this recipe/
     */
    @Nullable
    default MBDRecipe modifyFuelRecipe(MBDRecipe recipe) {
        return recipe;
    }

    /**
     * It will be called when the current fuel burning is finished, before searching next fuel.
     */
    default void onFuelBurningFinish(@Nullable MBDRecipe recipe) {}

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
     * Called in {@link RecipeLogic#onRecipeFinish()} when {@code ConsumeInputsAfterWorking} enabled and handled, before outputs are produced
     */
    default void onConsumeInputsAfterWorking() {

    }

    /**
     * Called in {@link RecipeLogic#onRecipeFinish()} after outputs are produced
     */
    default void onRecipeFinish() {

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
     * Always re-search recipe when the recipe is finished.
     * @return true - will re-search recipe when the last recipe is finished.
     */
    default boolean alwaysReSearchRecipe() {
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
     * Whether the inputs will be consumed after working?
     * @return false - it will be consumed before working.
     * true - it will be consumed after working (before output). During processing, if the inputs do not match the current recipe anymore,
     * the current recipe process will be discarded.
     */
    default boolean consumeInputsAfterWorking(MBDRecipe recipe) {
        return false;
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

    default @Nullable Component getCustomName() {
        return null;
    }

}
