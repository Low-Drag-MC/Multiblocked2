package com.lowdragmc.mbd2.common.machine.definition.config.fx;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSearch;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import com.lowdragmc.mbd2.integration.photon.PhotonFXBridge;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * One Photon effect a machine can play, as authored in the editor.
 *
 * <p>Deliberately free of every Photon type — a {@link ResourceLocation} and a handful of numbers.
 * That is what lets a definition authored with Photon installed load unchanged on a server or client
 * without it: the config round-trips through NBT either way, and only
 * {@link com.lowdragmc.mbd2.common.machine.fx.IMachineFXManager} decides whether anything plays.</p>
 *
 * <p>Used from two places, which is why {@link #name} exists at all: a
 * {@link com.lowdragmc.mbd2.common.machine.definition.config.MachineState} plays its whole list while
 * the machine is in that state, and
 * {@link com.lowdragmc.mbd2.common.machine.definition.config.ConfigMachineSettings#photonFXs()} is a
 * library the blueprint nodes trigger by name.</p>
 */
@Getter
@Setter
public class MachineFXConfig implements IConfigurable, IPersistedSerializable {

    /**
     * The identifier this effect plays under. Starting a second effect with the same identifier
     * replaces or is refused by the first — see {@code replaceExisting}.
     */
    @Configurable(name = "config.machine_fx.name", tips = "config.machine_fx.name.tooltip")
    private String name = "fx";

    /**
     * The Photon effect id, in {@code namespace:path} form with no {@code fx/} prefix and no
     * {@code .fx} suffix — {@code photon:example} resolves {@code assets/photon/fx/example.fx}.
     */
    @Configurable(name = "config.machine_fx.fx_location", tips = "config.machine_fx.fx_location.tooltip")
    @ConfigSearch(searchConfiguratorMethod = "searchFX")
    private ResourceLocation fxLocation = ResourceLocation.fromNamespaceAndPath("photon", "example");

    /** Offset from the centre of the machine's block. */
    @Configurable(name = "config.machine_fx.offset")
    private Vector3f offset = new Vector3f();

    /** Rotation in degrees. */
    @Configurable(name = "config.machine_fx.rotation")
    private Vector3f rotation = new Vector3f();

    @Configurable(name = "config.machine_fx.scale")
    private Vector3f scale = new Vector3f(1, 1, 1);

    @Configurable(name = "config.machine_fx.delay", tips = "config.machine_fx.delay.tooltip")
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    private int delay = 0;

    @Configurable(name = "config.machine_fx.forced_death", tips = {
            "config.machine_fx.forced_death.tooltip.0",
            "config.machine_fx.forced_death.tooltip.1",
    })
    private boolean forcedDeath = false;

    /**
     * Whether starting this effect while one is already playing under the same identifier replaces it.
     * When {@code false} the running effect wins and the new one is dropped, which is what makes a
     * per-tick "play" call idempotent.
     */
    @Configurable(name = "config.machine_fx.replace_existing", tips = {
            "config.machine_fx.replace_existing.tooltip.0",
            "config.machine_fx.replace_existing.tooltip.1",
    })
    private boolean replaceExisting = false;

    /**
     * Rotate {@link #offset} and {@link #rotation} by the machine's front facing, so an effect
     * authored against a north-facing machine follows it when it is placed another way.
     */
    @Configurable(name = "config.machine_fx.follow_facing", tips = "config.machine_fx.follow_facing.tooltip")
    private boolean followFacing = true;

    /**
     * How far from the player the effect may <em>start</em>. Block entities tick for the whole loaded
     * chunk radius, not the render distance, so without this a machine that comes into range spawns a
     * particle system nobody is near. It is a start gate only: an effect that is already running
     * keeps running until its state ends or the chunk unloads.
     */
    @Configurable(name = "config.machine_fx.max_distance", tips = "config.machine_fx.max_distance.tooltip")
    @ConfigNumber(range = {0, 512})
    private double maxDistance = 64;

    public MachineFXConfig() {
    }

    public MachineFXConfig(String name, ResourceLocation fxLocation) {
        this.name = name;
        this.fxLocation = fxLocation;
    }

    // ---- list plumbing -------------------------------------------------------------------------
    //
    // LDLib2 resolves @ConfigList / @ReadOnlyManaged methods reflectively on the declaring class, so
    // every list of these needs its own four stubs. Sharing the bodies here keeps the persisted form
    // — "an IntTag holding the element count, then per-element deserialization" — defined once
    // instead of once per list. See ToggleMachineFXs and ConfigMachineSettings#photonFXs.

    public static IntTag sizeTag(List<MachineFXConfig> fxs) {
        return IntTag.valueOf(fxs.size());
    }

    public static List<MachineFXConfig> listOfSize(IntTag tag) {
        var fxs = new ArrayList<MachineFXConfig>();
        for (int i = 0; i < tag.getAsInt(); i++) {
            fxs.add(new MachineFXConfig());
        }
        return fxs;
    }

    public static Configurator groupConfigurator(Supplier<MachineFXConfig> getter) {
        var group = new ConfiguratorGroup("", false).hideTitle();
        getter.get().buildConfigurator(group);
        return group;
    }

    /**
     * Candidate list for the fx picker, in the same shape as
     * {@link com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleMachineSound}'s sound
     * search.
     *
     * <p>Sourced through {@link PhotonFXBridge}, which answers with an empty list when Photon is
     * absent — so the picker degrades to "nothing found" instead of dragging a missing class into a
     * config screen that has to keep working either way.</p>
     */
    private SearchComponentConfigurator.ISearchConfigurator<ResourceLocation> searchFX() {
        return new SearchComponentConfigurator.ISearchConfigurator<>() {
            @Override
            public void search(String word, IResultHandler<ResourceLocation> searchHandler) {
                var wordLower = word.toLowerCase();
                for (var id : PhotonFXBridge.listFXIds()) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    if (id.toString().toLowerCase().contains(wordLower)) {
                        searchHandler.accept(id);
                    }
                }
            }

            @Override
            public @NotNull ResourceLocation defaultValue() {
                return fxLocation;
            }

            @Override
            public @NotNull String resultText(@NotNull ResourceLocation value) {
                return value.toString();
            }

            // The default candidateUIProvider renders mapping(value); an fx id is not a lang key,
            // so say so rather than letting Component.translatable fall back to printing it.
            @Override
            public @NotNull Component mapping(@NotNull ResourceLocation value) {
                return Component.literal(value.toString());
            }
        };
    }
}
