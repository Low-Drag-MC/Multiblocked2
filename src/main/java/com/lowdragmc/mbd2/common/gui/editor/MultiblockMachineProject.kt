package com.lowdragmc.mbd2.common.gui.editor

import com.lowdragmc.lowdraglib2.editor.project.IProject
import com.lowdragmc.lowdraglib2.editor.project.ProjectType
import com.lowdragmc.lowdraglib2.editor.resource.BuiltinResourceProvider
import com.lowdragmc.lowdraglib2.editor.resource.ColorsResource
import com.lowdragmc.lowdraglib2.editor.resource.FileResourceProvider
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath
import com.lowdragmc.lowdraglib2.editor.resource.IRendererResource
import com.lowdragmc.lowdraglib2.editor.resource.Resources
import com.lowdragmc.lowdraglib2.editor.resource.TexturesResource
import com.lowdragmc.lowdraglib2.editor.resource.UIResource
import com.lowdragmc.lowdraglib2.utils.data.BlockInfo
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture
import com.lowdragmc.lowdraglib2.utils.TagBuilder
import com.lowdragmc.mbd2.MBD2
import com.lowdragmc.mbd2.api.pattern.MultiblockShapeInfo
import com.lowdragmc.mbd2.api.pattern.predicates.PatternPredicate
import com.lowdragmc.mbd2.api.pattern.predicates.PredicateBlocks
import com.lowdragmc.mbd2.api.pattern.predicates.PredicateFluids
import com.lowdragmc.mbd2.common.gui.editor.multiblopck.MultiblockAreaView
import com.lowdragmc.mbd2.common.gui.editor.multiblopck.MultiblockPatternView
import com.lowdragmc.mbd2.common.gui.editor.multiblopck.MultiblockShapeInfoView
import com.lowdragmc.mbd2.common.gui.editor.multiblopck.PredicateResource
import com.lowdragmc.mbd2.common.machine.definition.MultiblockMachineDefinition
import com.lowdragmc.mbd2.common.machine.definition.config.BlockPlaceholder
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState
import com.lowdragmc.mbd2.common.machine.definition.config.StateMachine
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import com.lowdragmc.lowdraglib2.editor.ui.Editor
import com.lowdragmc.mbd2.client.MBDRenderers
import com.lowdragmc.mbd2.utils.ControllerBlockInfo
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.IntArrayTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LiquidBlock
import java.io.File
import java.util.Arrays

open class MultiblockMachineProject : MachineProject() {
    companion object {
        val TYPE: ProjectType = MultiblockMachineProjectType()

        @JvmStatic
        fun builtinPath(name: String): IResourcePath {
            return BuiltinResourceProvider.TYPE.createFullPath(name)
        }

        @JvmStatic
        fun serializeBlockPlaceholders(
            provider: HolderLookup.Provider,
            blockPlaceholders: Array<Array<Array<BlockPlaceholder>>>
        ): CompoundTag {
            val placeholders = ArrayList<BlockPlaceholder>()
            val placeholderMap = HashMap<BlockPlaceholder, Int>()
            val placeholderIndex = ArrayList<Int>()
            for (xSlice in blockPlaceholders) {
                for (ySlice in xSlice) {
                    for (placeholder in ySlice) {
                        if (!placeholderMap.containsKey(placeholder)) {
                            placeholderMap[placeholder] = placeholders.size
                            placeholders.add(placeholder)
                        }
                        placeholderIndex.add(placeholderMap[placeholder]!!)
                    }
                }
            }
            val tag = CompoundTag()
            val list = ListTag()
            for (placeholder in placeholders) {
                list.add(placeholder.serializeNBT(provider))
            }
            tag.put("holders", list)
            tag.putInt("x", blockPlaceholders.size)
            tag.putInt("y", blockPlaceholders[0].size)
            tag.putInt("z", blockPlaceholders[0][0].size)
            tag.putIntArray("pattern", placeholderIndex.toIntArray())
            return tag
        }

        @JvmStatic
        fun deserializeBlockPlaceholders(tag: CompoundTag): Array<Array<Array<BlockPlaceholder>>> {
            if (!tag.contains("holders") || tag.getInt("x") <= 0 || tag.getInt("y") <= 0 || tag.getInt("z") <= 0) {
                return defaultBlockPlaceholders()
            }
            val list = tag.getList("holders", Tag.TAG_COMPOUND.toInt())
            val x = tag.getInt("x")
            val y = tag.getInt("y")
            val z = tag.getInt("z")
            val pattern = tag.getIntArray("pattern")
            val blockPlaceholders = Array(x) { Array(y) { Array(z) { BlockPlaceholder.create(builtinPath("any")) } } }
            for (i in pattern.indices) {
                val index = pattern[i]
                val placeholder = if (index == -1) {
                    BlockPlaceholder.create(builtinPath("any"))
                } else {
                    BlockPlaceholder.fromTag(list.getCompound(index))
                }
                blockPlaceholders[i / (y * z)][(i / z) % y][i % z] = placeholder
            }
            return blockPlaceholders
        }

        fun defaultBlockPlaceholders(): Array<Array<Array<BlockPlaceholder>>> {
            return Array(1) { Array(1) { Array(1) { BlockPlaceholder.controller(builtinPath("any")) } } }
        }
    }

