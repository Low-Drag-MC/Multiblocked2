package com.lowdragmc.mbd2.integration.photon;

import com.lowdragmc.lowdraglib2.client.scene.ParticleManager;
import org.jetbrains.annotations.Nullable;

/**
 * Photon's particle host for an MBD2 editor scene, behind Photon-free signatures.
 *
 * <p>An LDLib2 scene works with the plain {@link ParticleManager} it builds itself, but Photon's
 * render pipeline asks {@code PhotonParticleManager.getRenderingManager()} whether it is drawing
 * into a UI sub-viewport, and takes its <em>world</em> branch when the answer is null — late
 * compositing against a frame that does not exist here, and post effects submitted to the global
 * stack instead of the scene's own. Substituting Photon's manager is what makes an embedded preview
 * render the way Photon's own editor does.</p>
 *
 * <p>The playback controls live here rather than on the caller because they only exist on Photon's
 * manager; the editor view holds a plain {@link ParticleManager} and hands it back.</p>
 *
 * @see PhotonFXBridge for why the Photon-typed work sits in a nested class
 */
public class PhotonFXScene {

    private PhotonFXScene() {}

    /**
     * A Photon-backed particle manager for an editor scene, or {@code null} when Photon is absent —
     * in which case the scene keeps LDLib2's default and simply never shows an effect.
     */
    @Nullable
    public static ParticleManager createParticleManager() {
        if (!PhotonFXBridge.isAvailable()) return null;
        return Client.create();
    }

    /** Start the scene clock. Photon's manager does not tick until it is playing. */
    public static void play(@Nullable ParticleManager manager) {
        if (manager != null) Client.control(manager, Control.PLAY);
    }

    public static void pause(@Nullable ParticleManager manager) {
        if (manager != null) Client.control(manager, Control.PAUSE);
    }

    /** Discard every particle and reset the scene clock. */
    public static void clear(@Nullable ParticleManager manager) {
        if (manager != null) Client.control(manager, Control.CLEAR);
    }

    private enum Control { PLAY, PAUSE, CLEAR }

    /**
     * The Photon-typed half.
     *
     * <p>The null check at each call site is what keeps this class unreachable without Photon —
     * a non-null manager can only have come from {@link #createParticleManager()}, which already
     * returned {@code null} there. The {@code instanceof} then only has to answer "is this Photon's
     * manager rather than LDLib2's default", which it can be if the scene was built before the
     * substitution.</p>
     */
    private static class Client {
        private static ParticleManager create() {
            return new com.lowdragmc.photon.client.PhotonParticleManager(
                    com.lowdragmc.photon.client.FXSceneOptions.DEFAULT);
        }

        private static void control(ParticleManager manager, Control control) {
            if (!(manager instanceof com.lowdragmc.photon.client.PhotonParticleManager photon)) return;
            switch (control) {
                case PLAY -> photon.play();
                case PAUSE -> photon.pause();
                case CLEAR -> photon.clear();
            }
        }
    }
}
