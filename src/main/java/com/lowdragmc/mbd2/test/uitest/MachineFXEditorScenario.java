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
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
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
    private static final String STATE_FX_NAME = "idle_glow";
    private static final String INHERITING_STATE = "working";
    private static final String ROOT_STATE = "base";
    /** Added after the view is built, to prove the tree follows the state machine on its own. */
    private static final String LATE_STATE = "late_state";
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
                                && resolves("editor.machine.machine_fx.states")
                                && resolves("editor.machine.machine_fx.library")
                                && resolves("editor.machine.machine_fx.inherited"))
                // The point of the redesign: one tree carrying both kinds of effect, so there is one
                // place to look rather than three.
                .check("the tree holds both the states and the library", ctx -> {
                    var view = ctx.<MachineProject>get(PROJECT).getMachineFXView();
                    var library = view.findLibraryRow(FX_NAME);
                    var stateOwned = view.findStateRow(ROOT_STATE, false);
                    var inherited = view.findStateRow(INHERITING_STATE, true);
                    ctx.attach("library_row", String.valueOf(library != null));
                    ctx.attach("state_row", String.valueOf(stateOwned != null));
                    ctx.attach("inherited_row", String.valueOf(inherited != null));
                    return library != null && stateOwned != null && inherited != null;
                })
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
                // Opening a view is not a request to play anything. The clock has to sit at zero until
                // the author starts it, or the transport is running against an empty scene before there
                // is even a selection to look at.
                .check("the preview does not start itself", ctx -> {
                    var view = ctx.<MachineProject>get(PROJECT).getMachineFXView();
                    ctx.attach("playing", String.valueOf(view.isPlayingForTest()));
                    ctx.attach("scene_time", String.valueOf(view.sceneTimeForTest()));
                    return !view.isPlayingForTest() && view.sceneTimeForTest() == 0;
                })
                .screenshot("01_machine_fx_view")
                // Key events go to the focused element and bubble up from it, so the transport keys
                // only ever arrive if focus is inside this view. Clicking something with no handler of
                // its own is the ordinary way that happens, and it is what the first attempt got wrong:
                // focus landed on the Editor, above every view, and space reached nothing.
                .click(".__machine-fx-now-playing__")
                .check("clicking inside the view gives it focus", ctx -> {
                    var view = ctx.<MachineProject>get(PROJECT).getMachineFXView();
                    var focused = ctx.requireUI().getFocusedElement();
                    ctx.attach("focused", focused == null ? "null" : focused.getClass().getSimpleName());
                    return focused == view;
                })
                .key(Keys.SPACE)
                .settleMs(150)
                .check("space starts playback", ctx ->
                        ctx.<MachineProject>get(PROJECT).getMachineFXView().isPlayingForTest())
                .key(Keys.SPACE)
                .settleMs(150)
                .check("space stops it again", ctx ->
                        !ctx.<MachineProject>get(PROJECT).getMachineFXView().isPlayingForTest())
                // The ruler's span is the whole reason it has an end to loop at, so the wheel has to
                // move it — a fixed ten seconds cannot show a long effect or resolve a short one.
                .check("the ruler starts at ten seconds", ctx -> {
                    var ticks = ctx.<MachineProject>get(PROJECT).getMachineFXView().visibleTicksForTest();
                    ctx.attach("visible_ticks", String.valueOf(ticks));
                    return ticks == 200;
                })
                .scroll(".__fx-timeline_ruler__", -1)
                .check("scrolling down widens it", ctx -> {
                    var ticks = ctx.<MachineProject>get(PROJECT).getMachineFXView().visibleTicksForTest();
                    ctx.attach("zoomed_out_ticks", String.valueOf(ticks));
                    return ticks > 200;
                })
                .scroll(".__fx-timeline_ruler__", 1)
                .check("and scrolling back up narrows it again", ctx -> {
                    var ticks = ctx.<MachineProject>get(PROJECT).getMachineFXView().visibleTicksForTest();
                    ctx.attach("zoomed_in_ticks", String.valueOf(ticks));
                    return ticks == 200;
                })
                .step("select the library entry in the tree", ctx -> {
                    var view = ctx.<MachineProject>get(PROJECT).getMachineFXView();
                    var node = view.findLibraryRow(FX_NAME);
                    if (node == null) throw new IllegalStateException("the library entry is not in the tree");
                    view.selectRow(node);
                })
                .settleMs(300)
                // Selecting an effect is what hands it to the scene's transform gizmo — the whole
                // reason the offset is now draggable instead of typed.
                .check("selecting an effect binds the transform gizmo to it", ctx -> {
                    var view = ctx.<MachineProject>get(PROJECT).getMachineFXView();
                    var target = view.getGizmoTarget();
                    ctx.attach("gizmo_target", target == null ? "null" : target.getName());
                    return target != null && FX_NAME.equals(target.getName());
                })
                .screenshot("02_machine_fx_configuration")
                // States are added and removed in Basic Settings, which has never heard of this view.
                // The tree has to notice on its own, and it has to hold on to the selection while it
                // does — a rebuild that silently stops the preview is how the old library list behaved.
                .step("add a state the way Basic Settings does", ctx -> ctx.<MachineProject>get(PROJECT)
                        .getDefinition().stateMachine().getRootState().addChild(LATE_STATE))
                .settleMs(200)
                .check("the tree picks up a state added by another view", ctx -> {
                    var view = ctx.<MachineProject>get(PROJECT).getMachineFXView();
                    var row = view.findStateRow(LATE_STATE);
                    ctx.attach("late_state_row", String.valueOf(row != null));
                    return row != null;
                })
                .check("and keeps the selection through the rebuild", ctx -> {
                    var target = ctx.<MachineProject>get(PROJECT).getMachineFXView().getGizmoTarget();
                    ctx.attach("gizmo_target", target == null ? "null" : target.getName());
                    return target != null && FX_NAME.equals(target.getName());
                })
                .step("remove it again", ctx -> {
                    var root = ctx.<MachineProject>get(PROJECT).getDefinition().stateMachine().getRootState();
                    root.getChildren().stream()
                            .filter(state -> LATE_STATE.equals(state.name()))
                            .findFirst()
                            .ifPresent(root::removeChild);
                })
                .settleMs(200)
                .check("and drops it again when it goes away", ctx ->
                        ctx.<MachineProject>get(PROJECT).getMachineFXView().findStateRow(LATE_STATE) == null)
                // Back to playing for the render legs below: the transport is sticky, so nothing else
                // is going to start it.
                .key(Keys.SPACE)
                .settleMs(150)
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
                // The scrub restarts and re-simulates rather than seeking, because a plain emitter
                // has no timeline for a seek to move — so this has to hold particles at a tick, not
                // merely not crash.
                .step("scrub the selection to tick 40", ctx -> {
                    var view = ctx.<MachineProject>get(PROJECT).getMachineFXView();
                    view.scrubToForTest(40);
                })
                .settleMs(300)
                .check("scrubbing leaves particles on screen at that tick", ctx -> {
                    var view = ctx.<MachineProject>get(PROJECT).getMachineFXView();
                    var manager = view.getParticleManager();
                    var count = manager == null ? 0 : manager.getParticleAmount();
                    ctx.attach("scrubbed_particle_count", String.valueOf(count));
                    return count > 0;
                })
                // The clock has to move with the seek, or the playhead and the readout both lie —
                // which is exactly what the first attempt at this did.
                .check("the scene clock reports the tick that was seeked to", ctx -> {
                    var time = ctx.<MachineProject>get(PROJECT).getMachineFXView().sceneTimeForTest();
                    ctx.attach("scene_time", String.valueOf(time));
                    return time == 40;
                })
                .screenshot("03b_machine_fx_scrubbed")
                // The seed is the whole reason a preview is worth replaying: two seeks to the same
                // tick must show the same frame, or comparing an edit against the last run is noise.
                .check("the same seek twice gives the same picture", ctx -> {
                    var view = ctx.<MachineProject>get(PROJECT).getMachineFXView();
                    var manager = view.getParticleManager();
                    view.scrubToForTest(0);
                    view.scrubToForTest(35);
                    var first = manager == null ? -1 : manager.getParticleAmount();
                    view.scrubToForTest(0);
                    view.scrubToForTest(35);
                    var second = manager == null ? -2 : manager.getParticleAmount();
                    ctx.attach("deterministic", first + " vs " + second);
                    return first == second && first > 0;
                })
                .check("a different seed gives a different roll", ctx -> {
                    var view = ctx.<MachineProject>get(PROJECT).getMachineFXView();
                    // Not asserting the pictures differ - two seeds can agree on a count by chance.
                    // What has to hold is that the seed is actually carried into the preview.
                    view.setPreviewSeedForTest(12345L);
                    var applied = view.previewSeedForTest();
                    ctx.attach("seed", String.valueOf(applied));
                    return applied == 12345L;
                })
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

        // A per-state effect as well, so the tree shows the shape the view exists for: a state that
        // owns a list, and a child that inherits it.
        var root = project.getDefinition().stateMachine().getRootState();
        root.machineFXs().setEnable(true);
        var stateFX = new MachineFXConfig(STATE_FX_NAME, TEST_FX);
        stateFX.setOffset(new Vector3f(0, 1, 0));
        stateFX.setMaxDistance(0);
        root.machineFXs().getFxs().add(stateFX);
        root.addChild(INHERITING_STATE);
        ctx.put(PROJECT, project);
        editor.loadProject(project, null);
        return new ModularUI(UI.of(editor), ctx.player());
    }

    /** Whether a lang key has a translation, as opposed to falling through to the key itself. */
    private static boolean resolves(String key) {
        return !key.equals(Component.translatable(key).getString());
    }
}
