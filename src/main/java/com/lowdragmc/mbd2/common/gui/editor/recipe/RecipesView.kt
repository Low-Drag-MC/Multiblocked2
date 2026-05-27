package com.lowdragmc.mbd2.common.gui.editor.recipe

import com.lowdragmc.lowdraglib2.configurator.IConfigurable
import com.lowdragmc.lowdraglib2.configurator.ui.BooleanConfigurator
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator
import com.lowdragmc.lowdraglib2.editor.ui.View
import com.lowdragmc.lowdraglib2.gui.ColorPattern
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture
import com.lowdragmc.lowdraglib2.gui.texture.Icons
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical
import com.lowdragmc.lowdraglib2.gui.ui.dsl
import com.lowdragmc.lowdraglib2.gui.ui.element
import com.lowdragmc.lowdraglib2.gui.ui.elements.*
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents
import com.lowdragmc.lowdraglib2.gui.ui.layout.pct
import com.lowdragmc.lowdraglib2.gui.ui.layout.px
import com.lowdragmc.lowdraglib2.gui.ui.layoutDsl
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder
import com.lowdragmc.mbd2.api.capability.recipe.IO
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability
import com.lowdragmc.mbd2.api.recipe.MBDRecipe
import com.lowdragmc.mbd2.api.recipe.RecipeCondition
import com.lowdragmc.mbd2.api.recipe.content.Content
import com.lowdragmc.mbd2.api.registry.MBDRegistries
import com.lowdragmc.mbd2.common.gui.editor.MBDEditor
import com.lowdragmc.mbd2.common.gui.editor.RecipeTypeProject
import dev.vfyjxf.taffy.style.AlignItems
import dev.vfyjxf.taffy.style.FlexDirection
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import org.lwjgl.glfw.GLFW

