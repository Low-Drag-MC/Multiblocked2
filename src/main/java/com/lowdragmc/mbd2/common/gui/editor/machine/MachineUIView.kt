package com.lowdragmc.mbd2.common.gui.editor.machine

import com.lowdragmc.lowdraglib2.gui.editor.view.UIEditorView
import com.lowdragmc.lowdraglib2.gui.editor.view.UITreeNode
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture
import com.lowdragmc.lowdraglib2.gui.texture.Icons
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate
import com.lowdragmc.lowdraglib2.gui.ui.data.FillDirection
import com.lowdragmc.lowdraglib2.gui.ui.element
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar
import com.lowdragmc.lowdraglib2.gui.ui.elements.button
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots
import com.lowdragmc.lowdraglib2.gui.ui.elements.progressBar
import com.lowdragmc.lowdraglib2.gui.ui.layout.px
import com.lowdragmc.lowdraglib2.gui.ui.layoutDsl
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder
import com.lowdragmc.mbd2.api.capability.recipe.IO
import com.lowdragmc.mbd2.common.gui.MBDBindingIDs
import com.lowdragmc.mbd2.common.gui.MBDSprites
import com.lowdragmc.mbd2.common.gui.editor.MBDEditor
import com.lowdragmc.mbd2.common.gui.editor.MachineProject
import com.lowdragmc.mbd2.common.trait.IUIProviderTrait
import com.lowdragmc.mbd2.common.trait.IUIProviderTrait.TraitUILayoutType
import com.lowdragmc.mbd2.common.trait.RecipeCapabilityTraitDefinition
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTraitDefinition
import dev.vfyjxf.taffy.style.AlignItems
import dev.vfyjxf.taffy.style.FlexDirection
import net.minecraft.world.item.Items

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
        }){ api { addPreIcon(Icons.ADD) } }.addClass("__white_icon__"))
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
            // traits
            for (provider in traits) {
                val def = provider.definition
                leaf(def.icon, def.name) {
                    addTraitUIToTemplate(provider)
                }
            }
            // progress bar
            leaf(MBDSprites.ARROW_BAR, "editor.machine.machine_ui.progress") {
                addUIElementToTemplate(createProgressBar(), ID_CENTER)
            }
            // fuel bar
            leaf(MBDSprites.FUEL_BAR, "editor.machine.machine_ui.fuel_progress") {
                addUIElementToTemplate(createFuelBar(), ID_CENTER)
            }
            // XEI lookup button
            leaf(ItemStackTexture(Items.KNOWLEDGE_BOOK), "editor.machine.machine_ui.xei_lookup") {
                addUIElementToTemplate(createXEILookupButton(), ID_CENTER)
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
     * (input/output/bar) in the current template. Falls back to before player inventory.
     */
    private fun addTraitUIToTemplate(provider: IUIProviderTrait) {
        if (currentUI?.rootElement == null) return

        val container = element({
            id = provider.definition.name
        }){ }
        provider.createTraitUITemplate(container)

        // find the appropriate target container by layout type and IO
        val targetId = when (provider.traitUILayoutType) {
            TraitUILayoutType.BAR -> ID_BAR
            TraitUILayoutType.SLOT -> {
                val io = (provider as? RecipeCapabilityTraitDefinition)?.recipeHandlerIO ?: IO.BOTH
                when (io) {
                    IO.OUT -> ID_OUTPUT
                    else -> ID_INPUT
                }
            }
        }

        addUIElementToTemplate(container, targetId)
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

        // trait UI templates
        for (provider in slotTraits) {
            val io = (provider as? SimpleCapabilityTraitDefinition<*, *>)?.guiIO ?: IO.BOTH
            val container = UIElement()
            container.id = provider.definition.name
            provider.createTraitUITemplate(container)
            when (io) {
                IO.OUT -> outputCol.addChild(container)
                else -> inputCol.addChild(container)
            }
        }

        // progress bar
        centerCol.addChild(createProgressBar())

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
            val container = UIElement()
            container.id = provider.definition.name
            provider.createTraitUITemplate(container)
            barContainer.addChild(container)
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

    private fun createProgressBar(): ProgressBar {
        return progressBar({
            layout = { size(20.px) }
            progressBarStyle = { interpolate(false) }
            id = MBDBindingIDs.PROGRESS_BAR
        }){}.apply {
            barContainer.layout { it.paddingAll(0f) }
            barContainer.style { it.background(MBDSprites.ARROW_BG) }
            bar.style { it.background(IGuiTexture.EMPTY).overflowVisible(false) }
            bar.addChild(element({
                layout = { size(20.px) }
                style = { background(MBDSprites.ARROW_BAR) }
            }) {  })
            label.setDisplay(false)
        }
    }

    private fun createFuelBar(): ProgressBar {
        return progressBar({
            layout = { size(18.px) }
            progressBarStyle = {
                interpolate(false)
                fillDirection(FillDirection.DOWN_TO_UP)
            }
            id = MBDBindingIDs.FUEL_BAR
        }){}.apply {
            barContainer.layout { it.paddingAll(0f) }
            barContainer.style { it.background(MBDSprites.FUEL_BG) }
            bar.style { it.background(IGuiTexture.EMPTY).overflowVisible(false) }
            bar.addChild(element({
                layout = { size(18.px) }
                style = { background(MBDSprites.FUEL_BAR) }
            }) {  })
            label.setDisplay(false)
        }
    }

    private fun createXEILookupButton(): Button {
        return button({
            layout = { size(18.px) }
            id = MBDBindingIDs.XEI_LOOKUP
        }){}.apply {
            noText()
            addPreIcon(ItemStackTexture(Items.KNOWLEDGE_BOOK))
        }
    }

    private fun addUIElementToTemplate(element: UIElement, targetId: String) {
        val root = currentUI?.rootElement ?: return
        val target = findElementById(root, targetId)
        if (target != null) {
            target.addChild(element)
        } else {
            addChildBeforeInventory(root, element)
        }
        markAsDirty()
        selectInHierarchy(element)
    }

    private fun addChildBeforeInventory(root: UIElement, child: UIElement) {
        val inventoryIndex = root.children.indexOfFirst { it is InventorySlots }
        if (inventoryIndex >= 0) {
            root.addChildAt(child, inventoryIndex)
        } else {
            root.addChild(child)
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

    override fun startSimulation() {
        super.startSimulation()
        // simulation
    }
}
