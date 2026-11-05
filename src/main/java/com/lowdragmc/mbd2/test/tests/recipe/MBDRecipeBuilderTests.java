package com.lowdragmc.mbd2.test.tests.recipe;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeBuilder;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.common.capability.recipe.ItemRecipeCapability;
import com.lowdragmc.mbd2.common.recipe.RainingCondition;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Issue #247: recipes rebuilt with {@link MBDRecipe#toBuilder()}, e.g. by a per-machine recipe modifier. */
@GameTestHolder(MBD2.MOD_ID)
public class MBDRecipeBuilderTests {

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void to_builder_gives_each_caller_its_own_recipe(GameTestHelper h) {
        var recipe = type("builder_isolation").recipeBuilder("isolated")
                .inputItems(Items.IRON_INGOT)
                .outputItems(Items.EMERALD)
                .buildRawRecipe();
        var first = recipe.toBuilder();
        var second = recipe.toBuilder();
        first.outputItems(Items.DIAMOND);

        var modified = first.buildRawRecipe();
        var untouched = second.buildRawRecipe();
        if (outputs(modified) != 2 || outputs(untouched) != 1 || outputs(recipe) != 1) {
            h.fail("expected 2, 1 and 1 outputs, got " + outputs(modified) + ", " + outputs(untouched) + " and " + outputs(recipe));
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void to_builder_does_not_add_the_type_template_again(GameTestHelper h) {
        var type = type("builder_template").prepareBuilder(b -> b.addCondition(new RainingCondition(0, 1)));
        var recipe = type.recipeBuilder("templated").outputItems(Items.EMERALD).buildRawRecipe();
        var rebuilt = recipe.toBuilder().buildRawRecipe();
        if (recipe.conditions.size() != 1 || rebuilt.conditions.size() != 1) {
            h.fail("expected one condition before and after, got " + recipe.conditions.size() + " and " + rebuilt.conditions.size());
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void built_recipes_do_not_share_the_builders_contents(GameTestHelper h) {
        var builder = type("builder_reuse").recipeBuilder("reused").inputItems(Items.IRON_INGOT);
        var first = builder.buildRawRecipe();
        builder.inputItems(Items.GOLD_INGOT).addCondition(new RainingCondition(0, 1));
        builder.data.putBoolean("second", true);
        var second = builder.buildRawRecipe();

        if (inputs(first) != 1 || !first.conditions.isEmpty() || first.data.contains("second")) {
            h.fail("building again changed the first recipe: " + inputs(first) + " inputs, "
                    + first.conditions.size() + " conditions, data " + first.data);
        }
        if (inputs(second) != 2 || second.conditions.size() != 1) {
            h.fail("expected 2 inputs and a condition, got " + inputs(second) + " and " + second.conditions.size());
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void copied_builders_keep_priority_and_visibility(GameTestHelper h) {
        var type = type("builder_copies").prepareBuilder(b -> b.priority(3).isXEIHidden(true).tierChanceBoost(0.5f));
        var fromType = type.recipeBuilder("from_type");
        if (fromType.priority != 3 || !fromType.isXEIHidden || fromType.tierChanceBoost != 0.5f) {
            h.fail("a builder from the type lost its defaults: priority " + fromType.priority
                    + ", hidden " + fromType.isXEIHidden + ", tier boost " + fromType.tierChanceBoost);
        }
        var recipe = fromType.priority(5).buildRawRecipe();
        for (var copy : new MBDRecipe[]{recipe.toBuilder().buildRawRecipe(), new MBDRecipeBuilder(recipe, type).buildRawRecipe()}) {
            if (copy.priority != 5 || !copy.isXEIHidden) {
                h.fail("a copy has priority " + copy.priority + " and hidden " + copy.isXEIHidden + ", expected 5 and true");
            }
        }
        h.succeed();
    }

    private static MBDRecipeType type(String name) {
        return new MBDRecipeType(MBD2.id("test_" + name));
    }

    private static int inputs(MBDRecipe recipe) {
        return recipe.inputs.getOrDefault(ItemRecipeCapability.CAP, List.of()).size();
    }

    private static int outputs(MBDRecipe recipe) {
        return recipe.outputs.getOrDefault(ItemRecipeCapability.CAP, List.of()).size();
    }
}
