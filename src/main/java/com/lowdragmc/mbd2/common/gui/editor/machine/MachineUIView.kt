package com.lowdragmc.mbd2.common.gui.editor.machine

import com.lowdragmc.lowdraglib2.gui.editor.view.UIEditorView
import com.lowdragmc.lowdraglib2.gui.editor.view.UITreeNode
import com.lowdragmc.lowdraglib2.gui.texture.Icons
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate
import com.lowdragmc.lowdraglib2.gui.ui.element
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label
import com.lowdragmc.lowdraglib2.gui.ui.elements.button
import com.lowdragmc.lowdraglib2.gui.ui.elements.dsl
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots
import com.lowdragmc.lowdraglib2.gui.ui.layout.px
import com.lowdragmc.lowdraglib2.gui.ui.layoutDsl
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder
import com.lowdragmc.mbd2.api.capability.recipe.IO
import com.lowdragmc.mbd2.common.gui.editor.MBDEditor
import com.lowdragmc.mbd2.common.gui.editor.MachineProject
import com.lowdragmc.mbd2.common.trait.IUIProviderTrait
import com.lowdragmc.mbd2.common.trait.IUIProviderTrait.TraitUILayoutType
import com.lowdragmc.mbd2.common.trait.RecipeCapabilityTraitDefinition
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTraitDefinition
import dev.vfyjxf.taffy.style.AlignItems
import dev.vfyjxf.taffy.style.FlexDirection
import dev.vfyjxf.taffy.style.FlexWrap

open class MachineUIView(val mbdEditor: MBDEditor, val project: MachineProject) : UIEditorView() {

    companion object {
        const val ID_INPUT = "trait_input"
        const val ID_CENTER = "trait_center"
        const val ID_OUTPUT = "trait_output"
        const val ID_BAR = "trait_bar"
    }

    init {
        icon = Icons.WIDGET_BASIC
        name = "editor.machine.machine_ui"

        saveButton.setDisplay(false)

        loadTemplate(project.definition.machineSettings().uiTemplate()) {
            project.definition.machineSettings().uiTemplate().apply {
                data = it.data.copy()
                copyStylesFrom(it)
            }
        }

        // add trait UI generation button to header right side
        header.addChild(button({
            layout = { size(14.px, 15.px) }
            onClick = { e ->
                if (e.button == 0) {
                    val menu = createTraitMenu()
                    if (!menu.isEmpty()) {
                        mbdEditor.openMenu(e.x, e.y, menu)
                        e.stopPropagation()
                    }
                }
            }
            noText()
            style = {
                tooltips("editor.machine.machine_ui.add_trait_ui")
            }
        }){ api { addPreIcon(Icons.ADD) } })
    }

    override fun screenTick() {
        super.screenTick()
        if (isTemplateDirty) {
            notifySaved()
        }
    }

    private fun createTraitMenu(): TreeBuilder.Menu {
        val traits = project.definition.machineSettings().traitDefinitions()
            .filterIsInstance<IUIProviderTrait>()

        return TreeBuilder.Menu.start().apply {
            for (provider in traits) {
                val def = provider.definition
                leaf(def.icon, def.name) {
                    addTraitUIToTemplate(provider)
                }
            }
            if (traits.isNotEmpty()) {
                leaf(Icons.WIDGET_BASIC, "editor.machine.machine_ui.generate_all") {
                    generateAllTraitUI()
                }
            }
        }
    }

    /**
     * Adds a single trait's UI elements into the appropriate container
     * (input/output/bar) in the current template. Falls back to root if no container found.
     */
    private fun addTraitUIToTemplate(provider: IUIProviderTrait) {
        val root = currentUI?.rootElement ?: return

        val container = element({
            layout = {
                flexDirection(FlexDirection.ROW)
                wrap(FlexWrap.WRAP)
                gap { all(2f) }
            }
        }){ }
        provider.createTraitUITemplate(container)

        // find the appropriate target container by layout type and IO
        val target = when (provider.traitUILayoutType) {
            TraitUILayoutType.BAR -> findElementById(root, ID_BAR) ?: root
            TraitUILayoutType.SLOT -> {
                val io = (provider as? RecipeCapabilityTraitDefinition)?.recipeHandlerIO ?: IO.BOTH
                when (io) {
                    IO.OUT -> findElementById(root, ID_OUTPUT) ?: root
                    else -> findElementById(root, ID_INPUT) ?: root
                }
            }
        }

        target.addChild(container)
        markAsDirty()

        // expand and select the added container in hierarchy
        selectInHierarchy(container)
    }

