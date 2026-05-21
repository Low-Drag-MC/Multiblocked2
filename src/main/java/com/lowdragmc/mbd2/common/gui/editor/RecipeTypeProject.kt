package com.lowdragmc.mbd2.common.gui.editor

import com.lowdragmc.lowdraglib2.editor.project.IProject
import com.lowdragmc.lowdraglib2.editor.project.ProjectType
import com.lowdragmc.lowdraglib2.editor.resource.ColorsResource
import com.lowdragmc.lowdraglib2.editor.resource.IRendererResource
import com.lowdragmc.lowdraglib2.editor.resource.Resources
import com.lowdragmc.lowdraglib2.editor.resource.TexturesResource
import com.lowdragmc.lowdraglib2.editor.resource.UIResource
import com.lowdragmc.lowdraglib2.editor.ui.Editor
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate
import com.lowdragmc.lowdraglib2.utils.TagBuilder
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType
import com.lowdragmc.mbd2.common.gui.editor.recipe.RecipeTypeUIView
import com.lowdragmc.mbd2.common.gui.editor.recipe.RecipesView
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import java.io.File

open class RecipeTypeProject : IProject {
    companion object {
        var VERSION: Int = 1
        val TYPE: ProjectType = RecipeTypeProjectType()
    }

    private class RecipeTypeProjectType : ProjectType(IGuiTexture.EMPTY, "recipe_type_project", ".rt", { RecipeTypeProject() }) {
        override fun getRootSavePath(project: IProject, projectRoot: File): File {
            return projectRoot.resolve("mbd2/recipe_type")
        }
    }

    val recipeType: MBDRecipeType = MBDRecipeType.createDefault()

    private val resources = Resources.of(
        IRendererResource.INSTANCE,
        ColorsResource.INSTANCE,
        TexturesResource.INSTANCE,
        UIResource.INSTANCE
    )

    override fun getResources(): Resources = resources

    var recipesView: RecipesView? = null
        protected set
    var recipeUIView: RecipeTypeUIView? = null
        protected set

    override fun getVersion(): String = "%d.0".format(VERSION)

    override fun getProjectType(): ProjectType = TYPE

    override fun serializeProject(provider: HolderLookup.Provider): CompoundTag {
        val ops = provider.createSerializationContext(NbtOps.INSTANCE)
        return TagBuilder.compound()
            .add("recipe_type", recipeType.serializeNBT(provider))
            .add("ui", UITemplate.CODEC.encodeStart(ops, recipeType.uiTemplate).getOrThrow())
            .add("fuel_ui", UITemplate.CODEC.encodeStart(ops, recipeType.fuelUITemplate).getOrThrow())
            .build()
    }

    override fun deserializeProject(provider: HolderLookup.Provider, tag: CompoundTag) {
        recipeType.deserializeNBT(provider, tag.getCompound("recipe_type"))
        val ops = provider.createSerializationContext(NbtOps.INSTANCE)
        if (tag.contains("ui")) {
            recipeType.uiTemplate = UITemplate.CODEC.parse(ops, tag.get("ui")).getOrThrow()
        }
        if (tag.contains("fuel_ui")) {
            recipeType.fuelUITemplate = UITemplate.CODEC.parse(ops, tag.get("fuel_ui")).getOrThrow()
        }
    }

    override fun onLoad(editor: Editor) {
        if (editor is MBDEditor) {
            super.onLoad(editor)
            editor.centerWindow.getLeftTop().addView(createRecipesView(editor).also { recipesView = it })
            editor.centerWindow.getLeftTop().addView(createRecipeUIView(editor).also { recipeUIView = it })
        }
    }

    protected fun createRecipesView(editor: MBDEditor): RecipesView {
        return RecipesView(editor, this)
    }

    protected fun createRecipeUIView(editor: MBDEditor): RecipeTypeUIView {
        return RecipeTypeUIView(editor, this)
    }

    override fun onClosed(editor: Editor) {
        recipesView?.removeSelf()
        recipesView = null
        recipeUIView?.removeSelf()
        recipeUIView = null
    }
}
