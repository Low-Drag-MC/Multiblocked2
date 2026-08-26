package com.lowdragmc.mbd2.integration.photon;

import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.definition.config.fx.MachineFXConfig;
import com.lowdragmc.mbd2.common.machine.fx.IMachineFXManager;
import com.lowdragmc.photon.client.fx.FXHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Photon-backed {@link IMachineFXManager}: one machine's live effects, keyed by identifier.
 *
 * <p>Held by the client-side {@link MBDMachine} and dies with it, which is what bounds the lifetime
 * of everything in here — {@code MBDMachine.onUnload} stops the lot. Individual effects also police
 * themselves through {@link MachineFXExecutor#updateFXObjectTick}, because Photon keeps ticking a
 * runtime long after MBD2 stops looking at it.</p>
 */
@OnlyIn(Dist.CLIENT)
public class PhotonMachineFXManager implements IMachineFXManager {

    private final MBDMachine machine;
    /** Insertion-ordered so {@link #stopAll} is deterministic and reads the way it was authored. */
    private final Map<String, MachineFXExecutor> effects = new LinkedHashMap<>();
    /** Locations {@link FXHelper} could not load, so a broken one is reported once, not per call. */
    private final Set<ResourceLocation> missing = new HashSet<>();

    public PhotonMachineFXManager(MBDMachine machine) {
        this.machine = machine;
    }

    @Override
    public void play(MachineFXConfig config, String identifier) {
        if (identifier == null || identifier.isEmpty()) return;
        // Cheap checks first: a machine farm calls this every state change on every machine, and
        // loading an FX definition means reading and inflating a file.
        if (!isWithinRange(config)) return;
        var existing = effects.get(identifier);
        if (existing != null && existing.isAlive() && !config.isReplaceExisting()) {
            // The running effect wins. This is what makes "play the current state's effects" safe to
            // call repeatedly instead of restarting them every time it runs.
            return;
        }
        var location = config.getFxLocation();
        if (missing.contains(location)) return;
        var fx = FXHelper.getFX(location);
        if (fx == null) {
            // FXHelper logs the failure with a stack trace but does NOT cache the miss, so without
            // this a typo'd location would re-open the resource and re-log on every call — and
            // Play Machine FX is documented as safe to fire every tick.
            missing.add(location);
            return;
        }
        if (existing != null) {
            existing.kill();
        }
        var executor = new MachineFXExecutor(fx, machine, this, identifier, config);
        executor.start();
        effects.put(identifier, executor);
    }

    /**
     * Block entities tick for the whole loaded chunk radius, not the render distance, so without
     * this a machine 200 blocks away still builds a particle system nobody can see.
     */
    private boolean isWithinRange(MachineFXConfig config) {
        if (config.getMaxDistance() <= 0) return true;
        var player = Minecraft.getInstance().player;
        if (player == null) {
            // No local player yet — the editor preview scene, where distance is meaningless.
            return true;
        }
        var pos = machine.getPos();
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                <= config.getMaxDistance() * config.getMaxDistance();
    }

    @Override
    public void stop(String identifier, boolean forcedDeath) {
        var effect = effects.remove(identifier);
        if (effect != null) {
            effect.kill(forcedDeath);
        }
    }

    @Override
    public void stopAll(boolean forcedDeath) {
        // Copy: kill -> destroy -> updateFXObjectTick can re-enter unregister on this same map.
        for (var effect : List.copyOf(effects.values())) {
            effect.kill(forcedDeath);
        }
        effects.clear();
    }

    @Override
    public void stopAllWithPrefix(String prefix, boolean forcedDeath) {
        // In-place iteration: the common case is "nothing to stop", and this is on the state-change
        // path for every machine. Building a filtered list first allocated even when it was empty.
        var iterator = effects.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getKey().startsWith(prefix)) {
                iterator.remove();
                entry.getValue().kill(forcedDeath);
            }
        }
    }

    @Override
    public boolean isPlaying(String identifier) {
        var effect = effects.get(identifier);
        return effect != null && effect.isAlive();
    }

    @Override
    public List<String> playingIdentifiers() {
        return List.copyOf(effects.keySet());
    }

    // ---- executor callbacks -------------------------------------------------------------------

    @Nullable
    MachineFXExecutor peek(String identifier) {
        return effects.get(identifier);
    }

    /** Drop {@code effect} only if it is still the one registered — a newer start must not be evicted. */
    void unregister(String identifier, MachineFXExecutor effect) {
        effects.remove(identifier, effect);
    }
}
