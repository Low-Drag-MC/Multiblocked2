package com.lowdragmc.mbd2.common.gui.editor.multiblopck

import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderTypes
import com.lowdragmc.lowdraglib2.client.utils.RenderBufferUtils
import com.lowdragmc.lowdraglib2.configurator.IConfigurable
import com.lowdragmc.lowdraglib2.configurator.ui.ArrayConfiguratorGroup
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath
import com.lowdragmc.lowdraglib2.gui.ColorPattern
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture
import com.lowdragmc.lowdraglib2.gui.texture.Icons
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer
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
import com.lowdragmc.lowdraglib2.utils.data.BlockInfo
import com.lowdragmc.mbd2.api.registry.MBDRegistries
import com.lowdragmc.mbd2.common.blockentity.MachineBlockEntity
import com.lowdragmc.mbd2.common.gui.editor.MBDEditor
import com.lowdragmc.mbd2.common.gui.editor.MultiblockMachineProject
import com.lowdragmc.mbd2.common.gui.editor.machine.MachineSceneView
import com.lowdragmc.mbd2.common.machine.definition.config.BlockPlaceholder
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import dev.vfyjxf.taffy.style.AlignItems
import dev.vfyjxf.taffy.style.FlexDirection
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component

class MultiblockPatternView(
    editor: MBDEditor,
    val multiblockProject: MultiblockMachineProject
) : MachineSceneView("editor.machine.multiblock_pattern", editor, multiblockProject) {

    private val scrollerView = ScrollerView()
    private val layerContainer = UIElement()
    private val selectedBlocks = LinkedHashSet<BlockPos>()
    private val expandedLayers = LinkedHashSet<Int>()
    private val blockRows = HashMap<BlockPos, UIElement>()
    private lateinit var visibleToggleGroup: Toggle.ToggleGroup
    private var visibleLayer = -1
    private var predicateClipboard: List<IResourcePath>? = null
    private var lastClickTick = 0L

    init {
        addChild(
            splitViewHorizontal {
                withPercentage(25f)
                withLeft(scrollerView.apply {
                    layoutDsl {
                        width(100.pct)
                        flex(1f)
                    }
                    layerContainer.layoutDsl {
                        width(100.pct)
                        flexDirection(FlexDirection.COLUMN)
                        gap { all(2.px) }
                    }
                    addScrollViewChild(layerContainer)
                })
                withRight(sceneEditor)
            }
        )
        sceneEditor.scene.setOnSelected { pos, _ -> selectFromScene(pos) }
        sceneEditor.scene.addEventListener(UIEvents.MOUSE_UP, { event ->
            if (event.button == 1) {
                openMenu(event.x, event.y)
            }
        })
        reloadLayers()
        reloadScene(true, false)
    }

    fun onBlockPlaceholdersChanged() {
        visibleLayer = -1
        reloadLayers()
        reloadScene(true, false)
    }

    private fun reloadLayers() {
        blockRows.clear()
        visibleToggleGroup = Toggle.ToggleGroup().setAllowEmpty(true)
        layerContainer.clearAllChildren()
        layerContainer.addChild(createAxisSelector())
        val layerCount = layerCount()
        for (index in 0 until layerCount) {
            layerContainer.addChild(createLayerGroup(index))
        }
    }

    private fun createAxisSelector(): UIElement {
        return element({
            layout = {
                width(100.pct)
                height(16.px)
                flexDirection(FlexDirection.ROW)
                alignItems(AlignItems.CENTER)
                gap { all(2.px) }
            }
        }) {
            label({
                layout = {
                    flex(1f)
                    height(100.pct)
                }
                text = Component.translatable("editor.machine.multiblock.multiblock_pattern.layer_direction")
                textStyle = {
                    textWrap(TextWrap.HOVER_ROLL)
                    textAlignVertical(Vertical.CENTER)
                }
            })
            axisButton(Direction.Axis.X)
            axisButton(Direction.Axis.Y)
            axisButton(Direction.Axis.Z)
        }
    }

    private fun UIContainer<*, *>.axisButton(axis: Direction.Axis) {
        button({
            layout = {
                size(14.px)
            }
            text(axis.name.lowercase(), false)
            style = {
                background(if (multiblockProject.layerAxis == axis) ColorPattern.T_GREEN.rectTexture() else ColorPattern.T_GRAY.rectTexture())
            }
            onClick = {
                multiblockProject.updateLayerAxis(axis)
                visibleLayer = -1
                reloadLayers()
                reloadScene(false, true)
            }
        })
    }

    private fun createLayerGroup(index: Int): UIElement {
        val children = UIElement().layoutDsl {
            width(100.pct)
            flexDirection(FlexDirection.COLUMN)
            gap { all(1.px) }
        }
        children.setDisplay(expandedLayers.contains(index))
        val layer = element({
            layout = {
                width(100.pct)
                flexDirection(FlexDirection.COLUMN)
                gap { all(1.px) }
            }
            style = { background(ColorPattern.T_GRAY.borderTexture(-2)) }
        }) {
            element({
                layout = {
                    width(100.pct)
                    height(16.px)
                    flexDirection(FlexDirection.ROW)
                    alignItems(AlignItems.CENTER)
                    gap { all(2.px) }
                }
            }) {
                button({
                    layout = { size(14.px) }
                    noText()
                    onClick = {
                        val expanded = !children.isDisplayed
                        children.setDisplay(expanded)
                        if (expanded) {
                            expandedLayers.add(index)
                        } else {
                            expandedLayers.remove(index)
                        }
                    }
                }) { api { addPreIcon(Icons.RIGHT) } }
                label({
                    layout = {
                        flex(1f)
                        height(100.pct)
                    }
                    text = Component.literal("${multiblockProject.layerAxis.name}: $index")
                    textStyle = { textAlignVertical(Vertical.CENTER) }
                })
                toggle({
                    layout = { size(14.px) }
                    toggleGroup = visibleToggleGroup
                    isOn = visibleLayer == index
                    noText()
                    toggleStyle = {
                        unmarkTexture(Icons.EYE.copy().setColor(ColorPattern.GRAY.color).scale(0.65f))
                        markTexture(Icons.EYE.copy().setColor(ColorPattern.GREEN.color).scale(0.65f))
                    }
                    onToggle { visible ->
                        visibleLayer = if (visible) {
                            index
                        } else if (visibleLayer == index) {
                            -1
                        } else {
                            visibleLayer
                        }
                        reloadScene(false, true)
                    }
                })
            }
            createRepetitionRow(index)
        }
        layer.addChild(children)
        slicePositions(index).forEach { pos ->
            children.addChild(createBlockRow(pos))
        }
        return layer
    }

    private fun UIContainer<*, *>.createRepetitionRow(index: Int) {
        val hasController = slicePositions(index).any { blockAt(it).isController }
        if (hasController) {
            label({
                layout = {
                    width(100.pct)
                    height(12.px)
                }
                text = Component.translatable("editor.machine.multiblock.multiblock_pattern.repetition_controller")
                textStyle = {
                    textWrap(TextWrap.HOVER_ROLL)
                    textAlignVertical(Vertical.CENTER)
                }
            })
            return
        }
        val repetition = multiblockProject.aisleRepetitions[index]
        element({
            layout = {
                width(100.pct)
                height(14.px)
                flexDirection(FlexDirection.ROW)
                alignItems(AlignItems.CENTER)
                gap { all(2.px) }
            }
        }) {
            numberField("min", { repetition[0] }) {
                repetition[0] = it.coerceAtLeast(1)
                if (repetition[0] > repetition[1]) repetition[1] = repetition[0]
            }
            numberField("max", { repetition[1] }) {
                repetition[1] = it.coerceAtLeast(1)
                if (repetition[0] > repetition[1]) repetition[0] = repetition[1]
            }
        }
    }

    private fun UIContainer<*, *>.numberField(label: String, getter: () -> Int, setter: (Int) -> Unit) {
        label({
            layout = { height(100.pct) }
            text = Component.literal(label)
            textStyle = { textAlignVertical(Vertical.CENTER);adaptiveWidth(true); }
        })
        textField({
            layout = {
                width(34.px)
                height(100.pct)
            }
        }) {
            observer { text ->
                text.toIntOrNull()?.let(setter)
            }
            dataSource { getter().toString() }
        }.asNumeric(1, 100)
    }

    private fun createBlockRow(pos: BlockPos): UIElement {
        val holder = blockAt(pos)
        return UIElement().dsl({
            layout = {
                width(100.pct)
                height(16.px)
                flexDirection(FlexDirection.ROW)
                alignItems(AlignItems.CENTER)
                gap { all(2.px) }
            }
            style = {
                background(if (selectedBlocks.contains(pos)) ColorPattern.T_GREEN.rectTexture() else IGuiTexture.EMPTY)
            }
        }) {
            element({
                layout = {
                    size(14.px)
                }
                style = { background(ItemStackTexture(*itemCandidates(holder))) }
            })
            label({
                layout = {
                    flex(1f)
                    height(100.pct)
                }
                text = Component.literal(formatLayerPos(pos))
                textStyle = {
                    textWrap(TextWrap.HOVER_ROLL)
                    textAlignVertical(Vertical.CENTER)
                }
            })
        }.build().apply {
            addEventListener(UIEvents.MOUSE_DOWN, { event ->
                if (event.button == 0) {
                    selectBlock(pos, event.isCtrlDown || event.isShiftDown)
                } else if (event.button == 1) {
                    openMenu(event.x, event.y)
                }
            })
            blockRows[pos] = this
        }
    }

    private fun selectFromScene(pos: BlockPos) {
        if (!inBounds(pos)) return
        val tick = modularUI?.tickCounter ?: 0
        if (!isCtrlDown() && !isShiftDown() && tick - lastClickTick in 1..9) {
            val predicates = blockAt(pos).predicates
            val same = ArrayList<BlockPos>()
            forEachPlaceholder { candidate, holder ->
                if (holder.predicates == predicates) same.add(candidate)
            }
            selectedBlocks.clear()
            selectedBlocks.addAll(same)
            updateBlockRowStyles()
            updatePredicateInspector()
            reloadScene(false, true)
        } else {
            selectBlock(pos, isCtrlDown() || isShiftDown())
        }
        lastClickTick = tick
    }

    private fun selectBlock(pos: BlockPos, additive: Boolean) {
        if (!additive) selectedBlocks.clear()
        if (additive && selectedBlocks.contains(pos)) {
            selectedBlocks.remove(pos)
        } else {
            selectedBlocks.add(pos)
        }
        updateBlockRowStyles()
        updatePredicateInspector()
        reloadScene(false, true)
    }

    private fun openMenu(mouseX: Float, mouseY: Float) {
        if (selectedBlocks.isEmpty()) return
        val menu = TreeBuilder.Menu.start()
        menu.leaf(Icons.COPY, "ldlib.gui.editor.menu.copy") {
            predicateClipboard = commonPredicates()
        }
        predicateClipboard?.let { copied ->
            menu.leaf(Icons.PASTE, "ldlib.gui.editor.menu.paste") {
                selectedBlocks.forEach { pos ->
                    blockAt(pos).predicates.apply {
                        clear()
                        addAll(copied)
                    }
                }
                reloadScene(false, true)
                reloadLayers()
                updateBlockRowStyles()
                updatePredicateInspector()
            }
        }
        if (selectedBlocks.size == 1) {
            menu.crossLine()
            menu.branch("editor.machine.multiblock.multiblock_pattern.set_as_controller") { branch ->
                Direction.entries.forEach { facing ->
                    branch.leaf(facing.serializedName) {
                        forEachPlaceholder { _, holder -> holder.setController(false) }
                        val holder = blockAt(selectedBlocks.first())
                        holder.setController(true)
                        holder.setFacing(facing)
                        reloadScene(false, true)
                        reloadLayers()
                        updateBlockRowStyles()
                        updatePredicateInspector()
                    }
                }
            }
        }
        editor.openMenu(mouseX, mouseY, menu)
    }

    private fun reloadScene(clearSelected: Boolean, keepZoom: Boolean) {
        val previousZoom = sceneEditor.scene.zoom
        level.clear()
        if (clearSelected) selectedBlocks.clear()
        val holders = mutableMapOf<BlockPos, BlockPlaceholder>()
        var frontSide = Direction.NORTH
        forEachPlaceholder { pos, holder ->
            if (visibleLayer >= 0 && layerIndex(pos) != visibleLayer) return@forEachPlaceholder
            holders[pos] = holder
            if (holder.isController) {
                frontSide = holder.facing
            }
        }
        sceneEditor.scene.setRenderedCore(holders.keys)

        holders.forEach { (pos, holder) ->
            if (holder.isController) {
                MBDRegistries.getFakeMachineDefinition().apply {
                    blockProperties().rotationState().property.ifPresent({
                        level.addBlock(pos, BlockInfo.fromBlockState(block().defaultBlockState()
                            .setValue(it, holder.getFacing())))
                    })
                }
                (level.getBlockEntity(pos) as? MachineBlockEntity)?.let {
                    val controllerMachine =multiblockProject.multiblockDefinition.createMachine(it)
                    it.setMachine(controllerMachine)
                    controllerMachine.loadAdditionalTraits()
                    controllerMachine.additionalTraits.forEach { trait -> trait.onLoadingTraitInPreview() }
                }
            } else {
                holder.predicates.asSequence()
                    .mapNotNull { PredicateResource.INSTANCE.resourceInstance.getResource(it) }
                    .filter { !it.controllerFront.isEnable || it.controllerFront.value == frontSide }
                    .firstNotNullOfOrNull { it.candidates?.firstOrNull() }
                    ?.let { level.addBlock(pos, it) }
            }
        }
        if (keepZoom) sceneEditor.scene.zoom = previousZoom
    }

    private fun updateBlockRowStyles() {
        blockRows.forEach { (pos, row) ->
            row.style {
                it.backgroundTexture(if (selectedBlocks.contains(pos)) {
                    ColorPattern.T_GREEN.rectTexture()
                } else {
                    IGuiTexture.EMPTY
                })
            }
        }
    }

    private fun updatePredicateInspector() {
        if (selectedBlocks.isEmpty()) {
            editor.inspectorView.clear()
        } else {
            editor.inspectorView.inspect(PredicateSelectionConfigurable(selectedBlocks.toList()))
        }
    }

    private inner class PredicateSelectionConfigurable(
        private val positions: List<BlockPos>
    ) : IConfigurable {
        override fun buildConfigurator(father: ConfiguratorGroup) {
            val placeholders = positions.filter(::inBounds).map(::blockAt)
            if (placeholders.isEmpty()) return
            val predicates: MutableList<IResourcePath> = ArrayList(placeholders.first().predicates)
            placeholders.drop(1).forEach { predicates.retainAll(it.predicates) }
            val group = ArrayConfiguratorGroup(
                "mbd2.gui.editor.group.predicate",
                false,
                { predicates },
                { getter, setter -> PredicatePathConfigurator(getter, setter) as Configurator },
                true
            )
            group.setAddDefault { MultiblockMachineProject.builtinPath("any") }
            group.setOnUpdate { values ->
                predicates.clear()
                predicates.addAll(values)
                placeholders.forEach { holder ->
                    holder.predicates.clear()
                    holder.predicates.addAll(values)
                }
                reloadScene(false, true)
                reloadLayers()
                updateBlockRowStyles()
            }
            father.addConfigurators(group)
        }
    }

    override fun renderAfterWorld(bufferSource: MultiBufferSource, partialTicks: Float) {
        val buffer = bufferSource.getBuffer(LDLibRenderTypes.positionColorNoDepth())
        val poseStack = PoseStack()
        RenderSystem.disableDepthTest()
        selectedBlocks.forEach { pos ->
            RenderBufferUtils.drawCubeFace(
                poseStack,
                buffer,
                pos.x.toFloat(),
                pos.y.toFloat(),
                pos.z.toFloat(),
                pos.x + 1f,
                pos.y + 1f,
                pos.z + 1f,
                0.1f,
                0.7f,
                0.1f,
                0.7f,
                false
            )
        }
        if (bufferSource is MultiBufferSource.BufferSource) {
            bufferSource.endBatch()
        }
    }

    private fun commonPredicates(): List<IResourcePath> {
        val first = selectedBlocks.firstOrNull() ?: return emptyList()
        val common = LinkedHashSet(blockAt(first).predicates)
        selectedBlocks.drop(1).forEach { common.retainAll(blockAt(it).predicates) }
        return common.toList()
    }

    private fun itemCandidates(holder: BlockPlaceholder): Array<net.minecraft.world.item.ItemStack> {
        return holder.predicates.asSequence()
            .mapNotNull { PredicateResource.INSTANCE.resourceInstance.getResource(it) }
            .flatMap { it.itemCandidates.asSequence() }
            .filter { !it.isEmpty }
            .toList()
            .toTypedArray()
    }

    private fun layerCount(): Int {
        val holders = multiblockProject.blockPlaceholders
        return when (multiblockProject.layerAxis) {
            Direction.Axis.X -> holders.size
            Direction.Axis.Y -> holders[0].size
            Direction.Axis.Z -> holders[0][0].size
        }
    }

    private fun slicePositions(index: Int): List<BlockPos> {
        val result = ArrayList<BlockPos>()
        forEachPlaceholder { pos, _ ->
            if (layerIndex(pos) == index) result.add(pos)
        }
        return result
    }

    private fun layerIndex(pos: BlockPos): Int {
        return when (multiblockProject.layerAxis) {
            Direction.Axis.X -> pos.x
            Direction.Axis.Y -> pos.y
            Direction.Axis.Z -> pos.z
        }
    }

    private fun formatLayerPos(pos: BlockPos): String {
        return when (multiblockProject.layerAxis) {
            Direction.Axis.X -> "Y: ${pos.y}, Z: ${pos.z}"
            Direction.Axis.Y -> "X: ${pos.x}, Z: ${pos.z}"
            Direction.Axis.Z -> "X: ${pos.x}, Y: ${pos.y}"
        }
    }

    private fun blockAt(pos: BlockPos): BlockPlaceholder {
        return multiblockProject.blockPlaceholders[pos.x][pos.y][pos.z]
    }

    private fun inBounds(pos: BlockPos): Boolean {
        val holders = multiblockProject.blockPlaceholders
        return pos.x in holders.indices && pos.y in holders[pos.x].indices && pos.z in holders[pos.x][pos.y].indices
    }

    private inline fun forEachPlaceholder(action: (BlockPos, BlockPlaceholder) -> Unit) {
        val holders = multiblockProject.blockPlaceholders
        for (x in holders.indices) {
            for (y in holders[x].indices) {
                for (z in holders[x][y].indices) {
                    action(BlockPos(x, y, z), holders[x][y][z])
                }
            }
        }
    }
}
