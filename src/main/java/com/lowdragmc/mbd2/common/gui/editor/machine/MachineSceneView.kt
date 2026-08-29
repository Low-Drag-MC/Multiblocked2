package com.lowdragmc.mbd2.common.gui.editor.machine

import com.lowdragmc.lowdraglib2.client.scene.ParticleManager
import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderTypes
import com.lowdragmc.lowdraglib2.client.utils.RenderBufferUtils
import com.lowdragmc.lowdraglib2.editor.ui.View
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.SceneEditor
import com.lowdragmc.lowdraglib2.gui.ColorPattern
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.TrackData
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.getValue
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.setValue
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture
import com.lowdragmc.lowdraglib2.gui.ui.default
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle
import com.lowdragmc.lowdraglib2.gui.ui.elements.ToggleSpec
import com.lowdragmc.lowdraglib2.gui.ui.layout.pct
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites
import com.lowdragmc.lowdraglib2.utils.ColorUtils
import com.lowdragmc.lowdraglib2.utils.data.BlockInfo
import com.lowdragmc.lowdraglib2.utils.virtuallevel.TrackedDummyWorld
import com.lowdragmc.mbd2.api.registry.MBDRegistries
import com.lowdragmc.mbd2.common.blockentity.MachineBlockEntity
import com.lowdragmc.mbd2.common.gui.editor.MBDEditor
import com.lowdragmc.mbd2.common.gui.editor.MachineProject
import com.lowdragmc.mbd2.common.machine.MBDMachine
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.BlockState
import org.joml.Vector3f

/**
 * @param particleManager the particle host for this scene's dummy world, or null for LDLib2's
 *   default. A constructor parameter rather than an overridable hook because the manager has to be
 *   on the level *before* `createScene` copies it onto the renderer — a hook called from this
 *   constructor could only write subclass state that the subclass's own initializers then run over.
 *   See [MachineFXView], which passes Photon's manager for its post-effect and sub-viewport handling.
 */
