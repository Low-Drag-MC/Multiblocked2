package com.lowdragmc.mbd2.integration.astralsorcery;

import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachinePlacedEvent;
import com.lowdragmc.mbd2.integration.astralsorcery.research.AstralResearcherTrait;
import com.lowdragmc.mbd2.integration.astralsorcery.starlight.MachineStarlightReceiverNodeProvider;
import hellfirepvp.astralsorcery.common.constellation.BaseConstellation;
import hellfirepvp.astralsorcery.common.lib.ConstellationsAS;
import hellfirepvp.astralsorcery.common.lib.LumenAS;
import hellfirepvp.astralsorcery.common.lib.RegistriesAS;
import hellfirepvp.astralsorcery.common.lumen.Lumen;
import hellfirepvp.astralsorcery.common.starlight.api.provider.TransmissionNodeProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Registrations and registry lookups shared by the Astral Sorcery integration. Only reached behind
 * {@link MBD2#isAstralSorceryLoaded()}.
 *
 * <p>Astral Sorcery has no API package: everything used here is {@code hellfirepvp.astralsorcery.common}
 * internals, so expect breakage when bumping its version.</p>
 */
public final class AstralSorceryIntegration {
    public static final String MOD_ID = "astralsorcery";
    /** Stands in for "no requirement" in an id field that cannot be empty. */
    public static final ResourceLocation ANY = MBD2.id("any");
    /** Ticks between a trait's checks of its node on Astral Sorcery's networks. */
    public static final int NODE_SYNC_INTERVAL = 20;

    private static final DeferredRegister<TransmissionNodeProvider<?>> TRANSMISSION_NODES =
            DeferredRegister.create(RegistriesAS.KEY_TRANSMISSION_NODES, MBD2.MOD_ID);
    /**
     * Our own node type rather than Astral Sorcery's {@code forwarding_receiver}: that one needs the block entity
     * to implement an Astral Sorcery interface, which MBD2's machine block entity cannot without a hard dependency.
     */
    public static final DeferredHolder<TransmissionNodeProvider<?>, MachineStarlightReceiverNodeProvider> STARLIGHT_RECEIVER_NODE =
            TRANSMISSION_NODES.register("machine_starlight_receiver", MachineStarlightReceiverNodeProvider::new);

    private AstralSorceryIntegration() {}

    public static void init(IEventBus modBus) {
        TRANSMISSION_NODES.register(modBus);
        NeoForge.EVENT_BUS.addListener(AstralSorceryIntegration::onMachinePlaced);
    }

    /** Fake players are nobody's research, and a machine a script already bound in its own placed handler keeps its owner. */
    private static void onMachinePlaced(MachinePlacedEvent event) {
        if (event.player instanceof ServerPlayer player && !(player instanceof FakePlayer)) {
            for (var trait : event.machine.getAdditionalTraits()) {
                if (trait instanceof AstralResearcherTrait researcher && !researcher.getSnapshot().isBound()) {
                    researcher.bindTo(player);
                }
            }
        }
    }

    /** The namespace defaults to Astral Sorcery's. */
    public static ResourceLocation parseId(String id) {
        return id.indexOf(':') < 0 ? ResourceLocation.fromNamespaceAndPath(MOD_ID, id) : ResourceLocation.parse(id);
    }

    // ----- lumen -----

    @Nullable
    public static Lumen lumen(@Nullable ResourceLocation id) {
        if (id == null) return null;
        var lumen = RegistriesAS.REGISTRY_LUMEN.get(id);
        // "none" is the registry's empty marker
        return lumen == null || lumen == LumenAS.NONE.get() ? null : lumen;
    }

    /**
     * Recipe types are built during mod construction, before any registry is filled, so an id can only be
     * rejected once the registry has content. An unknown id that slips through simply matches nothing.
     */
    public static boolean isKnownLumen(ResourceLocation id) {
        return RegistriesAS.REGISTRY_LUMEN.size() == 0 || lumen(id) != null;
    }

    public static ResourceLocation lumenId(Lumen lumen) {
        return Objects.requireNonNull(RegistriesAS.REGISTRY_LUMEN.getKey(lumen), "unregistered lumen");
    }

    /** Sorted, without {@code none}. */
    public static List<ResourceLocation> lumenIds() {
        return RegistriesAS.REGISTRY_LUMEN.keySet().stream()
                .filter(id -> lumen(id) != null)
                .sorted()
                .toList();
    }

    public static Component lumenName(ResourceLocation id) {
        var lumen = lumen(id);
        return lumen == null ? Component.literal(id.toString()) : lumen.getName();
    }

    /** Opaque ARGB. Prismatic lumen cycles its hue; this is its first frame. */
    public static int lumenColor(Lumen lumen) {
        return lumen.getColor(0).getColor() | 0xFF000000;
    }

    public static ResourceLocation defaultLumenId() {
        return LumenAS.AEVITAS.getId();
    }

    // ----- constellations -----

    @Nullable
    public static BaseConstellation constellation(@Nullable ResourceLocation id) {
        return id == null ? null : RegistriesAS.REGISTRY_CONSTELLATIONS.get(id);
    }

    /** @see #isKnownLumen */
    public static boolean isKnownConstellation(ResourceLocation id) {
        return RegistriesAS.REGISTRY_CONSTELLATIONS.size() == 0 || constellation(id) != null;
    }

    public static ResourceLocation parseConstellationId(String id) {
        var parsed = parseId(id);
        if (!isKnownConstellation(parsed)) {
            throw new IllegalStateException("Invalid constellation: " + id);
        }
        return parsed;
    }

    public static ResourceLocation constellationId(BaseConstellation constellation) {
        return Objects.requireNonNull(RegistriesAS.REGISTRY_CONSTELLATIONS.getKey(constellation), "unregistered constellation");
    }

    public static List<ResourceLocation> constellationIds() {
        return RegistriesAS.REGISTRY_CONSTELLATIONS.keySet().stream().sorted().toList();
    }

    public static List<ResourceLocation> constellationIdsOrAny() {
        var ids = new ArrayList<ResourceLocation>();
        ids.add(ANY);
        ids.addAll(constellationIds());
        return ids;
    }

    public static Component constellationName(ResourceLocation id) {
        var constellation = constellation(id);
        return constellation == null ? Component.literal(id.toString()) : constellation.getName();
    }

    public static Component constellationOrAnyName(ResourceLocation id) {
        return ANY.equals(id) ? Component.translatable("mbd2.astralsorcery.any_constellation") : constellationName(id);
    }

    /** Opaque ARGB. */
    public static int constellationColor(BaseConstellation constellation) {
        return constellation.getConstellationColor().getColor() | 0xFF000000;
    }

    public static ResourceLocation defaultConstellationId() {
        return ConstellationsAS.ARMARA.getId();
    }

    // ----- editor -----

    /**
     * A search over registry ids that matches both the id and its display name.
     *
     * @param ids read on every search, so a reload shows up
     */
    public static SearchComponentConfigurator.ISearchConfigurator<ResourceLocation> idSearchConfigurator(
            ResourceLocation defaultValue,
            Supplier<List<ResourceLocation>> ids,
            Function<ResourceLocation, Component> names) {
        return new SearchComponentConfigurator.ISearchConfigurator<>() {
            @Override
            public ResourceLocation defaultValue() {
                return defaultValue;
            }

            @Override
            public String resultText(ResourceLocation value) {
                return value.toString();
            }

            @Override
            public Component mapping(ResourceLocation value) {
                return Component.empty().append(names.apply(value))
                        .append(Component.literal(" (" + value + ")").withColor(0xFF888888));
            }

            @Override
            public void search(String word, IResultHandler<ResourceLocation> find) {
                var wordLower = word.toLowerCase(Locale.ROOT);
                for (var id : ids.get()) {
                    if (Thread.currentThread().isInterrupted()) return;
                    if (id.toString().contains(wordLower)
                            || names.apply(id).getString().toLowerCase(Locale.ROOT).contains(wordLower)) {
                        find.accept(id);
                    }
                }
            }
        };
    }

    public static SearchComponentConfigurator<ResourceLocation> idSearch(String name,
                                                                          Supplier<ResourceLocation> getter,
                                                                          Consumer<ResourceLocation> setter,
                                                                          ResourceLocation defaultValue,
                                                                          Supplier<List<ResourceLocation>> ids,
                                                                          Function<ResourceLocation, Component> names) {
        return new SearchComponentConfigurator<>(name, getter, setter, idSearchConfigurator(defaultValue, ids, names), true);
    }
}
