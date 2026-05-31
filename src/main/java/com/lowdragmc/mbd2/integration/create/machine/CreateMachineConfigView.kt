package com.lowdragmc.mbd2.integration.create.machine

import com.lowdragmc.lowdraglib2.gui.ColorPattern
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.TrackData
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.getValue
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.setValue
import com.lowdragmc.lowdraglib2.gui.texture.Icons
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical
import com.lowdragmc.lowdraglib2.gui.ui.element
import com.lowdragmc.lowdraglib2.gui.ui.elements.asNumeric
import com.lowdragmc.lowdraglib2.gui.ui.elements.label
import com.lowdragmc.lowdraglib2.gui.ui.elements.textField
import com.lowdragmc.lowdraglib2.gui.ui.elements.toggle
import com.lowdragmc.lowdraglib2.gui.ui.layout.pct
import com.lowdragmc.lowdraglib2.gui.ui.layout.px
import com.lowdragmc.mbd2.common.gui.editor.MBDEditor
import com.lowdragmc.mbd2.common.gui.editor.MachineProject
import com.lowdragmc.mbd2.common.gui.editor.machine.MachineConfigView
import dev.vfyjxf.taffy.style.AlignContent
import dev.vfyjxf.taffy.style.AlignItems
import dev.vfyjxf.taffy.style.FlexDirection
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.network.chat.Component

/**
 * Create-specific {@link MachineConfigView} that adds editor preview controls for kinetic
 * rotation: an "is rotating" toggle plus a stress field. Pushes the derived simulated
 * speed into {@link KineticInstanceRenderer#EDITOR_PREVIEW_SPEED} during the scene's
 * {@code renderAfterWorld} so the (non-KineticBE) preview machine actually spins.
 */
open class CreateMachineConfigView(editor: MBDEditor, project: MachineProject) : MachineConfigView(editor, project) {
    private val previewRotatingData: TrackData<Boolean> = TrackData(true)
    private val previewStressData: TrackData<Float> = TrackData(128f)
    var isPreviewRotating by previewRotatingData
    var previewStress by previewStressData

    init {
        sceneEditor.topBar.addChild(element({
            layout = {
                flexDirection(FlexDirection.ROW)
                justifyContent(AlignContent.FLEX_END)
                alignItems(AlignItems.CENTER)
                height(100.pct)
                gap { all(2.px) }
                padding { horizontal(4.px) }
            }
        }) {
            toggle(toggleSpec(Icons.ROTATION, ColorPattern.GREEN.color, "config.create_kinetic_machine.is_preview_rotating")) {
                bindUIData(previewRotatingData)
            }
            label({
                layout = { height(100.pct) }
                text = Component.translatable("config.create_kinetic_machine.preview_stress")
                textStyle = { textAlignVertical(Vertical.CENTER); adaptiveWidth(true) }
            })
            textField({
                layout = {
                    width(48.px)
                    height(100.pct)
                }
            }) {
                observer { text -> text.toFloatOrNull()?.let { previewStress = it } }
                dataSource { previewStress.toString() }
            }.asNumeric<_, Float>(0f, Float.MAX_VALUE)
        })
        // Push the simulated speed into the thread-local BEFORE the scene renders the world
        // (renderAfterWorld is too late — the block has already drawn). The after-hook clears it.
        sceneEditor.scene.setBeforeWorldRender { pushPreviewSpeed() }
    }

    private fun pushPreviewSpeed() {
        val definition = project.definition
        if (isPreviewRotating && definition is CreateKineticMachineDefinition) {
            val torque = definition.kineticMachineSettings().torque.coerceAtLeast(Float.MIN_VALUE)
            val maxRpm = definition.kineticMachineSettings().maxRPM.toFloat()
            val speed = (previewStress / torque).coerceAtMost(maxRpm)
            KineticInstanceRenderer.EDITOR_PREVIEW_SPEED.set(speed)
        } else {
            KineticInstanceRenderer.EDITOR_PREVIEW_SPEED.remove()
        }
    }

    override fun renderAfterWorld(bufferSource: MultiBufferSource, partialTicks: Float) {
        try {
            super.renderAfterWorld(bufferSource, partialTicks)
        } finally {
            KineticInstanceRenderer.EDITOR_PREVIEW_SPEED.remove()
        }
    }
}
