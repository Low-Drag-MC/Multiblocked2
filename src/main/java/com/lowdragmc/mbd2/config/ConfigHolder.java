package com.lowdragmc.mbd2.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * @author KilaBash
 * @date 2023/2/14
 * @implNote ConfigHolder
 */
public class ConfigHolder {
    public static ConfigHolder INSTANCE;

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ASYNC_RECIPE_SEARCHING = BUILDER
            .comment("Whether search for recipes asynchronously.")
            .define("asyncRecipeSearching", true);

    public static final ModConfigSpec.BooleanValue USE_VBO = BUILDER
            .comment("Whether use vbo for preview page rendering.")
            .define("useVBO", true);

    public static final ModConfigSpec.IntValue MULTIBLOCK_PREVIEW_DURATION = BUILDER
            .comment("Duration of the multiblock in-world preview (s)")
            .defineInRange("multiblockPreviewDuration", 10, 1, 999);

    public static final ModConfigSpec.IntValue MULTIBLOCK_PATTERN_ERROR_DURATION = BUILDER
            .comment("Duration of the multiblock in-world pattern error position (s)")
            .defineInRange("multiblockPatternErrorPosDuration", 10, 1, 999);

    public static final ModConfigSpec SPEC = BUILDER.build();

}
