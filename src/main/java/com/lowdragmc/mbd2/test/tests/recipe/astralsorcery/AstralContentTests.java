package com.lowdragmc.mbd2.test.tests.recipe.astralsorcery;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeSerializer;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.api.recipe.content.ContentModifier;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.integration.astralsorcery.AstralSorceryIntegration;
import com.lowdragmc.mbd2.integration.astralsorcery.lumen.AstralLumenRecipeCapability;
import com.lowdragmc.mbd2.integration.astralsorcery.lumen.LumenAmount;
import com.lowdragmc.mbd2.integration.astralsorcery.lumen.SerializerLumenAmount;
import com.lowdragmc.mbd2.integration.astralsorcery.research.AstralResearchCondition;
import com.lowdragmc.mbd2.integration.astralsorcery.sky.AstralCelestialEventCondition;
import com.lowdragmc.mbd2.integration.astralsorcery.sky.AstralConstellationCondition;
import com.lowdragmc.mbd2.integration.astralsorcery.starlight.AstralStarlightRecipeCapability;
import com.lowdragmc.mbd2.integration.astralsorcery.starlight.SerializerStarlightAmount;
import com.lowdragmc.mbd2.integration.astralsorcery.starlight.StarlightAmount;
import com.mojang.serialization.JsonOps;
import hellfirepvp.astralsorcery.common.lib.ConstellationsAS;
import hellfirepvp.astralsorcery.common.lib.LumenAS;
import hellfirepvp.astralsorcery.common.lumen.LumenStack;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

