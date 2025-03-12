package com.lowdragmc.mbd2.api.capability;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.machine.IMachine;
import net.neoforged.neoforge.capabilities.BlockCapability;

public class MBDCapabilities {
    public static final BlockCapability<IMachine, Void> CAPABILITY_MACHINE =
            BlockCapability.createVoid(
                    // Provide a name to uniquely identify the capability.
                    MBD2.id("machine"),
                    // Provide the queried type. Here, we want to look up `IItemHandler` instances.
                    IMachine.class);
}
