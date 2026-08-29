package com.lowdragmc.mbd2.common.gui.editor.machine

import com.lowdragmc.lowdraglib2.client.scene.ParticleManager
import com.lowdragmc.lowdraglib2.gui.ColorPattern
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture
import com.lowdragmc.lowdraglib2.gui.texture.Icons
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.default
import com.lowdragmc.lowdraglib2.gui.ui.element
import com.lowdragmc.lowdraglib2.gui.ui.elements.*
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents
import com.lowdragmc.lowdraglib2.gui.ui.layout.pct
import com.lowdragmc.lowdraglib2.gui.ui.layoutDsl
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder
import com.lowdragmc.lowdraglib2.gui.util.TreeNode
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.utils.TransformGizmo
import com.lowdragmc.lowdraglib2.math.ITransform
import com.lowdragmc.mbd2.client.MBDIcons
import com.lowdragmc.mbd2.common.gui.editor.MBDEditor
import com.lowdragmc.mbd2.common.gui.editor.MachineProject
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState
import com.lowdragmc.mbd2.common.machine.definition.config.fx.MachineFXConfig
import com.lowdragmc.mbd2.integration.photon.PhotonFXScene
import dev.vfyjxf.taffy.style.AlignContent
import dev.vfyjxf.taffy.style.FlexDirection
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import org.joml.Quaternionf
import org.joml.Vector3f

/**
 * Authors and previews every Photon effect a machine has — both kinds, in one place.
 *
 * <h2>Why one view owns both</h2>
 * A machine's effects live in two different places for good reasons: a [MachineState]'s list plays
 * while the machine sits in that state and inherits from the parent state when its toggle is off,
 * while [com.lowdragmc.mbd2.common.machine.definition.config.ConfigMachineSettings.photonFXs] is a
 * named library a blueprint fires by name at a moment of its choosing. Those are genuinely different
 * questions and the data stays separate.
 *
 * The *editing* used to be separate too, and that was the problem. The library had two editors — this
 * view and the machine-settings inspector — over one list, neither of which noticed the other's edits;
 * per-state lists were authored in the state inspector but could only be previewed here. So "which
 * effects does this machine have" had three answers in three places. Both lists are now
 * `@Persisted` rather than `@Configurable` (see the fields), which takes them out of the inspectors
 * and leaves this view as the only way in.
 *
 * The tree is the whole model laid out at once: the state hierarchy with each state's effects under
 * it, then the library. Selecting a state previews it through [MBDMachine.setMachineState], which
 * runs the very same `syncStateFX` path the in-world machine uses — so what you see here is the
 * behaviour, not an editor-only imitation of it.
 *
 * Only placed when Photon is installed (see [MachineProject.onLoad]) — the configuration still
 * round-trips through NBT without it, there is simply nothing to show.
 */