    private class MultiblockMachineProjectType : ProjectType(IGuiTexture.EMPTY, "multiblock_machine_project", ".mb", { MultiblockMachineProject() }) {
        override fun getRootSavePath(project: IProject, projectRoot: File): File {
            return projectRoot.resolve("mbd2/multiblock")
        }
    }

    val multiblockDefinition: MultiblockMachineDefinition
        get() = definition as MultiblockMachineDefinition

    var blockPlaceholders: Array<Array<Array<BlockPlaceholder>>> = defaultBlockPlaceholders()
        set(value) {
            field = value
            updateLayerAxis(layerAxis)
        }

    var layerAxis: Direction.Axis = Direction.Axis.Y
        protected set

    var aisleRepetitions: Array<IntArray> = createDefaultAisleRepetitions(layerAxis)
        protected set

    val multiblockShapeInfos: MutableList<MultiblockShapeInfo> = ArrayList()
    var multiblockAreaView: MultiblockAreaView? = null
        protected set
    var multiblockPatternView: MultiblockPatternView? = null
        protected set
    var multiblockShapeInfoView: MultiblockShapeInfoView? = null
        protected set

    override fun createResources(): Resources {
        return Resources.of(
            IRendererResource.INSTANCE,
            ColorsResource.INSTANCE,
            TexturesResource.INSTANCE,
            UIResource.INSTANCE,
            PredicateResource.INSTANCE
        )
    }

    override fun createDefinition(): MultiblockMachineDefinition {
        return MultiblockMachineDefinition(
            MBD2.id("new_multiblock"),
            StateMachine.createMultiblockDefault({ MachineState.baseBuilder() },
                {MBDRenderers.MACHINE_UNFORMED},
                {MBDRenderers.MACHINE_FORMED},
                {MBDRenderers.MACHINE_WORKING},
                {MBDRenderers.MACHINE_WAITING},
                {MBDRenderers.MACHINE_WAITING},
                ),
            null,
            null,
            null,
            null,
            null
        )
    }

    fun updateLayerAxis(axis: Direction.Axis) {
        layerAxis = axis
        aisleRepetitions = createDefaultAisleRepetitions(axis)
    }

    fun resizeBlockPlaceholders(xSize: Int, ySize: Int, zSize: Int) {
        val x = xSize.coerceAtLeast(1)
        val y = ySize.coerceAtLeast(1)
        val z = zSize.coerceAtLeast(1)
        val resized = Array(x) { newX ->
            Array(y) { newY ->
                Array(z) { newZ ->
                    blockPlaceholders.getOrNull(newX)
                        ?.getOrNull(newY)
                        ?.getOrNull(newZ)
                        ?: BlockPlaceholder.create(builtinPath("any"))
                }
            }
        }
        blockPlaceholders = resized
        ensureController()
    }

    fun updateAisleRepetition(index: Int, min: Int, max: Int) {
        if (index !in aisleRepetitions.indices) return
        val safeMin = min.coerceAtLeast(1)
        aisleRepetitions[index][0] = safeMin
        aisleRepetitions[index][1] = max.coerceAtLeast(safeMin)
    }

