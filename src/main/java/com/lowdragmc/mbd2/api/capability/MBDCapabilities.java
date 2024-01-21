package com.lowdragmc.mbd2.api.capability;

import com.lowdragmc.mbd2.api.machine.IMachine;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

public class MBDCapabilities {
    public static final Capability<IMachine> CAPABILITY_MACHINE = CapabilityManager.get(new CapabilityToken<>() {});

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(IMachine.class);
    }
}