// No @GameTestHolder: registered via MBDTestRegistry#onRegisterGameTests (mod-load guarded)
// to avoid NeoForge force-loading this soft-dep class when the mod is absent.
/** The content and condition paths every recipe from KubeJS, a datapack or an editor project goes through. */
public class AstralContentTests {
    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void lumen_parses_what_scripts_write(GameTestHelper h) {
        var lumen = SerializerLumenAmount.INSTANCE;
        var aevitas = LumenAS.AEVITAS.getId();
        var expectations = Map.<Object, LumenAmount>of(
                "250x astralsorcery:armara", new LumenAmount(LumenAS.ARMARA.getId(), 250),
                " vicio ", new LumenAmount(LumenAS.VICIO.getId(), SerializerLumenAmount.DEFAULT_AMOUNT),
                LumenAS.AEVITAS.get(), new LumenAmount(aevitas, SerializerLumenAmount.DEFAULT_AMOUNT),
                LumenAS.AEVITAS.get().stack(42), new LumenAmount(aevitas, 42),
                aevitas, new LumenAmount(aevitas, SerializerLumenAmount.DEFAULT_AMOUNT));
        for (var entry : expectations.entrySet()) {
            var parsed = lumen.of(entry.getKey());
            if (!parsed.equals(entry.getValue())) {
                h.fail(entry.getKey() + " parsed as " + parsed + ", expected " + entry.getValue());
                return;
            }
        }
        for (var invalid : List.<Object>of("250x astralsorcery:not_a_lumen", "astralsorcery:none", "0x aevitas",
                "-5x aevitas", "not an id", LumenStack.EMPTY, 42)) {
            if (!rejects(lumen::of, invalid)) {
                h.fail("lumen accepted " + invalid);
                return;
            }
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void starlight_parses_what_scripts_write(GameTestHelper h) {
        var starlight = SerializerStarlightAmount.INSTANCE;
        var armara = ConstellationsAS.ARMARA.getId();
        var expectations = Map.<Object, StarlightAmount>of(
                "100", StarlightAmount.any(100),
                12.5f, StarlightAmount.any(12.5f),
                "40x astralsorcery:armara", StarlightAmount.of(armara, 40),
                "armara", StarlightAmount.of(armara, SerializerStarlightAmount.DEFAULT_AMOUNT),
                ConstellationsAS.VICIO.get(), StarlightAmount.of(ConstellationsAS.VICIO.getId(), SerializerStarlightAmount.DEFAULT_AMOUNT));
        for (var entry : expectations.entrySet()) {
            var parsed = starlight.of(entry.getKey());
            if (!parsed.equals(entry.getValue())) {
                h.fail(entry.getKey() + " parsed as " + parsed + ", expected " + entry.getValue());
                return;
            }
        }
        for (var invalid : List.<Object>of("NaN", "Infinity", "0", "-1", "0x armara", "40x armra", Float.NaN, -3, new Object())) {
            if (!rejects(starlight::of, invalid)) {
                h.fail("starlight accepted " + invalid);
                return;
            }
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void modifiers_never_make_a_cost_negative(GameTestHelper h) {
        var discount = ContentModifier.addition(-1000);
        var lumen = SerializerLumenAmount.INSTANCE.copyWithModifier(new LumenAmount(LumenAS.AEVITAS.getId(), 500), discount);
        var starlight = SerializerStarlightAmount.INSTANCE.copyWithModifier(StarlightAmount.any(100), discount);
        var doubled = SerializerStarlightAmount.INSTANCE.copyWithModifier(StarlightAmount.any(12.5f), ContentModifier.multiplier(2));
        if (lumen.amount() != 0 || starlight.amount() != 0 || doubled.amount() != 25) {
            h.fail("expected 0 lumen, 0 and 25 starlight, got " + lumen + ", " + starlight + ", " + doubled);
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void recipe_survives_the_json_and_network_codecs(GameTestHelper h) {
        var type = Objects.requireNonNull(MBDRegistries.RECIPE_TYPES.get(AstralRecipeCapabilityFixtures.LUMEN_CONSUME_RECIPES));
        var armara = ConstellationsAS.ARMARA.getId();
        var recipe = type.recipeBuilder(MBD2.id("astral_codec_round_trip"))
                .inputs(AstralLumenRecipeCapability.CAP, "250x aevitas")
                .outputs(AstralLumenRecipeCapability.CAP, LumenAS.ARMARA.get())
                .inputs(AstralStarlightRecipeCapability.CAP, 12.5f, "40x astralsorcery:armara")
                .addCondition(new AstralConstellationCondition(ConstellationsAS.VICIO.getId(), 0.25f))
                .addCondition(new AstralCelestialEventCondition(AstralCelestialEventCondition.Event.LUNAR_ECLIPSE, true))
                .addCondition(new AstralResearchCondition(AstralResearchCondition.Tier.RESONANCE, armara, AstralSorceryIntegration.ANY)
                        .setReverse(true))
                .buildRawRecipe();

        var registries = h.getLevel().registryAccess();
        var codec = MBDRecipeSerializer.CODEC.codec();
        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        var fromJson = codec.parse(ops, codec.encodeStart(ops, recipe).getOrThrow()).getOrThrow();
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        MBDRecipe fromNetwork;
        try {
            MBDRecipeSerializer.STREAM_CODEC.encode(buf, recipe);
            fromNetwork = MBDRecipeSerializer.STREAM_CODEC.decode(buf);
        } finally {
            buf.release();
        }

        var expected = describe(recipe);
        if (expected.conditions.size() != 3) {
            h.fail("the recipe should carry 3 conditions, has " + expected.conditions);
            return;
        }
        for (var decoded : List.of(describe(fromJson), describe(fromNetwork))) {
            if (!decoded.equals(expected)) {
                h.fail("decoded as " + decoded + ", expected " + expected);
                return;
            }
        }
        h.succeed();
    }

    private record Described(List<Object> lumenIn, List<Object> lumenOut, List<Object> starlightIn, List<String> conditions) {}

    private static Described describe(MBDRecipe recipe) {
        return new Described(
                contents(recipe.inputs, AstralLumenRecipeCapability.CAP),
                contents(recipe.outputs, AstralLumenRecipeCapability.CAP),
                contents(recipe.inputs, AstralStarlightRecipeCapability.CAP),
                recipe.conditions.stream().map(AstralContentTests::describeCondition).toList());
    }

    private static List<Object> contents(Map<RecipeCapability<?>, List<Content>> contents, RecipeCapability<?> capability) {
        return contents.getOrDefault(capability, List.of()).stream().map(content -> content.content).toList();
    }

    private static String describeCondition(RecipeCondition condition) {
        var fields = switch (condition) {
            case AstralConstellationCondition c -> c.getConstellation() + " >= " + c.getMinDistribution();
            case AstralCelestialEventCondition c -> c.getEvent() + (c.isWholeDay() ? " whole day" : "");
            case AstralResearchCondition c -> c.getMinTier() + " " + c.getAttunement() + " " + c.getDiscovered();
            default -> "?";
        };
        return condition.getType() + "(" + fields + (condition.isReverse() ? ", reversed" : "") + ")";
    }

    private static boolean rejects(Function<Object, ?> parser, Object input) {
        try {
            parser.apply(input);
            return false;
        } catch (IllegalStateException e) {
            return true;
        }
    }
}
