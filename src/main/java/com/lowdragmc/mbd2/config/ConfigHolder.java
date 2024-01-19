package com.lowdragmc.mbd2.config;

import com.lowdragmc.mbd2.Multiblocked2;
import dev.toma.configuration.Configuration;
import dev.toma.configuration.config.Config;
import dev.toma.configuration.config.format.ConfigFormats;

/**
 * @author KilaBash
 * @date 2023/2/14
 * @implNote ConfigHolder
 */
@Config(id = Multiblocked2.MOD_ID)
public class ConfigHolder {
    public static ConfigHolder INSTANCE;

    public static void init() {
        INSTANCE = Configuration.registerConfig(ConfigHolder.class, ConfigFormats.yaml()).getConfigInstance();
    }
}
