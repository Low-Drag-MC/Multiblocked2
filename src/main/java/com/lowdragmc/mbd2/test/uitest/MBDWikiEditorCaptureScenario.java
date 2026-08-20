package com.lowdragmc.mbd2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.mbd2.common.gui.editor.MBDEditor;
import com.lowdragmc.mbd2.common.gui.editor.MachineProject;
import com.lowdragmc.mbd2.common.trait.entity.EntityHandlerTraitDefinition;
import com.lowdragmc.mbd2.common.trait.fluid.FluidTankCapabilityTraitDefinition;
import com.lowdragmc.mbd2.common.trait.forgeenergy.ForgeEnergyCapabilityTraitDefinition;
import com.lowdragmc.mbd2.common.trait.item.ItemSlotCapabilityTraitDefinition;
import com.lowdragmc.mbd2.common.trait.TraitDefinition;
import com.lowdragmc.mbd2.integration.ae2.trait.MEInterfaceTraitDefinition;
import com.lowdragmc.mbd2.integration.create.machine.CreateRotationTrait;
import com.lowdragmc.mbd2.integration.mekanism.trait.chemical.ChemicalTankCapabilityTraitDefinition;
import com.lowdragmc.mbd2.integration.mekanism.trait.heat.MekHeatCapabilityTraitDefinition;
import com.lowdragmc.mbd2.integration.naturesaura.trait.AuraHandlerTraitDefinition;
import com.lowdragmc.mbd2.integration.pneumaticcraft.trait.heat.PNCHeatExchangerTraitDefinition;
import com.lowdragmc.mbd2.integration.pneumaticcraft.trait.pressure.PNCPressureAirHandlerTraitDefinition;

/** Stable, real-client editor captures used by the MBD2 Wiki. */
@LDLRegisterClient(name = "mbd2_wiki_editor", group = "mbd2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class MBDWikiEditorCaptureScenario implements UIScenario {

    private static final String PROJECT = "machine_project";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(180).tags("wiki", "editor", "visual").requiresWorld(true).guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("MBD2 machine editor", MBDWikiEditorCaptureScenario::editorUI)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .waitUntil("the editor and project views are ready", ctx -> {
                    var project = ctx.<MachineProject>get(PROJECT);
                    return project.getMachineTraitView() != null
                            && project.getMachineTraitView().getViewContainer() != null;
                })
                .step("select the machine traits view", ctx -> {
                    var view = ctx.<MachineProject>get(PROJECT).getMachineTraitView();
                    view.getViewContainer().selectView(view);
                })
                .settleMs(300)
                .screenshot("01_machine_traits")
                .step("inspect the item slot trait", ctx -> {
                    var project = ctx.<MachineProject>get(PROJECT);
                    editor(ctx).inspectorView.inspect(
                            project.getDefinition().machineSettings().traitDefinitions().getFirst());
                })
                .settleMs(300)
                .screenshot("02_item_slot_configuration")
                .step("open the add trait menu", ctx -> {
                    var view = ctx.<MachineProject>get(PROJECT).getMachineTraitView();
                    editor(ctx).openMenu(view.getPositionX() + 90, view.getPositionY() + 70,
                            view.createMenu(), false);
                })
                .settleMs(180)
                .step("hover the add trait branch", ctx -> {
                    var menu = ctx.query("menu").nth(0).one().bounds();
                    ctx.input().moveTo(menu.centerX(), menu.centerY());
                })
                .waitUntil("the capability submenu is visible", ctx -> ctx.count("menu") > 1)
                .settleMs(300)
                .screenshot("03_add_trait_menu")
                .step("close the trait menu", ctx ->
                        ctx.query("menu").nth(0).one().element().removeSelf())
                .step("inspect Mekanism chemical storage", ctx -> inspect(ctx, ChemicalTankCapabilityTraitDefinition.class))
                .settleMs(240)
                .screenshot("04_mekanism_chemical_tank")
                .step("inspect Create rotation", ctx -> inspect(ctx, CreateRotationTrait.CreateRotationTraitDefinition.class))
                .settleMs(240)
                .screenshot("05_create_rotation")
                .step("inspect PneumaticCraft pressure", ctx -> inspect(ctx, PNCPressureAirHandlerTraitDefinition.class))
                .settleMs(240)
                .screenshot("06_pneumaticcraft_pressure")
                .step("inspect Nature's Aura storage", ctx -> inspect(ctx, AuraHandlerTraitDefinition.class))
                .settleMs(240)
                .screenshot("07_natures_aura")
                .step("inspect AE2 interface", ctx -> inspect(ctx, MEInterfaceTraitDefinition.class))
                .settleMs(240)
                .screenshot("08_ae2_interface")
                .closeScreen();
    }

    private static ModularUI editorUI(TestContext ctx) {
        var editor = new MBDEditor();
        var project = new MachineProject();
        project.getDefinition().machineSettings().addTraitDefinition(new ItemSlotCapabilityTraitDefinition());
        project.getDefinition().machineSettings().addTraitDefinition(new FluidTankCapabilityTraitDefinition());
        project.getDefinition().machineSettings().addTraitDefinition(new ForgeEnergyCapabilityTraitDefinition());
        project.getDefinition().machineSettings().addTraitDefinition(new EntityHandlerTraitDefinition());
        project.getDefinition().machineSettings().addTraitDefinition(new ChemicalTankCapabilityTraitDefinition());
        project.getDefinition().machineSettings().addTraitDefinition(new MekHeatCapabilityTraitDefinition());
        project.getDefinition().machineSettings().addTraitDefinition(CreateRotationTrait.DEFINITION);
        project.getDefinition().machineSettings().addTraitDefinition(new PNCHeatExchangerTraitDefinition());
        project.getDefinition().machineSettings().addTraitDefinition(new PNCPressureAirHandlerTraitDefinition());
        project.getDefinition().machineSettings().addTraitDefinition(new AuraHandlerTraitDefinition());
        project.getDefinition().machineSettings().addTraitDefinition(new MEInterfaceTraitDefinition());
        ctx.put(PROJECT, project);
        editor.loadProject(project, null);
        return new ModularUI(UI.of(editor), ctx.player());
    }

    private static MBDEditor editor(TestContext ctx) {
        return ctx.query().type(MBDEditor.class).one().as(MBDEditor.class);
    }

    private static void inspect(TestContext ctx, Class<? extends TraitDefinition> type) {
        var trait = ctx.<MachineProject>get(PROJECT).getDefinition().machineSettings().traitDefinitions()
                .stream().filter(type::isInstance).findFirst().orElseThrow();
        editor(ctx).inspectorView.inspect(trait);
    }
}
