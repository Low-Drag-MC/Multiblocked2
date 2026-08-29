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

    /**
     * Put the scene at tick {@code time} of the effects {@code restart} starts.
     *
     * <h2>Re-simulated, not seeked</h2>
     * Particles carry their own simulation state and cannot be rewound, so the only honest way to
     * show tick N is to run N ticks. This is a port of Photon's own editor seek
     * ({@code SceneView.simulateTo}), and the three things it does that a naive loop does not are all
     * load-bearing:
     *
     * <ul>
     *   <li><b>Forward is incremental.</b> Only a backward seek restarts. Restarting on every seek
     *       makes dragging a scrub bar feel like a reset button rather than a scrub.</li>
     *   <li><b>The last two ticks run un-fast.</b> A paused editor renders with
     *       {@code partialTicks = 0}, and interpolating at 0 returns the <em>origin</em> snapshot — so
     *       if the final ticks skipped their visual update the scene would draw stale or empty.</li>
     *   <li><b>The manager's clock is set to match.</b> Otherwise everything reading the scene time —
     *       a playhead, a readout — keeps reporting the tick it was at before the seek.</li>
     * </ul>
     *
     * <p>Cost is linear in the distance travelled, on the render thread, which is why a caller
     * dragging a playhead should coalesce to at most one call per frame.</p>
     */
    public static void simulateTo(@Nullable ParticleManager manager, Runnable restart, long time) {
        if (manager == null) {
            restart.run();
            return;
        }
        Client.simulateTo(manager, restart, Math.max(0, time));
    }

    /** The scene clock, in ticks. {@code 0} without Photon. */
    public static long currentTime(@Nullable ParticleManager manager) {
        return manager == null ? 0 : Client.currentTime(manager);
    }

    /** The scene clock including the fraction of the current tick, for a playhead that does not step. */
    public static float currentTime(@Nullable ParticleManager manager, float partialTicks) {
        return manager == null ? 0 : Client.currentTime(manager, partialTicks);
    }

    public static boolean isPlaying(@Nullable ParticleManager manager) {
        return manager != null && Client.isPlaying(manager);
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

        private static void simulateTo(ParticleManager manager, Runnable restart, long time) {
            if (!(manager instanceof com.lowdragmc.photon.client.PhotonParticleManager photon)) {
                restart.run();
                return;
            }
            var now = photon.getTime();
            if (time > now) {
                runTicks(photon, time - now);
            } else {
                photon.clear();
                restart.run();
                runTicks(photon, time);
            }
            photon.setTime(time);
        }

        private static void runTicks(com.lowdragmc.photon.client.PhotonParticleManager photon, long ticks) {
            // Bounded like Photon's own: a scrub target is user input and this is the render thread.
            var count = Math.min(ticks, 500L * 20);
            try {
                for (long i = 0; i < count; i++) {
                    // The last two run un-fast — see simulateTo's javadoc on partialTicks = 0.
                    com.lowdragmc.photon.client.PhotonParticleManager.setFastSimulation(i < count - 2);
                    // tickInternal, not tick: the latter is gated on the manager playing, and seeking
                    // while paused is the whole point.
                    photon.tickInternal();
                }
            } finally {
                // A throw mid-replay would otherwise leave every particle in the game skipping its
                // visual updates, since the flag is static and shared.
                com.lowdragmc.photon.client.PhotonParticleManager.setFastSimulation(false);
            }
        }

        private static long currentTime(ParticleManager manager) {
            return manager instanceof com.lowdragmc.photon.client.PhotonParticleManager photon
                    ? photon.getTime() : 0;
        }

        private static float currentTime(ParticleManager manager, float partialTicks) {
            return manager instanceof com.lowdragmc.photon.client.PhotonParticleManager photon
                    ? photon.getTime(partialTicks) : 0;
        }

        private static boolean isPlaying(ParticleManager manager) {
            return manager instanceof com.lowdragmc.photon.client.PhotonParticleManager photon
                    && photon.isPlaying();
        }
    }
}
