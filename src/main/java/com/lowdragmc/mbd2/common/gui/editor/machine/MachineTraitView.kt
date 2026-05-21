package com.lowdragmc.mbd2.common.gui.editor.machine

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture
import com.lowdragmc.lowdraglib2.gui.texture.Icons
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical
import com.lowdragmc.lowdraglib2.gui.ui.dsl
import com.lowdragmc.lowdraglib2.gui.ui.element
import com.lowdragmc.lowdraglib2.gui.ui.elements.*
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents
import com.lowdragmc.lowdraglib2.gui.ui.layout.pct
import com.lowdragmc.lowdraglib2.gui.ui.layoutDsl
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder
import com.lowdragmc.lowdraglib2.gui.util.TreeNode
import com.lowdragmc.mbd2.api.registry.MBDRegistries
import com.lowdragmc.mbd2.common.gui.editor.MBDEditor
import com.lowdragmc.mbd2.common.gui.editor.MachineProject
import com.lowdragmc.mbd2.common.trait.ITrait
import com.lowdragmc.mbd2.common.trait.TraitDefinition
import dev.vfyjxf.taffy.style.FlexDirection
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.network.chat.Component

open class MachineTraitView(editor: MBDEditor, project: MachineProject) : MachineSceneView("editor.machine.machine_traits", editor, project) {
    val scrollerView = scrollerView()
    val treeList = TreeList<TreeNode<TraitDefinition, Void>>()
    // runtime
    var selectedTrait: TraitDefinition? = null
        protected set

    private data class DraggingTrait(val draggedNode: TreeNode<TraitDefinition, Void>)
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
                        .setClickToExpand(true)
                        .setDoubleClickToExpand(false)
                        .setSupportMultipleSelection(false)
                        .setNodeUISupplier({ node ->
                            UIElement().dsl({
                                layout = {
                                    flexDirection(FlexDirection.ROW)
                                    flex(1)
                                    gap { all(2f) }
                                    height(10)
                                }
                            }) {
                                element({
                                    layout = {
                                        aspectRatio(1f)
                                        height(100.pct)
                                    }
                                    style = {
                                        background(node.key.icon)
                                    }
                                })
                                label({
                                    layout = {
                                        flex(1f)
                                        height(100.pct)
                                    }
                                    textStyle = {
                                        textWrap(TextWrap.HOVER_ROLL)
                                        textAlignVertical(Vertical.CENTER)
                                    }
                                }) {
                                    element.setOverflowVisible(false)
                                    dataSource { Component.literal(node.key.name) }
                                }
                            }.build()
                        })
                        .setOnNodeUICreated { node, nodeUI ->
                            nodeUI.dsl(null){
                                events {
                                    UIEvents.DRAG_END += {
                                        it!!.currentElement.style.overlayTexture(IGuiTexture.EMPTY)
                                    }
                                    UIEvents.DRAG_UPDATE += {
                                        val obj = it!!.dragHandler.getDraggingObject<Any?>()
                                        if (obj is DraggingTrait && obj.draggedNode !== node) {
                                            val mode = when {
                                                TreeList.isMouseOverNodeAbove(it) -> 0
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
                                        if (obj is DraggingTrait && obj.draggedNode !== node) {
                                            val target = node.key
                                            val toMoved = obj.draggedNode.key
                                            val definitions = project.definition.machineSettings().traitDefinitions()
                                            val targetIndex = definitions.indexOf(target)
                                            if (targetIndex < 0) {
                                                return@UIEventListener
                                            }
                                            if (TreeList.isMouseOverNodeAbove(it)) {
                                                project.definition.machineSettings().moveTraitDefinition(toMoved, targetIndex)
                                                reloadTraits()
                                            } else if (TreeList.isMouseOverNodeBelow(it)) {
                                                project.definition.machineSettings().moveTraitDefinition(toMoved, targetIndex + 1)
                                                reloadTraits()
                                            }
                                            // expanded nodes
                                        }
                                    }
                                }
                                events(true) {
                                    UIEvents.MOUSE_LEAVE += {
                                        if (nodeUI.isAncestorOf(modularUI?.lastMouseDownElement) && isMouseDown(0) && treeList.getSelected().size == 1) {
                                            nodeUI.startDrag(
                                                DraggingTrait(node),
                                                TextTexture(node.key.name())
                                            )
                                        }
                                    }
                                    UIEvents.DRAG_ENTER += {
                                        val obj = it!!.dragHandler.getDraggingObject<Any?>()
                                        if (obj is DraggingTrait && obj.draggedNode !== node) {
                                            val mode = when {
                                                TreeList.isMouseOverNodeAbove(it) -> 0
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
                        .setOnSelectedChanged { selectedTraits ->
                            if (selectedTraits.size == 1) {
                                val node = selectedTraits.first()
                                val trait = node.key
                                if (trait == selectedTrait) return@setOnSelectedChanged
                                editor.inspectorView.inspect(trait, null, {
                                    treeList.removeSelected(node, false)
                                    selectedTrait = null
                                })
                                selectedTrait = trait
                            } else {
                                if (selectedTrait == null) return@setOnSelectedChanged
                                if (editor.inspectorView.inspector.inspectedConfigurable == selectedTrait) {
                                    editor.inspectorView.clear()
                                }
                                selectedTrait = null
                            }
                        }
                    )
                    addEventListener(UIEvents.MOUSE_DOWN, { e ->
                        if (e.button == 1) {
                            var menu = createMenu()
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
        reloadTraits()
    }

    override fun loadScene() {
        super.loadScene()
        reloadAdditionalTraits()
    }

    /**
     * Reloads and processes additional traits of the preview machine.
     *
     * This function ensures that the preview machine loads its additional traits by invoking the
     * `loadAdditionalTraits` method on `previewMachine`. Once the traits are loaded, the function
     * iterates over each trait and triggers their `onLoadingTraitInPreview` method. This allows
     * each trait to perform any necessary actions required during the preview loading process.
     */
    open fun reloadAdditionalTraits() {
        previewMachine?.loadAdditionalTraits();
        previewMachine?.additionalTraits?.forEach(ITrait::onLoadingTraitInPreview)
    }

    fun reloadTraits() {
        // expanded nodes
        val expandedNodes = treeList.getExpandedNodes().map { node -> node.key }.toSet()
        treeList.setRoot(TreeBuilder.start<TraitDefinition, Void>(null).apply {
            for (definition in project.definition.machineSettings().traitDefinitions()) {
                leaf(definition, null)
            }
        }.build())
        treeList.expandAllNodesIf(treeList.root!!) { node ->
            expandedNodes.contains(node.key)
        }
        reloadAdditionalTraits()
    }

    override fun renderAfterWorld(bufferSource: MultiBufferSource, partialTicks: Float) {
        selectedTrait?.renderInEditor(bufferSource, partialTicks)
    }

    fun createMenu(): TreeBuilder.Menu {
        return TreeBuilder.Menu.start().apply {
            branch(Icons.ADD, "editor.machine.machine_traits.add_trait", { it.apply {
                for (holder in MBDRegistries.TRAIT_DEFINITION_TYPES) {
                    holder.value.get().apply {
                        leaf(icon, holder.annotation.name, {
                            project.definition.machineSettings().addTraitDefinition(this)
                            reloadTraits()
                        })
                    }
                }
            }})
            selectedTrait?.let { trait ->
                leaf(Icons.REMOVE, "editor.machine.machine_traits.remove_trait", {
                    project.definition.machineSettings().removeTraitDefinition(trait)
                    reloadTraits()
                })
            }
        }
    }
}
