package com.lowdragmc.mbd2.test.tests.trait.astralsorcery;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.integration.astralsorcery.lumen.LumenStorageCapabilityTraitDefinition;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import hellfirepvp.astralsorcery.common.lib.LumenAS;
import hellfirepvp.astralsorcery.common.lumen.Lumen;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

/** Each network test uses a lumen type no other fixture holds: the network is world data every test shares. */
public class LumenStorageTraitFixtures implements TestFixtureProvider {
    /** A 4000-per-type buffer, both directions, kept off the lumen network. */
    public static final ResourceLocation MACHINE_ID = MBD2.id("test_astral_lumen_storage");
    /** Same, but only accepts aevitas. */
    public static final ResourceLocation FILTERED_MACHINE_ID = MBD2.id("test_astral_lumen_filtered");
    /** Output-only on every side, internal included. */
    public static final ResourceLocation EXTRACT_ONLY_MACHINE_ID = MBD2.id("test_astral_lumen_extract_only");
    /** On the network as a source. */
    public static final ResourceLocation SOURCE_MACHINE_ID = MBD2.id("test_astral_lumen_source");
    /** Off the network, pulling {@link #SINK_LUMEN} every tick, {@link #SINK_RATE} at a time. */
    public static final ResourceLocation SINK_MACHINE_ID = MBD2.id("test_astral_lumen_sink");
    /** On the network as a source and drawing {@link #DRAWN_BY_SOURCE_LUMEN}, which no other fixture offers. */
    public static final ResourceLocation DRAWING_SOURCE_MACHINE_ID = MBD2.id("test_astral_lumen_drawing_source");

    public static final int CAPACITY = 4000;
    public static final int SINK_RATE = 100;
    public static final LumenAS.DeferredLumen<Lumen> SINK_LUMEN = LumenAS.VICIO;
    public static final LumenAS.DeferredLumen<Lumen> PROVIDED_LUMEN = LumenAS.DISCIDIA;
    public static final LumenAS.DeferredLumen<Lumen> DRAWN_BY_SOURCE_LUMEN = LumenAS.EVORSIO;

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        TestMachineBuilder.simple(MACHINE_ID)
                .withTrait(storage(def -> {}))
                .register(event);
        TestMachineBuilder.simple(FILTERED_MACHINE_ID)
                .withTrait(storage(def -> def.getLumenTypes().add(LumenAS.AEVITAS.getId())))
                .register(event);
        TestMachineBuilder.simple(EXTRACT_ONLY_MACHINE_ID)
                .withTrait(storage(def -> def.getCapabilityIO().setAllIO(IO.OUT)))
                .register(event);
        TestMachineBuilder.simple(SOURCE_MACHINE_ID)
                .withTrait(storage(def -> def.setProvideToNetwork(true)))
                .register(event);
        TestMachineBuilder.simple(SINK_MACHINE_ID)
                .withTrait(storage(def -> {
                    def.setDrawFromNetwork(true);
                    def.setDrawInterval(1);
                    def.setMaxReceive(SINK_RATE);
                    def.getLumenTypes().add(SINK_LUMEN.getId());
                }))
                .register(event);
        TestMachineBuilder.simple(DRAWING_SOURCE_MACHINE_ID)
                .withTrait(storage(def -> {
                    def.setProvideToNetwork(true);
                    def.setDrawFromNetwork(true);
                    def.getLumenTypes().add(DRAWN_BY_SOURCE_LUMEN.getId());
                }))
                .register(event);
    }

    /** Off the network unless {@code tweak} says otherwise. */
    public static LumenStorageCapabilityTraitDefinition storage(Consumer<LumenStorageCapabilityTraitDefinition> tweak) {
        var def = new LumenStorageCapabilityTraitDefinition();
        def.setCapacity(CAPACITY);
        def.setMaxReceive(CAPACITY);
        def.setMaxExtract(CAPACITY);
        def.setRecipeHandlerIO(IO.BOTH);
        def.setProvideToNetwork(false);
        def.setTransferParticles(false);
        tweak.accept(def);
        return def;
    }
}
