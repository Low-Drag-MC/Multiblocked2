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
import com.lowdragmc.mbd2.integration.photon.PhotonFXBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

/**
 * The machine FX editor view, on a real client with Photon loaded.
 *
 * <p>Everything below the UI is covered headlessly by {@code MachineFXConfigTests} and
 * {@code MachineFXRuntimeTests}. What only a client can answer is the part with no server
 * counterpart: that the view builds at all, that its labels resolve, and — the one that matters —
 * that the preview scene swapped LDLib2's particle manager for Photon's. That substitution is what
 * makes the render pipeline take its editor-scene branch (isolated post-effect stack, sub-viewport
 * camera) instead of the world one, and a gametest server has no particle engine to get it wrong
 * in.</p>
 *
 * <p>Photon ships no {@code .fx} of its own, so the render leg runs against
 * {@link #TEST_FX} — a single default particle emitter, exported from Photon's own
 * {@code ParticleEmitter} defaults and committed alongside this scenario. It is deliberately the
 * plainest effect there is: this test is about MBD2 driving Photon, not about the effect.</p>
 *
 * <p>Run with {@code gradlew runClient -PldTest=mbd2_machine_fx_editor}.</p>
 */
@LDLRegisterClient(name = "mbd2_machine_fx_editor", group = "mbd2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class MachineFXEditorScenario implements UIScenario {

    private static final String PROJECT = "machine_project";
    private static final String FX_NAME = "burst";
    /** {@code assets/mbd2/fx/test_machine_fx.fx} — a single looping particle emitter. */
    private static final ResourceLocation TEST_FX = MBD2.id("test_machine_fx");

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(180).tags("mbd2", "fx", "editor", "visual").requiresWorld(true).guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("MBD2 machine FX editor", MachineFXEditorScenario::editorUI)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .check("Photon is loaded, so the FX view exists at all", ctx -> {
                    var loaded = MBD2.isPhotonLoaded();
                    ctx.attach("photon", String.valueOf(loaded));
                    return loaded;
                })
                .waitUntil("the FX view is ready", ctx -> {
                    var view = ctx.<MachineProject>get(PROJECT).getMachineFXView();
                    return view != null && view.getViewContainer() != null;
                })
                .step("select the machine FX view", ctx -> {
                    var view = ctx.<MachineProject>get(PROJECT).getMachineFXView();
                    view.getViewContainer().selectView(view);
                })
                .settleMs(300)
                .check("the view tab title resolves", ctx -> resolves("editor.machine.machine_fx"))
                .check("the config labels resolve", ctx ->
                        resolves("config.machine_fx.fx_location")
                                && resolves("config.machine_fx.follow_facing")
                                && resolves("config.machine_state.machine_fxs"))
                // The point of running on a client: without this the scene would still show particles,
                // but through the world render path, in a UI sub-viewport it was never meant for.
                .check("the preview scene uses Photon's particle manager", ctx -> {
                    var manager = ctx.<MachineProject>get(PROJECT).getMachineFXView().getParticleManager();
                    ctx.attach("particle_manager",
                            manager == null ? "null" : manager.getClass().getName());
                    return manager != null
                            && manager.getClass().getName()
                            .equals("com.lowdragmc.photon.client.PhotonParticleManager");
                })
                .check("the test effect is loadable", ctx -> {
                    var found = PhotonFXBridge.hasFX(TEST_FX);
                    ctx.attach("test_fx", TEST_FX + " loadable=" + found);
                    ctx.attach("available_fx", PhotonFXBridge.listFXIds().toString());
                    return found;
                })
                .screenshot("01_machine_fx_view")
                .step("inspect the library entry", ctx -> {
                    var project = ctx.<MachineProject>get(PROJECT);
                    editor(ctx).inspectorView.inspect(
                            project.getDefinition().machineSettings().photonFXs().getFirst());
                })
                .settleMs(300)
                .screenshot("02_machine_fx_configuration")
                .step("play the effect on the preview machine", ctx -> {
                    var view = ctx.<MachineProject>get(PROJECT).getMachineFXView();
                    var config = ctx.<MachineProject>get(PROJECT)
                            .getDefinition().machineSettings().photonFXs().getFirst();
                    var machine = view.getPreviewMachine();
                    if (machine == null) throw new IllegalStateException("no preview machine");
                    machine.getFXManager().play(config, config.getName());
                })
                // long enough for the emitter to actually produce particles rather than catching the
                // first frame, where an effect legitimately looks like nothing
                .settleMs(900)
                .check("the effect is playing on the preview machine", ctx -> {
                    var machine = ctx.<MachineProject>get(PROJECT).getMachineFXView().getPreviewMachine();
                    var playing = machine != null && machine.getFXManager().isPlaying(FX_NAME);
                    ctx.attach("playing", String.valueOf(playing));
                    return playing;
                })
                // The check above only proves the runtime is alive; particles have to reach the
                // scene's own particle manager, which is what the screenshot then shows.
                .check("particles reached the scene's particle manager", ctx -> {
                    var manager = ctx.<MachineProject>get(PROJECT).getMachineFXView().getParticleManager();
                    var count = manager == null ? 0 : manager.getParticleAmount();
                    ctx.attach("particle_count", String.valueOf(count));
                    return count > 0;
                })
                .screenshot("03_machine_fx_playing")
                .step("stop the effect", ctx -> {
                    var machine = ctx.<MachineProject>get(PROJECT).getMachineFXView().getPreviewMachine();
                    if (machine != null) machine.getFXManager().stop(FX_NAME, true);
                })
                .settleMs(300)
                .check("stopping it retires the runtime", ctx -> {
                    var machine = ctx.<MachineProject>get(PROJECT).getMachineFXView().getPreviewMachine();
                    return machine != null && !machine.getFXManager().isPlaying(FX_NAME);
                })
                .screenshot("04_machine_fx_stopped")
                .closeScreen();
    }

    private static ModularUI editorUI(TestContext ctx) {
        var editor = new MBDEditor();
        var project = new MachineProject();
        var fx = new MachineFXConfig(FX_NAME, TEST_FX);
        // above the block, so the effect is not hidden inside the preview machine's model
        fx.setOffset(new Vector3f(0, 1, 0));
        // there is no local player in the preview scene, so keep the distance gate out of the way
        fx.setMaxDistance(0);
        project.getDefinition().machineSettings().photonFXs().add(fx);
        ctx.put(PROJECT, project);
        editor.loadProject(project, null);
        return new ModularUI(UI.of(editor), ctx.player());
    }

    private static MBDEditor editor(TestContext ctx) {
        return ctx.query().type(MBDEditor.class).one().as(MBDEditor.class);
    }

    /** Whether a lang key has a translation, as opposed to falling through to the key itself. */
    private static boolean resolves(String key) {
        return !key.equals(Component.translatable(key).getString());
    }
}
