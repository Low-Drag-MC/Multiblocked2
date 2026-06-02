package com.lowdragmc.mbd2.common.gui.editor.multiblopck

import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderTypes
import com.lowdragmc.lowdraglib2.client.utils.RenderBufferUtils
import com.lowdragmc.lowdraglib2.configurator.IConfigurable
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable
import com.lowdragmc.lowdraglib2.editor.ui.View
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.SceneEditor
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button
import com.lowdragmc.lowdraglib2.gui.ui.elements.Tab
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents
import com.lowdragmc.lowdraglib2.utils.ColorUtils
import com.lowdragmc.mbd2.common.gui.editor.MBDEditor
import com.lowdragmc.mbd2.common.gui.editor.MultiblockMachineProject
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

class MultiblockAreaView(
    val editor: MBDEditor,
    val multiblockProject: MultiblockMachineProject
) : View("editor.machine.multiblock_area") {
    private val runtime = AreaRuntime()
    private val sceneEditor = object : SceneEditor() {
        override fun renderAfterWorld(bufferSource: MultiBufferSource, partialTicks: Float) {
            super.renderAfterWorld(bufferSource, partialTicks)
            renderAreaOverlay(bufferSource)
            if (bufferSource is MultiBufferSource.BufferSource) {
                bufferSource.endBatch()
            }
        }
    }

    init {
        sceneEditor.layout { it.widthPercent(100f).heightPercent(100f) }
        sceneEditor.disableTransformGizmo()
        sceneEditor.scene.setRenderFacing(false)
        sceneEditor.scene.setRenderSelect(false)
        sceneEditor.scene.useCacheBuffer()
//        sceneEditor.scene.syncCompile()
        sceneEditor.topBar.addChild(Button().setText("Generate Pattern").setOnClick { runtime.generatePattern() }.layout {
            it.width(92f)
            it.heightPercent(100f)
        })
        sceneEditor.topBar.addChild(Button().setText("Add ShapeInfo").setOnClick { runtime.generateShapeInfo() }.layout {
            it.width(86f)
            it.heightPercent(100f)
        })
        Minecraft.getInstance().level?.let { sceneEditor.scene.createScene(it) }
        sceneEditor.scene.addEventListener(UIEvents.MOUSE_UP, { event ->
            if (event.button == 0 && isShiftDown()) {
                sceneEditor.scene.lastHoverPosFace?.pos()?.let { runtime.pickAreaPoint(it) }
            }
        })
        addChild(sceneEditor)
        reloadScene()
        editor.inspectorView.inspect(runtime)
    }

    override fun createTab(): Tab? {
        return super.createTab().apply {
            setOnTabSelected {
                editor.inspectorView.inspect(runtime)
            }
            setOnTabUnselected {
                if (editor.inspectorView.inspector.inspectedConfigurable == runtime) {
                    editor.inspectorView.clear()
                }
            }
        }
    }

    fun reloadScene() {
        val player = Minecraft.getInstance().player ?: return
        val level = Minecraft.getInstance().level ?: return
        val center = player.onPos
        val blocks = HashSet<BlockPos>()
        val radius = runtime.sceneRadius
        for (x in -radius..radius) {
            for (y in -radius..radius) {
                for (z in -radius..radius) {
                    val pos = center.offset(x, y, z).immutable()
                    if (level.isLoaded(pos)) {
                        blocks.add(pos)
                    }
                }
            }
        }
        sceneEditor.scene.setRenderedCore(blocks)
    }

    inner class AreaRuntime : IConfigurable {
        @Configurable(name = "editor.machine.multiblock.area_panel.sceneRadius", tips = ["editor.machine.multiblock.area_panel.sceneRadius.tips"])
        @ConfigNumber(range = [1.0, 50.0])
        var sceneRadius: Int = 5

        @Configurable(name = "editor.machine.multiblock.area_panel.from", tips = ["editor.machine.multiblock.area_panel.from.tips"])
        var from: BlockPos = Minecraft.getInstance().player?.onPos ?: BlockPos.ZERO

        @Configurable(name = "editor.machine.multiblock.area_panel.to", tips = ["editor.machine.multiblock.area_panel.to.tips"])
        var to: BlockPos = Minecraft.getInstance().player?.onPos ?: BlockPos.ZERO

        @Configurable(name = "editor.machine.multiblock.area_panel.controllerOffset", tips = ["editor.machine.multiblock.area_panel.controllerOffset.tips"])
        var controllerOffset: BlockPos = BlockPos.ZERO

        @Configurable(name = "editor.machine.multiblock.area_panel.controllerFace", tips = ["editor.machine.multiblock.area_panel.controllerFace.tips"])
        var controllerFace: Direction = Direction.NORTH

        private var pickFrom = true

        @ConfigSetter(field = "sceneRadius")
        fun setSceneRadiusValue(value: Int) {
            sceneRadius = value.coerceIn(1, 50)
            reloadScene()
        }

        @ConfigSetter(field = "from")
        fun setFromValue(value: BlockPos) {
            from = value
            clampControllerOffset()
        }

        @ConfigSetter(field = "to")
        fun setToValue(value: BlockPos) {
            to = value
            clampControllerOffset()
        }

        @ConfigSetter(field = "controllerOffset")
        fun setControllerOffsetValue(value: BlockPos) {
            controllerOffset = value
            clampControllerOffset()
        }

        fun pickAreaPoint(pos: BlockPos) {
            if (pickFrom) {
                setFromValue(pos)
            } else {
                setToValue(pos)
            }
            pickFrom = !pickFrom
            editor.inspectorView.inspect(this)
        }

        fun generatePattern() {
            val level = Minecraft.getInstance().level ?: return
            multiblockProject.generatePatternFromWorld(level, from, to, controllerOffset, controllerFace, modularUI) {
                multiblockProject.multiblockPatternView?.let { target ->
                    this@MultiblockAreaView.viewContainer?.selectView(target)
                }
            }
        }

        fun generateShapeInfo() {
            val level = Minecraft.getInstance().level ?: return
            multiblockProject.generateShapeInfoFromWorld(level, from, to, controllerOffset, controllerFace)
            multiblockProject.multiblockShapeInfoView?.let { target ->
                this@MultiblockAreaView.viewContainer?.selectView(target)
            }
        }

        private fun clampControllerOffset() {
            val minX = minOf(from.x, to.x)
            val minY = minOf(from.y, to.y)
            val minZ = minOf(from.z, to.z)
            val maxX = maxOf(from.x, to.x)
            val maxY = maxOf(from.y, to.y)
            val maxZ = maxOf(from.z, to.z)
            controllerOffset = BlockPos(
                controllerOffset.x.coerceIn(0, maxX - minX),
                controllerOffset.y.coerceIn(0, maxY - minY),
                controllerOffset.z.coerceIn(0, maxZ - minZ)
            )
        }
    }

    private fun renderAreaOverlay(bufferSource: MultiBufferSource) {
        val poseStack = PoseStack()
        val minX = minOf(runtime.from.x, runtime.to.x)
        val minY = minOf(runtime.from.y, runtime.to.y)
        val minZ = minOf(runtime.from.z, runtime.to.z)
        val maxX = maxOf(runtime.from.x, runtime.to.x) + 1
        val maxY = maxOf(runtime.from.y, runtime.to.y) + 1
        val maxZ = maxOf(runtime.from.z, runtime.to.z) + 1
        val controllerPos = BlockPos(minX + runtime.controllerOffset.x, minY + runtime.controllerOffset.y, minZ + runtime.controllerOffset.z)
        RenderSystem.disableDepthTest()
        val buffer = bufferSource.getBuffer(LDLibRenderTypes.noDepthLines())
        drawFrame(poseStack, buffer, minX, minY, minZ, maxX, maxY, maxZ, 0xffffffff.toInt())
        drawFrame(
            poseStack,
            buffer,
            controllerPos.x,
            controllerPos.y,
            controllerPos.z,
            controllerPos.x + 1,
            controllerPos.y + 1,
            controllerPos.z + 1,
            0xff00aaaa.toInt()
        )
        drawControllerFrontFace(poseStack, bufferSource, controllerPos, runtime.controllerFace)
    }

    private fun drawFrame(
        poseStack: PoseStack,
        buffer: VertexConsumer,
        minX: Int,
        minY: Int,
        minZ: Int,
        maxX: Int,
        maxY: Int,
        maxZ: Int,
        color: Int
    ) {
        RenderBufferUtils.drawCubeFrame(
            poseStack,
            buffer,
            minX.toFloat(),
            minY.toFloat(),
            minZ.toFloat(),
            maxX.toFloat(),
            maxY.toFloat(),
            maxZ.toFloat(),
            ColorUtils.red(color),
            ColorUtils.green(color),
            ColorUtils.blue(color),
            ColorUtils.alpha(color)
        )
    }

    private fun drawControllerFrontFace(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        pos: BlockPos,
        face: Direction
    ) {
        val buffer = bufferSource.getBuffer(LDLibRenderTypes.positionColorNoDepth())
        val minX = pos.x.toFloat()
        val minY = pos.y.toFloat()
        val minZ = pos.z.toFloat()
        val maxX = minX + 1f
        val maxY = minY + 1f
        val maxZ = minZ + 1f
        val offset = 0.004f
        val vertices = when (face) {
            Direction.NORTH -> arrayOf(
                floatArrayOf(minX, minY, minZ - offset),
                floatArrayOf(maxX, minY, minZ - offset),
                floatArrayOf(maxX, maxY, minZ - offset),
                floatArrayOf(minX, maxY, minZ - offset)
            )
            Direction.SOUTH -> arrayOf(
                floatArrayOf(maxX, minY, maxZ + offset),
                floatArrayOf(minX, minY, maxZ + offset),
                floatArrayOf(minX, maxY, maxZ + offset),
                floatArrayOf(maxX, maxY, maxZ + offset)
            )
            Direction.WEST -> arrayOf(
                floatArrayOf(minX - offset, minY, maxZ),
                floatArrayOf(minX - offset, minY, minZ),
                floatArrayOf(minX - offset, maxY, minZ),
                floatArrayOf(minX - offset, maxY, maxZ)
            )
            Direction.EAST -> arrayOf(
                floatArrayOf(maxX + offset, minY, minZ),
                floatArrayOf(maxX + offset, minY, maxZ),
                floatArrayOf(maxX + offset, maxY, maxZ),
                floatArrayOf(maxX + offset, maxY, minZ)
            )
            Direction.UP -> arrayOf(
                floatArrayOf(minX, maxY + offset, minZ),
                floatArrayOf(maxX, maxY + offset, minZ),
                floatArrayOf(maxX, maxY + offset, maxZ),
                floatArrayOf(minX, maxY + offset, maxZ)
            )
            Direction.DOWN -> arrayOf(
                floatArrayOf(minX, minY - offset, maxZ),
                floatArrayOf(maxX, minY - offset, maxZ),
                floatArrayOf(maxX, minY - offset, minZ),
                floatArrayOf(minX, minY - offset, minZ)
            )
        }
        drawQuad(poseStack, buffer, vertices[0], vertices[1], vertices[2], vertices[3], 0xff00e5ff.toInt())
    }

    private fun drawQuad(
        poseStack: PoseStack,
        buffer: VertexConsumer,
        p0: FloatArray,
        p1: FloatArray,
        p2: FloatArray,
        p3: FloatArray,
        color: Int
    ) {
        vertex(poseStack, buffer, p0, color)
        vertex(poseStack, buffer, p1, color)
        vertex(poseStack, buffer, p2, color)
        vertex(poseStack, buffer, p0, color)
        vertex(poseStack, buffer, p2, color)
        vertex(poseStack, buffer, p3, color)
        vertex(poseStack, buffer, p2, color)
        vertex(poseStack, buffer, p1, color)
        vertex(poseStack, buffer, p0, color)
        vertex(poseStack, buffer, p3, color)
        vertex(poseStack, buffer, p2, color)
        vertex(poseStack, buffer, p0, color)
    }

    private fun vertex(poseStack: PoseStack, buffer: VertexConsumer, pos: FloatArray, color: Int) {
        buffer.addVertex(poseStack.last().pose(), pos[0], pos[1], pos[2])
            .setColor(ColorUtils.red(color), ColorUtils.green(color), ColorUtils.blue(color), 0.45f)
    }
}