    fun regenerateShapeInfos() {
        multiblockShapeInfos.clear()
        multiblockShapeInfos.addAll(createAutoShapeInfos())
    }

    fun createAutoShapeInfos(): List<MultiblockShapeInfo> {
        ensureController()
        val blockPattern = MultiblockMachineDefinition.createBlockPattern(
            blockPlaceholders,
            layerAxis,
            aisleRepetitions,
            multiblockDefinition,
            true
        )
        val generated = ArrayList<MultiblockShapeInfo>()
        val repetition = aisleRepetitions.map { it[0] }.toIntArray()
        generated.add(MultiblockShapeInfo(blockPattern.getPreview(repetition)))
        for (layer in aisleRepetitions.indices) {
            val range = aisleRepetitions[layer]
            for (repeats in range[0] + 1..range[1]) {
                repetition[layer] = repeats
                generated.add(MultiblockShapeInfo(blockPattern.getPreview(repetition)))
                repetition[layer] = range[0]
            }
        }
        return generated
    }

    private fun ensureController() {
        if (blockPlaceholders.none { xSlice -> xSlice.any { ySlice -> ySlice.any(BlockPlaceholder::isController) } }) {
            blockPlaceholders[0][0][0].setController(true)
        }
    }

    fun ensureGlobalPredicate(name: String, predicate: PatternPredicate): IResourcePath {
        val providers = PredicateResource.INSTANCE.resourceInstance.builtinProviders[FileResourceProvider.TYPE].orEmpty()
        @Suppress("UNCHECKED_CAST")
        val provider = providers.filterIsInstance<FileResourceProvider<*>>()
            .firstOrNull { it.name == "global" } as? FileResourceProvider<PatternPredicate>
            ?: return builtinPath("any")
        val path = provider.createSubPath(name)
        if (!provider.hasResource(path)) {
            provider.addResource(path, predicate)
        }
        return path
    }

    fun generatePatternFromWorld(level: Level, from: BlockPos, to: BlockPos, controllerOffset: BlockPos, controllerFace: Direction) {
        val minX = minOf(from.x, to.x)
        val minY = minOf(from.y, to.y)
        val minZ = minOf(from.z, to.z)
        val maxX = maxOf(from.x, to.x)
        val maxY = maxOf(from.y, to.y)
        val maxZ = maxOf(from.z, to.z)
        val controllerPos = BlockPos(minX + controllerOffset.x, minY + controllerOffset.y, minZ + controllerOffset.z)
        val placeholders = Array(maxX - minX + 1) { x ->
            Array(maxY - minY + 1) { y ->
                Array(maxZ - minZ + 1) { z ->
                    val pos = BlockPos(minX + x, minY + y, minZ + z)
                    if (pos == controllerPos) {
                        BlockPlaceholder.controller(builtinPath("any")).setFacing(controllerFace)
                    } else {
                        BlockPlaceholder.create(predicatePathForWorldBlock(level, pos))
                    }
                }
            }
        }
        blockPlaceholders = placeholders
        multiblockPatternView?.onBlockPlaceholdersChanged()
        multiblockShapeInfoView?.reloadShapeInfos()
    }

    fun generateShapeInfoFromWorld(level: Level, from: BlockPos, to: BlockPos, controllerOffset: BlockPos, controllerFace: Direction) {
        val minX = minOf(from.x, to.x)
        val minY = minOf(from.y, to.y)
        val minZ = minOf(from.z, to.z)
        val maxX = maxOf(from.x, to.x)
        val maxY = maxOf(from.y, to.y)
        val maxZ = maxOf(from.z, to.z)
        val controllerPos = BlockPos(minX + controllerOffset.x, minY + controllerOffset.y, minZ + controllerOffset.z)
        val blocks = Array(maxX - minX + 1) { x ->
            Array(maxY - minY + 1) { y ->
                Array(maxZ - minZ + 1) { z ->
                    val pos = BlockPos(minX + x, minY + y, minZ + z)
                    if (pos == controllerPos) {
                        ControllerBlockInfo(controllerFace)
                    } else {
                        BlockInfo.fromBlockState(level.getBlockState(pos))
                    }
                }
            }
        }
        multiblockShapeInfos.add(MultiblockShapeInfo(blocks))
        multiblockShapeInfoView?.reloadShapeInfos()
    }

