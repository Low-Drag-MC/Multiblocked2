package com.lowdragmc.mbd2.test.tests.trait.astralsorcery;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.integration.astralsorcery.starlight.StarlightReceiverTraitDefinition;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import hellfirepvp.astralsorcery.common.lib.ConstellationsAS;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public class StarlightReceiverTraitFixtures implements TestFixtureProvider {
    /** 1000 per constellation, any constellation. */
    public static final ResourceLocation MACHINE_ID = MBD2.id("test_astral_starlight_receiver");
    /** Keeps only aevitas starlight. */
    public static final ResourceLocation FILTERED_MACHINE_ID = MBD2.id("test_astral_starlight_filtered");

    public static final float CAPACITY = 1000;

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        TestMachineBuilder.simple(MACHINE_ID)
                .withTrait(receiver(def -> {}))
                .register(event);
        TestMachineBuilder.simple(FILTERED_MACHINE_ID)
                .withTrait(receiver(def -> def.getConstellations().add(ConstellationsAS.AEVITAS.getId())))
                .register(event);
    }

    public static StarlightReceiverTraitDefinition receiver(Consumer<StarlightReceiverTraitDefinition> tweak) {
        var def = new StarlightReceiverTraitDefinition();
        def.setCapacity(CAPACITY);
        tweak.accept(def);
        return def;
    }
}
