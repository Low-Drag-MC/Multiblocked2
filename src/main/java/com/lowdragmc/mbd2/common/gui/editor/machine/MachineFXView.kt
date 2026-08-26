package com.lowdragmc.mbd2.common.gui.editor.machine

import com.lowdragmc.lowdraglib2.client.scene.ParticleManager
import com.lowdragmc.lowdraglib2.gui.ColorPattern
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture
import com.lowdragmc.lowdraglib2.gui.texture.Icons
import com.lowdragmc.lowdraglib2.gui.ui.default
import com.lowdragmc.lowdraglib2.gui.ui.element
import com.lowdragmc.lowdraglib2.gui.ui.elements.*
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents
import com.lowdragmc.lowdraglib2.gui.ui.layout.pct
import com.lowdragmc.lowdraglib2.gui.ui.layoutDsl
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder
import com.lowdragmc.lowdraglib2.gui.util.TreeNode
import com.lowdragmc.mbd2.common.gui.editor.MBDEditor
import com.lowdragmc.mbd2.common.gui.editor.MachineProject
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState
import com.lowdragmc.mbd2.common.machine.definition.config.fx.MachineFXConfig
import com.lowdragmc.mbd2.integration.photon.PhotonFXScene
import dev.vfyjxf.taffy.style.AlignContent
import dev.vfyjxf.taffy.style.FlexDirection
import net.minecraft.network.chat.Component

/**
 * Authors and previews a machine's Photon effects.
 *
 * The left pane is the machine-level named library
 * ([com.lowdragmc.mbd2.common.machine.definition.config.ConfigMachineSettings.photonFXs]); the right
 * is the shared preview machine with a real Photon particle host, so an entry looks here the way it
 * will look in the world.
 *
 * Per-state effects are authored on the state itself, over in Basic Settings — but they are
 * previewed here too, through the state selector in the top bar. Switching the preview machine's
 * state runs the very same `syncStateFX` path the in-world machine uses, so there is one behaviour
 * to get right rather than an editor-only imitation of it.
 *
 * Only placed when Photon is installed (see [MachineProject.onLoad]) — the configuration still
 * round-trips through NBT without it, there is simply nothing to show.
 */
