package com.lowdragmc.mbd2.common.data;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.runtime.AnnotationDetector;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.trait.TraitDefinition;
import com.lowdragmc.mbd2.common.trait.fluid.FluidTankCapabilityTraitDefinition;
import com.lowdragmc.mbd2.common.trait.forgeenergy.ForgeEnergyCapabilityTraitDefinition;
import com.lowdragmc.mbd2.common.trait.item.ItemSlotCapabilityTraitDefinition;
import com.lowdragmc.mbd2.integration.botania.trait.BotaniaManaCapabilityTraitDefinition;
import com.lowdragmc.mbd2.integration.gtm.trait.GTMEnergyCapabilityTraitDefinition;
import com.lowdragmc.mbd2.integration.mekanism.trait.chemical.ChemicalTankCapabilityTraitDefinition;
import com.lowdragmc.mbd2.integration.mekanism.trait.heat.MekHeatCapabilityTraitDefinition;
import net.minecraftforge.fml.ModLoader;

public class MBDTraitDefinitions {

    public static void init() {
        MBDRegistries.TRAIT_DEFINITIONS.unfreeze();
        register(ItemSlotCapabilityTraitDefinition.class);
        register(FluidTankCapabilityTraitDefinition.class);
        register(ForgeEnergyCapabilityTraitDefinition.class);
        // Register the mod capabilities
        if (MBD2.isBotaniaLoaded()) {
            register(BotaniaManaCapabilityTraitDefinition.class);
        }
        if (MBD2.isGTMLoaded()) {
            register(GTMEnergyCapabilityTraitDefinition.class);
        }
        if (MBD2.isMekanismLoaded()) {
            register(ChemicalTankCapabilityTraitDefinition.Gas.class);
            register(ChemicalTankCapabilityTraitDefinition.Infuse.class);
            register(ChemicalTankCapabilityTraitDefinition.Pigment.class);
            register(ChemicalTankCapabilityTraitDefinition.Slurry.class);
            register(MekHeatCapabilityTraitDefinition.class);
        }
        ModLoader.get().postEvent(new MBDRegistryEvent.Trait());
        MBDRegistries.TRAIT_DEFINITIONS.freeze();
    }

    public static void register(Class<? extends TraitDefinition> clazz) {
        if (clazz.isAnnotationPresent(LDLRegister.class)) {
            var annotation = clazz.getAnnotation(LDLRegister.class);
            if (!annotation.modID().isEmpty()) {
                if (!LDLib.isModLoaded(annotation.modID())) {
                    MBD2.LOGGER.info("Skipping registration of trait definition: " + clazz.getName() + " - Mod not loaded: " + annotation.modID());
                    return;
                }
            }
            try {
                var constructor = clazz.getConstructor();
                MBDRegistries.TRAIT_DEFINITIONS.register(clazz.getAnnotation(LDLRegister.class).name(),
                        new AnnotationDetector.Wrapper<>(annotation, clazz, () -> {
                            try {
                                return constructor.newInstance();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }));
            } catch (NoSuchMethodException e) {
                MBD2.LOGGER.error("Failed to register trait definition: " + clazz.getName() + " - No default constructor found");
            }
        } else {
            MBD2.LOGGER.error("Failed to register trait definition: " + clazz.getName() + " - No annotation found");
        }
    }


}