    /**
     * Generates a complete UI layout from all traits with well-known container IDs:
     * - IO row with "trait_input" / "trait_output" columns for SLOT traits
     * - "trait_bar" container for BAR traits
     * - Player inventory at the bottom
     */
    private fun generateAllTraitUI() {
        val traits = project.definition.machineSettings().traitDefinitions()
            .filterIsInstance<IUIProviderTrait>()

        val root = UIElement().layoutDsl {
            flexDirection(FlexDirection.COLUMN)
            gap { all(4f) }
        }.apply { addClass("panel_bg") }

        // title
        root.addChild(Label().apply { setText("Machine UI") })

        // IO row
        val slotTraits = traits.filter { it.traitUILayoutType == TraitUILayoutType.SLOT }
        val ioRow = UIElement().layoutDsl {
            flexDirection(FlexDirection.ROW)
            gap { all(4f) }
            alignItems(AlignItems.CENTER)
        }

        val inputCol = UIElement().layoutDsl {
            flexDirection(FlexDirection.COLUMN)
            gap { all(2f) }
            alignItems(AlignItems.CENTER)
            flex(1)
        }.apply { id = ID_INPUT }

        val centerCol = UIElement().layoutDsl {
        }.apply { id = ID_CENTER }

        val outputCol = UIElement().layoutDsl {
            flexDirection(FlexDirection.COLUMN)
            gap { all(2f) }
            alignItems(AlignItems.CENTER)
            flex(1)
        }.apply { id = ID_OUTPUT }

        for (provider in slotTraits) {
            val io = (provider as? SimpleCapabilityTraitDefinition<*, *>)?.guiIO ?: IO.BOTH
            val container = UIElement().layoutDsl {
                flexDirection(FlexDirection.ROW)
                wrap(FlexWrap.WRAP)
                gap { all(2f) }
            }
            provider.createTraitUITemplate(container)
            when (io) {
                IO.OUT -> outputCol.addChild(container)
                else -> inputCol.addChild(container)
            }
        }


        if (inputCol.children.isNotEmpty()) {
            ioRow.addChild(inputCol)
        }
        if (centerCol.children.isNotEmpty()) {
            ioRow.addChild(centerCol)
        }
        if (outputCol.children.isNotEmpty()) {
            ioRow.addChild(outputCol)
        }
        root.addChild(ioRow)

        // BAR container
        val barContainer = UIElement().layoutDsl {
            flexDirection(FlexDirection.COLUMN)
            gap { all(2f) }
        }.apply { setId(ID_BAR) }

        val barTraits = traits.filter { it.traitUILayoutType == TraitUILayoutType.BAR }
        for (provider in barTraits) {
            val wrapper = UIElement().layoutDsl {
                flexDirection(FlexDirection.ROW)
                gap { all(2f) }
            }
            provider.createTraitUITemplate(wrapper)
            barContainer.addChild(wrapper)
        }
        root.addChild(barContainer)

        // player inventory
        root.addChild(InventorySlots())

        // create new template and reload
        val newTemplate = UITemplate.of(root, StylesheetManager.MC)
        newTemplate.copyStylesFrom(project.definition.machineSettings().uiTemplate())
        project.definition.machineSettings().uiTemplate().apply {
            data = newTemplate.data.copy()
            copyStylesFrom(newTemplate)
        }
        loadTemplate(project.definition.machineSettings().uiTemplate()) {
            project.definition.machineSettings().uiTemplate().apply {
                data = it.data.copy()
                copyStylesFrom(it)
            }
        }
    }

    /**
     * Recursively finds a UIElement by its ID in the element tree.
     */
    private fun findElementById(element: UIElement, id: String): UIElement? {
        if (element.id == id) return element
        for (child in element.children) {
            val found = findElementById(child, id)
            if (found != null) return found
        }
        return null
    }

    /**
     * Expands the hierarchy path to the given element and selects it.
     */
    private fun selectInHierarchy(element: UIElement) {
        val node = findTreeNode(hierarchy.treeList.root, element) ?: return
        hierarchy.treeList.expandNodeAlongPath(node)
        hierarchy.treeList.setSelected(listOf(node), true)
    }

    /**
     * Recursively finds the UITreeNode for a given UIElement.
     */
    private fun findTreeNode(node: UITreeNode?, target: UIElement): UITreeNode? {
        if (node == null) return null
        if (node.key === target) return node
        for (child in node.children) {
            val found = findTreeNode(child, target)
            if (found != null) return found
        }
        return null
    }
}
