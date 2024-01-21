package com.lowdragmc.mbd2.config;

import com.lowdragmc.mbd2.MBD2;
import dev.toma.configuration.Configuration;
import dev.toma.configuration.config.Config;
import dev.toma.configuration.config.Configurable;
import dev.toma.configuration.config.format.ConfigFormats;

/**
 * @author KilaBash
 * @date 2023/2/14
 * @implNote ConfigHolder
 */
@Config(id = MBD2.MOD_ID)
public class ConfigHolder {
    public static ConfigHolder INSTANCE;

    public static void init() {
        INSTANCE = Configuration.registerConfig(ConfigHolder.class, ConfigFormats.yaml()).getConfigInstance();
    }

    @Configurable
    @Configurable.Comment({"if the recipe handling is waiting, damping value is the decreased ticks of the current progress.", " Default: 2"})
    public int recipeDampingValue = 2;

    @Configurable
    @Configurable.Comment({"Whether search for recipes asynchronously.", " Default: true"})
    public boolean asyncRecipeSearching = true;
}
