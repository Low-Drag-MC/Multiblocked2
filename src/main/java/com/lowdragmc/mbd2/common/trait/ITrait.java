package com.lowdragmc.mbd2.common.trait;


import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
import com.lowdragmc.mbd2.common.gui.editor.machine.MachineTraitPanel;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.common.capabilities.Capability;

import java.util.Collections;
import java.util.List;

/**
 * A trait that represent a capability / behaviour / function a machine has, e.g. item container, energy storage, fluid tank, etc.
 * <br/>
 * To provide capability behavior in the world see {@link ICapabilityProviderTrait}. For recipe handling, see {@link RecipeHandlerTrait}.
 * To provide UI representation, see {@link IUIProviderTrait}.
 */
public interface ITrait {
    /**
     * @return the machine this trait is attached to
     */
    MBDMachine getMachine();

    /**
     * @return the definition of this trait
     */
    TraitDefinition getDefinition();

    /**
     * It will be called when this trait is being previewed in the editor. see {@link MachineTraitPanel#reloadAdditionalTraits()}
     * <br/>
     * e.g. you can do some storage preparation, or render some preview model.
     */
    default void onLoadingTraitInPreview() {}

    /**
     * Called when the machine is being loaded.
     */
    default void onMachineLoad() {}

    /**
     * Called when the machine is unloaded.
     */
    default void onChunkUnloaded() {}

    /**
     * Called when the machine is being unloaded.
     */
    default void onMachineUnLoad() {}

    /**
     * Called when the machine is being removed.
     */
    default void onMachineRemoved() {}

    /**
     * Called when the machine is dropping
     */
    default void onMachineDrop(Entity entity, List<ItemStack> drops) {

    }

    /**
     * Called per server tick.
     */
    default void serverTick() {

    }

    /**
     * Called per client tick.
     */
    default void clientTick() {

    }

    /**
     * Called when neighbors changed.
     */
    default void onNeighborChanged(Block block, BlockPos fromPos, boolean isMoving) {

    }

    /**
     * Get all available recipe handler traits for this trait which will be used for recipe logic.
     */
    default List<IRecipeHandlerTrait<?>> getRecipeHandlerTraits() {
        return Collections.emptyList();
    }

    /**
     * Get all available capability provider traits for this trait which will be used for {@link net.minecraft.world.level.block.entity.BlockEntity#getCapability(Capability, Direction)}.
     */
    default List<ICapabilityProviderTrait<?>> getCapabilityProviderTraits() {
        return Collections.emptyList();
    }
}
