package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigPartSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Interface for proxy auto IO trait. The block can implement this to handle proxy auto IO from {@link ConfigPartSettings#proxyControllerCapabilities()}
 */
public interface IProxyAutoIOTrait extends ITrait {
    /**
     * Handle the auto IO. It will be called on the server side.
     * @param port port pos.
     * @param side the side of the port.
     */
    void handleAutoIO(BlockPos port, @NotNull Direction side, IO io);

    /**
     * Run the auto IO configured on {@code proxies} against the traits of {@code controller}, using
     * {@code port} as the block whose neighbours are pushed to / pulled from.
     * <br>
     * Every place that exposes proxied capabilities has to drive them through here as well, otherwise
     * the port forwards the trait but never moves anything (see issue #237). That covers both
     * {@link ConfigPartSettings#proxyControllerCapabilities()} on a part and the {@code proxyCapabilities}
     * of a matched {@code proxyWhileFormed} predicate.
     *
     * @param controller the machine owning the proxied traits
     * @param proxies    the proxy capability configs to apply
     * @param port       the position acting as the port, i.e. the proxying block
     * @param front      the facing the per-side IO config is resolved against
     * @param timer      tick counter the configured interval is applied to
     */
    static void handleProxyAutoIO(MBDMachine controller,
                                  List<ConfigPartSettings.ProxyCapability> proxies,
                                  BlockPos port,
                                  Direction front,
                                  long timer) {
        if (proxies.isEmpty()) return;
        for (var proxy : proxies) {
            var autoIO = proxy.autoIO();
            if (!autoIO.isEnable() || timer % Math.max(1, autoIO.getInterval()) != 0) continue;
            var filter = proxy.traitNameFilter();
            for (var trait : controller.getAdditionalTraits()) {
                if (!(trait instanceof IProxyAutoIOTrait autoIOTrait)) continue;
                // an unset filter means "every trait", same rule the capability side uses
                if (filter != null && !filter.isEmpty()
                        && !trait.getDefinition().getName().contains(filter)) continue;
                for (var side : Direction.values()) {
                    var io = autoIO.getIO(front, side);
                    if (io != IO.NONE) {
                        autoIOTrait.handleAutoIO(port, side, io);
                    }
                }
            }
        }
    }
}
