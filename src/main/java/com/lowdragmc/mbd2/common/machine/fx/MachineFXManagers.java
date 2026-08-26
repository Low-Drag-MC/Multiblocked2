package com.lowdragmc.mbd2.common.machine.fx;

import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.integration.photon.PhotonFXBridge;
import com.lowdragmc.mbd2.integration.photon.PhotonMachineFXManager;

/**
 * Picks the effect manager a machine gets.
 *
 * <p>The {@code new} below is the only place MBD2 constructs a Photon-backed object from common
 * code, and it sits behind {@link PhotonFXBridge#isAvailable()} for the reason the whole integration
 * is shaped this way: the JVM resolves {@link PhotonMachineFXManager} the first time that
 * instruction runs, so a guard that returns first means a missing Photon jar is never looked up.
 * Same idiom as {@code MBDMachine.triggerGeckolibAnim}.</p>
 */
public class MachineFXManagers {

    private MachineFXManagers() {}

    /**
     * Effects are client-side objects with no server counterpart, so a server machine gets
     * {@link IMachineFXManager#NOOP} — the machine's RPC methods relay to tracking clients before
     * they would ever reach it.
     */
    public static IMachineFXManager create(MBDMachine machine) {
        if (!PhotonFXBridge.isAvailable() || !machine.isRemote()) {
            return IMachineFXManager.NOOP;
        }
        return new PhotonMachineFXManager(machine);
    }
}
