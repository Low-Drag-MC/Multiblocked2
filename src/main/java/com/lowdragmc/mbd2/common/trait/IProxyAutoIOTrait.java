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
     * <h4>How runtime values apply here</h4>
     * The sides and interval come from the <b>port's</b> {@code ProxyCapability} config, which is a list
     * element on the part definition and has no per-machine slots of its own — overriding those would
     * need per-element addressing, and the predicate-driven proxies are rebuilt on every form, so there
     * is nothing stable to address. Those stay definition-only.
     * <p>
     * What <em>is</em> honoured is an explicit runtime override of the proxied trait's own
     * {@code auto_io.enable}: see {@link #isAutoIOSuppressed}. Without that,
     * {@link IAutoIOTrait#setAutoIOEnabled}{@code (false)} would stop a trait's direct auto IO but leave
     * a port happily moving the same items, and "turn auto IO off for this machine" is the whole point of
     * the feature.
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
                if (isAutoIOSuppressed(trait)) continue;
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

    /**
     * Whether {@code trait} has been explicitly switched off at runtime, in which case a port must not
     * move anything on its behalf either.
     * <p>
     * Deliberately keyed on {@link com.lowdragmc.mbd2.common.runtime.RuntimeValue#isOverridden()} rather
     * than on the effective value. A definition commonly authors a trait's own auto IO as disabled and
     * lets only the port drive it — gating on the effective value would break every one of those setups.
     * An override, by contrast, is a runtime instruction from a script, a blueprint or a player, and a
     * runtime instruction should beat authored config.
     */
    static boolean isAutoIOSuppressed(ITrait trait) {
        if (!(trait instanceof IAutoIOTrait autoIOTrait)) return false;
        var runtimeAutoIO = autoIOTrait.getRuntimeAutoIO();
        return runtimeAutoIO != null && runtimeAutoIO.enable.isOverridden() && !runtimeAutoIO.enable.get();
    }
}
