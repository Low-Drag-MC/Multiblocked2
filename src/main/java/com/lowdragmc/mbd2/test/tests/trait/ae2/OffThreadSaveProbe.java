package com.lowdragmc.mbd2.test.tests.trait.ae2;

import appeng.api.networking.GridServices;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridServiceProvider;
import net.minecraft.nbt.CompoundTag;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A grid service that records nodes saved off the server thread, as addons such as AE2 Lightning Tech refuse with an
 * exception. Only grids created after {@link #install} have it.
 */
public final class OffThreadSaveProbe implements IGridServiceProvider {
    private static final Set<Object> SAVED_OFF_THREAD = ConcurrentHashMap.newKeySet();
    private static boolean installed;

    public static synchronized void install() {
        if (!installed) {
            GridServices.register(OffThreadSaveProbe.class, OffThreadSaveProbe.class);
            installed = true;
        }
    }

    public static boolean savedOffThread(Object nodeOwner) {
        return SAVED_OFF_THREAD.contains(nodeOwner);
    }

    @Override
    public void saveNodeData(IGridNode gridNode, CompoundTag savedData) {
        if (!gridNode.getLevel().getServer().isSameThread()) {
            SAVED_OFF_THREAD.add(gridNode.getOwner());
        }
    }
}
