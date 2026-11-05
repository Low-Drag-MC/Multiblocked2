package com.lowdragmc.mbd2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Tab;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.gui.editor.MBDEditor;
import com.lowdragmc.mbd2.common.gui.editor.MachineProject;
import com.lowdragmc.mbd2.common.gui.editor.RecipeTypeProject;
import com.lowdragmc.mbd2.common.gui.editor.recipe.RecipeTypeUIView;
import com.lowdragmc.mbd2.common.trait.TraitDefinition;
import com.lowdragmc.mbd2.integration.astralsorcery.AstralSorceryIntegration;
import com.lowdragmc.mbd2.integration.astralsorcery.lumen.AstralLumenRecipeCapability;
import com.lowdragmc.mbd2.integration.astralsorcery.lumen.LumenAmount;
import com.lowdragmc.mbd2.integration.astralsorcery.lumen.LumenStorageCapabilityTraitDefinition;
import com.lowdragmc.mbd2.integration.astralsorcery.research.AstralResearchCondition;
import com.lowdragmc.mbd2.integration.astralsorcery.research.AstralResearcherTraitDefinition;
import com.lowdragmc.mbd2.integration.astralsorcery.sky.AstralCelestialEventCondition;
import com.lowdragmc.mbd2.integration.astralsorcery.sky.AstralConstellationCondition;
import com.lowdragmc.mbd2.integration.astralsorcery.starlight.AstralStarlightRecipeCapability;
import com.lowdragmc.mbd2.integration.astralsorcery.starlight.StarlightAmount;
import com.lowdragmc.mbd2.integration.astralsorcery.starlight.StarlightReceiverTraitDefinition;
import hellfirepvp.astralsorcery.common.lib.ConstellationsAS;
import hellfirepvp.astralsorcery.common.lib.LumenAS;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.Items;

/**
 * The Astral Sorcery traits, recipe contents and conditions in the real editor, captured step by step.
 * Run with {@code gradlew runClient -PldTest=mbd2_astral_editor}.
 */
