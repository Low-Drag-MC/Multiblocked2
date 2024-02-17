package com.lowdragmc.mbd2.api.trait;

import com.lowdragmc.mbd2.api.capability.recipe.IO;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nullable;

public interface ICapabilityProviderTrait<T> extends ITrait {

    /**
     * Get capability IO direction of the specific side.
     * <br/>
     * For example, whether you can insert or extract items from the specific side.
     */
    IO getCapabilityIO(@Nullable Direction side);

    /**
     * Get the capability for {@link ICapabilityProvider}.
     */
    Capability<T> getCapability();

    /**
     * Get the capability content for {@link ICapabilityProvider}.
     */
    T getCapContent(@Nullable Direction side);

    /**
     * Merge the content of the capability.
     * for example, when you create multiple item handlers, you can merge them into one.
     */
    default T mergeContents(T[] contents) {
        return contents.length > 0 ? contents[0] : getCapContent(null);
    }
}
