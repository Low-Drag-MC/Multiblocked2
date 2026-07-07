package com.lowdragmc.mbd2.test.tests.recipe.create;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.recipe.content.ContentModifier;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleFloat;
import com.lowdragmc.mbd2.integration.create.CreateRotation;
import com.lowdragmc.mbd2.integration.create.SerializerCreateRotation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Round-trip tests for the {@link CreateRotation} content type: codec, streamCodec,
 * NBT helpers, and {@link SerializerCreateRotation#copyWithModifier} behavior.
 */
// No @GameTestHolder: registered via MBDTestRegistry#onRegisterGameTests (mod-load guarded)
// to avoid NeoForge force-loading this soft-dep class when the mod is absent.
public class CreateRotationContentTests {

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void codec_round_trips_stress_mode(GameTestHelper h) {
        var provider = h.getLevel().registryAccess();
        var original = new CreateRotation(256f, CreateRotation.Mode.STRESS, ToggleFloat.of(true, 24f));
        var tag = original.toNbt(provider);
        var roundTrip = CreateRotation.fromNbt(provider, tag);
        if (roundTrip.value != 256f || roundTrip.mode != CreateRotation.Mode.STRESS
                || !roundTrip.torqueOverride.isEnable() || roundTrip.torqueOverride.getValue() != 24f) {
            h.fail("Stress round-trip mismatch: value=" + roundTrip.value + " mode=" + roundTrip.mode
                    + " override=" + roundTrip.torqueOverride.isEnable() + "/" + roundTrip.torqueOverride.getValue());
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void codec_round_trips_rpm_mode_with_disabled_override(GameTestHelper h) {
        var provider = h.getLevel().registryAccess();
        var original = new CreateRotation(64f, CreateRotation.Mode.RPM, ToggleFloat.ofDisabled());
        var tag = original.toNbt(provider);
        var roundTrip = CreateRotation.fromNbt(provider, tag);
        if (roundTrip.value != 64f || roundTrip.mode != CreateRotation.Mode.RPM
                || roundTrip.torqueOverride.isEnable()) {
            h.fail("RPM round-trip mismatch: " + roundTrip.value + "/" + roundTrip.mode
                    + " override.enable=" + roundTrip.torqueOverride.isEnable());
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void copy_with_modifier_scales_value_preserves_others(GameTestHelper h) {
        var original = new CreateRotation(50f, CreateRotation.Mode.RPM, ToggleFloat.of(true, 8f));
        var doubled = SerializerCreateRotation.INSTANCE.copyWithModifier(original, ContentModifier.of(2.0, 0));
        if (doubled.value != 100f) { h.fail("Expected value=100 after x2 modifier, got " + doubled.value); return; }
        if (doubled.mode != CreateRotation.Mode.RPM) { h.fail("Mode lost in copy"); return; }
        if (!doubled.torqueOverride.isEnable() || doubled.torqueOverride.getValue() != 8f) {
            h.fail("Torque override lost in copy");
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void of_coerces_number_to_stress_content(GameTestHelper h) {
        var c = SerializerCreateRotation.INSTANCE.of(128f);
        if (c.value != 128f || c.mode != CreateRotation.Mode.STRESS) {
            h.fail("Number coercion failed: " + c.value + "/" + c.mode);
            return;
        }
        h.succeed();
    }
}
