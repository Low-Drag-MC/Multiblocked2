package com.lowdragmc.mbd2.common.gui.editor.machine

import com.lowdragmc.lowdraglib2.gui.ColorPattern
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture
import com.lowdragmc.lowdraglib2.gui.texture.Icons
import com.lowdragmc.lowdraglib2.gui.ui.Style
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextArea
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper
import dev.vfyjxf.taffy.style.AlignItems
import dev.vfyjxf.taffy.style.FlexDirection
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

/**
 * The transport for a machine FX preview: play/pause, reset, seed, and a zoomable ruler you can drag
 * to inspect any tick.
 *
 * <h2>Why it sits under the scene</h2>
 * These controls are about *time*, and the thing they move is the picture above them — the same
 * relationship Photon's own {@code FXTimelineView} has with its scene. They started out as buttons in
 * the scene's top bar, mixed in with camera and overlay toggles, which said nothing about what they
 * act on and left no room for a ruler at all.
 *
 * <h2>Reproducibility is the point of the seed</h2>
 * A particle system is random, so replaying an effect to judge an edit means nothing unless the two
 * runs agree. The seed field pins the preview's {@code RandomSource} (see
 * {@code IMachineFXManager.setPreviewSeed}), which is the same mechanism Photon's editor uses; with it
 * fixed, dragging the playhead back and forth shows the same frames instead of a fresh roll each time.
 *
 * @param currentTicks the scene clock including the fraction of the current tick
 * @param onSeek       the tick to show; the host coalesces and re-simulates
 */
