package com.lowdragmc.mbd2.common.gui.editor.machine

import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderTypes
import com.lowdragmc.lowdraglib2.client.utils.RenderBufferUtils
import com.lowdragmc.lowdraglib2.gui.ColorPattern
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.TrackData
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.getValue
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.setValue
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture
import com.lowdragmc.lowdraglib2.gui.texture.Icons
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.default
import com.lowdragmc.lowdraglib2.gui.ui.dsl
import com.lowdragmc.lowdraglib2.gui.ui.element
import com.lowdragmc.lowdraglib2.gui.ui.elements.*
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents
import com.lowdragmc.lowdraglib2.gui.ui.layout.pct
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder
import com.lowdragmc.lowdraglib2.utils.ColorUtils
import com.lowdragmc.mbd2.MBD2
import com.lowdragmc.mbd2.common.gui.editor.MBDEditor
import com.lowdragmc.mbd2.common.gui.editor.MachineProject
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState
import com.mojang.blaze3d.vertex.PoseStack
import dev.vfyjxf.taffy.style.AlignContent
import dev.vfyjxf.taffy.style.AlignItems
import dev.vfyjxf.taffy.style.FlexDirection
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.network.chat.Component
import org.joml.Vector3f

open class MachineConfigView(editor: MBDEditor, project: MachineProject) : MachineSceneView("editor.machine.basic_settings", editor, project) {
    val machineStateHierarchy: MachineStateHierarchy = MachineStateHierarchy()
    private val drawShapeData: TrackData<Boolean> = TrackData(true)
    private val drawRenderingBoxData: TrackData<Boolean> = TrackData(false)
    private val drawAxesData: TrackData<Boolean> = TrackData(false)
    var isDrawShape by drawShapeData
    var isDrawRenderingBox by drawRenderingBoxData
    var isDrawAxes by drawAxesData

    // runtime
    var selectedState: MachineState? = null
        protected set

    init {
        this.dsl({}) {
            splitViewHorizontal {
                withPercentage(25f)
                withLeft(machineStateHierarchy)
                withRight(sceneEditor)
            }
        }
        // toggles
        sceneEditor.topBar.addChild(element({
            layout = {
                flexDirection(FlexDirection.ROW)
                justifyContent(AlignContent.FLEX_END)
                height(100.pct)
                flex(1f)
            }
        }) {
            // draw shape
            toggle(toggleSpec(Icons.icon(MBD2.MOD_ID, "cube_outline"), ColorPattern.WHITE.color, "editor.machine_scene.draw_shape_frame_lines")) {
                bindUIData(drawShapeData)
            }
            // draw rendering box
            toggle(toggleSpec(Icons.icon(MBD2.MOD_ID, "cube_outline"), ColorPattern.YELLOW.color, "editor.machine_scene.draw_rendering_box_frame_lines")) {
                bindUIData(drawRenderingBoxData)
            }
            // draw XYZ axes
            toggle(toggleSpec(Icons.TRANSFORM_TRANSLATE, ColorPattern.CYAN.color, "editor.machine_scene.draw_axes")) {
                bindUIData(drawAxesData)
            }
        })
        // inspect by default
        machineStateHierarchy.configToggle.isOn = true
    }

    override fun renderAfterWorld(
        bufferSource: MultiBufferSource,
        partialTicks: Float
    ) {
        previewMachine?.let { machine ->
            if (isDrawShape) {
                val buffer = bufferSource.getBuffer(LDLibRenderTypes.noDepthLines())
                machine.machineState.getShape(null).forAllEdges { x0, y0, z0, x1, y1, z1 ->
                    val normal = Vector3f((x1 - x0).toFloat(), (y1 - y0).toFloat(), (z1 - z0).toFloat()).normalize()
                    buffer.addVertex(x0.toFloat(), y0.toFloat(), z0.toFloat())
                        .setColor(-1)
                        .setNormal(normal.x, normal.y, normal.z);
                    buffer.addVertex(x1.toFloat(), y1.toFloat(), z1.toFloat())
                        .setColor(-1)
                        .setNormal(normal.x, normal.y, normal.z);
                }
            }
            if (isDrawRenderingBox) {
                val aabb = machine.machineState.getRenderingBox(null);
                if (aabb != null) {
                    val color = 0xffeedd00.toInt()
                    val buffer = bufferSource.getBuffer(LDLibRenderTypes.noDepthLines())
                    RenderBufferUtils.drawCubeFrame(PoseStack(), buffer,
                        aabb.minX.toFloat(),
                        aabb.minY.toFloat(),
                        aabb.minZ.toFloat(),
                        aabb.maxX.toFloat(),
                        aabb.maxY.toFloat(),
                        aabb.maxZ.toFloat(),
                        ColorUtils.red(color),
                        ColorUtils.green(color),
                        ColorUtils.blue(color),
                        ColorUtils.alpha(color)
                    )
                }
            }
            if (isDrawAxes) {
                val buffer = bufferSource.getBuffer(LDLibRenderTypes.noDepthLines())
                // X axis (red), Y axis (green), Z axis (blue) — one block long from the origin.
                buffer.addVertex(0f, 0f, 0f).setColor(0xffff5555.toInt()).setNormal(1f, 0f, 0f)
                buffer.addVertex(100f, 0f, 0f).setColor(0xffff5555.toInt()).setNormal(1f, 0f, 0f)
                buffer.addVertex(0f, 0f, 0f).setColor(0xff55ff55.toInt()).setNormal(0f, 1f, 0f)
                buffer.addVertex(0f, 100f, 0f).setColor(0xff55ff55.toInt()).setNormal(0f, 1f, 0f)
                buffer.addVertex(0f, 0f, 0f).setColor(0xff5555ff.toInt()).setNormal(0f, 0f, 1f)
                buffer.addVertex(0f, 0f, 100f).setColor(0xff5555ff.toInt()).setNormal(0f, 0f, 1f)
            }
        }
    }

