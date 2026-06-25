package com.lowdragmc.mbd2.common.gui.editor

import com.lowdragmc.lowdraglib2.editor.project.IProject
import com.lowdragmc.lowdraglib2.editor.project.ProjectType
import com.lowdragmc.lowdraglib2.editor.resource.*
import com.lowdragmc.lowdraglib2.editor.ui.Editor
import com.lowdragmc.lowdraglib2.gui.ColorPattern
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView
import com.lowdragmc.lowdraglib2.gui.ui.layout.px
import com.lowdragmc.lowdraglib2.gui.ui.layoutDsl
import com.lowdragmc.lowdraglib2.utils.TagBuilder
import com.lowdragmc.lowdraglib2.utils.data.BlockInfo
import com.lowdragmc.mbd2.MBD2
import com.lowdragmc.mbd2.api.pattern.MultiblockShapeInfo
import com.lowdragmc.mbd2.api.pattern.predicates.PatternPredicate
import com.lowdragmc.mbd2.api.pattern.predicates.PredicateFluids
import com.lowdragmc.mbd2.api.pattern.predicates.PredicateStates
import com.lowdragmc.mbd2.api.pattern.util.RotationHelper
import com.lowdragmc.mbd2.client.MBDRenderers
import com.lowdragmc.mbd2.common.gui.editor.multiblopck.MultiblockAreaView
import com.lowdragmc.mbd2.common.gui.editor.multiblopck.MultiblockPatternView
import com.lowdragmc.mbd2.common.gui.editor.multiblopck.MultiblockShapeInfoView
import com.lowdragmc.mbd2.common.gui.editor.multiblopck.PredicateResource
import com.lowdragmc.mbd2.common.machine.definition.MultiblockMachineDefinition
import com.lowdragmc.mbd2.common.machine.definition.config.BlockPlaceholder
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState
import com.lowdragmc.mbd2.common.machine.definition.config.StateMachine
import com.lowdragmc.mbd2.utils.ControllerBlockInfo
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.*
import net.minecraft.network.chat.Component
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LiquidBlock
import net.minecraft.world.level.block.state.BlockState
import java.io.File
import java.util.*
import java.util.function.Consumer

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
            StateMachine.createMultiblockDefault(
                { MachineState.baseBuilder() },
                { MBDRenderers.MACHINE_UNFORMED },
                { MBDRenderers.MACHINE_FORMED },
                { MBDRenderers.MACHINE_WORKING },
                { MBDRenderers.MACHINE_WAITING },
                { MBDRenderers.MACHINE_WAITING },
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

    fun generatePatternFromWorld(
        level: Level,
        from: BlockPos,
        to: BlockPos,
        controllerOffset: BlockPos,
        controllerFace: Direction,
        modularUI: ModularUI? = null,
        onComplete: () -> Unit = {}
    ) {
        val layout = canonicalize(from, to, controllerOffset, controllerFace)
        val rotInv = RotationHelper.inverse(RotationHelper.rotationFromFacing(controllerFace))

        // Pass A: walk the canonical grid, classify cells into requests.
        // Group by unique BlockState / Fluid so we add at most one predicate per distinct key.
        val cellRequests = Array(layout.sizeX) {
            Array(layout.sizeY) {
                arrayOfNulls<CellRequest>(layout.sizeZ)
            }
        }
        val stateKeys = LinkedHashMap<BlockState, PendingStateRequest>()
        val fluidKeys = LinkedHashMap<net.minecraft.world.level.material.Fluid, PendingFluidRequest>()
        for (x in 0 until layout.sizeX) {
            for (y in 0 until layout.sizeY) {
                for (z in 0 until layout.sizeZ) {
                    if (x == layout.controllerIdx.x && y == layout.controllerIdx.y && z == layout.controllerIdx.z) {
                        cellRequests[x][y][z] = CellRequest.Controller
                        continue
                    }
                    val worldPos = layout.gridToWorld[x][y][z]
                    if (worldPos == null) {
                        cellRequests[x][y][z] = CellRequest.Any
                        continue
                    }
                    val worldState = level.getBlockState(worldPos)
                    val state = worldState.rotate(rotInv)
                    val block = state.block
                    if (state.isAir || block == Blocks.AIR) {
                        cellRequests[x][y][z] = CellRequest.Any
                        continue
                    }
                    if (block is LiquidBlock) {
                        val fluid = block.fluid.source
                        val pending = fluidKeys.getOrPut(fluid) {
                            val id = BuiltInRegistries.FLUID.getKey(fluid)
                            PendingFluidRequest(
                                fluid = fluid,
                                defaultName = "fluid_${id.namespace}_${id.path.replace('/', '_')}",
                                predicateFactory = { PredicateFluids(fluid) }
                            )
                        }
                        cellRequests[x][y][z] = CellRequest.OfFluid(pending)
                        continue
                    }
                    val pending = stateKeys.getOrPut(state) {
                        val id = BuiltInRegistries.BLOCK.getKey(block)
                        val baseName = "states_${id.namespace}_${id.path.replace('/', '_')}"
                        val suffix = stateSuffix(state)
                        PendingStateRequest(
                            state = state,
                            defaultName = baseName + suffix,
                            predicateFactory = { PredicateStates(state) }
                        )
                    }
                    cellRequests[x][y][z] = CellRequest.OfState(pending)
                }
            }
        }

        val pendings: List<PendingRequest> = stateKeys.values + fluidKeys.values
        val commit: (FileResourceProvider<PatternPredicate>) -> Unit = { provider ->
            // Pass B: dedup against the chosen provider, materialize predicates, then build the grid.
            val resolved = HashMap<PendingRequest, IResourcePath>()
            for (pending in pendings) {
                resolved[pending] = resolvePending(provider, pending)
            }
            val placeholders = Array(layout.sizeX) { x ->
                Array(layout.sizeY) { y ->
                    Array(layout.sizeZ) { z ->
                        when (val req = cellRequests[x][y][z]) {
                            CellRequest.Controller ->
                                BlockPlaceholder.controller(builtinPath("any")).setFacing(Direction.NORTH)
                            CellRequest.Any, null ->
                                BlockPlaceholder.create(builtinPath("any"))
                            is CellRequest.OfState ->
                                BlockPlaceholder.create(resolved[req.pending] ?: builtinPath("any"))
                            is CellRequest.OfFluid ->
                                BlockPlaceholder.create(resolved[req.pending] ?: builtinPath("any"))
                        }
                    }
                }
            }
            blockPlaceholders = placeholders
            multiblockPatternView?.onBlockPlaceholdersChanged()
            multiblockShapeInfoView?.reloadShapeInfos()
            onComplete()
        }

        // If no predicates need to be added, skip the dialog and commit immediately
        // (uses the default provider — first available — purely as a dedup target; no writes happen
        // when pendings is empty).
        if (pendings.isEmpty()) {
            val defaultProvider = listFilePredicateProviders().firstOrNull()
            if (defaultProvider != null) {
                commit(defaultProvider)
            } else {
                // No provider available — commit with builtin "any" for everything.
                val placeholders = Array(layout.sizeX) { x ->
                    Array(layout.sizeY) { y ->
                        Array(layout.sizeZ) { z ->
                            if (cellRequests[x][y][z] == CellRequest.Controller) {
                                BlockPlaceholder.controller(builtinPath("any")).setFacing(Direction.NORTH)
                            } else {
                                BlockPlaceholder.create(builtinPath("any"))
                            }
                        }
                    }
                }
                blockPlaceholders = placeholders
                multiblockPatternView?.onBlockPlaceholdersChanged()
                multiblockShapeInfoView?.reloadShapeInfos()
                onComplete()
            }
            return
        }

        showProviderPickerDialog(modularUI, pendings.size, commit)
    }

    /**
     * Builds a controller-NORTH canonical grid mapping for a captured world region.
     *
     * Pattern data is stored as if the controller faces NORTH; matching/preview code
     * (see [RotationHelper], [BlockPattern], [MultiblockState]) rotates it onto the
     * controller's actual orientation. Capturing a region where the in-world controller
     * faces something other than NORTH must therefore be rotated back into the canonical
     * frame, or stairs/logs/etc. end up 180° off when previewed or auto-built.
     */
    private fun canonicalize(
        from: BlockPos,
        to: BlockPos,
        controllerOffset: BlockPos,
        controllerFace: Direction
    ): CanonicalLayout {
        val minX = minOf(from.x, to.x)
        val minY = minOf(from.y, to.y)
        val minZ = minOf(from.z, to.z)
        val maxX = maxOf(from.x, to.x)
        val maxY = maxOf(from.y, to.y)
        val maxZ = maxOf(from.z, to.z)
        val controllerPos = BlockPos(minX + controllerOffset.x, minY + controllerOffset.y, minZ + controllerOffset.z)
        val rotInv = RotationHelper.inverse(RotationHelper.rotationFromFacing(controllerFace))

        val canonicalByWorld = LinkedHashMap<BlockPos, BlockPos>()
        var minCx = Int.MAX_VALUE
        var minCy = Int.MAX_VALUE
        var minCz = Int.MAX_VALUE
        var maxCx = Int.MIN_VALUE
        var maxCy = Int.MIN_VALUE
        var maxCz = Int.MIN_VALUE
        for (x in 0..maxX - minX) {
            for (y in 0..maxY - minY) {
                for (z in 0..maxZ - minZ) {
                    val worldPos = BlockPos(minX + x, minY + y, minZ + z)
                    val relWorld = worldPos.subtract(controllerPos)
                    val relCanonical = relWorld.rotate(rotInv)
                    canonicalByWorld[worldPos] = relCanonical
                    if (relCanonical.x < minCx) minCx = relCanonical.x
                    if (relCanonical.y < minCy) minCy = relCanonical.y
                    if (relCanonical.z < minCz) minCz = relCanonical.z
                    if (relCanonical.x > maxCx) maxCx = relCanonical.x
                    if (relCanonical.y > maxCy) maxCy = relCanonical.y
                    if (relCanonical.z > maxCz) maxCz = relCanonical.z
                }
            }
        }
        val sizeX = maxCx - minCx + 1
        val sizeY = maxCy - minCy + 1
        val sizeZ = maxCz - minCz + 1
        val gridToWorld: Array<Array<Array<BlockPos?>>> = Array(sizeX) {
            Array(sizeY) { arrayOfNulls(sizeZ) }
        }
        for ((worldPos, rel) in canonicalByWorld) {
            gridToWorld[rel.x - minCx][rel.y - minCy][rel.z - minCz] = worldPos
        }
        return CanonicalLayout(
            sizeX = sizeX,
            sizeY = sizeY,
            sizeZ = sizeZ,
            controllerIdx = BlockPos(-minCx, -minCy, -minCz),
            gridToWorld = gridToWorld
        )
    }

    private data class CanonicalLayout(
        val sizeX: Int,
        val sizeY: Int,
        val sizeZ: Int,
        val controllerIdx: BlockPos,
        val gridToWorld: Array<Array<Array<BlockPos?>>>
    )

    private fun resolvePending(
        provider: FileResourceProvider<PatternPredicate>,
        pending: PendingRequest
    ): IResourcePath {
        // Try to reuse an existing resource in the provider that matches the same state/fluid.
        for (entry in provider) {
            val resource = entry.value
            when (pending) {
                is PendingStateRequest -> if (resource is PredicateStates && resource.states.any { it == pending.state }) {
                    return entry.key
                }
                is PendingFluidRequest -> if (resource is PredicateFluids && resource.fluids.any { it == pending.fluid }) {
                    return entry.key
                }
            }
        }
        // No match — create a fresh resource with a unique name.
        var name = pending.defaultName
        var path = provider.createSubPath(name)
        var attempt = 2
        while (provider.hasResource(path)) {
            name = "${pending.defaultName}_$attempt"
            path = provider.createSubPath(name)
            attempt++
        }
        provider.addResource(path, pending.predicateFactory())
        return path
    }

    private fun showProviderPickerDialog(
        modularUI: ModularUI?,
        pendingCount: Int,
        onConfirm: (FileResourceProvider<PatternPredicate>) -> Unit
    ) {
        val providers = listFilePredicateProviders()
        if (providers.isEmpty()) return
        val default = providers.firstOrNull { it.name == "global" } ?: providers.first()

        if (modularUI == null) {
            // No UI to host the dialog — fall back to the default provider so generation still
            // completes (matches the legacy "always global" behavior).
            onConfirm(default)
            return
        }

        val selected = arrayOf<FileResourceProvider<PatternPredicate>>(default)
        val rowButtons = HashMap<FileResourceProvider<PatternPredicate>, Button>()

        fun refreshRowStyles() {
            for ((provider, button) in rowButtons) {
                val isSelected = provider === selected[0]
                button.buttonStyle { style ->
                    style.baseTexture(if (isSelected) ColorPattern.T_GREEN.rectTexture() else IGuiTexture.EMPTY)
                    style.hoverTexture(ColorPattern.T_GRAY.rectTexture())
                    style.pressedTexture(ColorPattern.T_GRAY.rectTexture())
                }
            }
        }

        val list = UIElement()
        list.layout {
            it.widthPercent(100f)
                .flexDirection(dev.vfyjxf.taffy.style.FlexDirection.COLUMN)
                .gapAll(1f)
        }
        for (provider in providers) {
            val row = Button()
                .setText(Component.literal(provider.name))
                .setOnClick { _ ->
                    selected[0] = provider
                    refreshRowStyles()
                }
            row.layout {
                it.widthPercent(100f)
                    .height(16f)
                    .paddingHorizontal(4f)
                    .alignItems(dev.vfyjxf.taffy.style.AlignItems.CENTER)
            }
            rowButtons[provider] = row
            list.addChild(row)
        }
        refreshRowStyles()

        val scroller = ScrollerView()
        scroller.layout { it.widthPercent(100f).height(100f) }
        scroller.addScrollViewChild(list)

        val summary = Label()
            .setText(Component.translatable(
                "editor.machine.multiblock.generate_pattern.summary",
                pendingCount
            ))
            .textStyle { it.textAlignHorizontal(Horizontal.CENTER) }
            .layout { it.widthPercent(100f) }

        val dialog = Dialog()
            .setTitle("editor.machine.multiblock.generate_pattern.pick_provider")
            .addContent(summary)
            .addContent(scroller)
        dialog.overlay.layoutDsl {
            width(200.px)
        }
        dialog.addButton(Button()
            .setText("ldlib.gui.tips.confirm")
            .setOnClick { _ ->
                val chosen = selected[0]
                dialog.close()
                onConfirm(chosen)
            }
            .addClass("__confirm-button__"))
        dialog.addButton(Button()
            .setText("ldlib.gui.tips.cancel")
            .setOnClick { _ -> dialog.close() }
            .addClass("__cancel-button__"))
        dialog.show(modularUI)
    }

    private fun listFilePredicateProviders(): List<FileResourceProvider<PatternPredicate>> {
        val instance = PredicateResource.INSTANCE.resourceInstance
        val builtin = instance.builtinProviders[FileResourceProvider.TYPE].orEmpty()
        val custom = instance.customProviders[FileResourceProvider.TYPE].orEmpty()
        @Suppress("UNCHECKED_CAST")
        return (builtin + custom).filterIsInstance<FileResourceProvider<*>>() as List<FileResourceProvider<PatternPredicate>>
    }

    private fun stateSuffix(state: BlockState): String {
        val values = state.values
        if (values.isEmpty()) return ""
        val canonical = values.entries
            .sortedBy { it.key.name }
            .joinToString(",") { (k, v) -> "${k.name}=$v" }
        return "_" + Integer.toHexString(canonical.hashCode() and 0xFFFFFF)
    }

    private sealed interface CellRequest {
        data object Controller : CellRequest
        data object Any : CellRequest
        data class OfState(val pending: PendingStateRequest) : CellRequest
        data class OfFluid(val pending: PendingFluidRequest) : CellRequest
    }

    private sealed interface PendingRequest {
        val defaultName: String
        val predicateFactory: () -> PatternPredicate
    }

    private data class PendingStateRequest(
        val state: BlockState,
        override val defaultName: String,
        override val predicateFactory: () -> PatternPredicate
    ) : PendingRequest

    private data class PendingFluidRequest(
        val fluid: net.minecraft.world.level.material.Fluid,
        override val defaultName: String,
        override val predicateFactory: () -> PatternPredicate
    ) : PendingRequest

    fun generateShapeInfoFromWorld(level: Level, from: BlockPos, to: BlockPos, controllerOffset: BlockPos, controllerFace: Direction) {
        val layout = canonicalize(from, to, controllerOffset, controllerFace)
        val rotInv = RotationHelper.inverse(RotationHelper.rotationFromFacing(controllerFace))
        val blocks = Array(layout.sizeX) { x ->
            Array(layout.sizeY) { y ->
                Array(layout.sizeZ) { z ->
                    if (x == layout.controllerIdx.x && y == layout.controllerIdx.y && z == layout.controllerIdx.z) {
                        ControllerBlockInfo(Direction.NORTH)
                    } else {
                        val worldPos = layout.gridToWorld[x][y][z]
                        if (worldPos == null) {
                            BlockInfo.fromBlockState(Blocks.AIR.defaultBlockState())
                        } else {
                            BlockInfo.fromBlockState(level.getBlockState(worldPos).rotate(rotInv))
                        }
                    }
                }
            }
        }
        multiblockShapeInfos.add(MultiblockShapeInfo(blocks))
        multiblockShapeInfoView?.reloadShapeInfos()
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