@LDLRegisterClient(name = "mbd2_astral_editor", group = "mbd2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY, modID = AstralSorceryIntegration.MOD_ID)
public class AstralSorceryEditorScenario implements UIScenario {
    private static final String MACHINE = "machine_project";
    private static final String RECIPES = "recipe_project";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(200).tags("mbd2", "astralsorcery", "editor", "visual").requiresWorld(true).guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("machine editor", AstralSorceryEditorScenario::machineEditor)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .waitUntil("the trait view is ready", ctx -> {
                    var view = ctx.<MachineProject>get(MACHINE).getMachineTraitView();
                    return view != null && view.getViewContainer() != null;
                })
                .step("select the trait view", ctx -> {
                    var view = ctx.<MachineProject>get(MACHINE).getMachineTraitView();
                    view.getViewContainer().selectView(view);
                })
                .settleMs(300)
                .step("inspect lumen storage", ctx -> inspectTrait(ctx, LumenStorageCapabilityTraitDefinition.class))
                .settleMs(300)
                .screenshot("01_lumen_storage_trait")
                .step("inspect starlight receiver", ctx -> inspectTrait(ctx, StarlightReceiverTraitDefinition.class))
                .settleMs(300)
                .screenshot("02_starlight_receiver_trait")
                .step("inspect researcher", ctx -> inspectTrait(ctx, AstralResearcherTraitDefinition.class))
                .settleMs(300)
                .screenshot("03_researcher_trait")
                .closeScreen()

                .openModularUI("recipe type editor", AstralSorceryEditorScenario::recipeEditor)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .waitUntil("the recipe views are ready", ctx -> {
                    var project = ctx.<RecipeTypeProject>get(RECIPES);
                    return project.getRecipesView() != null && project.getRecipesView().getViewContainer() != null;
                })
                .step("select the recipes view", ctx -> {
                    var view = ctx.<RecipeTypeProject>get(RECIPES).getRecipesView();
                    view.getViewContainer().selectView(view);
                })
                .settleMs(300)
                .click("the recipe", ctx -> label(ctx, MBD2.id("starlit_infusion").toString()), 0)
                .settleMs(300)
                .screenshot("04_recipe_contents")
                .clickAt("the lumen input", ctx -> rowStart(ctx, I18n.get("recipe.capability.astral_lumen.name")), 0)
                .settleMs(300)
                .screenshot("05_lumen_content")
                .clickAt("the starlight input", ctx -> rowStart(ctx, I18n.get("recipe.capability.astral_starlight.name")), 0)
                .settleMs(300)
                .screenshot("06_starlight_content")
                .step("open the conditions tab", ctx -> {
                    var tab = ctx.query().type(Tab.class)
                            .where(element -> "Conditions".equals(((Tab) element).text.getText().getString()))
                            .one().as(Tab.class);
                    tab.getTabView().selectTab(tab);
                })
                .settleMs(300)
                .click("the constellation condition", ctx -> labelContaining(ctx, "is in the sky"), 0)
                .settleMs(300)
                .screenshot("07_constellation_condition")
                .click("the eclipse condition", ctx -> labelContaining(ctx, "Lunar Eclipse"), 0)
                .settleMs(300)
                .screenshot("08_eclipse_condition")
                .click("the research condition", ctx -> labelContaining(ctx, "Researcher has reached"), 0)
                .settleMs(300)
                .screenshot("09_research_condition")
                .step("generate the recipe ui and preview the recipe in it", ctx -> {
                    var view = ctx.<RecipeTypeProject>get(RECIPES).getRecipeUIView();
                    view.getViewContainer().selectView(view);
                    try {
                        var generate = RecipeTypeUIView.class.getDeclaredMethod("generateAllRecipeUI");
                        generate.setAccessible(true);
                        generate.invoke(view);
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException(e);
                    }
                    view.startSimulation();
                })
                .settleMs(600)
                .screenshot("10_recipe_ui_preview")
                .closeScreen();
    }

    private static ModularUI machineEditor(TestContext ctx) {
        var editor = new MBDEditor();
        var project = new MachineProject();
        var settings = project.getDefinition().machineSettings();
        settings.addTraitDefinition(new LumenStorageCapabilityTraitDefinition());
        settings.addTraitDefinition(new StarlightReceiverTraitDefinition());
        settings.addTraitDefinition(new AstralResearcherTraitDefinition());
        ctx.put(MACHINE, project);
        editor.loadProject(project, null);
        return new ModularUI(UI.of(editor), ctx.player());
    }

    private static ModularUI recipeEditor(TestContext ctx) {
        var editor = new MBDEditor();
        var project = new RecipeTypeProject();
        var type = project.getRecipeType();
        var armara = ConstellationsAS.ARMARA.getId();
        var id = MBD2.id("starlit_infusion");
        var recipe = type.recipeBuilder(id)
                .inputs(AstralLumenRecipeCapability.CAP, new LumenAmount(LumenAS.AEVITAS.getId(), 500))
                .inputs(AstralStarlightRecipeCapability.CAP, StarlightAmount.of(armara, 100))
                .outputItems(Items.AMETHYST_SHARD)
                .outputs(AstralLumenRecipeCapability.CAP, new LumenAmount(LumenAS.VICIO.getId(), 250))
                .addCondition(new AstralConstellationCondition(armara, 0.5f))
                .addCondition(new AstralCelestialEventCondition(AstralCelestialEventCondition.Event.LUNAR_ECLIPSE, true))
                .addCondition(new AstralResearchCondition(AstralResearchCondition.Tier.RESONANCE, armara, AstralSorceryIntegration.ANY))
                .duration(200)
                .buildRawRecipe();
        type.getBuiltinRecipes().put(id, recipe);
        ctx.put(RECIPES, project);
        editor.loadProject(project, null);
        return new ModularUI(UI.of(editor), ctx.player());
    }

    private static MBDEditor editor(TestContext ctx) {
        return ctx.query().type(MBDEditor.class).one().as(MBDEditor.class);
    }

    private static void inspectTrait(TestContext ctx, Class<? extends TraitDefinition> type) {
        var trait = ctx.<MachineProject>get(MACHINE).getDefinition().machineSettings().traitDefinitions()
                .stream().filter(type::isInstance).findFirst().orElseThrow();
        editor(ctx).inspectorView.inspect(trait);
    }

    private static UIElement label(TestContext ctx, String text) {
        return ctx.query().withText(text).visible().nth(0).one().element();
    }

    /** A content row's label is wider than it draws, so its centre lands on the next field: aim at its start. */
    private static float[] rowStart(TestContext ctx, String text) {
        var bounds = ctx.query().withText(text).visible().nth(0).one().bounds();
        return new float[]{bounds.x() + 6, bounds.centerY()};
    }

    private static UIElement labelContaining(TestContext ctx, String text) {
        return ctx.query().withTextContaining(text).visible().nth(0).one().element();
    }
}
