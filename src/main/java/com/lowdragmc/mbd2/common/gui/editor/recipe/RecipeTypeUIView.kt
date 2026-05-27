package com.lowdragmc.mbd2.common.gui.editor.recipe

import com.lowdragmc.lowdraglib2.gui.editor.view.UIEditorView
import com.lowdragmc.lowdraglib2.gui.editor.view.UITreeNode
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture
import com.lowdragmc.lowdraglib2.gui.texture.Icons
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate
import com.lowdragmc.lowdraglib2.gui.ui.element
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar
import com.lowdragmc.lowdraglib2.gui.ui.elements.button
import com.lowdragmc.lowdraglib2.gui.ui.elements.progressBar
import com.lowdragmc.lowdraglib2.gui.ui.layout.px
import com.lowdragmc.lowdraglib2.gui.ui.layoutDsl
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder
import com.lowdragmc.mbd2.api.capability.recipe.IO
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability
import com.lowdragmc.mbd2.api.registry.MBDRegistries
import com.lowdragmc.mbd2.common.gui.MBDSprites
import com.lowdragmc.mbd2.common.gui.editor.MBDEditor
import com.lowdragmc.mbd2.common.gui.editor.RecipeTypeProject
import dev.vfyjxf.taffy.style.*
import net.minecraft.network.chat.Component

