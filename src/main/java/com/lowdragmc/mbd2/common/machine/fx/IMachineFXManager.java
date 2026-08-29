package com.lowdragmc.mbd2.common.machine.fx;

import com.lowdragmc.mbd2.common.machine.definition.config.fx.MachineFXConfig;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A machine's live effects, keyed by identifier.
 *
 * <p>Photon-free on purpose. {@link com.lowdragmc.mbd2.common.machine.MBDMachine} calls this from
 * {@code clientTick} and from its RPC entry points without knowing whether Photon is installed or
 * even whether it is on a client — {@link MachineFXManagers#create} hands back {@link #NOOP} in both
 * of those cases, so there is no {@code isPhotonLoaded()} check scattered through the machine.</p>
 */
public interface IMachineFXManager {

    /** Does nothing and never holds an effect. Used on servers and without Photon. */
    IMachineFXManager NOOP = new IMachineFXManager() {
        @Override
        public void play(MachineFXConfig config, String identifier) {}

        @Override
        public void stop(String identifier, boolean forcedDeath) {}

        @Override
        public void stopAll(boolean forcedDeath) {}

        @Override
        public void stopAllWithPrefix(String prefix, boolean forcedDeath) {}

        @Override
        public boolean isPlaying(String identifier) {
            return false;
        }

        @Override
        public List<String> playingIdentifiers() {
            return List.of();
        }
    };

    /**
     * Start {@code config} under {@code identifier}. An effect already playing under the same
     * identifier is replaced or wins, per {@link MachineFXConfig#isReplaceExisting()} — so calling
     * this repeatedly for a state that is still active is a no-op rather than a particle storm.
     */
    void play(MachineFXConfig config, String identifier);

    void stop(String identifier, boolean forcedDeath);

    void stopAll(boolean forcedDeath);

    /**
     * Stop every effect whose identifier starts with {@code prefix}, leaving the rest alone.
     *
     * <p>A prefix rather than a collection because the caller — a machine leaving a state — wants
     * "everything that state owned" and would otherwise have to build the list by filtering
     * {@link #playingIdentifiers()} on every state change, for every machine, whether or not
     * anything is playing.</p>
     */
    void stopAllWithPrefix(String prefix, boolean forcedDeath);

    boolean isPlaying(String identifier);

    /** The identifiers currently holding a live effect. A copy — safe to iterate while stopping. */
    List<String> playingIdentifiers();

    /**
     * Make this machine's effects deterministic, so the same tick shows the same picture every time.
     *
     * <p>Particle systems are random by design, which is right in the world and wrong in an editor:
     * replaying an effect to compare a change against the last run only means anything if the two
     * runs agree. Photon's own editor solves it the same way — a seeded {@code RandomSource} on the
     * execution context, which every emitter and every particle ultimately draws from.</p>
     *
     * <p>{@code null} restores the world's shared randomness. A {@code long} rather than any Photon
     * type so this interface still loads on a dedicated server.</p>
     */
    default void setPreviewSeed(@Nullable Long seed) {
    }

    /** Rewind the seeded source, so a replay from tick 0 reproduces the previous run exactly. */
    default void resetPreviewRandom() {
    }
}