    private data class DraggingMachineState(val draggedNode: MachineState)
    inner class MachineStateHierarchy : UIElement() {
        val configToggle: Toggle = Toggle()
        val scrollerView: ScrollerView = ScrollerView()
        val treeList: TreeList<MachineState> = TreeList<MachineState>()

        init {
            layout.widthPercent(100f).heightPercent(100f)
            configToggle.apply {
                noText()
                toggleStyle.default {
                    baseTexture(Sprites.RECT_RD)
                    hoverTexture(Sprites.RECT_RD_T_SOLID)
                    unmarkTexture(IGuiTexture.EMPTY)
                    markTexture(Sprites.RECT_RD_T_SOLID)
                }
                toggleButton.layout.aspectRatioAuto().widthPercent(100f)
                markIcon.dsl({layout = {
                    flexDirection(FlexDirection.ROW);
                    alignItems(AlignItems.CENTER)
                    gap { all(2f) }
                    padding { horizontal(2f) }
                } }) {
                    api {
                        setOverflowVisible(false)
                    }
                    element({
                        layout = {
                            height(90.pct)
                            aspectRatio(1f)
                        }
                        style = {background(Icons.SETTINGS) }
                    })
                    label({
                        layout = {
                            flex(1)
                        }
                        text("config.definition.machine_settings")
                    })
                }.build()
                bindObserver { on ->
                    if (on) {
                        if (editor.inspectorView.inspector.inspectedConfigurable != project.definition) {
                            editor.inspectorView.inspect(project.definition, null, {
                                setOn(false, false)
                            })
                        }
                    } else {
                        setOn(true, false)
                    }
                }
            }.addClass("__ui-editor-view_builtin-styles-toggle__")
            scrollerView.layout.widthPercent(100f).flex(1f)
            scrollerView.addScrollViewChild(treeList
                .setStaticTree(true)
                .setDoubleClickToExpand(true)
                .setSupportMultipleSelection(false)
                .setNodeUISupplier(TreeList.optionalIconTextTemplate({ IGuiTexture.EMPTY}, { Component.literal(it.name) }))
                .setOnNodeUICreated { node, nodeUI ->
                    nodeUI.dsl(null){
                        events {
                            UIEvents.DRAG_END += {
                                it!!.currentElement.style.overlayTexture(IGuiTexture.EMPTY)
                            }
                            UIEvents.DRAG_UPDATE += {
                                val obj = it!!.dragHandler.getDraggingObject<Any?>()
                                if (obj is DraggingMachineState && obj.draggedNode !== node) {
                                    val mode = when {
                                        TreeList.isMouseOverNodeAbove(it) -> 0
                                        TreeList.isMouseOverNodeCenter(it) -> 1
                                        TreeList.isMouseOverNodeBelow(it) -> 2
                                        else -> -1
                                    }
                                    it.currentElement.style.overlayTexture(TreeList.createDraggingOverlay(mode))
                                } else {
                                    it.currentElement.style.overlayTexture(IGuiTexture.EMPTY)
                                }
                            }
                            UIEvents.DRAG_PERFORM += UIEventListener {
                                it!!.currentElement.style.overlayTexture(IGuiTexture.EMPTY)
                                val obj = it.dragHandler.getDraggingObject<Any?>()
                                if (obj is DraggingMachineState && obj.draggedNode !== node) {
                                    val target = node.key
                                    val toMoved = obj.draggedNode.key
                                    var parent = target.parent
                                    while (parent != null) {
                                        if (parent == toMoved) {
                                            return@UIEventListener
                                        }
                                        parent = parent.parent
                                    }

                                    var toMovedParent: MachineState = toMoved.parent ?: return@UIEventListener
                                    if (TreeList.isMouseOverNodeAbove(it)) {
                                        if (target.parent == null) return@UIEventListener
                                        toMovedParent.removeChild(toMoved)
                                        target.parent!!.addChildAt(toMoved,
                                            target.parent!!.getChildSiblingIndex(target))
                                    } else if (TreeList.isMouseOverNodeCenter(it)) {
                                        toMovedParent.removeChild(toMoved)
                                        target.addChild(toMoved)
                                    } else if (TreeList.isMouseOverNodeBelow(it)) {
                                        if (target.parent == null) return@UIEventListener
                                        toMovedParent.removeChild(toMoved)
                                        target.parent!!.addChildAt(toMoved,
                                            target.parent!!.getChildSiblingIndex(target) + 1)
                                    }
                                    reloadStateTree()
                                }
                            }
                        }
                        events(true) {
                            UIEvents.MOUSE_LEAVE += {
                                if (nodeUI.isAncestorOf(modularUI?.lastMouseDownElement) && isMouseDown(0) && treeList.getSelected().size == 1) {
                                    nodeUI.startDrag(
                                        DraggingMachineState(node),
                                        TextTexture(node.key.name())
                                    )
                                }
                            }
                            UIEvents.DRAG_ENTER += {
                                val obj = it!!.dragHandler.getDraggingObject<Any?>()
                                if (obj is DraggingMachineState && obj.draggedNode !== node) {
                                    val mode = when {
                                        TreeList.isMouseOverNodeAbove(it) -> 0
                                        TreeList.isMouseOverNodeCenter(it) -> 1
                                        TreeList.isMouseOverNodeBelow(it) -> 2
                                        else -> -1
                                    }
                                    it.currentElement.style.overlayTexture(TreeList.createDraggingOverlay(mode))
                                }
                            }
                            UIEvents.DRAG_LEAVE += {
                                it!!.currentElement.style.overlayTexture(IGuiTexture.EMPTY)
                            }
                        }
                    }.build()
                }
                .setOnSelectedChanged { selectedStates ->
                    if (selectedStates.size == 1) {
                        val state = selectedStates.first()
                        if (state == selectedState) return@setOnSelectedChanged
                        editor.inspectorView.inspect(state, null, {
                            treeList.removeSelected(state, false)
                            selectedState = null
                            previewMachine?.setMachineState(project.definition.stateMachine().rootState.name())
                        })
                        selectedState = state
                        previewMachine?.setMachineState(state.name())
                    } else {
                        if (selectedState == null) return@setOnSelectedChanged
                        if (editor.inspectorView.inspector.inspectedConfigurable == selectedState) {
                            editor.inspectorView.clear()
                        }
                        selectedState = null
                        previewMachine?.setMachineState(project.definition.stateMachine().rootState.name())
                    }
                }
                .setRoot(this@MachineConfigView.project.definition.stateMachine().rootState)
            )
            scrollerView.addEventListener(UIEvents.MOUSE_DOWN, { e ->
                if (e.button == 1) {
                    var menu = createMenu()
                    if (!menu.isEmpty()) {
                        editor.openMenu(e.x, e.y, menu)
                        e.stopPropagation()
                    }
                }
            })
            addChildren(configToggle, scrollerView)
        }

        fun reloadStateTree() {
            // expanded nodes
            val expandedNodes = treeList.getExpandedNodes().map { state -> state.name }.toSet()
            treeList.reloadList()
            treeList.expandAllNodesIf(treeList.root!!) { state ->
                expandedNodes.contains(state.name)
            }
        }

        fun createMenu(): TreeBuilder.Menu {
            return TreeBuilder.Menu.start().apply {
                leaf(Icons.ADD, "editor.machine_state.add", {
                    Dialog.stringEditorDialog("editor.machine_state.name", "new_state", {
                        // should be unique
                        !this@MachineConfigView.project.definition.stateMachine().hasState(it)
                    }, {
                        if (!this@MachineConfigView.project.definition.stateMachine().hasState(it)) {
                            val parent = selectedState ?: this@MachineConfigView.project.definition.stateMachine().rootState
                            parent.addChild(it)
                            reloadStateTree()
                        }
                    }).show(modularUI)
                })
                crossLine()
                selectedState?.let { state ->
                    state.parent()?.let { parent ->
                        leaf(Icons.REMOVE, "editor.machine_state.remove", {
                            parent.removeChild(state)
                            reloadStateTree()
                        })
                        leaf("ldlib.gui.editor.menu.rename", {
                            Dialog.stringEditorDialog("editor.machine_state.name", "new_state", {
                                // should be unique
                                !this@MachineConfigView.project.definition.stateMachine().hasState(it)
                            }, {
                                parent.removeChild(state)
                                parent.addChild(it).also { newChild ->
                                    state.children().forEach { child ->
                                        newChild.addChild(child)
                                    }
                                }
                                reloadStateTree()
                            }).show(modularUI)
                        })
                    }
                }
            }
        }
    }
}
