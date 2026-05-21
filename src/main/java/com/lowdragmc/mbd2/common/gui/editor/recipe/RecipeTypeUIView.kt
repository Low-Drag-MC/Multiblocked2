package com.lowdragmc.mbd2.common.gui.editor.recipe

import com.lowdragmc.lowdraglib2.gui.editor.view.UIEditorView
import com.lowdragmc.lowdraglib2.gui.texture.Icons
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar
import com.lowdragmc.lowdraglib2.gui.ui.layoutDsl
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import com.lowdragmc.mbd2.api.capability.recipe.IO
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability
import com.lowdragmc.mbd2.common.gui.editor.MBDEditor
import com.lowdragmc.mbd2.common.gui.editor.RecipeTypeProject
import dev.vfyjxf.taffy.style.AlignItems
import dev.vfyjxf.taffy.style.FlexDirection
import dev.vfyjxf.taffy.style.FlexWrap
import net.minecraft.network.chat.Component

open class RecipeTypeUIView(
    val mbdEditor: MBDEditor,
    val project: RecipeTypeProject
) : UIEditorView() {

    companion object {
        const val ID_INPUT = "recipe_input"
        const val ID_CENTER = "recipe_center"
        const val ID_OUTPUT = "recipe_output"
        const val ID_INFO = "recipe_info"
    }

    init {
        icon = Icons.WIDGET_BASIC
        name = "editor.machine.recipe_xei_ui"
        saveButton.setDisplay(false)

        loadTemplate(project.recipeType.uiTemplate) {
            project.recipeType.uiTemplate = it.copy()
        }

        header.addChild(Button()
            .setText("Generate")
            .setOnClick { e ->
                if (e.button == 0) {
                    generateRecipeUI()
                    e.stopPropagation()
                }
            }
            .layout { it.width(48f).height(15f) }
            .style { it.tooltips("editor.machine.recipe_type_ui_view.generate") }
        )
    }

    override fun screenTick() {
        super.screenTick()
        if (isTemplateDirty) {
            notifySaved()
        }
    }

    private fun generateRecipeUI() {
        val root = UIElement().layoutDsl {
            flexDirection(FlexDirection.COLUMN)
            gap { all(4f) }
            padding { all(4f) }
        }.apply {
            addClass("panel_bg")
        }

        root.addChild(Label().setText("Recipe"))

        val ioRow = UIElement().layoutDsl {
            flexDirection(FlexDirection.ROW)
            gap { all(6f) }
            alignItems(AlignItems.CENTER)
        }

        val inputCol = UIElement().layoutDsl {
            flexDirection(FlexDirection.ROW)
            wrap(FlexWrap.WRAP)
            gap { all(2f) }
            flex(1f)
        }.apply { id = ID_INPUT }

        val centerCol = UIElement().layoutDsl {
            flexDirection(FlexDirection.COLUMN)
            gap { all(2f) }
            alignItems(AlignItems.CENTER)
        }.apply { id = ID_CENTER }

        centerCol.addChild(ProgressBar().apply {
            setId("@progress_bar")
            setProgress(0.5f)
            layout { it.width(60f).height(14f) }
        })

        val outputCol = UIElement().layoutDsl {
            flexDirection(FlexDirection.ROW)
            wrap(FlexWrap.WRAP)
            gap { all(2f) }
            flex(1f)
        }.apply { id = ID_OUTPUT }

        addCapabilityTemplates(inputCol, IO.IN)
        addCapabilityTemplates(outputCol, IO.OUT)

        ioRow.addChild(inputCol)
        ioRow.addChild(centerCol)
        ioRow.addChild(outputCol)
        root.addChild(ioRow)

        val info = UIElement().layoutDsl {
            flexDirection(FlexDirection.COLUMN)
            gap { all(2f) }
        }.apply { id = ID_INFO }
        info.addChild(Label().apply {
            setId("@duration")
            setText(Component.translatable("recipe.duration.value", 100))
        })
        info.addChild(Label().apply {
            setId("@condition")
            setText("")
        })
        root.addChild(info)

        val generated = UITemplate.of(root, StylesheetManager.MC)
        generated.copyStylesFrom(project.recipeType.uiTemplate)
        project.recipeType.uiTemplate = generated
        loadTemplate(generated) {
            project.recipeType.uiTemplate = it.copy()
        }
    }

    private fun addCapabilityTemplates(parent: UIElement, io: IO) {
        maxContents(io).forEach { (capability, count) ->
            for (i in 0 until count) {
                parent.addChild(capability.createXEITemplate()
                    .setId("@${capability.name}_${ioId(io)}_$i"))
            }
        }
    }

    private fun maxContents(io: IO): Map<RecipeCapability<*>, Int> {
        val result = linkedMapOf<RecipeCapability<*>, Int>()
        project.recipeType.builtinRecipes.values
            .filter { !it.isFuel }
            .forEach { recipe ->
                val contents = if (io == IO.OUT) recipe.outputs else recipe.inputs
                contents.forEach { (capability, values) ->
                    if (values.isNotEmpty()) {
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
}
