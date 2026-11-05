package com.lowdragmc.mbd2.test.tests.trait.astralsorcery;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.integration.astralsorcery.research.AstralResearcherTraitDefinition;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import net.minecraft.resources.ResourceLocation;

/** The machine {@code AstralSorceryTraitUIScenario} opens: all three traits, and a UI that shows them. */
public class AstralTraitUIFixtures implements TestFixtureProvider {
    public static final ResourceLocation SHOWCASE_ID = MBD2.id("test_astral_ui_showcase");

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        TestMachineBuilder.simple(SHOWCASE_ID)
                .withTrait(LumenStorageTraitFixtures.storage(def -> {}))
                .withTrait(StarlightReceiverTraitFixtures.receiver(def -> {}))
                .withTrait(new AstralResearcherTraitDefinition())
                .withTraitUI()
                .register(event);
    }
}
