package com.lowdragmc.mbd2.common.data;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.capability.recipe.EntityRecipeCapability;
import com.lowdragmc.mbd2.integration.botania.BotaniaManaRecipeCapability;
import com.lowdragmc.mbd2.common.capability.recipe.FluidRecipeCapability;
import com.lowdragmc.mbd2.common.capability.recipe.ForgeEnergyRecipeCapability;
import com.lowdragmc.mbd2.common.capability.recipe.ItemRecipeCapability;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.integration.create.CreateStressRecipeCapability;
import com.lowdragmc.mbd2.integration.embers.EmbersEmberRecipeCapability;
import com.lowdragmc.mbd2.integration.gtm.GTMEnergyRecipeCapability;
import com.lowdragmc.mbd2.integration.mekanism.MekanismChemicalRecipeCapability;
import com.lowdragmc.mbd2.integration.mekanism.MekanismHeatRecipeCapability;
import com.lowdragmc.mbd2.integration.naturesaura.NaturesAuraRecipeCapability;
import com.lowdragmc.mbd2.integration.pneumaticcraft.PNCPressureAirRecipeCapability;
import net.minecraftforge.fml.ModLoader;

public class MBDRecipeCapabilities {

    public static void init() {
        MBDRegistries.RECIPE_CAPABILITIES.unfreeze();
        MBDRegistries.RECIPE_CAPABILITIES.register(ItemRecipeCapability.CAP.name, ItemRecipeCapability.CAP);
        MBDRegistries.RECIPE_CAPABILITIES.register(FluidRecipeCapability.CAP.name, FluidRecipeCapability.CAP);
        MBDRegistries.RECIPE_CAPABILITIES.register(EntityRecipeCapability.CAP.name, EntityRecipeCapability.CAP);
        MBDRegistries.RECIPE_CAPABILITIES.register(ForgeEnergyRecipeCapability.CAP.name, ForgeEnergyRecipeCapability.CAP);
        // Register the mod capabilities
        if (MBD2.isBotaniaLoaded()) {
            MBDRegistries.RECIPE_CAPABILITIES.register(BotaniaManaRecipeCapability.CAP.name, BotaniaManaRecipeCapability.CAP);
        }
        if (MBD2.isGTMLoaded()) {
            MBDRegistries.RECIPE_CAPABILITIES.register(GTMEnergyRecipeCapability.CAP.name, GTMEnergyRecipeCapability.CAP);
        }
        if (MBD2.isMekanismLoaded()) {
            MBDRegistries.RECIPE_CAPABILITIES.register(MekanismChemicalRecipeCapability.CAP_SLURRY.name, MekanismChemicalRecipeCapability.CAP_SLURRY);
            MBDRegistries.RECIPE_CAPABILITIES.register(MekanismChemicalRecipeCapability.CAP_GAS.name, MekanismChemicalRecipeCapability.CAP_GAS);
            MBDRegistries.RECIPE_CAPABILITIES.register(MekanismChemicalRecipeCapability.CAP_INFUSE.name, MekanismChemicalRecipeCapability.CAP_INFUSE);
            MBDRegistries.RECIPE_CAPABILITIES.register(MekanismChemicalRecipeCapability.CAP_PIGMENT.name, MekanismChemicalRecipeCapability.CAP_PIGMENT);
            MBDRegistries.RECIPE_CAPABILITIES.register(MekanismHeatRecipeCapability.CAP.name, MekanismHeatRecipeCapability.CAP);
        }
        if (MBD2.isCreateLoaded()) {
             MBDRegistries.RECIPE_CAPABILITIES.register(CreateStressRecipeCapability.CAP.name, CreateStressRecipeCapability.CAP);
        }
        if (MBD2.isNaturesAuraLoaded()) {
            MBDRegistries.RECIPE_CAPABILITIES.register(NaturesAuraRecipeCapability.CAP.name, NaturesAuraRecipeCapability.CAP);
        }
        if (MBD2.isPneumaticCraftLoaded()) {
            MBDRegistries.RECIPE_CAPABILITIES.register(PNCPressureAirRecipeCapability.CAP.name, PNCPressureAirRecipeCapability.CAP);
        }
        if (MBD2.isEmbersLoaded()) {
            MBDRegistries.RECIPE_CAPABILITIES.register(EmbersEmberRecipeCapability.CAP.name, EmbersEmberRecipeCapability.CAP);
        }
        ModLoader.get().postEvent(new MBDRegistryEvent.RecipeCapability());
        MBDRegistries.RECIPE_CAPABILITIES.freeze();
    }
}
