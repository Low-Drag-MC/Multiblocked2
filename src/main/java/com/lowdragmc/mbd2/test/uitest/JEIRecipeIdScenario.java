package com.lowdragmc.mbd2.test.uitest;

import com.lowdragmc.lowdraglib2.integration.xei.jei.LDLibJEIPlugin;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.integration.jei.MBDRecipeTypeCategory;
import com.lowdragmc.mbd2.test.tests.ui.JEIRecipeIdFixtures;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Issue #246: with advanced tooltips on, JEI names the recipe on its outputs, as it does for every other mod.
 *
 * <p>Run with {@code gradlew runClient -PldTest=mbd2_jei_recipe_id}.</p>
 */
@LDLRegisterClient(name = "mbd2_jei_recipe_id", group = "mbd2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY, modID = "jei")
public class JEIRecipeIdScenario implements UIScenario {
    private final Map<ResourceLocation, IRecipeLayoutDrawable<MBDRecipe>> layouts = new HashMap<>();
    private final Map<ResourceLocation, IRecipeSlotDrawable> outputs = new HashMap<>();
    private boolean advancedTooltips;

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(150).tags("mbd2", "jei", "xei").requiresWorld(true).guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.waitUntil("JEI to list the fixture recipes", ctx -> LDLibJEIPlugin.jeiRuntime != null && layOut())
                .step("turn advanced tooltips on", ctx -> {
                    advancedTooltips = ctx.mc().options.advancedItemTooltips;
                    ctx.mc().options.advancedItemTooltips = true;
                })
                .check("a recipe's output names the recipe",
                        ctx -> namedAs(JEIRecipeIdFixtures.PLAIN_ID, JEIRecipeIdFixtures.PLAIN_ID))
                .check("a copied recipe file is named by its own id, not the one copied inside it",
                        ctx -> namedAs(JEIRecipeIdFixtures.COPIED_ID, JEIRecipeIdFixtures.COPIED_ID)
                                && !namedAs(JEIRecipeIdFixtures.COPIED_ID, JEIRecipeIdFixtures.PLAIN_ID))
                .openScreen("show the copied recipe with its output hovered", ctx -> new LayoutScreen(JEIRecipeIdFixtures.COPIED_ID))
                .frames(4)
                .screenshot("recipe_id_tooltip")
                .teardown("restore", ctx -> {
                    ctx.mc().options.advancedItemTooltips = advancedTooltips;
                    ctx.mc().setScreen(null);
                });
    }

    private boolean namedAs(ResourceLocation recipe, ResourceLocation id) {
        @SuppressWarnings("removal") List<Component> tooltip = outputs.get(recipe).getTooltip();
        return tooltip.stream().anyMatch(line -> line.getString().contains(id.toString()));
    }

    private boolean layOut() {
        var type = MBDRegistries.RECIPE_TYPES.get(JEIRecipeIdFixtures.RECIPE_TYPE_ID);
        var connection = Minecraft.getInstance().getConnection();
        if (type == null || connection == null) return false;
        var holderIds = new IdentityHashMap<MBDRecipe, ResourceLocation>();
        for (var holder : connection.getRecipeManager().getAllRecipesFor(type)) {
            holderIds.put(holder.value(), holder.id());
        }
        var runtime = LDLibJEIPlugin.jeiRuntime;
        var recipes = runtime.getRecipeManager();
        var jeiType = MBDRecipeTypeCategory.TYPES.apply(type);
        var category = recipes.getRecipeCategory(jeiType);
        var focus = runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup();
        recipes.createRecipeLookup(jeiType).get().forEach(recipe -> {
            var id = holderIds.get(recipe);
            if (id == null) return;
            recipes.createRecipeLayoutDrawable(category, recipe, focus).ifPresent(layout -> {
                for (var slot : layout.getRecipeSlotsView().getSlotViews(RecipeIngredientRole.OUTPUT)) {
                    if (slot instanceof IRecipeSlotDrawable drawable && slot.getDisplayedIngredient().isPresent()) {
                        layouts.put(id, layout);
                        outputs.put(id, drawable);
                        return;
                    }
                }
            });
        });
        return outputs.containsKey(JEIRecipeIdFixtures.PLAIN_ID) && outputs.containsKey(JEIRecipeIdFixtures.COPIED_ID);
    }

    private class LayoutScreen extends Screen {
        private final ResourceLocation recipe;

        LayoutScreen(ResourceLocation recipe) {
            super(Component.empty());
            this.recipe = recipe;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.render(graphics, mouseX, mouseY, partialTick);
            var layout = layouts.get(recipe);
            var output = outputs.get(recipe);
            var x = 20;
            var y = 20;
            layout.setPosition(x, y);
            layout.drawRecipe(graphics, -1, -1);
            var slot = output.getAreaIncludingBackground();
            output.drawTooltip(graphics, x + slot.getX() + slot.getWidth() / 2, y + slot.getY() + slot.getHeight() / 2);
        }
    }
}
