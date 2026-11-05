package com.lowdragmc.mbd2.test.tests.trait.astralsorcery;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.integration.astralsorcery.research.AstralResearcherTraitDefinition;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import hellfirepvp.astralsorcery.common.lib.ConstellationsAS;
import hellfirepvp.astralsorcery.common.lib.LumenAS;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** The machine {@code AstralSorceryTraitUIScenario} opens; the filters make it show an empty bar beside full ones. */
public class AstralTraitUIFixtures implements TestFixtureProvider {
    public static final ResourceLocation SHOWCASE_ID = MBD2.id("test_astral_ui_showcase");

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        TestMachineBuilder.simple(SHOWCASE_ID)
                .withTrait(LumenStorageTraitFixtures.storage(def -> def.getLumenTypes()
                        .addAll(List.of(LumenAS.AEVITAS.getId(), LumenAS.ARMARA.getId(), LumenAS.VICIO.getId()))))
                .withTrait(StarlightReceiverTraitFixtures.receiver(def -> def.getConstellations()
                        .addAll(List.of(ConstellationsAS.ARMARA.getId(), ConstellationsAS.AEVITAS.getId()))))
                .withTrait(new AstralResearcherTraitDefinition())
                .withTraitUI()
                .register(event);
    }
}
