package com.lowdragmc.mbd2.common.gui.editor.multiblopck

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
import com.lowdragmc.lowdraglib2.utils.data.BlockInfo
import com.lowdragmc.mbd2.api.registry.MBDRegistries
import com.lowdragmc.mbd2.api.pattern.MultiblockShapeInfo
import com.lowdragmc.mbd2.common.blockentity.MachineBlockEntity
import com.lowdragmc.mbd2.common.gui.editor.MBDEditor
import com.lowdragmc.mbd2.common.gui.editor.MultiblockMachineProject
import com.lowdragmc.mbd2.common.gui.editor.machine.MachineSceneView
import com.lowdragmc.mbd2.utils.ControllerBlockInfo
import dev.vfyjxf.taffy.style.FlexDirection
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component

class MultiblockShapeInfoView(
    editor: MBDEditor,
    val multiblockProject: MultiblockMachineProject
) : MachineSceneView("editor.machine.multiblock.multiblock_shape_info", editor, multiblockProject) {
    private val scrollerView = ScrollerView()
    private val pageContainer = UIElement()
    private var selectedShapeInfo: MultiblockShapeInfo? = null
    private var showingBuiltinPages = false

    init {
        addChild(
            splitViewHorizontal {
                withPercentage(25f)
                withLeft(scrollerView.apply {
                    layoutDsl {
                        width(100.pct)
                        flex(1f)
                    }
                    pageContainer.layoutDsl {
                        width(100.pct)
                        flexDirection(FlexDirection.COLUMN)
                        gap { all(2.px) }
                    }
                    addScrollViewChild(pageContainer)
                })
                withRight(sceneEditor)
            }
        )
        reloadShapeInfos()
    }

    fun reloadShapeInfos() {
        pageContainer.clearAllChildren()
        level.clear()
        selectedShapeInfo = null
        val customPages = multiblockProject.multiblockShapeInfos
        val pages = if (customPages.isEmpty()) {
            showingBuiltinPages = true
            multiblockProject.createAutoShapeInfos()
        } else {
            showingBuiltinPages = false
            customPages.toList()
        }
        pages.forEachIndexed { index, shapeInfo ->
            pageContainer.addChild(createShapeInfoRow(index, shapeInfo))
        }
    }

    private fun createShapeInfoRow(index: Int, shapeInfo: MultiblockShapeInfo): UIElement {
        val row = UIElement().dsl({
            layout = {
                width(100.pct)
                height(18.px)
                flexDirection(FlexDirection.ROW)
                gap { all(2.px) }
            }
            style = {
                background(IGuiTexture.EMPTY)
            }
        }) {
            element({
                layout = {
                    height(100.pct)
                    aspectRatio(1f)
                }
                style = { background(if (showingBuiltinPages) Icons.RESOURCE else Icons.WIDGET_CUSTOM) }
            })
            label({
                layout = {
                    flex(1f)
                    height(100.pct)
                }
                textStyle = {
                    textWrap(TextWrap.HOVER_ROLL)
                    textAlignVertical(Vertical.CENTER)
                }
            }) {
                dataSource {
                    Component.literal(if (showingBuiltinPages) "auto-built" else "page: $index")
                }
            }
        }.build()
        row.addEventListener(UIEvents.MOUSE_DOWN, { event ->
            if (event.button == 0) {
                selectShapeInfo(shapeInfo)
            } else if (event.button == 1 && !showingBuiltinPages) {
                val menu = com.lowdragmc.lowdraglib2.gui.util.TreeBuilder.Menu.start()
                menu.leaf(Icons.REMOVE, "editor.machine.multiblock.multiblock_shape_info.remove") {
                    multiblockProject.multiblockShapeInfos.remove(shapeInfo)
                    clearShapeInfo()
                    reloadShapeInfos()
                }
                editor.openMenu(event.x, event.y, menu)
            }
        })
        return row
    }

    private fun selectShapeInfo(shapeInfo: MultiblockShapeInfo) {
        selectedShapeInfo = shapeInfo
        loadShapeScene(shapeInfo)
        if (showingBuiltinPages) {
            if (editor.inspectorView.inspector.inspectedConfigurable == shapeInfo) {
                editor.inspectorView.clear()
            }
        } else {
            editor.inspectorView.inspect(shapeInfo, null, null)
        }
    }

    private fun clearShapeInfo() {
        selectedShapeInfo = null
        level.clear()
        editor.inspectorView.clear()
    }

    private fun loadShapeScene(shapeInfo: MultiblockShapeInfo) {
        level.clear()
        val blocks = shapeInfo.blocks
        val blockInfos = mutableMapOf<BlockPos, BlockInfo>()
        for (x in blocks.indices) {
            for (y in blocks[x].indices) {
                for (z in blocks[x][y].indices) {
                    val pos = BlockPos(x, y, z)
                    val blockInfo = when (val info = blocks[x][y][z]) {
                        is ControllerBlockInfo -> {
                            BlockInfo(
                                MBDRegistries.getFakeMachineDefinition().block().defaultBlockState()
                                    .setValue(MBDRegistries.getFakeMachineDefinition().blockProperties()
                                        .rotationState().property.orElseThrow(), info.facing),
                                { blockEntity ->
                                    if (blockEntity is MachineBlockEntity) {
                                        val controllerMachine = project.definition.createMachine(blockEntity)
                                        blockEntity.setMachine(controllerMachine)
                                        controllerMachine.loadAdditionalTraits();
                                    }
                                }
                            )
                        }
                        else -> info
                    }
                    blockInfos[pos] = blockInfo
                }
            }
        }
        sceneEditor.scene.setRenderedCore(blockInfos.keys)
        blockInfos.forEach { (pos, info) ->
            level.addBlock(pos, info)
        }
    }
}
