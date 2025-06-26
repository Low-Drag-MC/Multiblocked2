package com.lowdragmc.mbd2.api.capability;

import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.api.machine.IMultiController;
import com.lowdragmc.mbd2.api.machine.IMultiPart;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

public class MBDCapabilities {
    public static final Capability<IMachine> CAPABILITY_MACHINE = CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<IMultiController> CAPABILITY_MULTI_CONTROLLER = CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<IMultiPart> CAPABILITY_MULTI_PART = CapabilityManager.get(new CapabilityToken<>() {});

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(IMachine.class);
        event.register(IMultiController.class);
        event.register(IMultiPart.class);
    }
}