class FXTimelineStrip(
    private val currentTicks: (Float) -> Float,
    private val isPlaying: () -> Boolean,
    private val onSeek: (Long) -> Unit,
    private val onPlayPause: () -> Unit,
    private val onReset: () -> Unit,
    private val seed: () -> Long,
    private val onSeedChanged: (Long) -> Unit,
) : UIElement() {

    /** The visible span, in ticks. Zoomed with the wheel; this is what "the end of the timeline" means. */
    var visibleTicks: Int = DEFAULT_TICKS
        private set

    private val playIcon = UIElement()
    private val readout = Label()
    private val seedField = TextField()
    private val ruler = Ruler()

    init {
        // panel_bg / preview_bg below are LDLib2's own cross-theme surface vocabulary: every built-in
        // sheet (mc, ore, modern, gdp) defines both. Reusing them is what makes the strip follow the
        // editor's theme — including the one AppearanceSettings actually defaults to, which is ore,
        // not modern. Inventing per-sheet rules here would mean guessing four palettes, and guessing
        // the text colour for a surface is how the first attempt ended up illegible.
        addClass("panel_bg")
        addClass("__fx-timeline__")
        // Pinned, because panel_bg brings a panel's padding (5) and gap (4) with it and this is a
        // 20px strip. Layout is the one thing that is not the theme's business.
        Style.defaultPipeline(layout) {
            it.flexDirection(FlexDirection.ROW)
            it.alignItems(AlignItems.CENTER)
            it.widthPercent(100f)
            it.height(STRIP_HEIGHT)
            it.paddingAll(2f)
            it.gapAll(2f)
        }
        playIcon.layout {
            it.heightPercent(100f)
            it.aspectRatio(1f)
        }
        readout.addClass("__fx-timeline_readout__")
        readout.layout { it.width(96f) }
        seedField.addClass("__fx-timeline_seed__")

        val play = transportButton(null, ColorPattern.GREEN.color, "editor.machine.machine_fx.play") {
            onPlayPause()
        }
        play.addChildAt(playIcon, 0)

        seedField.setNumbersOnlyLong(Long.MIN_VALUE, Long.MAX_VALUE)
        seedField.setText(seed().toString(), false)
        seedField.setTextResponder { text -> text.toLongOrNull()?.let(onSeedChanged) }
        seedField.style { it.tooltips("editor.machine.machine_fx.seed") }
        seedField.layout {
            it.width(62f)
            it.heightPercent(100f)
        }

        addChildren(
            play,
            transportButton(Icons.STOP, ColorPattern.RED.color, "editor.machine.machine_fx.reset") {
                onReset()
            },
            transportButton(Icons.REPLAY, ColorPattern.CYAN.color, "editor.machine.machine_fx.reseed") {
                onSeedChanged(java.util.Random().nextLong())
            },
            seedField,
            ruler,
            readout,
        )
        addEventListener(UIEvents.TICK) { refresh() }
        refresh()
    }

    /**
     * Space toggles playback, as it does wherever there is a timeline.
     *
     * Handled here but *listened for* by the host, because a key event is delivered to the focused
     * element and bubbles up from there — a listener on this strip would only ever fire while the
     * strip itself held focus, which is to say almost never. The host is focusable and is an ancestor
     * of everything in the view, so it is the one place the key reliably passes through.
     *
     * @return whether the key was the transport's, so the host can stop it going further
     */
    fun onKeyDown(event: UIEvent): Boolean {
        if (event.keyCode != GLFW.GLFW_KEY_SPACE) return false
        // A space typed into a field is a space, not a transport command.
        if (event.target is TextField || event.target is TextArea) return false
        onPlayPause()
        return true
    }

    private fun refresh() {
        val ticks = currentTicks(0f).toLong()
        readout.setText(Component.translatable("editor.machine.machine_fx.time",
            ticks, String.format("%.1f", ticks / 20f), visibleTicks / SECOND_TICKS))
        val playing = isPlaying()
        playIcon.style {
            it.backgroundTexture((if (playing) Icons.PLAY_FILL else Icons.PLAY)
                .copy().setColor(if (playing) ColorPattern.GREEN.color else ColorPattern.GRAY.color))
        }
        // Not while it is being typed in: writing the model value back mid-edit fights the caret.
        if (!seedField.isFocused) {
            val current = seed().toString()
            if (seedField.value != current) seedField.setText(current, false)
        }
    }

    private fun transportButton(
        icon: IGuiTexture?,
        color: Int,
        tooltip: String,
        onClick: () -> Unit,
    ): Button {
        val button = Button()
        button.addClass("__fx-timeline_button__")
        button.setOnClick { onClick() }
        button.noText()
        icon?.let { button.addPreIcon(it.copy().setColor(color)) }
        button.style { it.tooltips(tooltip) }
        button.layout {
            it.paddingAll(0f)
            it.heightPercent(100f)
            it.aspectRatio(1f)
        }
        return button
    }

    /** Where the playhead sits for a scene clock of [ticks], as a fraction of the visible span. */
    private fun playheadFraction(ticks: Float): Float = (ticks / visibleTicks).coerceIn(0f, 1f)

    /**
     * Ticks, a playhead, and drag-to-seek.
     *
     * Drawn rather than assembled from widgets because it is one continuous scale: a row of elements
     * would have to be rebuilt on every zoom, and the playhead has to sit between the gridlines rather
     * than in a cell.
     */
    private inner class Ruler : UIElement() {
        init {
            addClass("__fx-timeline_ruler__")
            Style.defaultPipeline(layout) {
                it.flex(1f)
                it.heightPercent(100f)
                it.paddingHorizontal(2f)
            }
            Style.defaultPipeline(style) {
                it.background(ColorPattern.DARK_GRAY.rectTexture())
            }
            // startDrag is what registers this element as a drag source. Without it LDLib2 never sends
            // DRAG_SOURCE_UPDATE and the ruler only answers discrete clicks — which is exactly how the
            // first version of this behaved.
            addEventListener(UIEvents.MOUSE_DOWN) { event ->
                if (event.button == 0) {
                    startDrag(DRAG_PAYLOAD, null)
                    seekTo(event.x)
                    event.stopPropagation()
                }
            }
            addEventListener(UIEvents.DRAG_SOURCE_UPDATE) { event ->
                if (event.dragHandler?.draggingObject === DRAG_PAYLOAD) {
                    seekTo(event.x)
                    event.stopPropagation()
                }
            }
            // Zoom about the pointer, so the tick under the cursor stays under it.
            addEventListener(UIEvents.MOUSE_WHEEL) { event ->
                val width = getSizeWidth()
                if (width <= 0) return@addEventListener
                val fraction = ((event.x - getPositionX()) / width).coerceIn(0f, 1f)
                val anchored = fraction * visibleTicks
                val zoom = if (event.deltaY > 0) 1f / ZOOM_STEP else ZOOM_STEP
                visibleTicks = (visibleTicks * zoom).toInt().coerceIn(MIN_TICKS, MAX_TICKS)
                // A zoom-in that puts the anchored tick past the end is one the user cannot undo by
                // scrolling back, so keep the span wide enough to still contain it.
                if (anchored > visibleTicks) {
                    visibleTicks = anchored.toInt().coerceIn(MIN_TICKS, MAX_TICKS)
                }
                event.stopPropagation()
            }
        }

        private fun seekTo(mouseX: Float) {
            val width = getSizeWidth()
            if (width <= 0) return
            val fraction = ((mouseX - getPositionX()) / width).coerceIn(0f, 1f)
            onSeek((fraction * visibleTicks).toLong())
        }

        override fun drawBackgroundAdditional(guiContext: GUIContext) {
            super.drawBackgroundAdditional(guiContext)
            val width = getSizeWidth()
            if (width <= 0) return
            val graphics = guiContext.graphics
            val top = getPositionY()
            val bottom = top + getSizeHeight()

            // One gridline per second while they stay legible, then per five, then per thirty — a
            // zoomed-out ruler drawing six hundred lines is a grey block, which says nothing.
            val step = gridStep()
            var tick = 0
            while (tick <= visibleTicks) {
                val x = getPositionX() + width * (tick / visibleTicks.toFloat())
                val major = tick % (step * 5) == 0
                val height = if (major) getSizeHeight() * 0.55f else getSizeHeight() * 0.3f
                DrawerHelper.drawSolidRect(graphics, x, bottom - height, 1f, height,
                    gridColour(if (major) MAJOR_ALPHA else MINOR_ALPHA))
                tick += step
            }

            val playhead = currentTicks(guiContext.partialTick)
            val x = getPositionX() + width * playheadFraction(playhead)
            DrawerHelper.drawSolidRect(graphics, x, top, 1f, getSizeHeight(), ColorPattern.RED.color)
        }

        /**
         * The readout's own text colour at [alpha].
         *
         * Derived rather than fixed because a light sheet needs dark gridlines and a dark one needs
         * light, and the text colour is a value every theme already sets correctly for its background.
         */
        private fun gridColour(alpha: Int): Int =
            (readout.textStyle.textColor() and 0x00FFFFFF) or (alpha shl 24)

        /** Seconds per gridline, chosen so the lines stay roughly a gridline's width apart. */
        private fun gridStep(): Int {
            val seconds = visibleTicks / SECOND_TICKS
            return when {
                seconds <= 15 -> SECOND_TICKS
                seconds <= 60 -> SECOND_TICKS * 5
                else -> SECOND_TICKS * 30
            }
        }
    }

    companion object {
        const val STRIP_HEIGHT = 24f
        const val SECOND_TICKS = 20

        /** Ten seconds: enough for most machine effects, and the wheel is there for the rest. */
        private const val DEFAULT_TICKS = 200
        private const val MIN_TICKS = 20
        /** Photon bounds its own seek replay at 500s; offering a range past that would be a lie. */
        private const val MAX_TICKS = 500 * SECOND_TICKS
        private const val ZOOM_STEP = 1.25f

        /** Gridline opacity; the hue comes from the theme. */
        private const val MINOR_ALPHA = 0x40
        private const val MAJOR_ALPHA = 0x99

        /** Identity for the drag, so a drag begun elsewhere cannot move the playhead. */
        private val DRAG_PAYLOAD = Any()

    }
}
