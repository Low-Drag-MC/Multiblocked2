package com.lowdragmc.mbd2.common.gui.editor.recipe

import com.lowdragmc.lowdraglib2.configurator.IConfigurable
import com.lowdragmc.lowdraglib2.configurator.ui.BooleanConfigurator
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator
import com.lowdragmc.lowdraglib2.editor.ui.View
import com.lowdragmc.lowdraglib2.gui.texture.Icons
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView
import com.lowdragmc.lowdraglib2.gui.ui.elements.StructuredTagEditor
import com.lowdragmc.lowdraglib2.gui.ui.elements.Tab
import com.lowdragmc.lowdraglib2.gui.ui.elements.TabView
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField
import com.lowdragmc.lowdraglib2.gui.ui.elements.splitViewHorizontal
import com.lowdragmc.lowdraglib2.gui.ui.elements.withLeft
import com.lowdragmc.lowdraglib2.gui.ui.elements.withPercentage
import com.lowdragmc.lowdraglib2.gui.ui.elements.withRight
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents
import com.lowdragmc.lowdraglib2.gui.ui.layout.pct
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
import net.minecraft.resources.ResourceLocation
import java.util.LinkedHashSet

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

    private data class ContentClipboard(val capability: RecipeCapability<*>, val content: Content)

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

    private fun reloadRecipeList() {
        selectedRecipes.retainAll(project.recipeType.builtinRecipes.keys)
        listRoot.clearAllChildren()
        recipes().forEach { recipe -> listRoot.addChild(createRecipeRow(recipe)) }
        updateRemoveRecipeButton()
    }

    private fun createRecipeRow(recipe: MBDRecipe): Button {
        val selected = selectedRecipes.contains(recipe.id)
        return Button().apply {
            setText(if (selected) "> ${recipe.id}" else recipe.id.toString())
            setOnClick { e ->
                if (e.button == 0) {
                    selectRecipe(recipe.id, UIElement.isCtrlDown())
                    e.stopPropagation()
                }
            }
            layout { it.widthPercent(100f).height(18f) }
            style { it.tooltips(recipe.id.toString()) }
        }
    }

    private fun selectRecipe(id: ResourceLocation, additive: Boolean) {
        if (additive) {
            if (!selectedRecipes.add(id)) selectedRecipes.remove(id)
        } else {
            selectedRecipes.clear()
            selectedRecipes.add(id)
        }
        selectedInputs.clear()
        selectedOutputs.clear()
        selectedConditions.clear()
        reloadRecipeList()
        reloadDetail()
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
            builder.isFuel = false
            builder.saveAsBuiltinRecipe()
            selectedRecipes.clear()
            selectedRecipes.add(id)
            reloadRecipeList()
            reloadDetail()
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
            1 -> detailRoot.addChild(createRecipeTabs(selected.first()))
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

    private fun createContentsTab(recipe: MBDRecipe): UIElement {
        return UIElement().layoutDsl {
            width(100.pct)
            height(100.pct)
            flexDirection(FlexDirection.COLUMN)
            gap { all(5f) }
        }.apply {
            addChild(createContentSection(recipe, recipe.inputs, IO.IN, selectedInputs, "Inputs"))
            addChild(createRecipeMetaSection(recipe))
            addChild(createContentSection(recipe, recipe.outputs, IO.OUT, selectedOutputs, "Outputs"))
        }
    }

    private fun createRecipeMetaSection(recipe: MBDRecipe): ConfiguratorGroup {
        return ConfiguratorGroup("Recipe", false).apply {
            setCanCollapse(false)
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
            pasteActive = contentClipboard != null
        ))
        section.addChild(createContentHeader())
        val scroller = ScrollerView().apply {
            layoutDsl { width(100.pct); flex(1f) }
            addScrollViewChild(UIElement().layoutDsl {
                width(100.pct)
                flexDirection(FlexDirection.COLUMN)
                gap { all(1f) }
            }.apply {
                contents.forEach { (capability, list) ->
                    list.forEach { content ->
                        addChild(createContentRow(recipe, capability, content, io, selection))
                    }
                }
            })
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
        pasteActive: Boolean
    ): UIElement {
        return UIElement().layoutDsl {
            width(100.pct)
            height(18f)
            flexDirection(FlexDirection.ROW)
            gap { all(3f) }
            alignItems(AlignItems.CENTER)
        }.apply {
            addChild(Label().setText(title).layout { it.flex(1f).height(18f) })
            addChild(iconButton("+", "Add", onAdd))
            addChild(iconButton("-", "Remove", onRemove).setActive(removeActive))
            addChild(iconButton("C", "Copy", onCopy).setActive(copyActive))
            addChild(iconButton("P", "Paste", onPaste).setActive(pasteActive))
        }
    }

    private fun iconButton(text: String, tooltip: String, action: (Float, Float) -> Unit): Button {
        return Button().apply {
            setText(text)
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
        return UIElement().layoutDsl {
            width(100.pct)
            height(14f)
            flexDirection(FlexDirection.ROW)
            gap { all(3f) }
            alignItems(AlignItems.CENTER)
        }.apply {
            addChild(headerLabel("Content", 52f))
            addChild(headerLabel("Tick", 26f))
            addChild(headerLabel("Chance", 44f))
            addChild(headerLabel("Tier", 44f))
            addChild(headerLabel("Slot", 64f))
            addChild(headerLabel("UI", 64f))
        }
    }

    private fun headerLabel(text: String, width: Float): Label {
        return Label().apply {
            setText(text)
            layout { it.width(width).height(14f) }
        }
    }

    private fun createContentRow(
        recipe: MBDRecipe,
        capability: RecipeCapability<*>,
        content: Content,
        io: IO,
        selection: LinkedHashSet<Content>
    ): UIElement {
        val selected = selection.contains(content)
        return UIElement().layoutDsl {
            width(100.pct)
            height(22f)
            flexDirection(FlexDirection.ROW)
            gap { all(3f) }
            alignItems(AlignItems.CENTER)
        }.apply {
            addChild(preview(capability, content))
            addChild(Button().apply {
                setText(if (selected) "*" else capability.name)
                setOnClick { e ->
                    if (e.button == 0) {
                        selectContent(capability, content, io, selection, UIElement.isCtrlDown())
                        e.stopPropagation()
                    }
                }
                layout { it.width(31f).height(18f) }
                style { it.tooltips(capability.getTraslateComponent()) }
            })
            addChild(Button().apply {
                setText(if (content.perTick) "T" else "-")
                setOnClick { e ->
                    if (e.button == 0) {
                        content.perTick = !content.perTick
                        reloadDetail()
                        e.stopPropagation()
                    }
                }
                layout { it.width(26f).height(18f) }
            })
            addChild(floatField(content.chance, 0f, 1f) { content.chance = it })
            addChild(floatField(content.tierChanceBoost, 0f, 1f) { content.tierChanceBoost = it })
            addChild(stringField(content.slotName) { content.slotName = it })
            addChild(stringField(content.uiName) { content.uiName = it })
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun preview(capability: RecipeCapability<*>, content: Content): UIElement {
        val cap = capability as RecipeCapability<Any>
        return cap.createPreviewWidget(cap.of(content.content)).layout { it.width(18f).height(18f) }
    }

    private fun floatField(value: Float, min: Float, max: Float, setter: (Float) -> Unit): TextField {
        return TextField().apply {
            setNumbersOnlyFloat(min, max)
            setText(value.toString(), false)
            setTextResponder { text ->
                text.toFloatOrNull()?.let { setter(it.coerceIn(min, max)) }
            }
            layout { it.width(44f).height(18f) }
        }
    }

    private fun stringField(value: String, setter: (String) -> Unit): TextField {
        return TextField().apply {
            setText(value, false)
            setTextResponder(setter)
            layout { it.width(64f).height(18f) }
        }
    }

    private fun selectContent(
        capability: RecipeCapability<*>,
        content: Content,
        io: IO,
        selection: LinkedHashSet<Content>,
        additive: Boolean
    ) {
        if (additive) {
            if (!selection.add(content)) selection.remove(content)
        } else {
            selectedInputs.clear()
            selectedOutputs.clear()
            selection.add(content)
        }
        if (selection.size == 1) {
            inspectContent(capability, content, io)
        } else {
            mbdEditor.inspectorView.clear()
        }
        reloadDetail()
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
                leaf(Icons.ADD, capability.name) {
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
                    reloadDetail()
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
        reloadDetail()
    }

    private fun copySelectedContent(
        contents: MutableMap<RecipeCapability<*>, MutableList<Content>>,
        selection: LinkedHashSet<Content>
    ) {
        val content = selection.singleOrNull() ?: return
        val capability = contents.entries.firstOrNull { it.value.contains(content) }?.key ?: return
        contentClipboard = ContentClipboard(capability, content.deepCopy(capability, null))
        reloadDetail()
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
        reloadDetail()
    }

    @Suppress("UNCHECKED_CAST")
    private fun inspectContent(capability: RecipeCapability<*>, content: Content, io: IO) {
        val cap = capability as RecipeCapability<Any>
        mbdEditor.inspectorView.inspect(IConfigurable.create { group ->
            group.addConfigurators(
                BooleanConfigurator("editor.machine.recipe_type.content.per_tick", { content.perTick }, { content.perTick = it }, false, true),
                NumberConfigurator("editor.machine.recipe_type.content.chance", { content.chance }, { content.chance = it.toFloat() }, 1f, true)
                    .setRange(0f, 1f),
                NumberConfigurator("editor.machine.recipe_type.content.tier_chance_boost", { content.tierChanceBoost }, { content.tierChanceBoost = it.toFloat() }, 0f, true)
                    .setRange(0f, 1f),
                StringConfigurator("editor.machine.recipe_type.content.slot_name", { content.slotName }, { content.slotName = it }, "", true),
                StringConfigurator("editor.machine.recipe_type.content.ui_name", { content.uiName }, { content.uiName = it }, "", true)
            )
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
            addChild(createSectionToolbar("Conditions",
                onAdd = { x, y -> showAddConditionMenu(recipe, x, y) },
                onRemove = { _, _ -> removeSelectedConditions(recipe) },
                onCopy = { _, _ -> copySelectedCondition() },
                onPaste = { _, _ -> pasteCondition(recipe) },
                removeActive = selectedConditions.isNotEmpty(),
                copyActive = selectedConditions.size == 1,
                pasteActive = conditionClipboard != null
            ))
            addChild(ScrollerView().apply {
                layoutDsl { width(100.pct); flex(1f) }
                addScrollViewChild(UIElement().layoutDsl {
                    width(100.pct)
                    flexDirection(FlexDirection.COLUMN)
                    gap { all(2f) }
                }.apply {
                    recipe.conditions.forEach { addChild(createConditionRow(it)) }
                })
            })
        }
    }

    private fun createConditionRow(condition: RecipeCondition): UIElement {
        val selected = selectedConditions.contains(condition)
        return UIElement().layoutDsl {
            width(100.pct)
            height(18f)
            flexDirection(FlexDirection.ROW)
            gap { all(4f) }
            alignItems(AlignItems.CENTER)
        }.apply {
            addChild(UIElement().layout { it.width(14f).height(14f) }.style { it.background(condition.icon) })
            addChild(Button().apply {
                setText(if (selected) "> ${condition.type}" else condition.type)
                setOnClick { e ->
                    if (e.button == 0) {
                        selectCondition(condition, UIElement.isCtrlDown())
                        e.stopPropagation()
                    }
                }
                layout { it.flex(1f).height(18f) }
                style { it.tooltips(condition.tooltips) }
            })
        }
    }

    private fun selectCondition(condition: RecipeCondition, additive: Boolean) {
        if (additive) {
            if (!selectedConditions.add(condition)) selectedConditions.remove(condition)
        } else {
            selectedConditions.clear()
            selectedConditions.add(condition)
        }
        if (selectedConditions.size == 1) {
            mbdEditor.inspectorView.inspect(selectedConditions.first(), null, null)
        } else {
            mbdEditor.inspectorView.clear()
        }
        reloadDetail()
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
                    reloadDetail()
                }
            }
        }
        if (!menu.isEmpty) mbdEditor.openMenu(x, y, menu)
    }

    private fun removeSelectedConditions(recipe: MBDRecipe) {
        recipe.conditions.removeAll(selectedConditions)
        selectedConditions.clear()
        mbdEditor.inspectorView.clear()
        reloadDetail()
    }

    private fun copySelectedCondition() {
        conditionClipboard = selectedConditions.singleOrNull()?.copy()
        reloadDetail()
    }

    private fun pasteCondition(recipe: MBDRecipe) {
        val condition = conditionClipboard?.copy() ?: return
        recipe.conditions.add(condition)
        selectedConditions.clear()
        selectedConditions.add(condition)
        mbdEditor.inspectorView.inspect(condition, null, null)
        reloadDetail()
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
        return project.recipeType.builtinRecipes.values.filter { !it.isFuel }.sortedBy { it.id.toString() }
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
