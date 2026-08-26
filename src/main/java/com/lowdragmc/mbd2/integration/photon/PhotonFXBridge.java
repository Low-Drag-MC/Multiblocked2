package com.lowdragmc.mbd2.integration.photon;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.mbd2.MBD2;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * The one door from common MBD2 code into Photon.
 *
 * <p>Every method here is guarded and every signature is Photon-free, which is the whole point: a
 * caller can be a config class that loads on a dedicated server with no Photon jar. The JVM resolves
 * a constant-pool entry the first time the instruction using it executes, so as long as the guard
 * returns before any Photon type is touched, the missing class is never looked up.</p>
 *
 * <p>Anything that needs to <em>name</em> a Photon type — an {@code FX}, a
 * {@code PhotonParticleManager} — belongs in a class under this package that only Photon-gated code
 * reaches, never here. See {@link PhotonMachineFXManager} and {@link PhotonFXScene}.</p>
 */
public class PhotonFXBridge {

    private PhotonFXBridge() {}

    /** Whether machine FX can do anything at all: Photon present, and we are on a client. */
    public static boolean isAvailable() {
        return MBD2.isPhotonLoaded() && LDLib2.isClient();
    }

    /**
     * Every fx id currently loadable — mod jars, resource packs and mounted {@code .fxpack}s alike.
     * Empty without Photon, which is what lets the editor's fx picker exist unconditionally.
     */
    public static List<ResourceLocation> listFXIds() {
        if (!isAvailable()) return List.of();
        return Client.listFXIds();
    }

    /** Whether {@code fxLocation} resolves to a loadable effect. */
    public static boolean hasFX(ResourceLocation fxLocation) {
        if (!isAvailable()) return false;
        return Client.hasFX(fxLocation);
    }

    /**
     * The Photon-typed half. A separate class so that loading {@link PhotonFXBridge} — which common
     * code does — never drags Photon's classes into verification, only calling into this one does.
     */
    private static class Client {
        private static List<ResourceLocation> listFXIds() {
            return com.lowdragmc.photon.client.fx.FXHelper.listAllFX();
        }

        private static boolean hasFX(ResourceLocation fxLocation) {
            return com.lowdragmc.photon.client.fx.FXHelper.getFX(fxLocation) != null;
        }
    }
}
