package com.lowdragmc.mbd2.api.pattern;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.ScrollDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.*;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Scene;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Switch;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;
import com.lowdragmc.lowdraglib2.utils.data.BlockInfo;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.TrackedDummyWorld;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.machine.IMultiController;
import com.lowdragmc.mbd2.api.pattern.predicates.PatternPredicate;
import com.lowdragmc.mbd2.common.machine.definition.MultiblockMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleCatalyst;
import com.lowdragmc.mbd2.integration.emi.MBDEMIPlugin;
import com.lowdragmc.mbd2.integration.jei.MBDJEIPlugin;
import com.lowdragmc.mbd2.integration.rei.MBDREIPlugin;
import com.lowdragmc.mbd2.utils.ControllerBlockInfo;
import com.lowdragmc.mbd2.utils.FormattingUtil;
import dev.emi.emi.api.stack.EmiStack;
import dev.vfyjxf.taffy.style.*;
import me.shedaniel.rei.api.common.util.EntryStacks;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.library.ingredients.itemStacks.TypedItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PatternPreview extends UIElement {
    public static final int WIDTH = 176;
    public static final int HEIGHT = 200;

    private final MultiblockMachineDefinition definition;
    private final MBPattern[] patterns;

    // mutable instance state
    private int pageIndex;
    private int currentLayer = -1; // -1 == all layers
    private boolean formed;
    // Mirror of the currently-rendered positions. Used as the dummy world's blockFilter
    // so the renderer's raycast (and Level.clip in general) passes through hidden blocks
    // — otherwise hidden blocks in the ray path would intercept the hit before the visible
    // layer block.
    private final java.util.Set<BlockPos> visibleSet = new HashSet<>();

    // UI handles
    private final Scene scene;
    private final Button pageSwitchButton;
    private final Switch descriptionSwitch;
    private final UIElement descriptionPanel;
    private final UIElement descriptionContent;
    private final ScrollerView predicatePanel;
    private final Toggle formedToggle;
    private final UIElement partsGrid;

    public static UIElement create(MultiblockMachineDefinition definition) {
        if (Minecraft.getInstance().level == null) {
            MBD2.LOGGER.error("Tried to init pattern preview before level load");
            throw new IllegalStateException("No level");
        }
        return new PatternPreview(definition);
    }

    private PatternPreview(MultiblockMachineDefinition definition) {
        this.definition = definition;
        this.patterns = Arrays.stream(definition.shapeInfoFactory().apply(definition))
                .map(shape -> buildPattern(definition, shape))
                .filter(Objects::nonNull)
                .toArray(MBPattern[]::new);

        // root
        getLayout()
                .width(WIDTH).height(HEIGHT)
                .flexDirection(FlexDirection.COLUMN)
                .paddingAll(4)
                .gapAll(2);
        addClass("panel_bg");

        // -- top bar --
        var topBar = new UIElement();
        topBar.getLayout()
                .widthPercent(100)
                .flexDirection(FlexDirection.ROW)
                .justifyContent(AlignContent.CENTER);
        topBar.getStyle().overflowVisible(false);
        topBar.addClass("preview_bg");
        var titleLabel = new Label();
        titleLabel.setText(Component.translatable(definition.getDescriptionId()));
        titleLabel.getLayout().widthPercent(100);
        titleLabel.textStyle(s -> s
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER));
        topBar.addChild(titleLabel);

        // -- middle --
        var middle = new UIElement();
        middle.getLayout()
                .widthPercent(100)
                .flex(1);

        scene = new Scene();
        scene.getLayout()
                .widthPercent(100)
                .heightPercent(100);
        scene.setRenderFacing(false);
        scene.setRenderSelect(true);
        scene.setOnSelected((pos, face) -> onPosSelected(pos));

        // top-left toolbox
        var topLeftToolbox = new UIElement();
        topLeftToolbox.getLayout()
                .positionType(TaffyPosition.ABSOLUTE)
                .top(2).left(2)
                .gapAll(2)
                .flexDirection(FlexDirection.ROW);
        topLeftToolbox.getStyle().backgroundTexture(IGuiTexture.EMPTY);
        topLeftToolbox.moveInlineAsDefault();

        var projectionSelector = new Selector<Boolean>()
                .setCandidates(List.of(true, false))
                .setValue(true, false)
                .setOnValueChanged(scene::useOrtho)
                .setCandidateUIProvider(c -> new Label()
                        .textStyle(s -> s
                                .textAlignHorizontal(Horizontal.LEFT)
                                .textAlignVertical(Vertical.CENTER))
                        .setText(c == null ? "---" : c
                                ? "editor.camera.ortho" : "editor.camera.perspective"));
        projectionSelector.layout(l -> l.width(50));
        projectionSelector.style(s -> s.tooltips("editor.camera.mode"));
        topLeftToolbox.addChild(projectionSelector);

        pageSwitchButton = new Button()
                .setText("0")
                .setOnClick(e -> setPage((pageIndex + 1) % Math.max(1, patterns.length)));
        pageSwitchButton.getLayout().aspectRatio(1f);
        if (patterns.length <= 1) {
            pageSwitchButton.setDisplay(false);
        }
        topLeftToolbox.addChild(pageSwitchButton);

        descriptionSwitch = new Switch();
        descriptionSwitch.style(s -> s.tooltips("editor.machine.multiblock.multiblock_shape_info.description"));
        topLeftToolbox.addChild(descriptionSwitch);
        // setOnToggleChanged wired below, after descriptionPanel is constructed

        // description panel (absolute, below toolbox)
        descriptionPanel = new UIElement();
        descriptionPanel.getLayout()
                .positionType(TaffyPosition.ABSOLUTE)
                .alignSelf(AlignItems.CENTER)
                .top(20)
                .widthPercent(90).heightPercent(70);
        descriptionPanel.setDisplay(false);

        var descScroller = new ScrollerView();
        descScroller.getLayout().widthPercent(100).heightPercent(100);

        descriptionContent = new UIElement();
        descriptionContent.getLayout()
                .widthPercent(100)
                .flexDirection(FlexDirection.COLUMN)
                .gapAll(2);
        descScroller.addScrollViewChild(descriptionContent);
        descriptionPanel.addChild(descScroller);
        descriptionSwitch.setOnSwitchChanged(descriptionPanel::setDisplay);

        // predicate panel (absolute, bottom-left, fixed height — slots added dynamically and
        // scroll vertically when there are more than fit).
        predicatePanel = new ScrollerView();
        predicatePanel.getLayout()
                .positionType(TaffyPosition.ABSOLUTE)
                .bottom(2).left(2)
                .width(28).maxHeightPercent(70);
        predicatePanel.viewPort.getLayout().paddingAll(0).paddingBottom(0);
        predicatePanel.viewPort.getStyle().backgroundTexture(IGuiTexture.EMPTY);
        predicatePanel.getScrollerViewStyle().adaptiveHeight(true);
//        predicatePanel.getScrollerViewStyle().mode(ScrollerMode.VERTICAL);
        predicatePanel.viewContainer.getLayout().gapAll(2);
        predicatePanel.setDisplay(false);

        // bottom-right toolbox: 3 square buttons
        var bottomRightToolbox = new UIElement();
        bottomRightToolbox.getLayout()
                .positionType(TaffyPosition.ABSOLUTE)
                .bottom(2).right(2)
                .gapAll(2)
                .flexDirection(FlexDirection.ROW);
        bottomRightToolbox.getStyle().backgroundTexture(IGuiTexture.EMPTY);
        bottomRightToolbox.moveInlineAsDefault();

        var plusButton = new Button()
                .setText("+")
                .setOnClick(e -> stepLayer(+1));
        plusButton.getLayout().width(14).height(14);
        plusButton.style(s -> s.tooltips("pattern_preview.layer.next"));

        var minusButton = new Button()
                .setText("-")
                .setOnClick(e -> stepLayer(-1));
        minusButton.getLayout().width(14).height(14);
        minusButton.style(s -> s.tooltips("pattern_preview.layer.last"));

        formedToggle = new Toggle();
        formedToggle.noText();
        formedToggle.setOnToggleChanged(this::onFormedToggle);
        formedToggle.style(s -> s.tooltips("pattern_preview.formed"));

        bottomRightToolbox.addChildren(plusButton, minusButton, formedToggle);

        middle.addChildren(scene, topLeftToolbox, descriptionPanel, predicatePanel, bottomRightToolbox);

        // -- parts list --
        var partsList = new ScrollerView();
        partsList.getLayout()
                .widthPercent(100)
                .height(41);

        partsGrid = new UIElement();
        partsGrid.getLayout()
                .widthPercent(100)
                .flexDirection(FlexDirection.ROW)
                .flexWrap(FlexWrap.WRAP);
        partsList.addScrollViewChild(partsGrid);

        addChildren(topBar, middle, partsList);

        setPage(0);
    }

    private void setPage(int idx) {
        if (patterns.length == 0) return;
        if (patterns.length == 1) {
            idx = 0;
        } else {
            idx = Math.floorMod(idx, patterns.length);
        }
        this.pageIndex = idx;
        this.currentLayer = -1;
        this.formed = false;
        var pattern = patterns[idx];

        // bind scene to this pattern's own world (releases the previous renderer)
        scene.useOrtho().useCacheBuffer().createScene(pattern.world).setClipContext(ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY);
        scene.setOrthoRange(0.5f);

        // reset controller state to unformed each time we land on a page;
        // briefly disable the filter so onStructureInvalid can walk the world if it needs to.
        if (pattern.controllerBase != null && pattern.controllerBase.isFormed()) {
            pattern.world.setBlockFilter(null);
            try {
                pattern.controllerBase.onStructureInvalid();
            } finally {
                // refreshScene below will re-set the filter to visibleSet::contains
            }
        }
        formedToggle.setOn(false, false);

        pageSwitchButton.setText(Component.literal(pageIndex  + ""));

        // description: rebuild content from this pattern's description
        descriptionContent.clearAllChildren();
        for (var line : pattern.description) {
            var lbl = new Label();
            lbl.setText(Component.translatable(line));
            lbl.textStyle(s -> s
                    .textAlignHorizontal(Horizontal.LEFT)
                    .textWrap(TextWrap.WRAP)
                    .adaptiveHeight(true));
            lbl.getLayout().widthPercent(100);
            descriptionContent.addChild(lbl);
        }
        descriptionSwitch.setOn(false, false);
        descriptionSwitch.setDisplay(!pattern.description.isEmpty());
        descriptionPanel.setDisplay(false);

        // predicates: hide until user clicks a block
        predicatePanel.setDisplay(false);

        refreshScene();
        refreshPartsGrid();
    }

    private void stepLayer(int delta) {
        if (patterns.length == 0) return;
        var pattern = patterns[pageIndex];
        int min = pattern.minY;
        int max = pattern.maxY;
        if (max < min) return;
        // state machine: -1 (all) -> min -> min+1 -> ... -> max -> -1
        int next;
        if (currentLayer == -1) {
            next = delta > 0 ? min : max;
        } else if (delta > 0) {
            next = currentLayer + 1 > max ? -1 : currentLayer + 1;
        } else {
            next = currentLayer - 1 < min ? -1 : currentLayer - 1;
        }
        this.currentLayer = next;
        refreshScene();
    }

    private void onFormedToggle(boolean on) {
        if (patterns.length == 0) return;
        var pattern = patterns[pageIndex];
        if (pattern.controllerBase == null) return;
        // BlockPattern.checkPatternAt and onStructureFormed traverse the world via getBlockState,
        // which our visibleSet filter would short-circuit to AIR for hidden positions. Drop
        // the filter for the duration of these calls; refreshScene below restores it.
        pattern.world.setBlockFilter(null);
        try {
            if (on) {
                var bp = pattern.controllerBase.getPattern();
                if (bp != null) {
                    bp.checkPatternAt(pattern.controllerBase.getMultiblockState(), true);
                }
                pattern.controllerBase.onStructureFormed();
            } else {
                pattern.controllerBase.onStructureInvalid();
            }
        } finally {
            // refreshScene below re-installs the filter
        }
        this.formed = on;
        scene.needCompileCache();
        refreshScene();
    }

    private void refreshScene() {
        if (patterns.length == 0) return;
        var pattern = patterns[pageIndex];
        Collection<BlockPos> poses;
        if (currentLayer == -1) {
            poses = new ArrayList<>(pattern.blockMap.keySet());
        } else {
            int y = currentLayer;
            poses = pattern.blockMap.keySet().stream().filter(p -> p.getY() == y).toList();
        }
        visibleSet.clear();
        visibleSet.addAll(poses);
        scene.setRenderedCore(poses);
        // Install a filter that mirrors what we render. Hidden blocks (other layers) now
        // return AIR from world.getBlockState, so the renderer's raycast passes through
        // them and selection lands on visible blocks instead of being eaten by occluders.
        pattern.world.setBlockFilter(visibleSet::contains);
    }

    private void refreshPartsGrid() {
        partsGrid.clearAllChildren();
        if (patterns.length == 0) return;
        var pattern = patterns[pageIndex];
        // items first
        for (var stack : pattern.itemParts) {
            var copy = stack.copy();
            var slot = new ItemSlot();
            slot.setItem(copy);
            slot.style(s -> s.tooltips(Component.translatable("pattern_preview.parts.amount",
                    FormattingUtil.formatNumbers(copy.getCount()))));
            slot.xeiRecipeIngredient(IngredientIO.INPUT);
            slot.xeiRecipeSlot(IngredientIO.INPUT, 1.0f);
            partsGrid.addChild(slot);
        }
        // fluids after
        for (var fluid : pattern.fluidParts) {
            var copy = fluid.copy();
            var slot = new FluidSlot();
            slot.setFluid(copy);
            slot.xeiRecipeIngredient(IngredientIO.INPUT);
            slot.xeiRecipeSlot(IngredientIO.INPUT, 1f);
            partsGrid.addChild(slot);
        }
    }

    private void onPosSelected(BlockPos pos) {
        if (patterns.length == 0) return;
        var pattern = patterns[pageIndex];
        var predicate = pattern.predicateMap.get(pos);
        predicatePanel.viewContainer.clearAllChildren();
        if (predicate == null) {
            predicatePanel.setDisplay(false);
            return;
        }
        var all = new ArrayList<PatternPredicate>();
        all.addAll(predicate.common);
        all.addAll(predicate.limited);
        all.removeIf(p -> p == null || p.getItemCandidates().isEmpty());
        if (all.isEmpty()) {
            predicatePanel.setDisplay(false);
            return;
        }
        for (var pp : all) {
            List<ItemStack> candidates = pp.getItemCandidates();
            if (candidates.isEmpty()) continue;
            var slot = new ItemSlot();
            if (candidates.size() == 1) {
                ItemStack first = candidates.getFirst();
                slot.setItem(first.copy());
//                slot.xeiRecipeIngredient(IngredientIO.INPUT, candidates::stream);
//                slot.xeiRecipeSlot(IngredientIO.INPUT, 1.0f, first.getCount(), candidates::stream);
            } else {
                slot.bindDataSource(ScrollDataSource.of(candidates));
            }

            slot.addEventListener(UIEvents.CLICK, e -> {
                if (LDLib2.isReiLoaded()) {
                    REIPlugin.lookupItemStack(slot, e.button);
                } else if (LDLib2.isJeiLoaded()) {
                    JEIPlugin.lookupItemStack(slot, e.button);
                } else if (LDLib2.isEmiLoaded()) {
                    EMIPlugin.lookupItemStack(slot, e.button);
                }
            });

            List<Component> tooltips = pp.getToolTips(predicate);
            if (!tooltips.isEmpty()) {
                slot.getStyle().tooltips(tooltips.toArray(Component[]::new));
            }
            predicatePanel.viewContainer.addChild(slot);
        }
        predicatePanel.setDisplay(true);
    }

    public static class JEIPlugin {
        public static void lookupItemStack(ItemSlot slot, int button) {
            if (LDLib2.isJeiLoaded() && (button == 0 || button == 1)) {
                MBDJEIPlugin.lookupIngredient(TypedItemStack.create(slot.getValue()), button == 0 ? RecipeIngredientRole.OUTPUT : RecipeIngredientRole.INPUT);
            }
        }
    }

    public static class REIPlugin {
        public static void lookupItemStack(ItemSlot slot, int button) {
            if (LDLib2.isReiLoaded() && (button == 0 || button == 1)) {
                MBDREIPlugin.lookupIngredient(EntryStacks.of(slot.getValue()), button == 0);
            }
        }
    }

    public static class EMIPlugin {
        public static void lookupItemStack(ItemSlot slot, int button) {
            if (LDLib2.isEmiLoaded() && (button == 0 || button == 1)) {
                MBDEMIPlugin.lookupIngredient(EmiStack.of(slot.getValue()), button == 0);
            }
        }
    }

    // ===== Pattern construction =====

    static BlockInfo resolve(MultiblockMachineDefinition definition, BlockInfo info) {
        if (info instanceof ControllerBlockInfo controllerInfo) {
            BlockState state = definition.block().defaultBlockState();
            Direction facing = controllerInfo.getFacing();
            var property = definition.blockProperties().rotationState().property;
            if (facing != null && property.isPresent() && property.get().getPossibleValues().contains(facing)) {
                state = state.setValue(property.get(), facing);
            }
            return BlockInfo.fromBlockState(state);
        }
        return info;
    }

    @Nullable
    private static MBPattern buildPattern(MultiblockMachineDefinition definition, MultiblockShapeInfo shapeInfo) {
        var world = new TrackedDummyWorld();
        var origin = new BlockPos(0, 50, 0);

        Map<BlockPos, BlockInfo> blockMap = new LinkedHashMap<>();
        BlockInfo[][][] blocks = shapeInfo.getBlocks();
        BlockPos controllerPos = null;

        for (int x = 0; x < blocks.length; x++) {
            for (int y = 0; y < blocks[x].length; y++) {
                for (int z = 0; z < blocks[x][y].length; z++) {
                    BlockInfo info = blocks[x][y][z];
                    if (info == null) continue;
                    BlockPos pos = origin.offset(x, y, z);
                    info = resolve(definition, info);
                    var state = info.getBlockState();
                    if (state == null || state.isAir()) continue;
                    blockMap.put(pos, info);
                    world.addBlock(pos, info);
                    if (IMultiController.ofController(world.getBlockEntity(pos)).isPresent()) {
                        controllerPos = pos;
                    }
                }
            }
        }

        if (blockMap.isEmpty()) return null;

        IMultiController controllerBase = null;
        Map<BlockPos, TraceabilityPredicate> predicateMap = new HashMap<>();
        if (controllerPos != null) {
            var be = world.getBlockEntity(controllerPos);
            if (be instanceof IMachineBlockEntity holder && holder.getMetaMachine() instanceof IMultiController controller) {
                holder.self().setLevel(world);
                controllerBase = controller;
                var bp = controller.getPattern();
                if (bp != null) {
                    var state = controller.getMultiblockState();
                    // Try the controller's own facing first; if that fails, sweep all 4 horizontal facings
                    // so we still populate predicateMap even when the saved shape orientation doesn't
                    // line up with the controller's default rotation.
                    boolean ok = bp.checkPatternAt(state, true);
                    if (!ok) {
                        for (var facing : new net.minecraft.core.Direction[]{
                                net.minecraft.core.Direction.NORTH,
                                net.minecraft.core.Direction.EAST,
                                net.minecraft.core.Direction.SOUTH,
                                net.minecraft.core.Direction.WEST}) {
                            if (bp.checkPatternAt(state, controllerPos, facing, true)) {
                                ok = true;
                                break;
                            }
                        }
                    }
                    // Always read predicates — even on failure the match context may hold
                    // partial entries from the last traversal, which is still useful UX-wise.
                    Object raw = state.getMatchContext().get("predicates");
                    if (raw instanceof Map<?, ?> map) {
                        for (var entry : map.entrySet()) {
                            if (entry.getKey() instanceof BlockPos bpPos
                                    && entry.getValue() instanceof TraceabilityPredicate tp) {
                                predicateMap.put(bpPos, tp);
                            }
                        }
                    }
                    if (!ok) {
                        MBD2.LOGGER.warn("Multiblock pattern preview: checkPatternAt failed for {} (captured {} predicates from last attempt)",
                                definition.id(), predicateMap.size());
                    }
                    // start unformed by default
                    controller.onStructureInvalid();
                }
            }
        }

        // Parts: accumulate real counts so the UI shows e.g. "16 stone bricks" / "3000 mB water".
        // Encounter order is preserved via LinkedHashMap: controller, catalyst, then blocks.
        Map<Integer, ItemStack> itemByKey = new LinkedHashMap<>();
        Map<String, FluidStack> fluidByKey = new LinkedHashMap<>();

        var controllerStack = definition.asStack();
        if (!controllerStack.isEmpty()) {
            itemByKey.put(itemKey(controllerStack), controllerStack.copy());
        }

        var catalyst = definition.multiblockSettings().catalyst();
        if (catalyst.isEnable()) {
            for (var stack : collectCatalystItems(catalyst)) {
                if (stack.isEmpty()) continue;
                int k = itemKey(stack);
                var existing = itemByKey.get(k);
                if (existing != null) {
                    existing.setCount(existing.getCount() + stack.getCount());
                } else {
                    itemByKey.put(k, stack.copy());
                }
            }
        }

        for (var entry : blockMap.entrySet()) {
            var pos = entry.getKey();
            if (pos.equals(controllerPos)) continue;
            BlockState state = entry.getValue().getBlockState();
            if (state == null || state.isAir()) continue;

            ItemStack stack = state.getBlock().getCloneItemStack(world, pos, state);
            if (!stack.isEmpty()) {
                int k = itemKey(stack);
                var existing = itemByKey.get(k);
                if (existing != null) {
                    existing.setCount(existing.getCount() + 1);
                } else {
                    var copy = stack.copy();
                    copy.setCount(1);
                    itemByKey.put(k, copy);
                }
                continue;
            }
            // pure fluid block (e.g. water source): 1 source block == 1000 mB
            FluidState fs = state.getFluidState();
            if (!fs.isEmpty() && fs.isSource()) {
                var fluid = fs.getType();
                var fkey = BuiltInRegistries.FLUID.getKey(fluid).toString();
                var existing = fluidByKey.get(fkey);
                if (existing != null) {
                    existing.setAmount(existing.getAmount() + 1000);
                } else {
                    fluidByKey.put(fkey, new FluidStack(fluid, 1000));
                }
                continue;
            }
            // last-resort: count by block-as-item even if getCloneItemStack returned empty
            var fallback = new ItemStack(state.getBlock());
            if (!fallback.isEmpty()) {
                int k = itemKey(fallback);
                var existing = itemByKey.get(k);
                if (existing != null) {
                    existing.setCount(existing.getCount() + 1);
                } else {
                    fallback.setCount(1);
                    itemByKey.put(k, fallback);
                }
            }
        }

        List<ItemStack> itemParts = new ArrayList<>(itemByKey.values());
        List<FluidStack> fluidParts = new ArrayList<>(fluidByKey.values());

        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (var pos : blockMap.keySet()) {
            minY = Math.min(minY, pos.getY());
            maxY = Math.max(maxY, pos.getY());
        }

        return new MBPattern(world, blockMap, predicateMap, controllerBase,
                itemParts, fluidParts, shapeInfo.getDescription(), minY, maxY);
    }

    private static int itemKey(ItemStack stack) {
        return Objects.hash(BuiltInRegistries.ITEM.getKey(stack.getItem()), stack.getComponents());
    }

    private static List<ItemStack> collectCatalystItems(ToggleCatalyst catalyst) {
        List<ItemStack> result = new ArrayList<>();
        if (!catalyst.getCandidates().isEnable()) return result;
        for (var block : catalyst.getCandidates().getValue()) {
            if (block != null) {
                result.add(new ItemStack(block));
            }
        }
        return result;
    }

    private record MBPattern(
            TrackedDummyWorld world,
            Map<BlockPos, BlockInfo> blockMap,
            Map<BlockPos, TraceabilityPredicate> predicateMap,
            @Nullable IMultiController controllerBase,
            List<ItemStack> itemParts,
            List<FluidStack> fluidParts,
            List<String> description,
            int minY, int maxY
    ) {}
}