    private fun predicatePathForWorldBlock(level: Level, pos: BlockPos): IResourcePath {
        val state = level.getBlockState(pos)
        if (state.isAir) {
            return builtinPath("any")
        }
        val block = state.block
        if (block is LiquidBlock) {
            val fluid = block.fluid.source
            val id = BuiltInRegistries.FLUID.getKey(fluid)
            val name = "fluid_${id.namespace}_${id.path.replace('/', '_')}"
            return ensureGlobalPredicate(name, PredicateFluids(fluid))
        }
        if (block == Blocks.AIR) {
            return builtinPath("any")
        }
        val id = BuiltInRegistries.BLOCK.getKey(block)
        val name = "block_${id.namespace}_${id.path.replace('/', '_')}"
        return ensureGlobalPredicate(name, PredicateBlocks(block))
    }

    private fun createDefaultAisleRepetitions(axis: Direction.Axis): Array<IntArray> {
        val aisleLength = when (axis) {
            Direction.Axis.X -> blockPlaceholders.size
            Direction.Axis.Y -> blockPlaceholders[0].size
            Direction.Axis.Z -> blockPlaceholders[0][0].size
        }
        return Array(aisleLength) { intArrayOf(1, 1) }
    }

    override fun getProjectType(): ProjectType {
        return TYPE
    }

    override fun serializeProject(provider: HolderLookup.Provider): CompoundTag {
        val shapeInfoList = ListTag()
        for (shapeInfo in multiblockShapeInfos) {
            shapeInfoList.add(shapeInfo.serializeNBT(provider))
        }
        return TagBuilder.compound()
            .add("definition", definition.serializeNBT(provider))
            .add("placeholders", serializeBlockPlaceholders(provider, blockPlaceholders))
            .add("layer_axis", StringTag.valueOf(layerAxis.name))
            .add("aisle_repetitions", IntArrayTag(Arrays.stream(aisleRepetitions).flatMapToInt(Arrays::stream).toArray()))
            .add("shape_infos", shapeInfoList)
            .build()
    }

    override fun deserializeProject(provider: HolderLookup.Provider, tag: CompoundTag) {
        definition.deserializeNBT(provider, tag.getCompound("definition"))
        blockPlaceholders = deserializeBlockPlaceholders(tag.getCompound("placeholders"))
        layerAxis = if (tag.contains("layer_axis")) {
            Direction.Axis.valueOf(tag.getString("layer_axis"))
        } else {
            Direction.Axis.Y
        }
        val aisleLength = when (layerAxis) {
            Direction.Axis.X -> blockPlaceholders.size
            Direction.Axis.Y -> blockPlaceholders[0].size
            Direction.Axis.Z -> blockPlaceholders[0][0].size
        }
        aisleRepetitions = createDefaultAisleRepetitions(layerAxis)
        val repetitions = tag.getIntArray("aisle_repetitions")
        for (i in 0 until minOf(aisleLength, repetitions.size / 2)) {
            aisleRepetitions[i][0] = repetitions[i * 2]
            aisleRepetitions[i][1] = repetitions[i * 2 + 1]
        }
        multiblockShapeInfos.clear()
        val shapeInfoList = tag.getList("shape_infos", Tag.TAG_COMPOUND.toInt())
        multiblockShapeInfos.addAll(shapeInfoList.map { MultiblockShapeInfo.loadFromTag(provider, it as CompoundTag) })
    }

    override fun onLoad(editor: Editor) {
        super.onLoad(editor)
        if (editor is MBDEditor) {
            editor.centerWindow.getLeftTop().addView(MultiblockAreaView(editor, this).also { multiblockAreaView = it })
            editor.centerWindow.getLeftTop().addView(MultiblockPatternView(editor, this).also { multiblockPatternView = it })
            editor.centerWindow.getLeftTop().addView(MultiblockShapeInfoView(editor, this).also { multiblockShapeInfoView = it })
        }
    }

    override fun onClosed(editor: Editor) {
        multiblockAreaView?.removeSelf()
        multiblockAreaView = null
        multiblockPatternView?.removeSelf()
        multiblockPatternView = null
        multiblockShapeInfoView?.removeSelf()
        multiblockShapeInfoView = null
        super.onClosed(editor)
    }
}
