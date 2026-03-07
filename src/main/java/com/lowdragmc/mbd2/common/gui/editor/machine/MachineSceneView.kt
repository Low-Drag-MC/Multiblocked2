package com.lowdragmc.mbd2.common.gui.editor.machine

import com.lowdragmc.lowdraglib2.editor.ui.View
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.SceneEditor
import com.lowdragmc.lowdraglib2.gui.ColorPattern
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture
import com.lowdragmc.lowdraglib2.gui.ui.default
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle
import com.lowdragmc.lowdraglib2.gui.ui.elements.ToggleSpec
import com.lowdragmc.lowdraglib2.gui.ui.layout.pct
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites
import com.lowdragmc.lowdraglib2.utils.data.BlockInfo
import com.lowdragmc.lowdraglib2.utils.virtuallevel.TrackedDummyWorld
import com.lowdragmc.mbd2.api.registry.MBDRegistries
import com.lowdragmc.mbd2.common.blockentity.MachineBlockEntity
import com.lowdragmc.mbd2.common.gui.editor.MBDEditor
import com.lowdragmc.mbd2.common.gui.editor.MachineProject
import com.lowdragmc.mbd2.common.machine.MBDMachine
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.BlockPos

open class MachineSceneView(
    name: String,
    val editor: MBDEditor,
    val project: MachineProject,
) : View(name) {
    val sceneEditor: SceneEditor = createSceneEditor()
    var previewMachine: MBDMachine? = null
        protected set
    val level: TrackedDummyWorld = TrackedDummyWorld()

    init {
        sceneEditor.layout.widthPercent(100f).heightPercent(100f)
        sceneEditor.scene
            .createScene(level)
            .setTickWorld(true)
            .setRenderedCore(listOf(BlockPos.ZERO))
        loadScene();
    }

    open fun createSceneEditor() = MachineSceneEditor()

    /**
     * load the scene, it will reset everything to the default state, in general, you don't need to call this method.
     * to change renderer, using [com.lowdragmc.mbd2.common.gui.editor.machine.MachineSceneView.previewMachine] instead.
     */
    open fun loadScene() {
        this.level.clear();
        this.level.addBlock(BlockPos.ZERO, BlockInfo.fromBlock(MBDRegistries.getFakeMachineDefinition().block()))
        (this.level.getBlockEntity(BlockPos.ZERO) as? MachineBlockEntity)?.let {
            it.setMachine(project.definition.createMachine(it).also { m -> previewMachine = m })
        }
    }


    /**
     * It's a quick tool function to create a toggle for the scene top bar.
     */
    fun toggleSpec(icon: IGuiTexture, color: Int, vararg tooltips: String): (ToggleSpec<Toggle>.() -> Unit) = {
        toggleStyle = {
            this.default {
                baseTexture(Sprites.BORDER1_RT1_DARK)
                hoverTexture(Sprites.BORDER1_RT1)
            }
            unmarkTexture(icon.copy().setColor(ColorPattern.GRAY.color).scale(0.65f))
            markTexture(icon.copy().setColor(color).scale(0.65f))
        }
        style = { tooltips(*tooltips) }
        layout = {
            padding { all(0f) };
            height(100.pct)
            aspectRatio(1f)
        }
        noText()
    }

    /**
     * Renders additional elements in the scene after the world is rendered.
     *
     * @param bufferSource The source of buffers used for rendering.
     * @param partialTicks The partial progress between ticks, used for smooth rendering.
     */
    protected open fun renderAfterWorld(bufferSource: MultiBufferSource, partialTicks: Float) {

    }

    inner class MachineSceneEditor : SceneEditor() {
        override fun renderAfterWorld(bufferSource: MultiBufferSource, partialTicks: Float) {
            this@MachineSceneView.renderAfterWorld(bufferSource, partialTicks)
            super.renderAfterWorld(bufferSource, partialTicks)
        }
    }
}