open class RecipesView(val mbdEditor: MBDEditor, val project: RecipeTypeProject) :
    View("editor.machine.recipe_type.recipes", Icons.FILE) {

    private val listRoot = UIElement()
    private val detailRoot = UIElement()
    private val selectedRecipes = LinkedHashSet<ResourceLocation>()
    private val selectedInputs = LinkedHashSet<Content>()
    private val selectedOutputs = LinkedHashSet<Content>()
    private val selectedConditions = LinkedHashSet<RecipeCondition>()
    private var contentClipboard: ContentClipboard? = null
    private var conditionClipboard: RecipeCondition? = null
    private lateinit var removeRecipesButton: Button
    private val contentListRoots = mutableMapOf<IO, UIElement>()
    private val contentRows = mutableMapOf<Content, UIElement>()
    private val contentToolbars = mutableMapOf<IO, ToolbarButtons>()
    private var conditionListRoot: UIElement? = null
    private val conditionRows = mutableMapOf<RecipeCondition, UIElement>()
    private var conditionToolbar: ToolbarButtons? = null
    private var editingRecipe: ResourceLocation? = null

    private data class ContentClipboard(val capability: RecipeCapability<*>, val content: Content)
    private data class ToolbarButtons(val remove: Button, val copy: Button, val paste: Button)

    init {
        addChild(splitViewHorizontal {
            withPercentage(30f)
            withLeft(createLeftPane())
            withRight(detailRoot.layoutDsl {
                width(100.pct)
                height(100.pct)
                flexDirection(FlexDirection.COLUMN)
                gap { all(4f) }
                padding { all(4f) }
            })
        })
        reloadRecipeList()
        reloadDetail()
    }

    private fun createLeftPane(): UIElement {
        val pane = UIElement().layoutDsl {
            width(100.pct)
            height(100.pct)
            flexDirection(FlexDirection.COLUMN)
            gap { all(3f) }
            padding { all(3f) }
        }
        val scroller = ScrollerView().apply {
            layoutDsl {
                width(100.pct)
                flex(1f)
            }
            addScrollViewChild(listRoot.layoutDsl {
                width(100.pct)
                flexDirection(FlexDirection.COLUMN)
                gap { all(2f) }
            })
        }
        pane.addChild(Button().apply {
            setText("editor.machine.recipe_type")
            setOnClick { e ->
                if (e.button == 0) {
                    inspectRecipeType()
                    e.stopPropagation()
                }
            }
            layout { it.widthPercent(100f) }
            style { it.tooltips(project.recipeType.registryName.toString()) }
            label {
                element.layout { e -> e.widthPercent(100f).flex(1f) }
                element.textStyle { it.textWrap(TextWrap.HOVER_ROLL).adaptiveWidth(false) }
                element.setOverflowVisible(false)
            }
        })
        val buttons = UIElement().layoutDsl {
            width(100.pct)
            height(18f)
            flexDirection(FlexDirection.ROW)
            gap { all(3f) }
        }
        buttons.addChild(Button().apply {
            setText("Add")
            setOnClick { e ->
                if (e.button == 0) {
                    showAddRecipeDialog()
                    e.stopPropagation()
                }
            }
            layout { it.flex(1f).height(18f) }
        })
        removeRecipesButton = Button().apply {
            setText("Remove")
            setOnClick { e ->
                if (e.button == 0 && selectedRecipes.isNotEmpty()) {
                    removeSelectedRecipes()
                    e.stopPropagation()
                }
            }
            layout { it.flex(1f).height(18f) }
        }
        buttons.addChild(removeRecipesButton)
        pane.addChildren(scroller, buttons)
        return pane
    }

    fun singleSelectedRecipe(): MBDRecipe? {
        val ids = selectedRecipes.toList()
        if (ids.size != 1) return null
        return project.recipeType.builtinRecipes[ids.first()]
    }

    private fun reloadRecipeList() {
        selectedRecipes.retainAll(project.recipeType.builtinRecipes.keys)
        listRoot.clearAllChildren()
        recipes().forEach { recipe -> listRoot.addChild(createRecipeRow(recipe)) }
        updateRemoveRecipeButton()
    }

    private fun createRecipeRow(recipe: MBDRecipe): UIElement {
        val selected = selectedRecipes.contains(recipe.id)
        return UIElement().layoutDsl {
            width(100.pct)
            height(20f)
            flexDirection(FlexDirection.ROW)
            alignItems(AlignItems.CENTER)
            gap { all(3f) }
            padding { all(2f) }
        }.apply {
            style {
                it.backgroundTexture(if (selected) ColorPattern.T_BLUE.rectTexture() else IGuiTexture.EMPTY)
                it.tooltips(recipe.id.toString())
            }
            if (editingRecipe == recipe.id) {
                val field = createRecipeRenameField(recipe)
                addChild(field)
                field.focus()
            } else {
                addEventListener(UIEvents.MOUSE_DOWN) { e ->
                    if (e.button == 0) {
                        selectRecipe(recipe.id, UIElement.isCtrlDown())
                        e.stopPropagation()
                    }
                }
                addChild(Label().apply {
                    setText(recipe.id.toString())
                    layout { it.flex(1f) }
                    textStyle { it.textWrap(TextWrap.HOVER_ROLL) }
                    setOverflowVisible(false)
                })
                addChild(Button().apply {
                    setText("R")
                    setOnClick { e ->
                        if (e.button == 0) {
                            editingRecipe = recipe.id
                            reloadRecipeList()
                            e.stopPropagation()
                        }
                    }
                    layout { it.width(18f).height(18f) }
                    style { it.tooltips("Rename") }
                })
            }
        }
    }

    private fun selectRecipe(id: ResourceLocation, additive: Boolean) {
        val previousSelection = selectedRecipes.toList()
        if (additive) {
            if (!selectedRecipes.add(id)) selectedRecipes.remove(id)
        } else {
            selectedRecipes.clear()
            selectedRecipes.add(id)
        }
        clearItemSelections()
        reloadRecipeList()
        if (previousSelection != selectedRecipes.toList()) {
            reloadDetail()
        }
        val selected = selectedRecipes.mapNotNull { project.recipeType.builtinRecipes[it] }
        if (selected.size == 1) {
            mbdEditor.inspectorView.inspect(selected.first(), null, null)
        } else {
            mbdEditor.inspectorView.clear()
        }
    }

    private fun createRecipeRenameField(recipe: MBDRecipe): TextField {
        val initial = recipe.id.toString()
        val field = TextField().apply {
            setText(initial, false)
            setResourceLocationOnly()
            layout { it.flex(1f).height(18f) }
        }
        var done = false
        fun commit() {
            if (done) return
            done = true
            renameRecipe(recipe, field.value)
            editingRecipe = null
            reloadRecipeList()
        }
        fun cancel() {
            if (done) return
            done = true
            editingRecipe = null
            reloadRecipeList()
        }
        field.addEventListener(UIEvents.KEY_DOWN) { e ->
            when (e.keyCode) {
                GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                    commit()
                    e.stopPropagation()
                }
                GLFW.GLFW_KEY_ESCAPE -> {
                    cancel()
                    e.stopPropagation()
                }
            }
        }
        field.addEventListener(UIEvents.BLUR) { commit() }
        return field
    }

    private fun renameRecipe(recipe: MBDRecipe, text: String?) {
        val id = text?.let(ResourceLocation::tryParse) ?: return
        if (id == recipe.id || project.recipeType.builtinRecipes.containsKey(id)) return
        val copied = recipe.deepCopied(id)
        project.recipeType.builtinRecipes.remove(recipe.id)
        project.recipeType.builtinRecipes[id] = copied
        if (selectedRecipes.remove(recipe.id)) {
            selectedRecipes.add(id)
            reloadDetail()
            mbdEditor.inspectorView.inspect(copied, null, null)
        }
    }

    private fun inspectRecipeType() {
        selectedRecipes.clear()
        clearItemSelections()
        reloadRecipeList()
        reloadDetail()
        mbdEditor.inspectorView.inspect(project.recipeType, null, null)
    }

    private fun clearItemSelections() {
        selectedInputs.clear()
        selectedOutputs.clear()
        selectedConditions.clear()
        updateContentRowStyles()
        updateConditionRowStyles()
    }

    private fun showAddRecipeDialog() {
        val initial = autoRecipeId().toString()
        Dialog.stringEditorDialog("editor.machine.recipe_type.add_recipe", initial, { text ->
            val id = ResourceLocation.tryParse(text)
            id != null && !project.recipeType.builtinRecipes.containsKey(id)
        }) { text ->
            val id = ResourceLocation.parse(text)
            if (project.recipeType.builtinRecipes.containsKey(id)) return@stringEditorDialog
            val builder = project.recipeType.recipeBuilder(id)
            builder.duration = 100
            builder.saveAsBuiltinRecipe()
            selectedRecipes.clear()
            selectedRecipes.add(id)
            clearItemSelections()
            reloadRecipeList()
            reloadDetail()
            mbdEditor.inspectorView.inspect(project.recipeType.builtinRecipes[id], null, null)
        }.show(modularUI)
    }

    private fun removeSelectedRecipes() {
        selectedRecipes.forEach { project.recipeType.builtinRecipes.remove(it) }
        selectedRecipes.clear()
        selectedInputs.clear()
        selectedOutputs.clear()
        selectedConditions.clear()
        mbdEditor.inspectorView.clear()
        reloadRecipeList()
        reloadDetail()
    }

    private fun updateRemoveRecipeButton() {
        if (::removeRecipesButton.isInitialized) {
            removeRecipesButton.setActive(selectedRecipes.isNotEmpty())
        }
    }

    private fun reloadDetail() {
        detailRoot.clearAllChildren()
        val selected = selectedRecipes.mapNotNull { project.recipeType.builtinRecipes[it] }
        when (selected.size) {
            0 -> {
                mbdEditor.inspectorView.clear()
                detailRoot.addChild(Label().setText("Select a recipe"))
            }
            1 -> detailRoot.addChild(createRecipeTabs(ensureEditableRecipe(selected.first())))
            else -> {
                mbdEditor.inspectorView.clear()
                detailRoot.addChild(Label().setText("${selected.size} recipes selected"))
            }
        }
    }

    private fun createRecipeTabs(recipe: MBDRecipe): TabView {
        return TabView().apply {
            tabContentContainer.layout { it.flex(1f) }
            layout { it.widthPercent(100f).heightPercent(100f) }
            addTab(Tab().setText("Contents"), createContentsTab(recipe))
            addTab(Tab().setText("Conditions"), createConditionsTab(recipe))
            addTab(Tab().setText("Custom Data"), createCustomDataTab(recipe))
        }
    }

    private fun ensureEditableRecipe(recipe: MBDRecipe): MBDRecipe {
        val editableInputs = linkedMapOf<RecipeCapability<*>, MutableList<Content>>()
        val editableOutputs = linkedMapOf<RecipeCapability<*>, MutableList<Content>>()
        recipe.inputs.forEach { (capability, contents) -> editableInputs[capability] = ArrayList(contents) }
        recipe.outputs.forEach { (capability, contents) -> editableOutputs[capability] = ArrayList(contents) }
        val editable = MBDRecipe(
            recipe.recipeType,
            recipe.id,
            editableInputs,
            editableOutputs,
            ArrayList(recipe.conditions),
            recipe.data,
            recipe.duration,
            recipe.isXEIHidden,
            recipe.priority
        )
        project.recipeType.builtinRecipes[recipe.id] = editable
        return editable
    }

    private fun createContentsTab(recipe: MBDRecipe): UIElement {
        return UIElement().layoutDsl {
            width(100.pct)
            height(100.pct)
            flexDirection(FlexDirection.COLUMN)
            gap { all(5f) }
        }.apply {
            setOverflowVisible(false)
            addChild(createContentSection(recipe, recipe.inputs, IO.IN, selectedInputs, "recipe.inputs"))
            addChild(createRecipeMetaSection(recipe))
            addChild(createContentSection(recipe, recipe.outputs, IO.OUT, selectedOutputs, "recipe.outputs"))
        }
    }

    private fun createRecipeMetaSection(recipe: MBDRecipe): ConfiguratorGroup {
        return ConfiguratorGroup("Recipe", false).apply {
            addConfigurators(
                NumberConfigurator("recipe.duration", { recipe.duration }, { recipe.duration = it.toInt() }, 100, true)
                    .setRange(1, Int.MAX_VALUE),
                NumberConfigurator("recipe.priority", { recipe.priority }, { recipe.priority = it.toInt() }, 0, true),
                BooleanConfigurator("recipe.xei_hidden", { recipe.isXEIHidden }, { recipe.isXEIHidden = it }, false, true)
            )
        }
    }

    private fun createContentSection(
        recipe: MBDRecipe,
        contents: MutableMap<RecipeCapability<*>, MutableList<Content>>,
        io: IO,
        selection: LinkedHashSet<Content>,
        title: String
    ): UIElement {
        val section = UIElement().layoutDsl {
            width(100.pct)
            flex(1f)
            flexDirection(FlexDirection.COLUMN)
            gap { all(2f) }
        }
        section.addChild(createSectionToolbar(title,
            onAdd = { x, y -> showAddContentMenu(recipe, contents, io, x, y) },
            onRemove = { _, _ -> removeSelectedContents(contents, selection) },
            onCopy = { _, _ -> copySelectedContent(contents, selection) },
            onPaste = { _, _ -> pasteContent(contents, io) },
            removeActive = selection.isNotEmpty(),
            copyActive = selection.size == 1,
            pasteActive = contentClipboard != null,
            buttonSink = { contentToolbars[io] = it }
        ))
        section.addChild(createContentHeader())
        val listRoot = UIElement().layoutDsl {
            width(100.pct)
            flexDirection(FlexDirection.COLUMN)
            gap { all(1f) }
        }
        contentListRoots[io] = listRoot
        reloadContentList(contents, io, selection)
        val scroller = ScrollerView().apply {
            layoutDsl { width(100.pct); flex(1f) }
            addScrollViewChild(listRoot)
        }
        section.addChild(scroller)
        return section
    }

    private fun createSectionToolbar(
        title: String,
        onAdd: (Float, Float) -> Unit,
        onRemove: (Float, Float) -> Unit,
        onCopy: (Float, Float) -> Unit,
        onPaste: (Float, Float) -> Unit,
        removeActive: Boolean,
        copyActive: Boolean,
        pasteActive: Boolean,
        buttonSink: (ToolbarButtons) -> Unit = {}
    ): UIElement {
        return UIElement().layoutDsl {
            width(100.pct)
            height(18f)
            flexDirection(FlexDirection.ROW)
            gap { all(3f) }
            alignItems(AlignItems.CENTER)
        }.apply {
            addChild(Label().setText(title)
                .textStyle { it.textAlignVertical(Vertical.CENTER)}
                .layout { it.flex(1f).height(18f) })
            addChild(iconButton("+", IGuiTexture.EMPTY,"Add", onAdd))
            val remove = iconButton("-", IGuiTexture.EMPTY,"Remove", onRemove).setActive(removeActive) as Button
            val copy = iconButton("", Icons.COPY,"Copy", onCopy).setActive(copyActive) as Button
            val paste = iconButton("", Icons.PASTE,"Paste", onPaste).setActive(pasteActive) as Button
            addChild(remove)
            addChild(copy)
            addChild(paste)
            buttonSink(ToolbarButtons(remove, copy, paste))
        }
    }

    private fun iconButton(text: String, icon: IGuiTexture, tooltip: String, action: (Float, Float) -> Unit): Button {
        return Button().apply {
            if (text.isEmpty()) noText() else setText(text)
            if (icon != IGuiTexture.EMPTY) {
                addPreIcon(icon)
                addClass("__white_icon__")
            }
            setOnClick { e ->
                if (e.button == 0) {
                    action(e.x, e.y)
                    e.stopPropagation()
                }
            }
            layout { it.width(18f).height(18f) }
            style { it.tooltips(tooltip) }
        }
    }

    private fun createContentHeader(): UIElement {
        return element({
            layout = {
                width(100.pct)
                height(14f)
                flexDirection(FlexDirection.ROW)
                padding { all(3f) }
                gap { all(3f) }
                alignItems(AlignItems.CENTER)
            }
        }) {
            label({
                layout = { width(71f) }
                textStyle = { textWrap(TextWrap.HOVER_ROLL) }
                style = { overflowVisible(false) }
                text = Component.translatable("content.content")
            }) {  }
            label({
                layout = { width(28) }
                text("/t")
            }) {  }
            for (tab in listOf(
                "editor.machine.recipe_type.content.chance",
                "editor.machine.recipe_type.content.tier_chance_boost",
                "editor.machine.recipe_type.content.slot_name",
                "editor.machine.recipe_type.content.ui_name"
            )) {
                label({
                    layout = { flex(1f) }
                    textStyle = { textWrap(TextWrap.HOVER_ROLL) }
                    style = { overflowVisible(false) }
                    text = Component.translatable(tab)
                }) {  }
            }
        }
    }

    private fun headerLabel(text: String, width: Float): Label {
        return Label().apply {
            setText(text)
            layout { it.width(width).height(14f) }
        }
    }

    private fun createContentRow(
        capability: RecipeCapability<*>,
        content: Content,
        io: IO,
        selection: LinkedHashSet<Content>
    ): UIElement {
        return element({
            layout = {
                width(100.pct)
                height(22f)
                flexDirection(FlexDirection.ROW)
                padding { all(3f) }
                gap { all(3f) }
                alignItems(AlignItems.CENTER)
            }
        }) {
            events {
                UIEvents.MOUSE_DOWN += {
                    if (it.button == 0) {
                        selectContent(capability, content, io, selection, isCtrlDown())
                        it.stopPropagation()
                    }
                }
            }
            add(preview(capability, content).dsl({}))
            label({
                layout = { width(50f) }
                textStyle = { textWrap(TextWrap.HOVER_ROLL) }
                style = { overflowVisible(false) }
                text = capability.traslateComponent
            }) {  }
            switch({
                isOn = content.perTick
            }) {
                dataSource { content.perTick }
                observer { content.perTick = it }
                element.setOnSwitchChanged { content.perTick = it }
            }
            textField({
                layout = { flex(1) }
                text = content.chance
            }) {
                asNumeric(0f, 1f)
                dataSource { content.chance.toString() }
                observer { content.chance = it.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0f }
            }
            textField({
                layout = { flex(1) }
                text = content.tierChanceBoost
            }) {
                asNumeric(0f, 1f)
                dataSource { content.tierChanceBoost.toString() }
                observer { content.tierChanceBoost = it.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0f }
            }
            textField({
                layout = { flex(1) }
                text = content.slotName
            }) {
                dataSource { content.slotName }
                observer { content.slotName = it }
            }
            textField({
                layout = { flex(1) }
                text = content.uiName
            }) {
                dataSource { content.uiName }
                observer { content.uiName = it }
            }
        }.apply {
            updateContentRowStyle(this, content)
            contentRows[content] = this
        }
    }

    private fun reloadContentList(
        contents: MutableMap<RecipeCapability<*>, MutableList<Content>>,
        io: IO,
        selection: LinkedHashSet<Content>
    ) {
        val root = contentListRoots[io] ?: return
        selection.retainAll(contents.values.flatten().toSet())
        root.clearAllChildren()
        contents.forEach { (capability, list) ->
            list.forEach { content ->
                root.addChild(createContentRow(capability, content, io, selection))
            }
        }
        updateContentToolbar(io, selection)
    }

    private fun updateContentRowStyles() {
        contentRows.forEach { (content, row) -> updateContentRowStyle(row, content) }
        updateContentToolbar(IO.IN, selectedInputs)
        updateContentToolbar(IO.OUT, selectedOutputs)
    }

    private fun updateContentRowStyle(row: UIElement, content: Content) {
        row.style {
            it.backgroundTexture(if (selectedInputs.contains(content) || selectedOutputs.contains(content)) {
                ColorPattern.T_BLUE.rectTexture()
            } else {
                IGuiTexture.EMPTY
            })
        }
    }

    private fun updateContentToolbar(io: IO, selection: LinkedHashSet<Content>) {
        contentToolbars[io]?.let {
            it.remove.setActive(selection.isNotEmpty())
            it.copy.setActive(selection.size == 1)
            it.paste.setActive(contentClipboard != null)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun preview(capability: RecipeCapability<*>, content: Content): UIElement {
        val cap = capability as RecipeCapability<Any>
        return cap.createPreview({ cap.of(content.content) }).layout { it.width(18f).height(18f) }
    }

    private fun selectContent(
        capability: RecipeCapability<*>,
        content: Content,
        io: IO,
        selection: LinkedHashSet<Content>,
        additive: Boolean
    ) {
        if (io == IO.IN) {
            selectedOutputs.clear()
        } else {
            selectedInputs.clear()
        }
        selectedConditions.clear()
        if (additive) {
            if (!selection.add(content)) selection.remove(content)
        } else {
            selection.clear()
            selection.add(content)
        }
        updateContentRowStyles()
        updateConditionRowStyles()
        if (selection.size == 1) {
            inspectContent(capability, content, io)
        } else {
            mbdEditor.inspectorView.clear()
        }
    }

    private fun showAddContentMenu(
        recipe: MBDRecipe,
        contents: MutableMap<RecipeCapability<*>, MutableList<Content>>,
        io: IO,
        x: Float,
        y: Float
    ) {
        val menu = TreeBuilder.Menu.start().apply {
            for (capability in MBDRegistries.RECIPE_CAPABILITIES) {
                leaf(IGuiTexture.EMPTY, capability.name) {
                    val content = Content(capability.createDefaultContent(), false, 1f, 0f)
                    contents.computeIfAbsent(capability) { mutableListOf() }.add(content)
                    if (io == IO.IN) {
                        selectedInputs.clear()
                        selectedInputs.add(content)
                    } else {
                        selectedOutputs.clear()
                        selectedOutputs.add(content)
                    }
                    inspectContent(capability, content, io)
                    reloadContentList(contents, io, if (io == IO.IN) selectedInputs else selectedOutputs)
                }
            }
        }
        if (!menu.isEmpty) mbdEditor.openMenu(x, y, menu)
    }

    private fun removeSelectedContents(
        contents: MutableMap<RecipeCapability<*>, MutableList<Content>>,
        selection: LinkedHashSet<Content>
    ) {
        contents.values.forEach { it.removeAll(selection) }
        contents.entries.removeIf { it.value.isEmpty() }
        selection.clear()
        mbdEditor.inspectorView.clear()
        reloadContentList(contents, if (selection === selectedInputs) IO.IN else IO.OUT, selection)
    }

    private fun copySelectedContent(
        contents: MutableMap<RecipeCapability<*>, MutableList<Content>>,
        selection: LinkedHashSet<Content>
    ) {
        val content = selection.singleOrNull() ?: return
        val capability = contents.entries.firstOrNull { it.value.contains(content) }?.key ?: return
        contentClipboard = ContentClipboard(capability, content.deepCopy(capability, null))
        updateContentToolbar(IO.IN, selectedInputs)
        updateContentToolbar(IO.OUT, selectedOutputs)
    }

    private fun pasteContent(contents: MutableMap<RecipeCapability<*>, MutableList<Content>>, io: IO) {
        val clipboard = contentClipboard ?: return
        val pasted = clipboard.content.deepCopy(clipboard.capability, null)
        contents.computeIfAbsent(clipboard.capability) { mutableListOf() }.add(pasted)
        if (io == IO.IN) {
            selectedInputs.clear()
            selectedInputs.add(pasted)
        } else {
            selectedOutputs.clear()
            selectedOutputs.add(pasted)
        }
        inspectContent(clipboard.capability, pasted, io)
        reloadContentList(contents, io, if (io == IO.IN) selectedInputs else selectedOutputs)
    }

    @Suppress("UNCHECKED_CAST")
    private fun inspectContent(capability: RecipeCapability<*>, content: Content, io: IO) {
        val cap = capability as RecipeCapability<Any>
        mbdEditor.inspectorView.inspect(IConfigurable.create { group ->
            content.buildConfigurator(group)
            cap.createContentConfigurator(group, { cap.of(content.content) }, { content.content = it })
        }, null, null)
    }

    private fun createConditionsTab(recipe: MBDRecipe): UIElement {
        return UIElement().layoutDsl {
            width(100.pct)
            height(100.pct)
            flexDirection(FlexDirection.COLUMN)
            gap { all(3f) }
        }.apply {
            addChild(createSectionToolbar("recipe.conditions",
                onAdd = { x, y -> showAddConditionMenu(recipe, x, y) },
                onRemove = { _, _ -> removeSelectedConditions(recipe) },
                onCopy = { _, _ -> copySelectedCondition() },
                onPaste = { _, _ -> pasteCondition(recipe) },
                removeActive = selectedConditions.isNotEmpty(),
                copyActive = selectedConditions.size == 1,
                pasteActive = conditionClipboard != null,
                buttonSink = { conditionToolbar = it }
            ))
            val rows = UIElement().layoutDsl {
                width(100.pct)
                flexDirection(FlexDirection.COLUMN)
                gap { all(2f) }
            }
            conditionListRoot = rows
            reloadConditionList(recipe)
            addChild(ScrollerView().apply {
                layoutDsl { width(100.pct); flex(1f) }
                addScrollViewChild(rows)
            })
        }
    }

    private fun createConditionRow(condition: RecipeCondition): UIElement {
        return element({
            layout = {
                width(100.pct)
                height(18f)
                flexDirection(FlexDirection.ROW)
                gap { all(4f) }
                alignItems(AlignItems.CENTER)
            }
        }) {
            events {
                UIEvents.MOUSE_DOWN += {
                    if (it.button == 0) {
                        selectCondition(condition, isCtrlDown())
                        it.stopPropagation()
                    }
                }
            }
            api {
                updateConditionRowStyle(element, condition)
            }
            element({
                layout = { size(14.px) }
                style = { background(condition.icon) }
            }) {}
            label({
                layout = { flex(1f) }
                textStyle = { textWrap(TextWrap.HOVER_ROLL) }
                style = { overflowVisible(false) }
            }) {
                dataSource { condition.tooltips }
            }
        }.apply {
            conditionRows[condition] = this
        }
    }

    private fun reloadConditionList(recipe: MBDRecipe) {
        selectedConditions.retainAll(recipe.conditions.toSet())
        val root = conditionListRoot ?: return
        root.clearAllChildren()
        recipe.conditions.forEach { root.addChild(createConditionRow(it)) }
        updateConditionToolbar()
    }

    private fun updateConditionRowStyles() {
        conditionRows.forEach { (condition, row) -> updateConditionRowStyle(row, condition) }
        updateConditionToolbar()
    }

    private fun updateConditionRowStyle(row: UIElement, condition: RecipeCondition) {
        row.style {
            it.backgroundTexture(if (selectedConditions.contains(condition)) {
                ColorPattern.T_BLUE.rectTexture()
            } else {
                IGuiTexture.EMPTY
            })
        }
    }

    private fun updateConditionToolbar() {
        conditionToolbar?.let {
            it.remove.setActive(selectedConditions.isNotEmpty())
            it.copy.setActive(selectedConditions.size == 1)
            it.paste.setActive(conditionClipboard != null)
        }
    }

    private fun selectCondition(condition: RecipeCondition, additive: Boolean) {
        if (additive) {
            if (!selectedConditions.add(condition)) selectedConditions.remove(condition)
        } else {
            selectedInputs.clear()
            selectedOutputs.clear()
            selectedConditions.clear()
            selectedConditions.add(condition)
        }
        updateContentRowStyles()
        updateConditionRowStyles()
        if (selectedConditions.size == 1) {
            mbdEditor.inspectorView.inspect(selectedConditions.first(), null, null)
        } else {
            mbdEditor.inspectorView.clear()
        }
    }

    private fun showAddConditionMenu(recipe: MBDRecipe, x: Float, y: Float) {
        val menu = TreeBuilder.Menu.start().apply {
            for (holder in MBDRegistries.RECIPE_CONDITIONS) {
                val sample = holder.value.get()
                leaf(sample.icon, holder.annotation.name) {
                    val condition = holder.value.get()
                    recipe.conditions.add(condition)
                    selectedConditions.clear()
                    selectedConditions.add(condition)
                    mbdEditor.inspectorView.inspect(condition, null, null)
                    reloadConditionList(recipe)
                }
            }
        }
        if (!menu.isEmpty) mbdEditor.openMenu(x, y, menu)
    }

    private fun removeSelectedConditions(recipe: MBDRecipe) {
        recipe.conditions.removeAll(selectedConditions)
        selectedConditions.clear()
        mbdEditor.inspectorView.clear()
        reloadConditionList(recipe)
    }

    private fun copySelectedCondition() {
        conditionClipboard = selectedConditions.singleOrNull()?.copy()
        updateConditionToolbar()
    }

    private fun pasteCondition(recipe: MBDRecipe) {
        val condition = conditionClipboard?.copy() ?: return
        recipe.conditions.add(condition)
        selectedConditions.clear()
        selectedConditions.add(condition)
        mbdEditor.inspectorView.inspect(condition, null, null)
        reloadConditionList(recipe)
    }

    private fun createCustomDataTab(recipe: MBDRecipe): StructuredTagEditor {
        return StructuredTagEditor()
            .setCompoundTagOnly()
            .setValue(recipe.data, false)
            .setTagResponder { tag ->
                recipe.data = (tag as? CompoundTag)?.copy() ?: CompoundTag()
            }
            .layout { it.widthPercent(100f).heightPercent(100f) } as StructuredTagEditor
    }

    private fun recipes(): List<MBDRecipe> {
        return project.recipeType.builtinRecipes.values.sortedBy { it.id.toString() }
    }

    private fun autoRecipeId(): ResourceLocation {
        val prefix = project.recipeType.registryName.path + "/recipe_"
        var index = 0
        var id = ResourceLocation.fromNamespaceAndPath(project.recipeType.registryName.namespace, prefix + index++)
        while (project.recipeType.builtinRecipes.containsKey(id)) {
            id = ResourceLocation.fromNamespaceAndPath(id.namespace, prefix + index++)
        }
        return id
    }
}
