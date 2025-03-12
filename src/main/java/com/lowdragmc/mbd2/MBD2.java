package com.lowdragmc.mbd2;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.mbd2.client.ClientProxy;
import com.lowdragmc.mbd2.common.CommonProxy;
import com.lowdragmc.lowdraglib.Platform;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Random;

@Mod(MBD2.MOD_ID)
public class MBD2 {
    public static final String MOD_ID = "mbd2";
    public static final String NAME = "Multiblocked2";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);
    public static final Random RND = new Random();
    @Getter(lazy = true)
    private static final File location = createDir();

    public MBD2(IEventBus eventBus, ModContainer modContainer) {
        MBD2.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            new ClientProxy(eventBus);
        } else {
            new CommonProxy(eventBus);
        }
    }

    private static File createDir() {
        var location = new File(LDLib.getLDLibDir(), "assets/" + MOD_ID);
        if (location.mkdirs()) {
            LOGGER.info("create mbd2 resources folder");
        }
        return location;
    }

    public static void init() {
        LOGGER.info("{} is initializing on platform: {}", NAME, Platform.platformName());
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static boolean isGeckolibLoaded() {
        return LDLib.isModLoaded("geckolib");
    }

    public static boolean isBotaniaLoaded() {
        return LDLib.isModLoaded("botania");
    }

    public static boolean isNaturesAuraLoaded() {
        return LDLib.isModLoaded("naturesaura");
    }

    public static boolean isPneumaticCraftLoaded() {
        return LDLib.isModLoaded("pneumaticcraft");
    }

    public static boolean isEmbersLoaded() {
        return LDLib.isModLoaded("embers");
    }

    public static boolean isGTMLoaded() {
        return LDLib.isModLoaded("gtceu");
    }

    public static boolean isMekanismLoaded() {
        return LDLib.isModLoaded("mekanism");
    }

    public static boolean isCreateLoaded() {
        return LDLib.isModLoaded("create");
    }

    public static boolean isPhotonLoaded() {
        return LDLib.isModLoaded("photon");
    }

    public static boolean isKubeJSLoaded() {
        return LDLib.isModLoaded("kubejs");
    }
}