open class MachineFXView(editor: MBDEditor, project: MachineProject) :
    MachineSceneView("editor.machine.machine_fx", editor, project, PhotonFXScene.createParticleManager()) {

    /**
     * What a row in the tree stands for.
     *
     * A heterogeneous tree rather than two lists side by side, because the thing an author actually
     * wants to see is one machine's worth of effects in state order — including which states have
     * none of their own.
     */
    sealed interface Row {
        /** The two section headers. */
        data object States : Row
        data object Library : Row

        /** A state. Its children are [Fx] rows, its own or its parent's. */
        data class State(val state: MachineState) : Row

        /**
         * One effect. [owner] is null for a library entry, otherwise the state whose list holds it;
         * [inherited] marks a row shown under a state that does not own it, which is read-only here —
         * it is edited under the state that does.
         */
        data class Fx(val fx: MachineFXConfig, val owner: MachineState?, val inherited: Boolean = false) : Row
    }

    private val scrollerView = ScrollerView()
    private val treeList = TreeList<TreeNode<Row, Void>>()

    /**
     * What is running right now, refreshed on the UI tick.
     *
     * The preview used to give no way to tell whether anything was playing at all — an effect that
     * silently failed to start looked exactly like one whose particles were simply out of frame. This
     * reads the manager's own live list, so it says what the machine says.
     */
    private val nowPlaying = Label()

    /**
     * The preview's random seed.
     *
     * Fixed rather than fresh per run, because a preview whose randomness rerolls on every replay
     * cannot be used to compare one edit against the last — which is what a preview is for. The
     * "reseed" button is how you ask for a different roll.
     */
    private var previewSeed: Long = 0

    /** Transport + ruler, under the scene. @see FXTimelineStrip */
    private val timeline = FXTimelineStrip(
        { partial -> PhotonFXScene.currentTime(particleManager, partial) },
        { PhotonFXScene.isPlaying(particleManager) },
        { time -> requestSeek(time) },
        { togglePlay() },
        { seekTo(0) },
        { previewSeed },
        { seed -> setPreviewSeed(seed) },
    )

    /**
     * The newest scrub target, applied at most once per tick.
     *
     * A drag fires many events per second and a backward seek replays the whole simulation, so
     * applying every one of them is what turns a scrub into a slideshow. -1 means nothing pending.
     * Photon's own scene view coalesces the same way and for the same reason.
     */
    private var pendingSeek: Long = -1

    /**
     * The scene's Photon host — read back off the level rather than cached, so there is one source
     * of truth. Null without Photon; every [PhotonFXScene] call tolerates that.
     */
    val particleManager: ParticleManager? get() = level.particleManager

    // runtime
    /**
     * Every selected effect. They all play; [selectedFX] is the one being edited.
     *
     * Nullable behind the getter on purpose: [MachineSceneView]'s constructor calls [loadScene], and
     * a superclass constructor runs before this class's property initialisers — so anything
     * loadScene() touches here is still null however it is declared. Reading through `orEmpty()` is
     * the honest way to say that, rather than a non-null type that is a lie for one call.
     */
    private var selectedFXsOrNull: List<MachineFXConfig>? = null
    val selectedFXs: List<MachineFXConfig> get() = selectedFXsOrNull.orEmpty()
    var selectedFX: MachineFXConfig? = null
        protected set
    var selectedState: MachineState? = null
        protected set
    /** What the scene's transform gizmo is currently dragging, if anything. */
    var gizmoTarget: MachineFXConfig? = null
        private set

    init {
        addChild(
            splitViewHorizontal {
                withPercentage(25f)
                withLeft(buildLeftColumn())
                withRight(buildScenePane())
            }
        )
        sceneEditor.topBar.addChild(element({
            layout = {
                flexDirection(FlexDirection.ROW)
                justifyContent(AlignContent.FLEX_END)
                height(100.pct)
                flex(1f)
            }
        }) {
            // Which way the machine faces, so `followFacing` can actually be judged.
            //
            // Shown greyed rather than hidden when the preview cannot turn this machine: the scene
            // places the fake machine's block, which carries FACING, and a machine whose rotation
            // state is horizontal-only reads HORIZONTAL_FACING off it and gets nothing. Saying so is
            // better than a control that silently does nothing, or no control and no reason.
            selector<net.minecraft.core.Direction>({
                candidates(if (canPreviewFacing()) previewFacings() else listOf(previewFacing))
                selected(previewFacing)
                candidateUI { facing ->
                    Label().setText(Component.literal(facing?.getName() ?: ""))
                }
                onChange { facing -> setPreviewFacing(facing) }
                style = {
                    tooltips(if (canPreviewFacing()) "editor.machine.machine_fx.facing"
                    else "editor.machine.machine_fx.facing.unavailable")
                }
                layout = {
                    height(100.pct)
                    width(58f)
                }
            }) {
                api { setActive(canPreviewFacing()) }
            }
            // Play only what is selected: with several effects running it is otherwise impossible to
            // tell which one you are looking at.
            button({
                noText()
                onClick = { soloSelection() }
                buttonStyle = {
                    this.default {
                        baseTexture(Sprites.BORDER1_RT1_DARK)
                        hoverTexture(Sprites.BORDER1_RT1)
                    }
                }
                style = { tooltips("editor.machine.machine_fx.solo") }
                layout = {
                    padding { all(0f) }
                    height(100.pct)
                    aspectRatio(1f)
                }
            }) {
                api { addPreIcon(Icons.EYE.copy().setColor(ColorPattern.YELLOW.color).scale(0.65f)) }
            }
            // The same reference geometry Basic Settings has. This is the view that positions things
            // by hand, so it is the one that needed them most.
            toggle(toggleSpec(MBDIcons.CUBE_OUTLINE, ColorPattern.WHITE.color,
                "editor.machine_scene.draw_shape_frame_lines")) {
                bindUIData(shapeToggleData())
            }
            toggle(toggleSpec(MBDIcons.CUBE_OUTLINE, ColorPattern.YELLOW.color,
                "editor.machine_scene.draw_rendering_box_frame_lines")) {
                bindUIData(renderingBoxToggleData())
            }
            toggle(toggleSpec(Icons.TRANSFORM_TRANSLATE, ColorPattern.CYAN.color,
                "editor.machine_scene.draw_axes")) {
                bindUIData(axesToggleData())
            }
        })
        // Editing a value in the inspector restarts the effect it belongs to. Without this, changing
        // the FX id — the first thing anyone does to a new entry — appeared to do nothing at all,
        // because the running effect keeps whatever it was emitted with until something restarts it.
        editor.inspectorView.addEventListener(Configurator.CHANGE_EVENT) {
            if (selectedFX != null) replaySelection()
        }
        // Keys reach the focused element and bubble up from it, so a listener only ever hears its own
        // subtree. Being focusable is what puts this view in that chain at all: a click on anything
        // inside it that is not itself focusable focuses the nearest focusable ancestor. Without this
        // that was the Editor, which sits above every view, and the transport keys reached nothing.
        setFocusable(true)
        addClass("__machine-fx-view__")
        addEventListener(UIEvents.KEY_DOWN) { event ->
            if (timeline.onKeyDown(event)) event.stopPropagation()
        }
        reloadTree()
        // The tree is built from a model two other views also edit, so it has to keep checking rather
        // than trust that everyone who changes a state remembers to tell it. @see syncTreeWithModel
        addEventListener(UIEvents.TICK) { syncTreeWithModel() }
        // Note what is *not* here: a play(). The scene opens stopped at tick 0 and stays there until
        // the author starts it, because a view that begins playing the moment it opens burns its clock
        // on an empty scene and leaves nothing for the transport to do.
    }

    /** The tree, with the live "what is actually running" readout pinned under it. */
    private fun buildLeftColumn(): UIElement {
        scrollerView.layoutDsl {
            width(100.pct)
            flex(1f)
        }
        scrollerView.addScrollViewChild(treeList
            .setStaticTree(true)
            .setFlattenRoot(true)
            .setSupportMultipleSelection(true)
            .setNodeUISupplier { node -> nodeRow(node) }
            .setOnSelectedChanged { selected -> onSelectionChanged(selected) }
        )
        scrollerView.addEventListener(UIEvents.MOUSE_DOWN) { e ->
            if (e.button == 1) {
                val menu = createMenu()
                if (!menu.isEmpty()) {
                    editor.openMenu(e.x, e.y, menu)
                    e.stopPropagation()
                }
            }
        }
        nowPlaying.addClass("__machine-fx-now-playing__")
        nowPlaying.layoutDsl {
            width(100.pct)
            padding { all(2f) }
        }
        nowPlaying.addEventListener(UIEvents.TICK) { refreshNowPlaying() }
        refreshNowPlaying()

        val column = UIElement()
        column.layoutDsl {
            flexDirection(FlexDirection.COLUMN)
            width(100.pct)
            height(100.pct)
        }
        column.addChildren(scrollerView, nowPlaying)
        return column
    }

    /**
     * Names the effects that are actually alive.
     *
     * Just the names: the timeline under the scene owns the clock, and an elapsed figure here would
     * be a second, differently-sourced answer to the same question. It used to read each effect's own
     * {@code TimelinePlayer}, which for the plain emitters most machines use never advances at all —
     * so it sat at a constant and looked broken.
     */
    private fun refreshNowPlaying() {
        val playing = previewMachine?.getFXManager()?.playingIdentifiers().orEmpty()
        if (playing.isEmpty()) {
            nowPlaying.setText(Component.translatable("editor.machine.machine_fx.nothing_playing")
                .withStyle(ChatFormatting.DARK_GRAY))
            return
        }
        nowPlaying.setText(Component.literal(playing.joinToString(", ")))
    }

    /** The scene, with its transport underneath - the controls act on the picture above them. */
    private fun buildScenePane(): UIElement {
        val pane = UIElement()
        pane.layoutDsl {
            flexDirection(FlexDirection.COLUMN)
            width(100.pct)
            height(100.pct)
        }
        sceneEditor.layoutDsl { flex(1f) }
        // Per-tick rather than per-event: see pendingSeek.
        timeline.addEventListener(UIEvents.TICK) {
            flushSeek()
            loopIfPastEnd()
        }
        pane.addChildren(sceneEditor, timeline)
        return pane
    }

    /** Pin the preview's randomness and re-show the tick we were on, so the change is visible at once. */
    private fun setPreviewSeed(seed: Long) {
        previewSeed = seed
        val at = PhotonFXScene.currentTime(particleManager)
        seekTo(at)
    }

    /**
     * Wrap round at the end of the visible span while playing.
     *
     * A preview that runs off the end and sits there empty is no use for judging a looping effect,
     * and the ruler has an end precisely because the zoom gives it one. Restarting rather than
     * stopping is what makes a short effect readable at all.
     */
    private fun loopIfPastEnd() {
        if (!PhotonFXScene.isPlaying(particleManager)) return
        if (selectedFXs.isEmpty()) return
        if (PhotonFXScene.currentTime(particleManager) < timeline.visibleTicks) return
        // seekTo pauses, so playback has to be picked back up — this is a wrap, not a stop.
        seekTo(0)
        PhotonFXScene.play(particleManager)
    }

    private fun togglePlay() {
        if (PhotonFXScene.isPlaying(particleManager)) {
            PhotonFXScene.pause(particleManager)
        } else {
            PhotonFXScene.play(particleManager)
        }
    }

    /** Ask to show tick [time]; applied on the next tick. @see pendingSeek */
    private fun requestSeek(time: Long) {
        pendingSeek = time.coerceAtLeast(0)
    }

    private fun flushSeek() {
        if (pendingSeek < 0) return
        val target = pendingSeek
        pendingSeek = -1
        seekTo(target)
    }

    /**
     * Show the scene at tick [time].
     *
     * Pauses first, because the point of landing on a tick is to look at it - left playing, the scene
     * would run on from wherever the seek put it and the playhead would walk away from the cursor.
     */
    private fun seekTo(time: Long) {
        PhotonFXScene.pause(particleManager)
        PhotonFXScene.simulateTo(particleManager, {
            val manager = previewMachine?.getFXManager()
            if (manager != null) {
                // Pin and rewind the randomness before anything is emitted: that is what makes two
                // seeks to the same tick show the same frame.
                manager.setPreviewSeed(previewSeed)
                manager.resetPreviewRandom()
                for (fx in selectedFXs) {
                    manager.play(fx, fx.name)
                }
            }
        }, time)
    }

    // ---- tree ---------------------------------------------------------------------------------

    private fun libraryList() = project.definition.machineSettings().photonFXs()

    /** Every state in the machine, parents before children. */
    private fun allStates(): List<MachineState> =
        project.definition.stateMachine().rootState.flatten().map { it.key }

    /**
     * One row: an icon and a label that reads its text back from the model every frame.
     *
     * The label is bound rather than set once because the text is derived from data the rest of the
     * view edits — a rename, or toggling a state between owning its list and inheriting one. Set once,
     * those edits left the tree showing the old text until something happened to rebuild it, which is
     * a stale UI that looks like a failed edit.
     */
    private fun nodeRow(node: TreeNode<Row, Void>): UIElement {
        val row = node.key
        val container = UIElement()
        container.layoutDsl {
            flexDirection(FlexDirection.ROW)
            gap { all(2f) }
            height(10f)
            flex(1f)
        }
        val icon = iconOf(row)
        if (icon != IGuiTexture.EMPTY) {
            container.addChild(UIElement().also { element ->
                element.layoutDsl {
                    aspectRatio(1f)
                    height(100.pct)
                }
                element.style { it.backgroundTexture(icon) }
            })
        }
        container.addChild(Label().bindDataSource(SupplierDataSource.of { labelOf(row) }))
        return container
    }

    private fun iconOf(row: Row): IGuiTexture = when (row) {
        is Row.States, is Row.Library -> Icons.FOLDER
        is Row.State -> IGuiTexture.EMPTY
        is Row.Fx -> IGuiTexture.EMPTY
    }

    private fun labelOf(row: Row): Component = when (row) {
        is Row.States -> Component.translatable("editor.machine.machine_fx.states")
        is Row.Library -> Component.translatable("editor.machine.machine_fx.library")
        is Row.State -> stateLabel(row.state)
        is Row.Fx ->
            if (row.inherited) Component.literal(row.fx.name).withStyle(ChatFormatting.DARK_GRAY)
            else Component.literal(row.fx.name)
    }

    /**
     * A state row says where its effects come from, because that is the question the inheritance rule
     * raises and the tree would otherwise leave a guess: an inheriting state shows its parent's list
     * greyed, which looks exactly like owning it.
     */
    private fun stateLabel(state: MachineState): Component {
        val own = state.machineFXs().isEnable
        val label = Component.literal(state.name())
        val source = if (own) {
            Component.translatable("editor.machine.machine_fx.own", state.machineFXs().fxs.size)
        } else {
            val from = inheritedFrom(state)
            if (from == null) Component.translatable("editor.machine.machine_fx.inherited.none")
            else Component.translatable("editor.machine.machine_fx.inherited", from.name())
        }
        return label.append(Component.literal("  ").append(source).withStyle(ChatFormatting.DARK_GRAY))
    }

    /** The nearest ancestor that actually owns a list, or null when nothing up the chain does. */
    private fun inheritedFrom(state: MachineState): MachineState? {
        var parent = state.parent
        while (parent != null) {
            if (parent.machineFXs().isEnable) return parent
            parent = parent.parent
        }
        return null
    }

    /**
     * What the tree is currently a picture of, so a model that moved under it can be spotted.
     *
     * States are added, removed, renamed and reordered in Basic Settings, which has never heard of
     * this view; a tree that only rebuilt on its own context menu went stale the moment anyone touched
     * the state machine, and a renamed state left rows pointing at a [MachineState] the machine no
     * longer has.
     *
     * What goes in is deliberately *structure* — which objects are where — and never the text of a
     * row. Row labels are bound (see [nodeRow]) and already follow an edit with no rebuild at all, so
     * putting a name in here would rebuild the tree on every keystroke of a rename in the inspector,
     * dropping the selection, and with it the inspector, halfway through typing. States are the one
     * exception, because [MachineState.equals] is by name: that is what makes a rename — which
     * replaces the object rather than mutating it — show up, and the identity alongside it covers a
     * state swapped for another of the same name.
     */
    private var treeStructure: List<Any> = emptyList()

    private fun treeStructureOf(): List<Any> {
        val structure = ArrayList<Any>()
        for (state in allStates()) {
            structure.add(state)
            structure.add(System.identityHashCode(state))
            val own = state.machineFXs().isEnable
            structure.add(own)
            structure.addAll(if (own) state.machineFXs().fxs else state.getRealMachineFXs())
        }
        structure.add(Row.Library)
        structure.addAll(libraryList())
        return structure
    }

    /** Rebuild if — and only if — someone changed the model out from under the tree. */
    private fun syncTreeWithModel() {
        if (treeStructureOf() != treeStructure) reloadTree()
    }

    fun reloadTree() {
        val expanded = treeList.getExpandedNodes().mapNotNull { (it.key as? Row.State)?.state?.name }.toSet()
        // Rebuilding clears the selection, and losing it on an edit made elsewhere would silently
        // stop the preview. Rows are value types over identities, so a row that survived the rebuild
        // compares equal to the one that stood for the same thing before it.
        val selected = treeList.getSelected().map { it.key }
        treeList.setRoot(TreeBuilder.start<Row, Void>(Row.States).apply {
            branch(Row.States) { states ->
                for (state in allStates()) {
                    states.branch(Row.State(state)) { node ->
                        val own = state.machineFXs().isEnable
                        val fxs = if (own) state.machineFXs().fxs else state.getRealMachineFXs()
                        for (fx in fxs) {
                            node.leaf(Row.Fx(fx, state, !own), null)
                        }
                    }
                }
            }
            branch(Row.Library) { library ->
                for (fx in libraryList()) {
                    library.leaf(Row.Fx(fx, null), null)
                }
            }
        }.build())
        treeList.root?.let { root ->
            treeList.expandAllNodesIf(root) { node ->
                val key = node.key
                key is Row.States || key is Row.Library
                        || (key is Row.State && expanded.contains(key.state.name))
            }
        }
        treeStructure = treeStructureOf()
        val restored = selected.mapNotNull { row -> findRow { it == row } }
        if (restored.isNotEmpty()) treeList.setSelected(restored, true)
    }

    // ---- selection ----------------------------------------------------------------------------

    /**
     * Drives the preview from whatever is selected.
     *
     * Several effects can be selected at once, because judging one in isolation is not the question an
     * author has — "do these three read as one thing" is. They all play; the first is the one the
     * inspector edits and the gizmo drags, since both of those need a single subject.
     */
    private fun onSelectionChanged(selected: Collection<TreeNode<Row, Void>>) {
        stopSelected()
        selectedFX = null
        selectedFXsOrNull = null
        selectedState = null
        bindGizmo(null)

        val rows = selected.map { it.key }
        val stateRow = rows.filterIsInstance<Row.State>().firstOrNull()
        // Inherited rows are owned by an ancestor; editing one here would edit that state silently.
        val fxRows = rows.filterIsInstance<Row.Fx>().filterNot { it.inherited }

        if (stateRow != null && fxRows.isEmpty()) {
            selectedState = stateRow.state
            // The real path: this starts and stops the state's effects exactly as the world does.
            previewMachine?.setMachineState(stateRow.state.name())
            // The toggle is the inherit/override control — ToggleMachineFXs' only configurator now
            // that its list is @Persisted, which makes it precisely the right thing to inspect.
            editor.inspectorView.inspect(stateRow.state.machineFXs(), null, {
                selectedState = null
            })
            return
        }
        if (fxRows.isEmpty()) {
            if (editor.inspectorView.inspector.inspectedConfigurable != null) {
                editor.inspectorView.clear()
            }
            return
        }

        // Preview a state entry in its own state, so what plays alongside it is what would.
        fxRows.first().owner?.let { previewMachine?.setMachineState(it.name()) }
        selectedFXsOrNull = fxRows.map { it.fx }
        selectedFX = selectedFXs.first()
        editor.inspectorView.inspect(selectedFX, null, {
            selectedFX = null
            selectedFXsOrNull = null
            bindGizmo(null)
        })
        bindGizmo(selectedFX)
        replaySelection()
    }

    // ---- gizmo --------------------------------------------------------------------------------

    /**
     * Drives an effect's offset / rotation / scale from the scene's transform gizmo.
     *
     * The numbers in the inspector are the authored ones, which is to say they are relative to the
     * block's centre and to a north-facing machine — the same space [MachineFXExecutor] reads them in.
     * So this only has to add the half-block that puts the origin at the middle of the machine, and
     * the gizmo is dragging the very value that ships.
     *
     * Rotation is the awkward one: the config stores euler degrees and the gizmo speaks quaternions,
     * and euler → quaternion → euler is not stable — drag a rotation around and the numbers walk. So
     * the quaternion is authoritative for as long as this target is bound and euler is only ever
     * written *out* of it, never read back in.
     */
    private class FXTransform(private val fx: MachineFXConfig) : ITransform {
        /** Authoritative while bound; seeded once from the authored euler. See the class doc. */
        private val liveRotation: Quaternionf = eulerToQuaternion(fx.rotation)

        override fun localPosition(): Vector3f = Vector3f(fx.offset).add(BLOCK_CENTRE)
        override fun localRotation(): Quaternionf = Quaternionf(liveRotation)
        override fun localScale(): Vector3f = fx.scale

        override fun localPosition(localPosition: Vector3f) {
            fx.offset = Vector3f(localPosition).sub(BLOCK_CENTRE)
        }

        override fun localRotation(localRotation: Quaternionf) {
            liveRotation.set(localRotation)
            fx.rotation = quaternionToEuler(liveRotation)
        }

        override fun localScale(localScale: Vector3f) {
            fx.scale = Vector3f(localScale)
        }

        // No parent in the preview scene, so world and local are the same space.
        override fun position(): Vector3f = localPosition()
        override fun rotation(): Quaternionf = localRotation()
        override fun position(position: Vector3f) = localPosition(position)
        override fun rotation(rotation: Quaternionf) = localRotation(rotation)
    }

    private fun bindGizmo(fx: MachineFXConfig?) {
        gizmoTarget = fx
        if (fx == null) {
            sceneEditor.setTransformGizmoTarget(null as ITransform?)
            return
        }
        // Start in translate rather than leaving the gizmo at NONE. Positioning an effect against the
        // machine is what this view is for, so the handles should be on the screen the moment there is
        // something to drag — not one more click away, behind a bar most people never notice.
        if (sceneEditor.transformGizmoMode == TransformGizmo.Mode.NONE) {
            sceneEditor.transformGizmoMode = TransformGizmo.Mode.TRANSLATE
        }
        sceneEditor.setTransformGizmoTarget(FXTransform(fx)) {
            // Restart it so the change is visible immediately: a particle system already in flight
            // keeps the pose it was emitted with, so without this the gizmo appears to do nothing
            // until the effect happens to loop.
            replaySelection()
        }
    }

    /**
     * Restart the selected entry.
     *
     * Stops it first regardless of the entry's own `replace existing` setting: in the editor the
     * button means "show me that again", which is the opposite of the in-world rule where a running
     * effect normally wins.
     */
    private fun replaySelection() {
        if (selectedFXs.isEmpty()) return
        // Back to the top, but *not* back to playing: play/pause is the author's to set, and a replay
        // that starts the clock behind their back is the same complaint as a view that starts itself.
        // clear() only drops particles and resets the clock; it leaves the transport alone.
        PhotonFXScene.clear(particleManager)
        stopSelected()
        val manager = previewMachine?.getFXManager() ?: return
        manager.setPreviewSeed(previewSeed)
        manager.resetPreviewRandom()
        for (fx in selectedFXs) {
            manager.play(fx, fx.name)
        }
    }

    /** Silence everything, then play just the selection. */
    private fun soloSelection() {
        previewMachine?.getFXManager()?.stopAll(true)
        replaySelection()
    }

    private fun stopSelected() {
        val manager = previewMachine?.getFXManager() ?: return
        for (fx in selectedFXs) {
            manager.stop(fx.name, true)
        }
    }

    override fun loadScene() {
        super.loadScene()
        // A fresh preview machine orphans the old one's effects; drop the particles too, or they
        // hang in the scene with nothing left to tick them against.
        PhotonFXScene.clear(particleManager)
        selectedState?.let { previewMachine?.setMachineState(it.name()) }
        replaySelection()
    }

    // ---- editing ------------------------------------------------------------------------------

    /** The list the current selection would add to, and a name that is free within it. */
    private fun targetList(): Pair<MutableList<MachineFXConfig>, String>? {
        val state = selectedState
        val fx = selectedFX
        return when {
            state != null -> {
                // Adding to an inheriting state has to give it a list of its own first, or the entry
                // would be written into the parent's and appear on every sibling.
                if (!state.machineFXs().isEnable) {
                    state.machineFXs().isEnable = true
                }
                state.machineFXs().fxs to "state"
            }
            fx != null -> owningList(fx) ?: (libraryList() to "library")
            else -> libraryList() to "library"
        }
    }

    private fun owningList(fx: MachineFXConfig): Pair<MutableList<MachineFXConfig>, String>? {
        if (libraryList().contains(fx)) return libraryList() to "library"
        for (state in allStates()) {
            if (state.machineFXs().fxs.contains(fx)) return state.machineFXs().fxs to "state"
        }
        return null
    }

    fun createMenu(): TreeBuilder.Menu {
        return TreeBuilder.Menu.start().apply {
            leaf(Icons.ADD, "editor.machine.machine_fx.add", {
                val (list, _) = targetList() ?: return@leaf
                Dialog.stringEditorDialog("editor.machine.machine_fx.name", "new_fx",
                    { name -> isFreeName(list, name) },
                    { name ->
                        if (isFreeName(list, name)) {
                            list.add(MachineFXConfig().apply { this.name = name })
                            reloadTree()
                            // Select it: rebuilding the tree drops the selection, so a freshly added
                            // effect would otherwise sit there playing nothing with no hint why.
                            findRow { it is Row.Fx && it.fx.name == name }?.let { selectRow(it) }
                        }
                    }).show(modularUI)
            })
            selectedFX?.let { fx ->
                val owner = owningList(fx)
                if (owner != null) {
                    val list = owner.first
                    leaf(Icons.REMOVE, "editor.machine.machine_fx.remove", {
                        previewMachine?.getFXManager()?.stop(fx.name, true)
                        list.remove(fx)
                        if (editor.inspectorView.inspector.inspectedConfigurable == fx) {
                            editor.inspectorView.clear()
                        }
                        selectedFX = null
                        reloadTree()
                    })
                    leaf("ldlib.gui.editor.menu.rename", {
                        Dialog.stringEditorDialog("editor.machine.machine_fx.name", fx.name,
                            { name -> name == fx.name || isFreeName(list, name) },
                            { name ->
                                if (name == fx.name || isFreeName(list, name)) {
                                    // The identifier is the name, so the running effect is keyed under
                                    // the old one and would otherwise be unreachable forever.
                                    previewMachine?.getFXManager()?.stop(fx.name, true)
                                    fx.name = name
                                    reloadTree()
                                }
                            }).show(modularUI)
                    })
                    // The two lists answer different questions, so moving between them is a real
                    // authoring step: a state effect that turns out to want firing on demand, or the
                    // reverse. Copy rather than move — the source list may still want it.
                    leaf(Icons.COPY, "editor.machine.machine_fx.copy_to_library", {
                        val copy = copyOf(fx)
                        copy.name = freeName(libraryList(), fx.name)
                        libraryList().add(copy)
                        reloadTree()
                    })
                }
            }
            selectedState?.let { state ->
                leaf("editor.machine.machine_fx.paste_from_library", {
                    val list = state.machineFXs().apply { isEnable = true }.fxs
                    for (fx in libraryList()) {
                        val copy = copyOf(fx)
                        copy.name = freeName(list, fx.name)
                        list.add(copy)
                    }
                    reloadTree()
                })
            }
        }
    }

    /** A field-for-field copy, via the persisted form so a new field cannot be forgotten here. */
    private fun copyOf(fx: MachineFXConfig): MachineFXConfig {
        val provider = com.lowdragmc.lowdraglib2.Platform.getFrozenRegistry()
        return MachineFXConfig().apply { deserializeNBT(provider, fx.serializeNBT(provider)) }
    }

    /** Names are identifiers, so they have to be unique within the list that holds them. */
    private fun isFreeName(list: List<MachineFXConfig>, name: String) =
        name.isNotEmpty() && list.none { it.name == name }

    private fun freeName(list: List<MachineFXConfig>, wanted: String): String {
        if (isFreeName(list, wanted)) return wanted
        var i = 2
        while (!isFreeName(list, "${wanted}_$i")) i++
        return "${wanted}_$i"
    }

    companion object {
        /** An effect's offset is measured from the middle of the machine's block, not its corner. */
        private val BLOCK_CENTRE = Vector3f(0.5f, 0.5f, 0.5f)

        /** Photon's own convention — see {@code IFXEffectExecutor.setRotation(x, y, z)}. */
        private fun eulerToQuaternion(euler: Vector3f): Quaternionf = Quaternionf().rotationXYZ(
            Math.toRadians(euler.x.toDouble()).toFloat(),
            Math.toRadians(euler.y.toDouble()).toFloat(),
            Math.toRadians(euler.z.toDouble()).toFloat()
        )

        private fun quaternionToEuler(quaternion: Quaternionf): Vector3f {
            val radians = quaternion.getEulerAnglesXYZ(Vector3f())
            return Vector3f(
                Math.toDegrees(radians.x.toDouble()).toFloat(),
                Math.toDegrees(radians.y.toDouble()).toFloat(),
                Math.toDegrees(radians.z.toDouble()).toFloat()
            )
        }
    }

    /** The first node whose row satisfies [predicate]. Walks the built tree rather than rebuilding it. */
    fun findRow(predicate: (Row) -> Boolean): TreeNode<Row, Void>? {
        fun walk(node: TreeNode<Row, Void>): TreeNode<Row, Void>? {
            if (predicate(node.key)) return node
            for (child in node.children) {
                walk(child as TreeNode<Row, Void>)?.let { return it }
            }
            return null
        }
        return treeList.root?.let { walk(it) }
    }

    // ---- queries (also the surface the UI test drives) -----------------------------------------

    /** The row for a library entry called [name]. */
    fun findLibraryRow(name: String): TreeNode<Row, Void>? =
        findRow { it is Row.Fx && it.owner == null && it.fx.name == name }

    /**
     * The row for an effect shown under [stateName]. [inherited] picks between the state's own entry
     * and one it is only displaying on its parent's behalf — which is the distinction the tree exists
     * to make visible.
     */
    fun findStateRow(stateName: String, inherited: Boolean): TreeNode<Row, Void>? =
        findRow { it is Row.Fx && it.owner?.name() == stateName && it.inherited == inherited }

    /** The row for the state itself. */
    fun findStateRow(stateName: String): TreeNode<Row, Void>? =
        findRow { it is Row.State && it.state.name() == stateName }

    /** Select [node] exactly as clicking it would. */
    fun selectRow(node: TreeNode<Row, Void>) {
        treeList.setSelected(listOf(node), true)
    }

    /** Drive a seek from a UI test, exactly as dragging the playhead does. */
    fun scrubToForTest(ticks: Int) {
        requestSeek(ticks.toLong())
        flushSeek()
    }

    /** The scene clock, in ticks - what the timeline's readout and playhead show. */
    fun sceneTimeForTest(): Long = PhotonFXScene.currentTime(particleManager)

    /** Whether the transport is running. False until the author starts it. */
    fun isPlayingForTest(): Boolean = PhotonFXScene.isPlaying(particleManager)

    /** Drive the seed from a UI test, exactly as typing one or pressing reseed does. */
    fun setPreviewSeedForTest(seed: Long) = setPreviewSeed(seed)

    fun previewSeedForTest(): Long = previewSeed

    /** The visible span of the ruler, in ticks - what the zoom changes. */
    fun visibleTicksForTest(): Int = timeline.visibleTicks
}