open class MachineSceneView(
    name: String,
    val editor: MBDEditor,
    val project: MachineProject,
    particleManager: ParticleManager? = null,
) : View(name) {
    val sceneEditor: SceneEditor = createSceneEditor()
    var previewMachine: MBDMachine? = null
        protected set
    val level: TrackedDummyWorld = TrackedDummyWorld()

    // Reference geometry. Lives here rather than on one subclass because judging anything positioned
    // against the machine — a shape, a rendering box, an effect's offset — needs the same three lines
    // drawn behind it. Basic Settings had them and the FX view, which is the view that positions
    // things by hand, did not.
    private val drawShapeData: TrackData<Boolean> = TrackData(true)
    private val drawRenderingBoxData: TrackData<Boolean> = TrackData(false)
    private val drawAxesData: TrackData<Boolean> = TrackData(false)
    var isDrawShape by drawShapeData
    var isDrawRenderingBox by drawRenderingBoxData
    var isDrawAxes by drawAxesData

    /**
     * Which way the preview machine faces. Effects authored against north follow it through
     * `MachineFXExecutor`'s own facing rotation, which is the point of being able to turn it.
     */
    var previewFacing: Direction = Direction.NORTH
        private set

    init {
        sceneEditor.layout.widthPercent(100f).heightPercent(100f)
        particleManager?.let { level.particleManager = it }
        sceneEditor.scene
            .createScene(level)
            .setTickWorld(true)
            .setRenderedCore(listOf(BlockPos.ZERO))
        loadScene();
    }

    /** Exposed so a subclass can bind the shared toggles with the usual [toggleSpec] DSL. */
    protected fun shapeToggleData() = drawShapeData
    protected fun renderingBoxToggleData() = drawRenderingBoxData
    protected fun axesToggleData() = drawAxesData

    open fun createSceneEditor() = MachineSceneEditor()

    /**
     * load the scene, it will reset everything to the default state, in general, you don't need to call this method.
     * to change renderer, using [com.lowdragmc.mbd2.common.gui.editor.machine.MachineSceneView.previewMachine] instead.
     */
    open fun loadScene() {
        this.level.clear();
        this.level.addBlock(BlockPos.ZERO, BlockInfo.fromBlockState(previewBlockState()))
        (this.level.getBlockEntity(BlockPos.ZERO) as? MachineBlockEntity)?.let {
            it.setMachine(project.definition.createMachine(it).also { m -> previewMachine = m })
        }
    }

    /**
     * The block the preview machine sits in, turned to [previewFacing] when that is possible.
     *
     * The scene places the *fake* machine's block — the project's own is not registered until it is
     * saved — but `MBDMachine.getFrontFacing()` reads the *project* definition's rotation property off
     * whatever block state is there. The two only line up when both use the same property, so a
     * machine whose rotation state is horizontal-only cannot be turned in here at all;
     * [canPreviewFacing] is what the UI asks before offering the control, rather than offering it and
     * silently doing nothing.
     */
    private fun previewBlockState(): BlockState {
        val base = MBDRegistries.getFakeMachineDefinition().block().defaultBlockState()
        val property = project.definition.blockProperties().rotationState().property.orElse(null)
            ?: return base
        if (!base.hasProperty(property) || !property.possibleValues.contains(previewFacing)) return base
        return base.setValue(property, previewFacing)
    }

    /** Whether this machine's facing can be shown in the preview at all. @see previewBlockState */
    fun canPreviewFacing(): Boolean {
        val base = MBDRegistries.getFakeMachineDefinition().block().defaultBlockState()
        val property = project.definition.blockProperties().rotationState().property.orElse(null)
            ?: return false
        return base.hasProperty(property)
    }

    /** The facings this machine actually accepts, for a picker to offer. */
    fun previewFacings(): List<Direction> =
        Direction.entries.filter { project.definition.blockProperties().rotationState().test(it) }

    /** Turn the preview machine. Rebuilds the scene, so effects restart against the new facing. */
    fun setPreviewFacing(facing: Direction) {
        if (previewFacing == facing) return
        previewFacing = facing
        loadScene()
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
        drawReferenceGeometry(bufferSource)
    }

    /** The shape / rendering-box / axes overlays, drawn for whichever view enabled them. */
    private fun drawReferenceGeometry(bufferSource: MultiBufferSource) {
        val machine = previewMachine ?: return
        if (isDrawShape) {
            val buffer = bufferSource.getBuffer(LDLibRenderTypes.noDepthLines())
            machine.machineState.getShape(null).forAllEdges { x0, y0, z0, x1, y1, z1 ->
                val normal = Vector3f((x1 - x0).toFloat(), (y1 - y0).toFloat(), (z1 - z0).toFloat()).normalize()
                buffer.addVertex(x0.toFloat(), y0.toFloat(), z0.toFloat())
                    .setColor(-1)
                    .setNormal(normal.x, normal.y, normal.z)
                buffer.addVertex(x1.toFloat(), y1.toFloat(), z1.toFloat())
                    .setColor(-1)
                    .setNormal(normal.x, normal.y, normal.z)
            }
        }
        if (isDrawRenderingBox) {
            val aabb = machine.machineState.getRenderingBox(null)
            if (aabb != null) {
                val color = 0xffeedd00.toInt()
                val buffer = bufferSource.getBuffer(LDLibRenderTypes.noDepthLines())
                RenderBufferUtils.drawCubeFrame(
                    PoseStack(), buffer,
                    aabb.minX.toFloat(), aabb.minY.toFloat(), aabb.minZ.toFloat(),
                    aabb.maxX.toFloat(), aabb.maxY.toFloat(), aabb.maxZ.toFloat(),
                    ColorUtils.red(color), ColorUtils.green(color),
                    ColorUtils.blue(color), ColorUtils.alpha(color)
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

    inner class MachineSceneEditor : SceneEditor() {
        override fun renderAfterWorld(bufferSource: MultiBufferSource, partialTicks: Float) {
            this@MachineSceneView.renderAfterWorld(bufferSource, partialTicks)
            super.renderAfterWorld(bufferSource, partialTicks)
        }
    }
}