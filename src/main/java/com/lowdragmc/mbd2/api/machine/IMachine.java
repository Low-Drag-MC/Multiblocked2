package com.lowdragmc.mbd2.api.machine;

import com.lowdragmc.mbd2.api.capability.MBDCapabilities;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeCapabilityHolder;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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

    static Optional<IMachine> ofMachine(@Nonnull Level level, @Nonnull BlockPos pos) {
        return ofMachine(level.getBlockEntity(pos));
    }

    /**
     * Get machine definition.
     */
    MachineDefinition getDefinition();

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
     * Get the offset timer.
     */
    long getOffsetTimer();

    /**
     * Mark the machine as dirty.
     */
    default void markDirty() {
        getHolder().setChanged();
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
    void saveCustomPersistedData(CompoundTag tag, boolean forDrop);

    /**
     * Use for data not able to be saved with the SyncData system, like optional mod compatiblity in internal machines.
     * @param tag the CompoundTag to load data from
     */
    void loadCustomPersistedData(CompoundTag tag);

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

    //////////////////////////////////////
    //********   RECIPE LOGIC  *********//
    //////////////////////////////////////

    @Nonnull
    MBDRecipeType getRecipeType();

    /**
     * Called when recipe logic status changed
     */
    default void notifyStatusChanged(RecipeLogic.Status oldStatus, RecipeLogic.Status newStatus) {
    }

    /**
     * Recipe logic
     */
    @Nonnull
    RecipeLogic getRecipeLogic();

    /**
     * Override it to modify recipe on the fly e.g. applying overclock, change chance, etc
     * @param recipe recipe from detected from MBDRecipe
     * @return modified recipe.
     *         null -- this recipe is unavailable
     */
    @Nullable
    default MBDRecipe doModifyRecipe(MBDRecipe recipe) {
        return recipe;
//        return getDefinition().getRecipeModifier().apply(self(), recipe);
    }

    /**
     * Called in {@link RecipeLogic#setupRecipe(MBDRecipe)} ()}
     */
    default void beforeWorking() {

    }

    /**
     * Called per tick in {@link RecipeLogic#handleRecipeWorking()}
     */
    default void onWorking() {

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

}
