package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigPartSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Interface for proxy auto IO trait. The block can implement this to handle proxy auto IO from {@link ConfigPartSettings#proxyControllerCapabilities()}
 */
public interface IProxyAutoIOTrait extends ITrait {
    /**
     * Handle the auto IO. It will be called on the server side.
     * @param port port pos.
     * @param side the side of the port.
     */
    void handleAutoIO(BlockPos port, Direction side, IO io);

}
