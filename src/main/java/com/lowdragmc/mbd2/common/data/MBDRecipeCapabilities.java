package com.lowdragmc.mbd2.common.data;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.utils.ReflectionUtils;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.capability.recipe.*;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import net.neoforged.fml.ModLoader;

public class MBDRecipeCapabilities {

    public static void init() {
        MBDRegistries.RECIPE_CAPABILITIES.unfreeze();

        ReflectionUtils.findAnnotationStaticField(LDLRegister.class, data -> {
            if (data.containsKey("registry") && data.get("registry") instanceof java.lang.String targetRegistry) {
                if (!targetRegistry.equals("mbd2:recipe_capability")) return false;
            }
            var modId = data.getOrDefault("modID", "").toString();
            if (modId.isEmpty()) return true;
            return LDLib2.isModLoaded(modId);
        }, (field, o) -> {
            if (o instanceof RecipeCapability<?> capability) {
                MBDRegistries.RECIPE_CAPABILITIES.register(capability.name, capability);
            }
        }, () -> {});

        // Register the mod capabilities
//        if (MBD2.isBotaniaLoaded()) {
//            MBDRegistries.RECIPE_CAPABILITIES.register(BotaniaManaRecipeCapability.CAP.name, BotaniaManaRecipeCapability.CAP);
//        }
//        if (MBD2.isGTMLoaded()) {
//            MBDRegistries.RECIPE_CAPABILITIES.register(GTMEnergyRecipeCapability.CAP.name, GTMEnergyRecipeCapability.CAP);
//        }
//        if (MBD2.isMekanismLoaded()) {
//            MBDRegistries.RECIPE_CAPABILITIES.register(MekanismChemicalRecipeCapability.CAP_SLURRY.name, MekanismChemicalRecipeCapability.CAP_SLURRY);
//            MBDRegistries.RECIPE_CAPABILITIES.register(MekanismChemicalRecipeCapability.CAP_GAS.name, MekanismChemicalRecipeCapability.CAP_GAS);
//            MBDRegistries.RECIPE_CAPABILITIES.register(MekanismChemicalRecipeCapability.CAP_INFUSE.name, MekanismChemicalRecipeCapability.CAP_INFUSE);
//            MBDRegistries.RECIPE_CAPABILITIES.register(MekanismChemicalRecipeCapability.CAP_PIGMENT.name, MekanismChemicalRecipeCapability.CAP_PIGMENT);
//            MBDRegistries.RECIPE_CAPABILITIES.register(MekanismHeatRecipeCapability.CAP.name, MekanismHeatRecipeCapability.CAP);
//        }
//        if (MBD2.isCreateLoaded()) {
//             MBDRegistries.RECIPE_CAPABILITIES.register(CreateStressRecipeCapability.CAP.name, CreateStressRecipeCapability.CAP);
//             MBDRegistries.RECIPE_CAPABILITIES.register(CreateRPMRecipeCapability.CAP.name, CreateRPMRecipeCapability.CAP);
//        }
//        if (MBD2.isNaturesAuraLoaded()) {
//            MBDRegistries.RECIPE_CAPABILITIES.register(NaturesAuraRecipeCapability.CAP.name, NaturesAuraRecipeCapability.CAP);
//        }
//        if (MBD2.isPneumaticCraftLoaded()) {
//            MBDRegistries.RECIPE_CAPABILITIES.register(PNCPressureAirRecipeCapability.CAP.name, PNCPressureAirRecipeCapability.CAP);
//            MBDRegistries.RECIPE_CAPABILITIES.register(PNCHeatRecipeCapability.CAP.name, PNCHeatRecipeCapability.CAP);
//        }
//        if (MBD2.isEmbersLoaded()) {
//            MBDRegistries.RECIPE_CAPABILITIES.register(EmbersEmberRecipeCapability.CAP.name, EmbersEmberRecipeCapability.CAP);
//        }
        MBDRegistries.RECIPE_CAPABILITIES.freeze();
    }
}
