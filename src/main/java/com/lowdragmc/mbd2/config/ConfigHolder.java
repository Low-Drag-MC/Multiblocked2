package com.lowdragmc.mbd2.config;

import com.lowdragmc.mbd2.MBD2;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * @author KilaBash
 * @date 2023/2/14
 * @implNote ConfigHolder
 */
@Mod.EventBusSubscriber(modid = MBD2.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ConfigHolder {
    public static ConfigHolder INSTANCE;

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue ASYNC_RECIPE_SEARCHING = BUILDER
            .comment("Whether search for recipes asynchronously.")
            .define("asyncRecipeSearching", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean asyncRecipeSearching;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        asyncRecipeSearching = ASYNC_RECIPE_SEARCHING.get();
    }
}
