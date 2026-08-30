package com.lowdragmc.mbd2.test.uitest;

import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.editor.resource.IResourceProvider;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceContainer;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.nodegraphtookit.editor.GraphEditorView;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.mbd2.common.blueprint.builtin.BuiltinBlueprints;
import com.lowdragmc.mbd2.common.gui.editor.MBDEditor;
import com.lowdragmc.mbd2.common.gui.editor.MachineProject;
import com.lowdragmc.mbd2.common.gui.editor.blueprint.MachineBlueprintResource;
import net.minecraft.nbt.CompoundTag;

/**
 * The blueprint library inside the real machine editor: opening a built-in, and copying one out.
 *
 * <h2>What this covers</h2>
 * The two halves of what makes a built-in blueprint usable, neither of which exists headlessly. Opening
 * one has to produce a view that is genuinely read-only — no save button, and a graph the editor refuses
 * to change. Copying one has to be able to <em>leave</em> the built-in library, because a resource that
 * cannot be edited and cannot be forked is a dead end.
 *
 * <p>The copy goes through the real dialog rather than through {@code copyResourceTo} directly, because
 * the thing that would break is the wiring between them — a provider list that omits the writable
 * providers, a name field that reports the placeholder, a confirm button that closes before it reads.</p>
 */
@LDLRegisterClient(name = "mbd2_blueprint_library", group = "mbd2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class BlueprintLibraryScenario implements UIScenario {

    private static final String BUILTIN = "redstone_control";
    /** The name the copy is made under. Removed again at the end, so a run leaves no file behind. */
    private static final String COPY_NAME = "uitest_redstone_copy";
    private static final String COPIED_PATH = "copied-path";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(120).tags("mbd2", "blueprint", "builtin", "editor")
                .requiresWorld(true).guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("MBD2 editor", BlueprintLibraryScenario::editorUI)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .frames(3)
                .step("show the blueprint library", ctx ->
                        editor(ctx).resourceView.selectResourceInstance(MachineBlueprintResource.INSTANCE))
                .frames(3)
                .step("select the built-in provider and its first blueprint", ctx -> {
                    var container = blueprintContainer(ctx);
                    var path = path(BUILTIN);
                    if (!container.locateResource(path, false)) {
                        throw new IllegalStateException("the built-in blueprint library is not shown");
                    }
                })
                .frames(3)
                .check("the built-in provider is the one selected", ctx ->
                        blueprintContainer(ctx).getSelectedProvider() != null
                                && blueprintContainer(ctx).getSelectedProvider().hasResource(path(BUILTIN)))
                .screenshot("01_blueprint-library")

                // ---- opening a built-in --------------------------------------------------------
                .step("open the built-in blueprint", ctx ->
                        providerContainer(ctx).editResource(path(BUILTIN)))
                .frames(4)
                .check("it opened", ctx -> ctx.query().type(GraphEditorView.class).count() > 0)
                .check("it opened read-only", ctx -> {
                    var view = ctx.query().type(GraphEditorView.class).one().as(GraphEditorView.class);
                    ctx.attach("readOnly", String.valueOf(view.isReadOnly()));
                    return view.isReadOnly() && view.graphView.isReadOnly();
                })
                .check("the save button is not offered", ctx ->
                        !ctx.query().type(GraphEditorView.class).one()
                                .as(GraphEditorView.class).saveButton.isDisplayed())
                .screenshot("02_blueprint-builtin-opened")
                // Sitting on the graph, which is what a reader actually does, and where the
                // GraphChangeset crash lived: the blackboard's background type search built models in
                // the open graph while the render thread merged its change set. A test that screenshots
                // and moves on never reaches the tick that died — these frames are the regression.
                .frames(120)
                .check("the graph survives being left open", ctx ->
                        ctx.query().type(GraphEditorView.class).one()
                                .as(GraphEditorView.class).getGraph() != null)
                // The node picker is the most model-touching thing a read-only view can still reach,
                // and it is what a reader clicks when they want to know what else is available.
                .step("open the node picker over the graph", ctx -> {
                    var view = ctx.query().type(GraphEditorView.class).one().as(GraphEditorView.class);
                    view.getCurrentView().itemLibrary.show(200, 200, item -> {});
                })
                .frames(60)
                .check("the picker did not take the graph down", ctx ->
                        ctx.query().type(GraphEditorView.class).one()
                                .as(GraphEditorView.class).getGraph() != null)

                // ---- copying one out -----------------------------------------------------------
                .step("copy the built-in", ctx -> providerContainer(ctx).copyResource(path(BUILTIN)))
                .frames(3)
                .check("the copy dialog asks where to put it", ctx ->
                        ctx.query(".__copy-target-selector__").count() == 1
                                && ctx.query().type(Dialog.class).count() > 0)
                .check("it offers a writable provider", ctx -> {
                    var targets = MachineBlueprintResource.INSTANCE.getResourceInstance()
                            .listWritableProviders();
                    ctx.attach("targets", targets.stream().map(IResourceProvider::getName).toList().toString());
                    // The built-in provider must NOT be one of them — copying into it would be a write
                    // to a read-only library, and the dialog would be offering something that fails.
                    return !targets.isEmpty()
                            && targets.stream().noneMatch(p -> p.hasResource(path(BUILTIN)));
                })
                .check("the dialog offers a name field", ctx ->
                        ctx.query(".__copy-name-field__").count() == 1)
                .screenshot("03_blueprint-copy-dialog")
                .step("name the copy", ctx ->
                        // Set rather than typed: going through the input layer would exercise the char
                        // validator, which is not what this scenario is about.
                        ctx.query(".__copy-name-field__").one().as(TextField.class)
                                .setText(COPY_NAME, false))
                .frames(2)
                // Press and release in one step, off bounds resolved once: the dialog closes on the
                // click, so the builder's click() — which re-resolves the selector for the release —
                // cannot find the button any more by the time it gets there.
                .step("confirm the copy", ctx -> {
                    var bounds = ctx.query(".__confirm-button__").one().bounds();
                    ctx.input().moveTo(bounds.centerX(), bounds.centerY());
                    ctx.input().mouseDown(bounds.centerX(), bounds.centerY(), 0);
                    ctx.input().mouseUp(bounds.centerX(), bounds.centerY(), 0);
                })
                .frames(3)
                .check("the copy landed in a writable provider", ctx -> {
                    for (var provider : MachineBlueprintResource.INSTANCE.getResourceInstance()
                            .listWritableProviders()) {
                        var copied = provider.createSubPath(COPY_NAME);
                        if (provider.getResource(copied) instanceof CompoundTag) {
                            ctx.put(COPIED_PATH, copied);
                            ctx.attach("provider", provider.getName());
                            return true;
                        }
                    }
                    return false;
                })
                .check("the copy is a real graph, not an empty one", ctx -> {
                    var path = ctx.<IResourcePath>get(COPIED_PATH, null);
                    if (path == null) return false;
                    var tag = MachineBlueprintResource.INSTANCE.getResourceInstance().getResource(path);
                    if (tag == null) return false;
                    var graph = MachineBlueprintResource.INSTANCE.deserializeGraphResource(tag, null);
                    return !graph.graphModel.getNodeModels().isEmpty();
                })
                .check("the copy is editable, unlike the original", ctx -> {
                    var path = ctx.<IResourcePath>get(COPIED_PATH, null);
                    if (path == null) return false;
                    for (var provider : MachineBlueprintResource.INSTANCE.getResourceInstance()
                            .listWritableProviders()) {
                        if (provider.hasResource(path)) return provider.canEdit(path);
                    }
                    return false;
                })
                .screenshot("04_blueprint-copied")
                .teardown("remove the copy", ctx -> {
                    var path = ctx.<IResourcePath>get(COPIED_PATH, null);
                    if (path == null) return;
                    for (var provider : MachineBlueprintResource.INSTANCE.getResourceInstance()
                            .listWritableProviders()) {
                        if (provider.hasResource(path)) provider.removeResource(path);
                    }
                })
                .closeScreen();
    }

    private static IResourcePath path(String name) {
        var path = IResourcePath.parse(BuiltinBlueprints.path(name));
        if (path == null) throw new IllegalStateException("cannot parse the built-in path for " + name);
        return path;
    }

    private static MBDEditor editor(TestContext ctx) {
        return ctx.query().type(MBDEditor.class).one().as(MBDEditor.class);
    }

    @SuppressWarnings("unchecked")
    private static ResourceContainer<CompoundTag> blueprintContainer(TestContext ctx) {
        for (var ref : ctx.query().type(ResourceContainer.class).list()) {
            var container = (ResourceContainer<CompoundTag>) ref.as(ResourceContainer.class);
            if (container.resourceInstance.resource == MachineBlueprintResource.INSTANCE) {
                return container;
            }
        }
        throw new IllegalStateException("the blueprint resource container is not in the editor");
    }

    private static com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceProviderContainer<CompoundTag>
            providerContainer(TestContext ctx) {
        var container = blueprintContainer(ctx).getSelectedProviderContainer();
        if (container == null) throw new IllegalStateException("no provider is selected");
        return container;
    }

    private static ModularUI editorUI(TestContext ctx) {
        var editor = new MBDEditor();
        editor.loadProject(new MachineProject(), null);
        return new ModularUI(UI.of(editor), ctx.requirePlayer());
    }
}
