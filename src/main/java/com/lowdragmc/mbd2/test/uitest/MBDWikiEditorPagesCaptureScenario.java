package com.lowdragmc.mbd2.test.uitest;

import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.gui.editor.MBDEditor;
import com.lowdragmc.mbd2.common.gui.editor.MachineProject;
import com.lowdragmc.mbd2.common.gui.editor.MultiblockMachineProject;
import com.lowdragmc.mbd2.common.gui.editor.RecipeTypeProject;
import com.lowdragmc.mbd2.common.trait.fluid.FluidTankCapabilityTraitDefinition;
import com.lowdragmc.mbd2.common.trait.forgeenergy.ForgeEnergyCapabilityTraitDefinition;
import com.lowdragmc.mbd2.common.trait.item.ItemSlotCapabilityTraitDefinition;
import net.minecraft.world.item.Items;

/** Captures the major MBD2 authoring views for documentation pages. */
@LDLRegisterClient(name = "mbd2_wiki_editor_pages", group = "mbd2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class MBDWikiEditorPagesCaptureScenario implements UIScenario {

    private static final String PROJECT = "project";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(220).tags("wiki", "editor", "visual").requiresWorld(true).guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("machine authoring views", MBDWikiEditorPagesCaptureScenario::machineEditor)
                .awaitScreen(ModularUIScreen.class).awaitModularUI()
                .waitUntil("machine views are ready", ctx -> machine(ctx).getMachineConfigView() != null)
                .step("show Basic Settings", ctx -> {
                    var project = machine(ctx);
                    select(project.getMachineConfigView());
                    editor(ctx).inspectorView.inspect(project.getDefinition());
                })
                .settleMs(350)
                .screenshot("01_editor_overview_basic_settings")
                .step("inspect the base machine state", ctx -> {
                    var project = machine(ctx);
                    editor(ctx).inspectorView.inspect(project.getDefinition().stateMachine().getRootState());
                })
                .settleMs(300)
                .screenshot("02_states_and_rendering")
                .step("show the Machine UI editor", ctx -> select(machine(ctx).getMachineUIView()))
                .settleMs(350)
                .screenshot("03_machine_ui")
                .closeScreen()

                .openModularUI("multiblock pattern authoring", MBDWikiEditorPagesCaptureScenario::multiblockEditor)
                .awaitScreen(ModularUIScreen.class).awaitModularUI()
                .waitUntil("multiblock views are ready", ctx -> multiblock(ctx).getMultiblockPatternView() != null)
                .step("show the Multiblock Pattern view", ctx ->
                        select(multiblock(ctx).getMultiblockPatternView()))
                .settleMs(400)
                .screenshot("04_multiblock_pattern")
                .closeScreen()

                .openModularUI("recipe type authoring", MBDWikiEditorPagesCaptureScenario::recipeEditor)
                .awaitScreen(ModularUIScreen.class).awaitModularUI()
                .waitUntil("recipe views are ready", ctx -> recipe(ctx).getRecipesView() != null)
                .step("show recipes and inspect the recipe type", ctx -> {
                    var project = recipe(ctx);
                    select(project.getRecipesView());
                    editor(ctx).inspectorView.inspect(project.getRecipeType());
                })
                .settleMs(220)
                .step("select the example recipe", ctx -> {
                    var view = recipe(ctx).getRecipesView();
                    ctx.input().moveTo(view.getPositionX() + 100, view.getPositionY() + 32);
                    ctx.input().mouseDown(view.getPositionX() + 100, view.getPositionY() + 32, 0);
                    ctx.input().mouseUp(view.getPositionX() + 100, view.getPositionY() + 32, 0);
                })
                .waitUntil("the example recipe is selected", ctx ->
                        recipe(ctx).getRecipesView().singleSelectedRecipe() != null)
                .settleMs(350)
                .screenshot("05_recipe_type_and_recipes")
                .step("show the recipe display UI editor", ctx -> select(recipe(ctx).getRecipeUIView()))
                .settleMs(350)
                .screenshot("06_recipe_display_ui")
                .closeScreen();
    }

    private static ModularUI machineEditor(TestContext ctx) {
        var editor = new MBDEditor();
        var project = new MachineProject();
        project.getDefinition().machineSettings().addTraitDefinition(new ItemSlotCapabilityTraitDefinition());
        project.getDefinition().machineSettings().addTraitDefinition(new FluidTankCapabilityTraitDefinition());
        project.getDefinition().machineSettings().addTraitDefinition(new ForgeEnergyCapabilityTraitDefinition());
        ctx.put(PROJECT, project);
        editor.loadProject(project, null);
        return new ModularUI(UI.of(editor), ctx.player());
    }

    private static ModularUI multiblockEditor(TestContext ctx) {
        var editor = new MBDEditor();
        var project = new MultiblockMachineProject();
        ctx.put(PROJECT, project);
        editor.loadProject(project, null);
        return new ModularUI(UI.of(editor), ctx.player());
    }

    private static ModularUI recipeEditor(TestContext ctx) {
        var editor = new MBDEditor();
        var project = new RecipeTypeProject();
        project.getRecipeType().recipeBuilder(MBD2.id("wiki_smelting"))
                .duration(100)
                .inputItems(Items.RAW_IRON)
                .outputItems(Items.IRON_INGOT)
                .saveAsBuiltinRecipe();
        ctx.put(PROJECT, project);
        editor.loadProject(project, null);
        return new ModularUI(UI.of(editor), ctx.player());
    }

    private static void select(View view) {
        if (view == null || view.getViewContainer() == null) {
            throw new IllegalStateException("Editor view is not attached");
        }
        view.getViewContainer().selectView(view);
    }

    private static MBDEditor editor(TestContext ctx) {
        return ctx.query().type(MBDEditor.class).one().as(MBDEditor.class);
    }

    private static MachineProject machine(TestContext ctx) {
        return ctx.get(PROJECT);
    }

    private static MultiblockMachineProject multiblock(TestContext ctx) {
        return ctx.get(PROJECT);
    }

    private static RecipeTypeProject recipe(TestContext ctx) {
        return ctx.get(PROJECT);
    }
}
