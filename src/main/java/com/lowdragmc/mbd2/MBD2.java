package com.lowdragmc.mbd2;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.mbd2.client.ClientProxy;
import com.lowdragmc.mbd2.common.CommonProxy;
import com.lowdragmc.lowdraglib2.Platform;
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
            new CommonProxy(eventBus, modContainer);
            new ClientProxy(eventBus);
        } else {
            new CommonProxy(eventBus, modContainer);
        }
    }

    private static File createDir() {
        var location = new File(LDLib2.getAssetsDir(), MOD_ID);
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
        return LDLib2.isModLoaded("geckolib");
    }

    public static boolean isBotaniaLoaded() {
        return LDLib2.isModLoaded("botania");
    }

    public static boolean isNaturesAuraLoaded() {
        return LDLib2.isModLoaded("naturesaura");
    }

    public static boolean isPneumaticCraftLoaded() {
        return LDLib2.isModLoaded("pneumaticcraft");
    }

    public static boolean isEmbersLoaded() {
        return LDLib2.isModLoaded("embers");
    }

    public static boolean isGTMLoaded() {
        return LDLib2.isModLoaded("gtceu");
    }

    public static boolean isMekanismLoaded() {
        return LDLib2.isModLoaded("mekanism");
    }

    public static boolean isCreateLoaded() {
        return LDLib2.isModLoaded("create");
    }

    public static boolean isPhotonLoaded() {
        return LDLib2.isModLoaded("photon");
    }

    public static boolean isKubeJSLoaded() {
        return LDLib2.isModLoaded("kubejs");
    }

    public static boolean isAE2Loaded() {
        return LDLib2.isModLoaded("ae2");
    }

    public static boolean isEmbeddiumLoaded() {
        return LDLib2.isModLoaded("embeddium");
    }
}
