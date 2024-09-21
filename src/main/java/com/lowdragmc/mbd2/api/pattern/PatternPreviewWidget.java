package com.lowdragmc.mbd2.api.pattern;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.editor.Icons;
import com.lowdragmc.lowdraglib.gui.texture.*;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.utils.*;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.machine.IMultiController;
import com.lowdragmc.mbd2.api.pattern.predicates.SimplePredicate;
import com.lowdragmc.mbd2.common.block.MBDMachineBlock;
import com.lowdragmc.mbd2.common.machine.definition.MultiblockMachineDefinition;
import com.lowdragmc.mbd2.config.ConfigHolder;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.emi.emi.screen.RecipeScreen;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.shedaniel.rei.impl.client.gui.screen.AbstractDisplayViewingScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public class PatternPreviewWidget extends WidgetGroup {
    private boolean isLoaded;
    private static TrackedDummyWorld LEVEL;
    private static BlockPos LAST_POS = new BlockPos(0, 50, 0);
    private static final Map<MultiblockMachineDefinition, MBPattern[]> CACHE = new HashMap<>();
    private final SceneWidget sceneWidget;
    public final MultiblockMachineDefinition controllerDefinition;
    public final MBPattern[] patterns;
    private int index;
    public int layer;
    public int candidatePage;
    private final CycleItemStackHandler predicatesItemHandler;
    private final CycleItemStackHandler candidatesItemHandler;
    private final SlotWidget[] predicates;

    protected PatternPreviewWidget(MultiblockMachineDefinition controllerDefinition) {
        super(0, 0, 160, 160);
        setClientSideWidget();

        // predicates
        predicates = new SlotWidget[5];
        var predicateItems = new ArrayList<List<ItemStack>>();
        for (int i = 0; i < 5; i++) {
            predicateItems.add(Collections.emptyList());
        }
        predicatesItemHandler = new CycleItemStackHandler(predicateItems);
        for (int i = 0; i < predicates.length; i++) {
            var slot = new SlotWidget(predicatesItemHandler, i,
                    6, 9 + i * 18, false, false)
                    .setIngredientIO(IngredientIO.INPUT);
            predicates[i] = slot;
            addWidget(slot);
        }

        // prepare scene
        addWidget(new ImageWidget(26, 7, 106, 106, ResourceBorderTexture.BORDERED_BACKGROUND_INVERSE));
        addWidget(sceneWidget = new SceneWidget(26 + 3, 7 + 3, 106 - 6, 106 - 6, LEVEL)
                .setOnSelected(this::onPosSelected)
                .setRenderFacing(false)
                .setRenderFacing(false));
        if (ConfigHolder.useVBO) {
            if (!RenderSystem.isOnRenderThread()) {
                RenderSystem.recordRenderCall(sceneWidget::useCacheBuffer);
            } else {
                sceneWidget.useCacheBuffer();
            }
        }

        // load patterns
        this.controllerDefinition = controllerDefinition;
        this.layer = -1;

        this.patterns = CACHE.computeIfAbsent(controllerDefinition, definition -> {
            HashSet<ItemStackKey> drops = new HashSet<>();
            drops.add(new ItemStackKey(this.controllerDefinition.asStack()));
            return Arrays.stream(controllerDefinition.shapeInfoFactory().apply(controllerDefinition))
                    .map(it -> initializePattern(it, drops))
                    .filter(Objects::nonNull)
                    .toArray(MBPattern[]::new);
        });

        // id
        addWidget(new ImageWidget(26 + 3, 7 + 3, 106 - 6, 15,
                new TextTexture(controllerDefinition.getDescriptionId(), -1)
                        .setType(TextTexture.TextType.ROLL)
                        .setWidth(106 - 6)
                        .setDropShadow(true)));

        // buttons
        var buttonTexture = new GuiTextureGroup(
                new ColorRectTexture(ColorUtils.color(255,221,221,221)),
                new ColorBorderTexture(-1, ColorUtils.color(255, 73,73,73))
        );
        var pageButton = new ButtonWidget(136, 11, 18, 18, new GuiTextureGroup(
                buttonTexture,
                new ItemStackTexture(Items.PAPER),
                new TextTexture("0", ColorPattern.BLACK.color).setSupplier(() -> Integer.toString(index)).scale(0.8f)
        ), cd -> setPage((index + 1 >= patterns.length) ? 0 : index + 1)).setHoverBorderTexture(-1, -1)
                .setHoverTooltips("pattern_preview.page");
        var layerButton = new ButtonWidget(136, 34, 18, 18, new GuiTextureGroup(
                buttonTexture,
                new ResourceTexture("mbd2:textures/gui/multiblock_info_page_layer.png"),
                new TextTexture("", ColorPattern.BLACK.color).setSupplier(() -> layer == -1 ? "" : Integer.toString(layer)).scale(0.8f)
        ), cd -> updateLayer())
                .setHoverBorderTexture(-1, -1)
                .setHoverTooltips("pattern_preview.layer");
        var formedButton = new SwitchWidget(136, 57, 18, 18, (cd, pressed) -> onFormedSwitch(pressed))
                .setTexture(new GuiTextureGroup(buttonTexture, new ResourceTexture("mbd2:textures/gui/multiblock_info_page_unformed.png")),
                        new GuiTextureGroup(buttonTexture, new ResourceTexture("mbd2:textures/gui/multiblock_info_page.png")))
                .setHoverBorderTexture(-1, -1)
                .setHoverTooltips("pattern_preview.formed");
        if (patterns.length > 1) {
            addWidget(pageButton);
        }
        addWidget(layerButton);
        addWidget(formedButton);

        // candidates
        var items = new ArrayList<List<ItemStack>>();
        for (int i = 0; i < 14; i++) {
            items.add(Collections.emptyList());
        }
        candidatesItemHandler = new CycleItemStackHandler(items);
        for (int x = 0; x < 7; x++) {
            for (int y = 0; y < 2; y++) {
                var slot = new SlotWidget(candidatesItemHandler, x + y * 7,
                        6 + x * 18, 117 + y * 18, false, false)
                        .setIngredientIO(IngredientIO.INPUT);
                addWidget(slot);
            }
        }

        // switch candidates page button
        var buttonBackground = ResourceBorderTexture.BUTTON_COMMON;
        var upButton = new ButtonWidget(136 + 1, 117 + 1, 16, 16,
                new GuiTextureGroup(buttonBackground, Icons.UP.copy().scale(0.8f)),cd -> updateCandidatePage(candidatePage - 1))
                .setHoverBorderTexture(-1, -1);
        var downButton = new ButtonWidget(136 + 1, 135 + 1, 16, 16,
                new GuiTextureGroup(buttonBackground, Icons.DOWN.copy().scale(0.8f)), cd -> updateCandidatePage(candidatePage + 1))
                .setHoverBorderTexture(-1, -1);
        addWidget(upButton);
        addWidget(downButton);

        // set initial page
        setPage(0);
    }

    private void updateCandidatePage(int page) {
        if (this.candidatePage == page) return;
        var maxPage = Math.max(0, (patterns[index].parts.size() - 1) / 14);
        if (page > maxPage || page < 0) return;
        this.candidatePage = page;
        setupPatternCandidates(patterns[index]);
    }

    private void updateLayer() {
        MBPattern pattern = patterns[index];
        if (layer + 1 >= -1 && layer + 1 <= pattern.maxY - pattern.minY) {
            layer += 1;
            if (pattern.controllerBase.isFormed()) {
                onFormedSwitch(false);
            }
        } else {
            layer = -1;
            if (!pattern.controllerBase.isFormed()) {
                onFormedSwitch(true);
            }
        }
        setupScene(pattern);
    }

    private void setupScene(MBPattern pattern) {
        Stream<BlockPos> stream = pattern.blockMap.keySet().stream()
                .filter(pos -> layer == -1 || layer + pattern.minY == pos.getY());
        if (pattern.controllerBase.isFormed()) {
            LongSet set = pattern.controllerBase.getMultiblockState().getMatchContext().getOrDefault("renderMask",
                    LongSets.EMPTY_SET);
            Set<BlockPos> modelDisabled = set.stream().map(BlockPos::of).collect(Collectors.toSet());
            if (!modelDisabled.isEmpty()) {
                sceneWidget.setRenderedCore(
                        stream.filter(pos -> !modelDisabled.contains(pos)).collect(Collectors.toList()), null);
            } else {
                sceneWidget.setRenderedCore(stream.toList(), null);
            }
        } else {
            sceneWidget.setRenderedCore(stream.toList(), null);
        }
    }

    public static PatternPreviewWidget getPatternWidget(MultiblockMachineDefinition controllerDefinition) {
        if (LEVEL == null) {
            if (Minecraft.getInstance().level == null) {
                MBD2.LOGGER.error("Try to init pattern previews before level load");
                throw new IllegalStateException();
            }
            LEVEL = new TrackedDummyWorld();
        }
        return new PatternPreviewWidget(controllerDefinition);
    }

    public void setPage(int index) {
        if (index >= patterns.length || index < 0) return;
        this.index = index;
        this.layer = -1;
        this.candidatePage = 0;
        MBPattern pattern = patterns[index];
        setupScene(pattern);
        setupPatternCandidates(pattern);
    }

    private void setupPatternCandidates(MBPattern pattern) {
        var parts = pattern.parts;
        var items = new ArrayList<List<ItemStack>>();
        for (int i = 0; i < 14; i++) {
            var index = i + candidatePage * 14;
            if (pattern.parts.size() > index) {
                items.add(parts.get(index));
            } else {
                items.add(Collections.emptyList());
            }
        }
        candidatesItemHandler.updateStacks(items);
    }

    private void onFormedSwitch(boolean isFormed) {
        MBPattern pattern = patterns[index];
        IMultiController controllerBase = pattern.controllerBase;
        if (isFormed) {
            this.layer = -1;
            loadControllerFormed(pattern.blockMap.keySet(), controllerBase);
        } else {
            sceneWidget.setRenderedCore(pattern.blockMap.keySet(), null);
            controllerBase.onStructureInvalid();
        }
    }

    private void onPosSelected(BlockPos pos, Direction facing) {
        if (index >= patterns.length || index < 0) return;
        TraceabilityPredicate predicate = patterns[index].predicateMap.get(pos);
        var allPredicates = new ArrayList<SimplePredicate>();
        allPredicates.addAll(predicate.common);
        allPredicates.addAll(predicate.limited);
        allPredicates.removeIf(p -> p == null || p.candidates == null); // why it happens?
        var candidateStacks = new ArrayList<List<ItemStack>>();
        var predicateTips = new ArrayList<List<Component>>();
        for (var simplePredicate : allPredicates) {
            List<ItemStack> itemStacks = simplePredicate.getCandidates();
            if (!itemStacks.isEmpty()) {
                candidateStacks.add(itemStacks);
                predicateTips.add(simplePredicate.getToolTips(predicate));
            }
        }
        var predicateItems = new ArrayList<List<ItemStack>>();
        for (int i = 0; i < 5; i++) {
            if (candidateStacks.size() > i) {
                predicateItems.add(candidateStacks.get(i));
            } else {
                predicateItems.add(Collections.emptyList());
            }
        }
        predicatesItemHandler.updateStacks(predicateItems);
        for (int i = 0; i < 5; i++) {
            if (predicateTips.size() > i) {
                predicates[i].setHoverTooltips(predicateTips.get(i));
            } else {
                predicates[i].setHoverTooltips(Collections.emptyList());
            }
        }
    }

    public static BlockPos locateNextRegion(int range) {
        BlockPos pos = LAST_POS;
        LAST_POS = LAST_POS.offset(range, 0, range);
        return pos;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        // I can only think of this way
        if (!isLoaded && LDLib.isEmiLoaded() && Minecraft.getInstance().screen instanceof RecipeScreen) {
            setPage(0);
            isLoaded = true;
        } else if (!isLoaded && LDLib.isReiLoaded() && Minecraft.getInstance().screen instanceof AbstractDisplayViewingScreen) {
            setPage(0);
            isLoaded = true;
        }
    }

    @Override
    public void drawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.enableBlend();
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
    }

    private MBPattern initializePattern(MultiblockShapeInfo shapeInfo, HashSet<ItemStackKey> blockDrops) {
        Map<BlockPos, BlockInfo> blockMap = new HashMap<>();
        IMultiController controllerBase = null;
        BlockPos multiPos = locateNextRegion(500);

        BlockInfo[][][] blocks = shapeInfo.getBlocks();
        for (int x = 0; x < blocks.length; x++) {
            BlockInfo[][] aisle = blocks[x];
            for (int y = 0; y < aisle.length; y++) {
                BlockInfo[] column = aisle[y];
                for (int z = 0; z < column.length; z++) {
                    BlockState blockState = column[z].getBlockState();
                    BlockPos pos = multiPos.offset(x, y, z);
                    if (column[z].getBlockEntity(pos) instanceof IMachineBlockEntity holder &&
                            holder.getMetaMachine() instanceof IMultiController controller) {
                        holder.getSelf().setLevel(LEVEL);
                        controllerBase = controller;
                    }
                    blockMap.put(pos, BlockInfo.fromBlockState(blockState));
                }
            }
        }

        LEVEL.addBlocks(blockMap);
        if (controllerBase != null) {
            LEVEL.setInnerBlockEntity(controllerBase.getHolder());
        }

        Map<ItemStackKey, PartInfo> parts = gatherBlockDrops(blockMap);
        blockDrops.addAll(parts.keySet());

        Map<BlockPos, TraceabilityPredicate> predicateMap = new HashMap<>();
        if (controllerBase != null) {
            loadControllerFormed(predicateMap.keySet(), controllerBase);
            predicateMap = controllerBase.getMultiblockState().getMatchContext().get("predicates");
        }
        return controllerBase == null ? null : new MBPattern(blockMap, parts.values().stream().sorted((one, two) -> {
            if (one.isController) return -1;
            if (two.isController) return +1;
            if (one.isTile && !two.isTile) return -1;
            if (two.isTile && !one.isTile) return +1;
            if (one.blockId != two.blockId) return two.blockId - one.blockId;
            return two.amount - one.amount;
        }).map(PartInfo::getItemStack).filter(list -> !list.isEmpty()).collect(Collectors.toList()), predicateMap,
                controllerBase);
    }

    private void loadControllerFormed(Collection<BlockPos> poses, IMultiController controllerBase) {
        BlockPattern pattern = controllerBase.getPattern();
        if (pattern != null && pattern.checkPatternAt(controllerBase.getMultiblockState(), true)) {
            controllerBase.onStructureFormed();
        }
        if (controllerBase.isFormed()) {
            LongSet set = controllerBase.getMultiblockState().getMatchContext().getOrDefault("renderMask",
                    LongSets.EMPTY_SET);
            Set<BlockPos> modelDisabled = set.stream().map(BlockPos::of).collect(Collectors.toSet());
            if (!modelDisabled.isEmpty()) {
                sceneWidget.setRenderedCore(
                        poses.stream().filter(pos -> !modelDisabled.contains(pos)).collect(Collectors.toList()), null);
            } else {
                sceneWidget.setRenderedCore(poses, null);
            }
        } else {
            MBD2.LOGGER.warn("Pattern formed checking failed: {}", controllerBase.getBlockState().getBlock().getDescriptionId());
        }
    }

    private Map<ItemStackKey, PartInfo> gatherBlockDrops(Map<BlockPos, BlockInfo> blocks) {
        Map<ItemStackKey, PartInfo> partsMap = new Object2ObjectOpenHashMap<>();
        for (Map.Entry<BlockPos, BlockInfo> entry : blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState blockState = ((Level) PatternPreviewWidget.LEVEL).getBlockState(pos);
            ItemStack itemStack = blockState.getBlock().getCloneItemStack(PatternPreviewWidget.LEVEL, pos, blockState);

            if (itemStack.isEmpty() && !blockState.getFluidState().isEmpty()) {
                Fluid fluid = blockState.getFluidState().getType();
                itemStack = fluid.getBucket().getDefaultInstance();
            }

            ItemStackKey itemStackKey = new ItemStackKey(itemStack);
            partsMap.computeIfAbsent(itemStackKey, key -> new PartInfo(key, entry.getValue())).amount++;
        }
        return partsMap;
    }

    private static class PartInfo {

        final ItemStackKey itemStackKey;
        boolean isController = false;
        boolean isTile = false;
        final int blockId;
        int amount = 0;

        PartInfo(final ItemStackKey itemStackKey, final BlockInfo blockInfo) {
            this.itemStackKey = itemStackKey;
            this.blockId = Block.getId(blockInfo.getBlockState());
            this.isTile = blockInfo.hasBlockEntity();

            if (blockInfo.getBlockState().getBlock() instanceof MBDMachineBlock block) {
                if (block.getDefinition() instanceof MultiblockMachineDefinition)
                    this.isController = true;
            }
        }

        public List<ItemStack> getItemStack() {
            return Arrays.stream(itemStackKey.getItemStack())
                    .map(itemStack -> {
                        var item = itemStack.copy();
                        item.setCount(amount);
                        return item;
                    }).filter(item -> !item.isEmpty()).toList();
        }
    }

    public static class MBPattern {

        @NotNull
        final List<List<ItemStack>> parts;
        @NotNull
        final Map<BlockPos, TraceabilityPredicate> predicateMap;
        @NotNull
        final Map<BlockPos, BlockInfo> blockMap;
        @NotNull
        final IMultiController controllerBase;
        final int maxY, minY;

        public MBPattern(@NotNull Map<BlockPos, BlockInfo> blockMap, @NotNull List<List<ItemStack>> parts,
                         @NotNull Map<BlockPos, TraceabilityPredicate> predicateMap,
                         @NotNull IMultiController controllerBase) {
            this.parts = parts;
            this.blockMap = blockMap;
            this.predicateMap = predicateMap;
            this.controllerBase = controllerBase;
            int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
            for (BlockPos pos : blockMap.keySet()) {
                min = Math.min(min, pos.getY());
                max = Math.max(max, pos.getY());
            }
            minY = min;
            maxY = max;
        }
    }
}