open class MachineFXView(editor: MBDEditor, project: MachineProject) :
    MachineSceneView("editor.machine.machine_fx", editor, project, PhotonFXScene.createParticleManager()) {

    private val scrollerView = ScrollerView()
    private val treeList = TreeList<TreeNode<MachineFXConfig, Void>>()

    /**
     * The scene's Photon host — read back off the level rather than cached, so there is one source
     * of truth. Null without Photon; every [PhotonFXScene] call tolerates that.
     */
    val particleManager: ParticleManager? get() = level.particleManager

    // runtime
    var selectedFX: MachineFXConfig? = null
        protected set

    init {
        addChild(
            splitViewHorizontal {
                withPercentage(25f)
                withLeft(scrollerView.apply {
                    layoutDsl {
                        width(100.pct)
                        flex(1f)
                    }
                    addScrollViewChild(treeList
                        .setStaticTree(true)
                        .setFlattenRoot(true)
                        .setSupportMultipleSelection(false)
                        .setNodeUISupplier(TreeList.optionalIconTextTemplate(
                            { IGuiTexture.EMPTY },
                            { node -> Component.literal(node.key.name) }))
                        .setOnSelectedChanged { selected ->
                            if (selected.size == 1) {
                                val node = selected.first()
                                val fx = node.key
                                if (fx == selectedFX) return@setOnSelectedChanged
                                stopSelected()
                                editor.inspectorView.inspect(fx, null, {
                                    treeList.removeSelected(node, false)
                                    selectedFX = null
                                })
                                selectedFX = fx
                                replaySelected()
                            } else {
                                if (selectedFX == null) return@setOnSelectedChanged
                                if (editor.inspectorView.inspector.inspectedConfigurable == selectedFX) {
                                    editor.inspectorView.clear()
                                }
                                stopSelected()
                                selectedFX = null
                            }
                        }
                    )
                    addEventListener(UIEvents.MOUSE_DOWN, { e ->
                        if (e.button == 1) {
                            val menu = createMenu()
                            if (!menu.isEmpty()) {
                                editor.openMenu(e.x, e.y, menu)
                                e.stopPropagation()
                            }
                        }
                    })
                })
                withRight(sceneEditor)
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
            // Which machine state to preview. Goes through setMachineState so the per-state effect
            // lists start and stop exactly as they do in the world.
            selector<MachineState>({
                candidates(allStates())
                selected(project.definition.stateMachine().rootState)
                // Null-tolerant on purpose: Selector.setCandidates re-runs the provider for its own
                // preview slot before anything is selected, so it is called with null exactly once.
                candidateUI { state ->
                    Label().setText(Component.literal(state?.name() ?: ""))
                }
                onChange { state -> state?.let { previewMachine?.setMachineState(it.name()) } }
                style = { tooltips("editor.machine.machine_fx.preview_state") }
                layout = {
                    height(100.pct)
                    width(70f)
                }
            })
            // Replay the selected library entry from the top.
            button({
                noText()
                onClick = { replaySelected() }
                buttonStyle = {
                    this.default {
                        baseTexture(Sprites.BORDER1_RT1_DARK)
                        hoverTexture(Sprites.BORDER1_RT1)
                    }
                }
                style = { tooltips("editor.machine.machine_fx.replay") }
                layout = {
                    padding { all(0f) }
                    height(100.pct)
                    aspectRatio(1f)
                }
            }) {
                api { addPreIcon(Icons.REPLAY.copy().setColor(ColorPattern.CYAN.color).scale(0.65f)) }
            }
            // Photon's manager does not tick unless it is playing; on by default.
            toggle(toggleSpec(Icons.PLAY, ColorPattern.GREEN.color, "editor.machine.machine_fx.play")) {
                api {
                    setOnToggleChanged { on ->
                        if (on) PhotonFXScene.play(particleManager) else PhotonFXScene.pause(particleManager)
                    }
                    setOn(true, false)
                }
            }
        })
        PhotonFXScene.play(particleManager)
        reloadFXList()
    }

    /** Every state in the machine, parents before children, so the selector reads as the tree does. */
    private fun allStates(): List<MachineState> =
        project.definition.stateMachine().rootState.flatten().map { it.key }

    /**
     * Restart the selected entry.
     *
     * Stops it first regardless of the entry's own `replace existing` setting: in the editor the
     * button means "show me that again", which is the opposite of the in-world rule where a running
     * effect normally wins.
     */
    private fun replaySelected() {
        val fx = selectedFX ?: return
        stopSelected()
        previewMachine?.getFXManager()?.play(fx, fx.name)
    }

    private fun stopSelected() {
        val fx = selectedFX ?: return
        previewMachine?.getFXManager()?.stop(fx.name, true)
    }

    override fun loadScene() {
        super.loadScene()
        // A fresh preview machine orphans the old one's effects; drop the particles too, or they
        // hang in the scene with nothing left to tick them against.
        PhotonFXScene.clear(particleManager)
        selectedFX?.let { previewMachine?.getFXManager()?.play(it, it.name) }
    }

    private fun fxList() = project.definition.machineSettings().photonFXs()

    fun reloadFXList() {
        treeList.setRoot(TreeBuilder.start<MachineFXConfig, Void>(null).apply {
            for (fx in fxList()) {
                leaf(fx, null)
            }
        }.build())
    }

    fun createMenu(): TreeBuilder.Menu {
        return TreeBuilder.Menu.start().apply {
            leaf(Icons.ADD, "editor.machine.machine_fx.add", {
                Dialog.stringEditorDialog("editor.machine.machine_fx.name", "new_fx",
                    { name -> isFreeName(name) },
                    { name ->
                        if (isFreeName(name)) {
                            fxList().add(MachineFXConfig().apply { this.name = name })
                            reloadFXList()
                        }
                    }).show(modularUI)
            })
            selectedFX?.let { fx ->
                leaf(Icons.REMOVE, "editor.machine.machine_fx.remove", {
                    previewMachine?.getFXManager()?.stop(fx.name, true)
                    fxList().remove(fx)
                    if (editor.inspectorView.inspector.inspectedConfigurable == fx) {
                        editor.inspectorView.clear()
                    }
                    selectedFX = null
                    reloadFXList()
                })
                leaf("ldlib.gui.editor.menu.rename", {
                    Dialog.stringEditorDialog("editor.machine.machine_fx.name", fx.name,
                        { name -> isFreeName(name) },
                        { name ->
                            if (isFreeName(name)) {
                                // The identifier is the name, so the running effect is keyed under
                                // the old one and would otherwise be unreachable forever.
                                previewMachine?.getFXManager()?.stop(fx.name, true)
                                fx.name = name
                                reloadFXList()
                            }
                        }).show(modularUI)
                })
            }
        }
    }

    /** Names are identifiers, so they have to be unique within the machine. */
    private fun isFreeName(name: String) = name.isNotEmpty() && fxList().none { it.name == name }
}
