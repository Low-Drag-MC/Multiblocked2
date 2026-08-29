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
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.gui.editor.MBDEditor;
import com.lowdragmc.mbd2.common.gui.editor.MachineProject;
import com.lowdragmc.mbd2.common.machine.definition.config.fx.MachineFXConfig;
import net.minecraft.resources.ResourceLocation;

/**
 * A machine project carrying Photon effects, opened in the editor — with Photon and without.
 *
 * <h2>What it is for</h2>
 * Photon is an optional dependency, so a pack authored with it has to keep opening on a client that
 * does not have it: the effect configuration is plain NBT and survives, and the only thing that should
 * disappear is the view that previews it. Every claim there is about class resolution — MBD2 keeps
 * Photon types behind nested classes reached only past {@code isPhotonLoaded()} — and a claim about
 * class resolution is not worth anything until the jar is actually missing.
 *
 * <pre>
 *   gradlew runClient -PldTest=tag:photon-optional             # Photon present
 *   gradlew runClient -PldTest=tag:photon-optional -PnoPhoton  # Photon off the runtime classpath
 * </pre>
 *
 * <p>Every assertion holds both ways. The one legitimate difference — whether the FX view exists at
 * all — is asserted <em>against</em> {@link MBD2#isPhotonLoaded()} rather than skipped, so the run
 * with Photon also proves the gate is not simply always closed.</p>
 *
 * @see PhotonOptionalBlueprintScenario for the other half: a blueprint that names a Photon node
 */
@LDLRegisterClient(name = "mbd2_photon_optional_editor", group = "mbd2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class PhotonOptionalEditorScenario implements UIScenario {

    private static final String PROJECT = "machine_project";
    private static final String LIBRARY_FX = "burst";
    private static final String STATE_FX = "idle_glow";
    private static final ResourceLocation TEST_FX = MBD2.id("test_machine_fx");

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(150).tags("mbd2", "fx", "editor", "photon-optional")
                .requiresWorld(true).guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("MBD2 editor, Photon optional", PhotonOptionalEditorScenario::editorUI)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .settleMs(400)
                // Loading the project is where a missing Photon would take the editor down, because it
                // is the moment MachineProject.onLoad decides whether to construct the FX view.
                .check("the project opened", ctx -> {
                    var project = ctx.<MachineProject>get(PROJECT);
                    ctx.attach("photon", String.valueOf(MBD2.isPhotonLoaded()));
                    return project.getMachineConfigView() != null
                            && project.getMachineTraitView() != null
                            && project.getMachineUIView() != null;
                })
                .check("the FX view exists exactly when Photon does", ctx -> {
                    var view = ctx.<MachineProject>get(PROJECT).getMachineFXView();
                    ctx.attach("fx_view", view == null ? "absent" : "present");
                    return (view != null) == MBD2.isPhotonLoaded();
                })
                // The configuration is the part that must survive regardless: a pack that loses its
                // effects on a Photon-less client and then saves has lost them for everyone.
                .check("the effect configuration survived", ctx -> {
                    var definition = ctx.<MachineProject>get(PROJECT).getDefinition();
                    var library = definition.machineSettings().photonFXs();
                    var state = definition.stateMachine().getRootState().getRealMachineFXs();
                    ctx.attach("library", library.stream().map(MachineFXConfig::getName).toList().toString());
                    ctx.attach("state", state.stream().map(MachineFXConfig::getName).toList().toString());
                    return library.size() == 1 && LIBRARY_FX.equals(library.getFirst().getName())
                            && state.size() == 1 && STATE_FX.equals(state.getFirst().getName());
                })
                .screenshot("01_editor_photon_optional")
                .closeScreen();
    }

    private static ModularUI editorUI(TestContext ctx) {
        var editor = new MBDEditor();
        var project = new MachineProject();
        project.getDefinition().machineSettings().photonFXs()
                .add(new MachineFXConfig(LIBRARY_FX, TEST_FX));
        var root = project.getDefinition().stateMachine().getRootState();
        root.machineFXs().setEnable(true);
        root.machineFXs().getFxs().add(new MachineFXConfig(STATE_FX, TEST_FX));
        ctx.put(PROJECT, project);
        editor.loadProject(project, null);
        return new ModularUI(UI.of(editor), ctx.player());
    }
}
