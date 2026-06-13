package com.lowdragmc.mbd2.api.pattern.predicates;

import com.google.common.base.Suppliers;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.IToggleConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigList;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.ReadOnlyManaged;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Inspector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Menu;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Scene;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TreeList;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.registry.ILDLRegister;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.lowdragmc.lowdraglib2.utils.data.BlockInfo;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.TrackedDummyWorld;
import com.lowdragmc.mbd2.api.block.ProxyPartBlock;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.pattern.MultiblockState;
import com.lowdragmc.mbd2.api.pattern.TraceabilityPredicate;
import com.lowdragmc.mbd2.api.pattern.error.PatternStringError;
import com.lowdragmc.mbd2.api.pattern.error.SinglePredicateError;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigPartSettings;
import com.lowdragmc.mbd2.common.machine.definition.config.toggle.ToggleDirection;
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState;
import com.lowdragmc.mbd2.common.machine.definition.config.StateMachine;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import dev.vfyjxf.taffy.style.FlexDirection;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaEdge;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PatternPredicate implements IPersistedSerializable, IConfigurable, ILDLRegister<PatternPredicate, Supplier<PatternPredicate>> {

    @LDLRegister(name = "any", registry = "mbd2:pattern_predicate", manual = true)
    private static final class PredicateAny extends PatternPredicate {
        public PredicateAny() {
            super(x -> true, null);
        }
    }

    @LDLRegister(name = "air", registry = "mbd2:pattern_predicate", manual = true)
    private static final class PredicateAir extends PatternPredicate {
        public PredicateAir() {
            super(blockWorldState -> blockWorldState.getWorld().isEmptyBlock(blockWorldState.getPos()), null);
        }
    }

    public final static PatternPredicate ANY = new PredicateAny();
    public final static PatternPredicate AIR = new PredicateAir();

    public final static Codec<PatternPredicate> CODEC = createCodec();
    static Codec<PatternPredicate> createCodec() {
        return MBDRegistries.PATTERN_PREDICATES.optionalCodec().dispatch(ILDLRegister::getRegistryHolderOptional,
                optional -> optional.map(holder ->
                                MapCodec.assumeMapUnsafe(PersistedParser.createCodec(holder.value())))
                        .orElseGet(() -> MapCodec.unit(ANY)));
    }

    @Configurable(name = "config.block_pattern.predicate.minCount", tips = { "config.block_pattern.predicate.minCount.tooltip.0", "config.block_pattern.predicate.minCount.tooltip.1" })
    @ConfigNumber(range = {-1, Integer.MAX_VALUE})
    public int minCount = -1;
    @Configurable(name = "config.block_pattern.predicate.maxCount", tips = { "config.block_pattern.predicate.maxCount.tooltip.0", "config.block_pattern.predicate.maxCount.tooltip.1" })
    @ConfigNumber(range = {-1, Integer.MAX_VALUE})
    public int maxCount = -1;
    @Configurable(name = "config.block_pattern.predicate.minLayerCount", tips = { "config.block_pattern.predicate.minLayerCount.tooltip.0", "config.block_pattern.predicate.minLayerCount.tooltip.1" })
    @ConfigNumber(range = {-1, Integer.MAX_VALUE})
    public int minLayerCount = -1;
    @Configurable(name = "config.block_pattern.predicate.maxLayerCount", tips = { "config.block_pattern.predicate.maxLayerCount.tooltip.0", "config.block_pattern.predicate.maxLayerCount.tooltip.1" })
    @ConfigNumber(range = {-1, Integer.MAX_VALUE})
    public int maxLayerCount = -1;
    @Configurable(name = "config.block_pattern.predicate.previewCount", tips = { "config.block_pattern.predicate.previewCount.tooltip.0", "config.block_pattern.predicate.previewCount.tooltip.1" })
    @ConfigNumber(range = {-1, Integer.MAX_VALUE})
    public int previewCount = -1;
    @Configurable(name = "config.block_pattern.predicate.proxyWhileFormed", tips = "config.block_pattern.predicate.proxyWhileFormed.tooltip", subConfigurable = true)
    public ProxyWhileFormed proxyWhileFormed = new ProxyWhileFormed();
    @Configurable(name = "config.block_pattern.predicate.io", tips = "config.block_pattern.predicate.io.tooltip")
    public IO io = IO.BOTH;
    @Configurable(name = "config.block_pattern.predicate.slotName", tips = "config.block_pattern.predicate.slotName.tooltip")
    public String slotName;
    @Configurable(name = "config.block_pattern.predicate.nbt", tips = "config.block_pattern.predicate.nbt.tooltip")
    public CompoundTag nbt = new CompoundTag();
    @Configurable(name = "config.block_pattern.predicate.controller_nbt", tips = "config.block_pattern.predicate.controller_nbt.tooltip")
    public CompoundTag controllerNbt = new CompoundTag();
    @Configurable(name = "config.block_pattern.predicate.controllerFront", tips = "config.block_pattern.predicate.controllerFront.tooltip", subConfigurable = true)
    public ToggleDirection controllerFront = new ToggleDirection();
    @Configurable(name = "config.block_pattern.predicate.rotateFollowController", tips = {
            "config.block_pattern.predicate.rotateFollowController.tooltip.0",
            "config.block_pattern.predicate.rotateFollowController.tooltip.1"})
    public boolean rotateFollowController = true;
    @Configurable(name = "config.block_pattern.predicate.tooltips", tips = "config.block_pattern.predicate.tooltips.tooltip", collapse = false)
    public final List<Component> toolTips = new ArrayList<>();
    @Configurable(name = "config.block_pattern.predicate.allowOpenUI", tips = { "config.block_pattern.predicate.allowOpenUI.tooltip.0", "config.block_pattern.predicate.allowOpenUI.tooltip.1" })
    public boolean allowOpenUI = true;

    // runtime
    private boolean isBuilt;
    @Nullable
    protected Supplier<BlockInfo[]> candidates;
    protected Predicate<MultiblockState> predicate;
    protected Supplier<IGuiTexture> previewTexture = () -> IGuiTexture.EMPTY;

    protected PatternPredicate() {
        this(x -> true, null);
    }

    public PatternPredicate(Predicate<MultiblockState> predicate, @Nullable Supplier<BlockInfo[]> candidates) {
        this.predicate = predicate;
        this.candidates = candidates;
    }

    public PatternPredicate buildPredicate() {
        previewTexture = Suppliers.memoize(() -> candidates == null ? new TextTexture(name()) : new ItemStackTexture(Arrays.stream(candidates.get()).map(BlockInfo::getItemStackForm).toArray(ItemStack[]::new)));
        isBuilt = true;
        return this;
    }

    protected void makeSureBuilt() {
        if (!isBuilt) {
            buildPredicate();
        }
    }

    @Nullable
    public BlockInfo[] getCandidates() {
        makeSureBuilt();
        return candidates == null ? null : candidates.get();
    }

    public List<Component> getToolTips(TraceabilityPredicate predicates) {
        List<Component> result = new ArrayList<>();
        if (!toolTips.isEmpty()) {
            result.addAll(toolTips);
        }
        if (minCount == maxCount && maxCount != -1) {
            result.add(Component.translatable("mbd2.multiblock.pattern.error.limited_exact", minCount));
        } else if (minCount != maxCount && minCount != -1 && maxCount != -1) {
            result.add(Component.translatable("mbd2.multiblock.pattern.error.limited_within", minCount, maxCount));
        } else {
            if (minCount != -1) {
                result.add(Component.translatable("mbd2.multiblock.pattern.error.limited.1", minCount));
            }
            if (maxCount != -1) {
                result.add(Component.translatable("mbd2.multiblock.pattern.error.limited.0", maxCount));
            }
        }
        if (predicates == null) return result;
        if (predicates.isSingle()) {
            result.add(Component.translatable("mbd2.multiblock.pattern.single"));
        }
        if (predicates.hasAir()) {
            result.add(Component.translatable("mbd2.multiblock.pattern.replaceable_air"));
        }
        return result;
    }

    private boolean isProxyBlock(MultiblockState blockWorldState) {
        return blockWorldState.getBlockState().getBlock() == ProxyPartBlock.BLOCK;
    }

    public boolean test(MultiblockState blockWorldState) {
        makeSureBuilt();
        if (isProxyBlock(blockWorldState)) {
            return checkInnerConditions(blockWorldState);
        }
        boolean previousRotation = blockWorldState.pushRotationActive(rotateFollowController);
        boolean matched;
        try {
            matched = predicate.test(blockWorldState);
        } finally {
            blockWorldState.popRotationActive(previousRotation);
        }
        return matched && checkInnerConditions(blockWorldState);
    }

    public boolean testLimited(MultiblockState blockWorldState) {
        makeSureBuilt();
        if (isProxyBlock(blockWorldState)) {
            return checkInnerConditions(blockWorldState);
        }
        boolean previousRotation = blockWorldState.pushRotationActive(rotateFollowController);
        boolean matched;
        try {
            matched = testGlobal(blockWorldState) && testLayer(blockWorldState);
        } finally {
            blockWorldState.popRotationActive(previousRotation);
        }
        return matched && checkInnerConditions(blockWorldState);
    }

    private boolean checkInnerConditions(MultiblockState blockWorldState) {
        if (io != IO.BOTH) {
            if (blockWorldState.io == IO.BOTH) {
                blockWorldState.io = io;
            } else if (blockWorldState.io != io) {
                blockWorldState.io = null;
            }
        }
        if (!nbt.isEmpty() && !blockWorldState.world.isClientSide) {
            var tag = blockWorldState.getTileEntityData();
            if (tag != null) {
                var merged = tag.copy().merge(nbt);
                if (!tag.equals(merged)) {
                    blockWorldState.setError(new PatternStringError("The NBT fails to match"));
                    return false;
                }
            }
        }
        if (!controllerNbt.isEmpty() && !blockWorldState.world.isClientSide) {
            var tag = blockWorldState.getControllerTileEntityData();
            if (tag != null) {
                var merged = tag.copy().merge(controllerNbt);
                if (!tag.equals(merged)) {
                    blockWorldState.setError(new PatternStringError("The Controller NBT fails to match"));
                    return false;
                }
            }
        }
        if (controllerFront.isEnable()) {
            var controller = blockWorldState.getController();
            if (controller != null) {
                var front = controller.getFrontFacing();
                if (front.isPresent() && front.get() != controllerFront.getValue()) {
                    blockWorldState.setError(new PatternStringError("The Controller Front side fails to match"));
                    return false;
                }
            }
        }
        if (slotName != null && !slotName.isEmpty()) {
            Map<Long, Set<String>> slots = blockWorldState.getMatchContext().getOrCreate("slots", Long2ObjectArrayMap::new);
            slots.computeIfAbsent(blockWorldState.getPos().asLong(), s->new HashSet<>()).add(slotName);
        }
        int predicateId = Optional.ofNullable(blockWorldState.getCheckingPattern())
                .map(pattern -> pattern.getPredicateId(this))
                .orElse(-1);
        if (predicateId >= 0) {
            Long2IntOpenHashMap matchedPredicates = blockWorldState.getMatchContext().getOrCreate("matchedPredicates", Long2IntOpenHashMap::new);
            matchedPredicates.defaultReturnValue(-1);
            matchedPredicates.put(blockWorldState.getPos().asLong(), predicateId);
        }
        if (proxyWhileFormed.isEnable()) {
            Map<Long, ProxyWhileFormedMatch> proxyMap = blockWorldState.getMatchContext().getOrCreate("proxyWhileFormed", HashMap::new);
            proxyMap.put(blockWorldState.getPos().asLong(), new ProxyWhileFormedMatch(predicateId, proxyWhileFormed));
        }
        if (allowOpenUI) {
            blockWorldState.getMatchContext().getOrCreate("openUIMask", it.unimi.dsi.fastutil.longs.LongOpenHashSet::new).add(blockWorldState.getPos().asLong());
        }
        return true;
    }

    private boolean testGlobal(MultiblockState blockWorldState) {
        if (minCount == -1 && maxCount == -1) return true;
        Integer count = blockWorldState.getGlobalCount().get(this);
        boolean base = predicate.test(blockWorldState);
        count = (count == null ? 0 : count) + (base ? 1 : 0);
        blockWorldState.getGlobalCount().put(this, count);
        if (maxCount == -1 || count <= maxCount) return base;
        blockWorldState.setError(new SinglePredicateError(this, 0));
        return false;
    }

    private boolean testLayer(MultiblockState blockWorldState) {
        if (minLayerCount == -1 && maxLayerCount == -1) return true;
        Integer count = blockWorldState.getLayerCount().get(this);
        boolean base = predicate.test(blockWorldState);
        count = (count == null ? 0 : count) + (base ? 1 : 0);
        blockWorldState.getLayerCount().put(this, count);
        if (maxLayerCount == -1 || count <= maxLayerCount) return base;
        blockWorldState.setError(new SinglePredicateError(this, 2));
        return false;
    }

    public List<ItemStack> getItemCandidates() {
        makeSureBuilt();
        if (LDLib2.isClient()) {
            return candidates == null ? Collections.emptyList() : Arrays.stream(this.candidates.get()).filter(info -> info.getBlockState().getBlock() != Blocks.AIR)
                    .map(blockInfo -> blockInfo.getItemStackForm(Minecraft.getInstance().level, BlockPos.ZERO)).collect(Collectors.toList());
        }
        return candidates == null ? Collections.emptyList() : Arrays.stream(this.candidates.get()).filter(info -> info.getBlockState().getBlock() != Blocks.AIR).map(BlockInfo::getItemStackForm).collect(Collectors.toList());
    }

    public IGuiTexture getPreviewTexture() {
        makeSureBuilt();
        return previewTexture.get();
    }

    @Override
    public String getTranslateKey() {
        return "config.%s.%s".formatted(group(), name());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void buildConfigurator(ConfiguratorGroup father) {
        createPreview(father);
        IConfigurable.super.buildConfigurator(father);
    }


    /**
     * Preview of the renderer.
     */
    @OnlyIn(Dist.CLIENT)
    protected void createPreview(ConfiguratorGroup father) {
        father.addConfigurators(new Configurator("ldlib.gui.editor.group.preview").addChild(createPreviewScene()));
    }

    @OnlyIn(Dist.CLIENT)
    protected Scene createPreviewScene() {
        makeSureBuilt();
        var level = new TrackedDummyWorld();

        level.addBlock(BlockPos.ZERO, Optional.ofNullable(candidates).map(Supplier::get)
                .filter(x -> x.length > 0)
                .map(x -> x[0])
                .orElse(BlockInfo.EMPTY));

        var scene = new Scene();
        scene.setRenderFacing(false);
        scene.setRenderSelect(false);
        scene.createScene(level);
        assert scene.getRenderer() != null;
        scene.getRenderer().setOnLookingAt(null); // better performance
        scene.setRenderedCore(Collections.singleton(BlockPos.ZERO), null);
        scene.layout(layout -> {
            layout.setPipelineState(StyleOrigin.DEFAULT);
            layout.setAspectRatio(1.0f);
            layout.setWidthPercent(80);
            layout.setAlignSelf(YogaAlign.CENTER);
            layout.setPadding(YogaEdge.ALL, 3);
            layout.setPipelineState(StyleOrigin.INLINE);
        });
        scene.addEventListener(UIEvents.TICK, e -> {
            var mui = e.currentElement.getModularUI();
            if (mui == null) return;
            if (mui.getTickCounter() % 20 != 0) return;
            level.removeBlock(BlockPos.ZERO);
            level.addBlock(BlockPos.ZERO, Optional.ofNullable(candidates).map(Supplier::get)
                    .filter(x -> x.length > 0)
                    .map(x -> x[(int) ((mui.getTickCounter() / 20L) % x.length)])
                    .orElse(BlockInfo.EMPTY));
        });
        scene.style(style -> Style.defaultPipeline(style, s -> s.backgroundTexture(Sprites.BORDER1_RT1)));
        scene.addClass("preview_bg");
        return scene;
    }

    @Getter
    @Setter
    public static class ProxyWhileFormed implements IToggleConfigurable {
        @Persisted
        protected boolean enable;
        @Persisted
        protected StateMachine<MachineState> stateMachine = createDefaultStateMachine();
        @Configurable(name = "config.block_pattern.predicate.proxyWhileFormed.proxyCapabilities", tips = "config.block_pattern.predicate.proxyWhileFormed.proxyCapabilities.tooltip")
        @ConfigList(configuratorMethod = "proxyCapabilitiesConfigurator", addDefaultMethod = "defaultProxyCapability")
        @ReadOnlyManaged(serializeMethod = "proxyCapabilitiesSerialize", deserializeMethod = "proxyCapabilitiesDeserialize")
        protected final List<ConfigPartSettings.ProxyCapability> proxyCapabilities = new ArrayList<>();

        @OnlyIn(Dist.CLIENT)
        @SuppressWarnings("unused")
        protected Configurator proxyCapabilitiesConfigurator(Supplier<ConfigPartSettings.ProxyCapability> getter, java.util.function.Consumer<ConfigPartSettings.ProxyCapability> setter) {
            var group = new ConfiguratorGroup("", false).hideTitle();
            getter.get().buildConfigurator(group);
            return group;
        }

        @SuppressWarnings("unused")
        protected ConfigPartSettings.ProxyCapability defaultProxyCapability() {
            return new ConfigPartSettings.ProxyCapability();
        }

        @SuppressWarnings("unused")
        protected IntTag proxyCapabilitiesSerialize(List<ConfigPartSettings.ProxyCapability> list) {
            return IntTag.valueOf(list.size());
        }

        @SuppressWarnings("unused")
        protected List<ConfigPartSettings.ProxyCapability> proxyCapabilitiesDeserialize(IntTag tag) {
            var list = new ArrayList<ConfigPartSettings.ProxyCapability>(tag.getAsInt());
            for (int i = 0; i < tag.getAsInt(); i++) {
                list.add(defaultProxyCapability());
            }
            return list;
        }

        public static StateMachine<MachineState> createDefaultStateMachine() {
            return new StateMachine<>(MachineState.baseBuilder()
                    .child("formed", formed -> formed
                            .child("working", working -> working.child("waiting"))
                            .child("suspend"))
                    .child("unformed")
                    .build());
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public void buildConfigurator(ConfiguratorGroup father) {
            IToggleConfigurable.super.buildConfigurator(father);
            father.addConfigurator(new ProxyStateMachineConfigurator());
        }

        private StateMachine<MachineState> safeStateMachine() {
            if (stateMachine == null) {
                stateMachine = createDefaultStateMachine();
            }
            return stateMachine;
        }

        @OnlyIn(Dist.CLIENT)
        private class ProxyStateMachineConfigurator extends Configurator {
            private final TreeList<MachineState> treeList = new TreeList<>();
            private final Inspector inspector = new Inspector();
            @Nullable
            private MachineState selectedState;

            private ProxyStateMachineConfigurator() {
                var stateScroller = new ScrollerView();
                inlineContainer.layout(layout -> {
                    layout.flexDirection(FlexDirection.COLUMN);
                    layout.gapAll(2);
                });
                stateScroller.layout(layout -> {
                    layout.widthPercent(100);
                    layout.height(90);
                });
                inspector.layout(layout -> {
                    layout.widthPercent(100);
                    layout.height(180);
                });
                treeList.setStaticTree(true)
                        .setDoubleClickToExpand(true)
                        .setSupportMultipleSelection(false)
                        .setNodeUISupplier(TreeList.optionalIconTextTemplate(state -> IGuiTexture.EMPTY,
                                state -> Component.literal(state.name())))
                        .setOnSelectedChanged(this::onSelectedChanged);
                stateScroller.addScrollViewChild(treeList);
                stateScroller.addEventListener(UIEvents.MOUSE_DOWN, event -> {
                    if (event.button == 1) {
                        var menu = createStateMenu();
                        if (!menu.isEmpty() && getModularUI() != null) {
                            getModularUI().ui.rootElement.addChild(new Menu<>(menu.build(), TreeBuilder.Menu::uiProvider)
                                    .setHoverTextureProvider(TreeBuilder.Menu::hoverTextureProvider)
                                    .setOnNodeClicked(TreeBuilder.Menu::handle)
                                    .layout(layout -> {
                                        var offset = getModularUI().ui.rootElement.worldToLocalLayoutOffset(new org.joml.Vector2f(event.x, event.y));
                                        layout.left(offset.x);
                                        layout.top(offset.y);
                                    }));
                            event.stopPropagation();
                        }
                    }
                });
                addInlineChildren(stateScroller, inspector);
                reloadStateTree();
            }

            private void onSelectedChanged(Set<MachineState> selectedStates) {
                if (selectedStates.size() == 1) {
                    selectedState = selectedStates.iterator().next();
                    inspector.inspect(selectedState, configurator -> notifyChanges(), () -> {
                        var state = selectedState;
                        if (state != null) {
                            treeList.removeSelected(state, false);
                        }
                        selectedState = null;
                    });
                } else {
                    inspector.clear();
                    selectedState = null;
                }
            }

            private void reloadStateTree() {
                var expandedNodes = new HashSet<String>();
                for (var state : treeList.getExpandedNodes()) {
                    expandedNodes.add(state.name());
                }
                treeList.setRoot(safeStateMachine().getRootState());
                if (treeList.getRoot() != null) {
                    treeList.expandAllNodesIf(treeList.getRoot(), state -> expandedNodes.contains(state.name()));
                }
                safeStateMachine().initStateMachine();
                notifyChanges();
            }

            private TreeBuilder.Menu createStateMenu() {
                var menu = TreeBuilder.Menu.start();
                menu.leaf(Icons.ADD, "editor.machine_state.add", () -> Dialog.stringEditorDialog(
                        "editor.machine_state.name",
                        "new_state",
                        name -> !safeStateMachine().hasState(name),
                        name -> {
                            if (!safeStateMachine().hasState(name)) {
                                var parent = selectedState == null ? safeStateMachine().getRootState() : selectedState;
                                parent.addChild(name);
                                reloadStateTree();
                            }
                        }).show(getModularUI()));
                if (selectedState != null && selectedState.parent() != null) {
                    menu.crossLine();
                    menu.leaf(Icons.REMOVE, "editor.machine_state.remove", () -> {
                        selectedState.parent().removeChild(selectedState);
                        inspector.clear();
                        selectedState = null;
                        reloadStateTree();
                    });
                    menu.leaf("ldlib.gui.editor.menu.rename", () -> Dialog.stringEditorDialog(
                            "editor.machine_state.name",
                            selectedState.name(),
                            name -> !safeStateMachine().hasState(name),
                            name -> {
                                if (!safeStateMachine().hasState(name) && selectedState != null && selectedState.parent() != null) {
                                    var state = selectedState;
                                    var parent = state.parent();
                                    var children = new ArrayList<>(state.children());
                                    var index = parent.getChildSiblingIndex(state);
                                    parent.removeChild(state);
                                    var renamed = copyStateWithName(state, parent, name);
                                    parent.addChildAt(renamed, index);
                                    for (var child : children) {
                                        renamed.addChild(child);
                                    }
                                    selectedState = renamed;
                                    reloadStateTree();
                                    treeList.setSelected(Set.of(renamed), true);
                                }
                            }).show(getModularUI()));
                }
                return menu;
            }

            private MachineState copyStateWithName(MachineState state, MachineState parent, String name) {
                var renamed = new MachineState(name, parent,
                        state.renderer().getValue(),
                        state.shape().getValue(),
                        state.lightLevel().getValue(),
                        state.renderingBox().getValue());
                renamed.renderer().setEnable(state.renderer().isEnable());
                renamed.shape().setEnable(state.shape().isEnable());
                renamed.lightLevel().setEnable(state.lightLevel().isEnable());
                renamed.renderingBox().setEnable(state.renderingBox().isEnable());
                renamed.isGlobalVisible(state.isGlobalVisible());
                renamed.renderingRadius(state.renderingRadius());
                return renamed;
            }
        }
    }

    public record ProxyWhileFormedMatch(int predicateId, ProxyWhileFormed proxy) {}
}
