package com.lowdragmc.mbd2.test.tests.geckolib;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.integration.geckolib.Animation;
import com.lowdragmc.mbd2.integration.geckolib.AnimationInfo;
import com.lowdragmc.mbd2.integration.geckolib.AnimationStage;
import com.lowdragmc.mbd2.integration.geckolib.GeckolibResourcePath;
import com.lowdragmc.lowdraglib2.LDLib2;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.File;
import java.util.List;

@GameTestHolder(MBD2.MOD_ID)
public class GeckolibAnimationConfigTests {

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void animation_config_round_trips_stages(GameTestHelper helper) {
        var animation = new Animation().setName("working");
        animation.getStages().add(new AnimationStage()
                .setAnimationName("machine.work")
                .setLoopType(AnimationStage.LoopType.LOOP));
        animation.getStages().add(new AnimationStage()
                .setWait(true)
                .setAdditionalTicks(5));

        var copy = new Animation();
        copy.deserializeNBT(animation.serializeNBT());

        if (!"working".equals(copy.getName())) {
            helper.fail("Animation name did not round trip");
        }
        if (copy.getStages().size() != 2) {
            helper.fail("Animation stages did not round trip");
        }
        if (!"machine.work".equals(copy.getStages().getFirst().getAnimationName())) {
            helper.fail("Animation stage name did not round trip");
        }
        if (!copy.getStages().getLast().isWait() || copy.getStages().getLast().getAdditionalTicks() != 5) {
            helper.fail("Wait stage did not round trip");
        }

        helper.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void animation_info_source_is_runtime_only_and_reaches_stages(GameTestHelper helper) {
        var animation = new Animation();
        animation.attachAnimationInfoSource(() -> List.of(
                new AnimationInfo("machine.idle", 12.5, "loop", true),
                new AnimationInfo("machine.work", 20, "play_once", false)
        ));

        var stage = new AnimationStage();
        animation.getStages().add(stage);

        if (stage.getAvailableAnimationNames().size() != 2) {
            helper.fail("Animation info source was not attached to stage");
        }

        var copy = new Animation();
        copy.deserializeNBT(animation.serializeNBT());

        if (!copy.getAvailableAnimationNames().isEmpty()) {
            helper.fail("Animation info source should not be serialized");
        }

        copy.attachAnimationInfoSource(() -> List.of(new AnimationInfo("machine.reload", 4, "hold_on_last_frame", false)));
        copy.getStages().add(new AnimationStage());

        if (!List.of("machine.reload").equals(copy.getStages().getFirst().getAvailableAnimationNames())) {
            helper.fail("Animation info source did not refresh after reattach");
        }

        helper.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void asset_file_paths_are_converted_to_resource_locations(GameTestHelper helper) {
        var file = new File(LDLib2.getAssetsDir(), "mbd2/animations/fire_pedestal.animation.json");
        var resourceLocation = GeckolibResourcePath.fromAssetFile(file).orElse(null);

        if (!ResourceLocation.fromNamespaceAndPath("mbd2", "animations/fire_pedestal.animation.json").equals(resourceLocation)) {
            helper.fail("LDLib2 asset file path did not convert to the expected resource location");
        }
        if (!LDLib2.getAssetsDir().toPath().toAbsolutePath().normalize().equals(GeckolibResourcePath.projectAssetsDirectory())) {
            helper.fail("GeckoLib file selector should use LDLib2 assets directory");
        }

        helper.succeed();
    }
}
