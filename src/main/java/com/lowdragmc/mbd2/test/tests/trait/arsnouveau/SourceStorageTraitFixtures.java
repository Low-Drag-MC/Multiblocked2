package com.lowdragmc.mbd2.test.tests.trait.arsnouveau;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.trait.ToggleAutoIO;
import com.lowdragmc.mbd2.integration.arsnouveau.trait.SourceStorageCapabilityTraitDefinition;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public class SourceStorageTraitFixtures implements TestFixtureProvider {
    /** The plain case: a 10k buffer, both directions, visible to Ars Nouveau's devices. */
    public static final ResourceLocation MACHINE_ID = MBD2.id("test_ars_source_machine");
    /** Same, with {@code expose_to_devices} off — the machine Ars Nouveau should not be able to find. */
    public static final ResourceLocation PRIVATE_MACHINE_ID = MBD2.id("test_ars_source_private_machine");
    /** Pulls source from whatever is on its right (east, facing north). */
    public static final ResourceLocation AUTO_IN_MACHINE_ID = MBD2.id("test_ars_source_auto_in");
    /** Pushes source to whatever is on its right. */
    public static final ResourceLocation AUTO_OUT_MACHINE_ID = MBD2.id("test_ars_source_auto_out");
    /** Output-only sides, to prove {@code capability_io} gates the wrapper rather than only the UI. */
    public static final ResourceLocation EXTRACT_ONLY_MACHINE_ID = MBD2.id("test_ars_source_extract_only");

    public static final int CAPACITY = 10000;

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        TestMachineBuilder.simple(MACHINE_ID)
                .withTrait(storage(def -> {}))
                .register(event);
        TestMachineBuilder.simple(PRIVATE_MACHINE_ID)
                .withTrait(storage(def -> def.setExposeToDevices(false)))
                .register(event);
        TestMachineBuilder.simple(AUTO_IN_MACHINE_ID)
                .withTrait(storage(def -> autoIO(def.getAutoIO(), IO.IN)))
                .register(event);
        TestMachineBuilder.simple(AUTO_OUT_MACHINE_ID)
                .withTrait(storage(def -> autoIO(def.getAutoIO(), IO.OUT)))
                .register(event);
        TestMachineBuilder.simple(EXTRACT_ONLY_MACHINE_ID)
                // setAllIO covers the internal (null-side) IO too, which is the one an Arcane Relay uses
                .withTrait(storage(def -> def.getCapabilityIO().setAllIO(IO.OUT)))
                .register(event);
    }

    private static SourceStorageCapabilityTraitDefinition storage(Consumer<SourceStorageCapabilityTraitDefinition> tweak) {
        var def = new SourceStorageCapabilityTraitDefinition();
        def.setCapacity(CAPACITY);
        def.setMaxReceive(CAPACITY);
        def.setMaxExtract(CAPACITY);
        def.setRecipeHandlerIO(IO.BOTH);
        tweak.accept(def);
        return def;
    }

    private static void autoIO(ToggleAutoIO autoIO, IO io) {
        autoIO.setEnable(true);
        autoIO.setInterval(1);
        // right of a north-facing machine is east — the side the auto-IO tests put the neighbour on
        autoIO.setRightIO(io);
    }
}