open class RecipeTypeUIView(
    val mbdEditor: MBDEditor,
    val project: RecipeTypeProject
) : UIEditorView() {

    companion object {
        const val ID_INPUT = "recipe_input"
        const val ID_CENTER = "recipe_center"
        const val ID_OUTPUT = "recipe_output"
        const val ID_BAR = "recipe_bar"
        const val ID_BAR_INPUT = "recipe_bar_input"
        const val ID_BAR_OUTPUT = "recipe_bar_output"
        const val ID_FOOTER = "recipe_footer"

        const val ID_PROGRESS_BAR = "@progress_bar"
        const val ID_CONDITION = "@condition"
        const val ID_CUSTOM_DATA = "@custom_data"
        const val ID_DURATION = "@duration"
    }

    init {
        icon = Icons.WIDGET_BASIC
        name = "editor.machine.recipe_xei_ui"
        saveButton.setDisplay(false)

        loadTemplate(project.recipeType.uiTemplate) {
            project.recipeType.uiTemplate = it.copy()
        }

        // "+" button in the header opens a menu of leaves for incremental authoring
        header.addChild(button({
            layout = { size(14.px, 15.px) }
            onClick = { e ->
                if (e.button == 0) {
                    val menu = createContentMenu()
                    if (!menu.isEmpty()) {
                        mbdEditor.openMenu(e.x, e.y, menu)
                        e.stopPropagation()
                    }
                }
            }
            noText()
            style = {
                tooltips("editor.machine.recipe_type_ui_view.add_content")
            }
        }) { api { addPreIcon(Icons.ADD) } }.addClass("__white_icon__"))
    }

    override fun screenTick() {
        super.screenTick()
        if (isTemplateDirty) {
            notifySaved()
        }
    }

    override fun startSimulation() {
        super.startSimulation()
        val recipe = project.recipesView?.singleSelectedRecipe() ?: return
        canvas.canvasModularUI?.let { mui ->
            project.recipeType.bindXEIRecipeUI(mui.ui, recipe)
        }
    }

    private fun createContentMenu(): TreeBuilder.Menu {
        val capabilities = MBDRegistries.RECIPE_CAPABILITIES.values()

        return TreeBuilder.Menu.start().apply {
            // slot capabilities — one leaf per (capability, IO)
            for (cap in capabilities) {
                if (cap.xeiLayoutType() == RecipeCapability.XEILayoutType.SLOT) {
                    branch(Icons.WIDGET_BASIC, cap.traslateComponent) { sub ->
                        sub.leaf(Icons.IMPORT, "editor.machine.recipe_type_ui_view.add_input") {
                            addCapabilitySlotToTemplate(cap, IO.IN)
                        }
                        sub.leaf(Icons.EXPORT, "editor.machine.recipe_type_ui_view.add_output") {
                            addCapabilitySlotToTemplate(cap, IO.OUT)
                        }
                    }
                } else {
                    branch(Icons.WIDGET_BASIC, cap.traslateComponent) { sub ->
                        sub.leaf(Icons.IMPORT, "editor.machine.recipe_type_ui_view.add_input") {
                            addCapabilityBarToTemplate(cap, IO.IN)
                        }
                        sub.leaf(Icons.EXPORT, "editor.machine.recipe_type_ui_view.add_output") {
                            addCapabilityBarToTemplate(cap, IO.OUT)
                        }
                    }
                }
            }
            // progress bar
            leaf(MBDSprites.ARROW_BAR, "editor.machine.recipe_type_ui_view.progress") {
                addProgressBarToTemplate()
            }
            // duration label
            leaf(Icons.INFORMATION, "editor.machine.recipe_type_ui_view.duration") {
                addDurationLabelToTemplate()
            }
            // condition icon
            leaf(Icons.HELP, "editor.machine.recipe_type_ui_view.condition") {
                addConditionIconToTemplate()
            }
            // custom data button
            leaf(Icons.JSON, "editor.machine.recipe_type_ui_view.custom_data") {
                addCustomDataButtonToTemplate()
            }
            // generate all
            leaf(Icons.WIDGET_BASIC, "editor.machine.recipe_type_ui_view.generate") {
                generateAllRecipeUI()
            }
        }
    }

    // ---------- single-element add helpers ----------

    private fun addCapabilitySlotToTemplate(capability: RecipeCapability<*>, io: IO) {
        val targetId = if (io == IO.OUT) ID_OUTPUT else ID_INPUT
        val element = capability.createXEITemplate()
        element.id = nextCapabilityElementId(capability, io)
        addUIElementToTemplate(element, targetId)
    }

    private fun addCapabilityBarToTemplate(capability: RecipeCapability<*>, io: IO) {
        val targetId = if (io == IO.OUT) ID_BAR_OUTPUT else ID_BAR_INPUT
        val element = capability.createXEITemplate()
        element.id = nextCapabilityElementId(capability, io)
        addUIElementToTemplate(element, targetId)
    }

    private fun addProgressBarToTemplate() {
        addUIElementToTemplate(createProgressBar(), ID_CENTER)
    }

    private fun addDurationLabelToTemplate() {
        addUIElementToTemplate(createDurationLabel(), ID_BAR)
    }

    private fun addConditionIconToTemplate() {
        addUIElementToTemplate(createConditionIcon(), ID_FOOTER)
    }

    private fun addCustomDataButtonToTemplate() {
        addUIElementToTemplate(createCustomDataButton(), ID_FOOTER)
    }

    // ---------- generate all ----------

    private fun generateAllRecipeUI() {
        val root = UIElement().layoutDsl {
            flexDirection(FlexDirection.COLUMN)
            gap { all(4f) }
            width(176.px)
        }.apply { addClass("panel_bg") }

        // IO row (slot zone)
        val ioRow = UIElement().layoutDsl {
            flexDirection(FlexDirection.ROW)
            gap { all(6f) }
            alignItems(AlignItems.CENTER)
        }

        val inputCol = UIElement().layoutDsl {
            flexDirection(FlexDirection.ROW)
            wrap(FlexWrap.WRAP)
            gap { all(4f) }
            alignItems(AlignItems.CENTER)
            justifyContent(AlignContent.CENTER)
            flex(1f)
        }.apply { id = ID_INPUT }

        val centerCol = UIElement().layoutDsl {
            flexDirection(FlexDirection.COLUMN)
            gap { all(2f) }
            alignItems(AlignItems.CENTER)
        }.apply { id = ID_CENTER }

        val outputCol = UIElement().layoutDsl {
            flexDirection(FlexDirection.ROW)
            wrap(FlexWrap.WRAP)
            gap { all(4f) }
            alignItems(AlignItems.CENTER)
            justifyContent(AlignContent.CENTER)
            flex(1f)
        }.apply { id = ID_OUTPUT }

        // slot capabilities
        addSlotsForIO(inputCol, IO.IN)
        addSlotsForIO(outputCol, IO.OUT)

        // center progress bar
        centerCol.addChild(createProgressBar())

        ioRow.addChild(inputCol)
        ioRow.addChild(centerCol)
        ioRow.addChild(outputCol)
        root.addChild(ioRow)

        // bar zone — input col on the left, output col on the right
        val barContainer = UIElement().layoutDsl {
            flexDirection(FlexDirection.COLUMN)
            gap { all(2f) }
        }.apply { id = ID_BAR }

        val barRow = UIElement().layoutDsl {
            flexDirection(FlexDirection.ROW)
            gap { all(4f) }
            alignItems(AlignItems.STRETCH)
        }
        val barInput = UIElement().layoutDsl {
            flexDirection(FlexDirection.COLUMN)
            gap { all(2f) }
            flex(1f)
        }.apply { id = ID_BAR_INPUT }
        val barOutput = UIElement().layoutDsl {
            flexDirection(FlexDirection.COLUMN)
            gap { all(2f) }
            flex(1f)
        }.apply { id = ID_BAR_OUTPUT }
        addBarsForIO(barInput, IO.IN)
        addBarsForIO(barOutput, IO.OUT)
        barRow.addChild(barInput)
        barRow.addChild(barOutput)
        barContainer.addChild(barRow)

        // duration label
        barContainer.addChild(createDurationLabel())

        // footer — condition icon and custom data button in a single row
        val footer = UIElement().layoutDsl {
            position(TaffyPosition.ABSOLUTE)
            flexDirection(FlexDirection.ROW)
            gap { all(4f) }
            alignItems(AlignItems.CENTER)
            pos {
                right(0.px)
                bottom(0.px)
            }
        }.apply { id = ID_FOOTER }
        footer.addChild(createConditionIcon())
        footer.addChild(createCustomDataButton())
        barContainer.addChild(footer)

        root.addChild(barContainer)

        val generated = UITemplate.of(root, StylesheetManager.MC)
        generated.copyStylesFrom(project.recipeType.uiTemplate)
        project.recipeType.uiTemplate = generated
        loadTemplate(generated) {
            project.recipeType.uiTemplate = it.copy()
        }
    }

    private fun addSlotsForIO(parent: UIElement, io: IO) {
        maxContents(io, RecipeCapability.XEILayoutType.SLOT).forEach { (cap, count) ->
            for (i in 0 until count) {
                parent.addChild(cap.createXEITemplate()
                    .setId("@${cap.name}_${ioId(io)}_$i"))
            }
        }
    }

    private fun addBarsForIO(parent: UIElement, io: IO) {
        maxContents(io, RecipeCapability.XEILayoutType.BAR).forEach { (cap, count) ->
            for (i in 0 until count) {
                parent.addChild(cap.createXEITemplate()
                    .setId("@${cap.name}_${ioId(io)}_$i"))
            }
        }
    }

    // ---------- helpers ----------

    private fun createConditionIcon(): UIElement {
        return element({
            id = ID_CONDITION
            layout = { size(10.px) }
            style = { background(Icons.HELP) }
        }) {}
    }

    private fun createCustomDataButton(): UIElement {
        return button({
            id = ID_CUSTOM_DATA
            layout = { size(10.px) }
            noText()
        }) { api { addPreIcon(Icons.JSON) } }
    }

    private fun createDurationLabel(): Label {
        return Label().apply {
            id = ID_DURATION
            setText(Component.translatable("recipe.duration.value", 100))
            layout { it.widthPercent(100f) }
            setOverflowVisible(false)
        }
    }

    private fun createProgressBar(): ProgressBar {
        return progressBar({
            layout = { size(20.px) }
            progressBarStyle = { interpolate(false) }
            id = ID_PROGRESS_BAR
        }) {}.apply {
            barContainer.layout { it.paddingAll(0f) }
            barContainer.style { it.background(MBDSprites.ARROW_BG) }
            bar.style { it.background(IGuiTexture.EMPTY).overflowVisible(false) }
            bar.addChild(element({
                layout = { size(20.px) }
                style = { background(MBDSprites.ARROW_BAR) }
            }) {})
            label.setDisplay(false)
        }
    }

    private fun maxContents(io: IO, layoutType: RecipeCapability.XEILayoutType): Map<RecipeCapability<*>, Int> {
        val result = linkedMapOf<RecipeCapability<*>, Int>()
        project.recipeType.builtinRecipes.values
            .forEach { recipe ->
                val contents = if (io == IO.OUT) recipe.outputs else recipe.inputs
                contents.forEach { (capability, values) ->
                    if (values.isNotEmpty() && capability.xeiLayoutType() == layoutType) {
                        result[capability] = maxOf(result[capability] ?: 0, values.size)
                    }
                }
            }
        return result
    }

    private fun ioId(io: IO): String {
        return when (io) {
            IO.IN -> "import"
            IO.OUT -> "export"
            IO.BOTH -> "both"
            IO.NONE -> "none"
        }
    }

    private fun nextCapabilityElementId(capability: RecipeCapability<*>, io: IO): String {
        val prefix = "@${capability.name}_${ioId(io)}_"
        val root = currentUI?.rootElement ?: return prefix + "0"
        var maxIndex = -1
        traverseElements(root) { el ->
            val id = el.id
            if (id != null && id.startsWith(prefix)) {
                val tail = id.substring(prefix.length)
                tail.toIntOrNull()?.let { idx ->
                    if (idx > maxIndex) maxIndex = idx
                }
            }
        }
        return prefix + (maxIndex + 1)
    }

    private fun traverseElements(element: UIElement, consumer: (UIElement) -> Unit) {
        consumer(element)
        for (child in element.children) {
            traverseElements(child, consumer)
        }
    }

    private fun addUIElementToTemplate(element: UIElement, targetId: String) {
        val root = currentUI?.rootElement ?: return
        val target = findElementById(root, targetId)
        if (target != null) {
            target.addChild(element)
        } else {
            root.addChild(element)
        }
        markAsDirty()
        selectInHierarchy(element)
    }

    private fun findElementById(element: UIElement, id: String): UIElement? {
        if (element.id == id) return element
        for (child in element.children) {
            val found = findElementById(child, id)
            if (found != null) return found
        }
        return null
    }

    private fun selectInHierarchy(element: UIElement) {
        val node = findTreeNode(hierarchy.treeList.root, element) ?: return
        hierarchy.treeList.expandNodeAlongPath(node)
        hierarchy.treeList.setSelected(listOf(node), true)
    }

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
