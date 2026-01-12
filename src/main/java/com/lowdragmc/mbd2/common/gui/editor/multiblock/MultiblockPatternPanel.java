package com.lowdragmc.mbd2.common.gui.editor.multiblock;

import com.lowdragmc.lowdraglib.client.utils.RenderBufferUtils;
import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.editor.Icons;
import com.lowdragmc.lowdraglib.gui.editor.configurator.*;
import com.lowdragmc.lowdraglib.gui.editor.ui.ConfigPanel;
import com.lowdragmc.lowdraglib.gui.editor.ui.Editor;
import com.lowdragmc.lowdraglib.gui.editor.ui.MenuPanel;
import com.lowdragmc.lowdraglib.gui.editor.ui.sceneeditor.SceneEditorWidget;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.SceneWidget;
import com.lowdragmc.lowdraglib.gui.widget.SelectorWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.*;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.blockentity.MachineBlockEntity;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import com.lowdragmc.mbd2.common.gui.editor.MultiblockMachineProject;
import com.lowdragmc.mbd2.common.gui.editor.multiblock.widget.PatternLayerList;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.util.Either;
import lombok.Getter;
import lombok.val;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3i;

import java.io.File;
import java.util.*;

public class MultiblockPatternPanel extends WidgetGroup {
    public static String COPY_TAG = "mb_predicates";
    @Getter
    protected final MachineEditor editor;
    @Getter
    protected final MultiblockMachineProject project;
    @Getter
    protected final TrackedDummyWorld level;
    @Getter
    protected final SceneEditorWidget scene;
    protected final WidgetGroup buttonGroup;
    // runtime
    private long lastClickTime;
    private BlockPosFace clickedPosFace;
    private boolean isSelected;
    @Getter
    private int visibleLayer = -1;
    private final Set<Vector3i> selectedBlocks = new HashSet<>();

    public MultiblockPatternPanel(MachineEditor editor, MultiblockMachineProject project) {
        super(0, MenuPanel.HEIGHT + 16, Editor.INSTANCE.getSize().getWidth() - ConfigPanel.WIDTH, Editor.INSTANCE.getSize().height - MenuPanel.HEIGHT - 16);
        this.editor = editor;
        this.project = project;
        addWidget(scene = new SceneEditorWidget(0, 0, this.getSize().width, this.getSize().height, null));
        addWidget(buttonGroup = new WidgetGroup(0, 0, this.getSize().width, this.getSize().height));
        scene.disableTransformGizmo();
        scene.setRenderFacing(false);
        scene.setRenderSelect(false);
        scene.createScene(level = new TrackedDummyWorld());
        scene.setAfterWorldRender(this::renderAfterWorld);
        scene.useCacheBuffer();
        reloadScene(true, false);
        prepareButtonGroup();
        buttonGroup.setSize(new Size(Math.max(0, buttonGroup.widgets.size() * 25 - 5), 20));
        buttonGroup.setSelfPosition(new Position(this.getSize().width - buttonGroup.getSize().width - 25, 25));
    }

    /**
     * Called when the panel is selected/switched to.
     */
    public void onPanelSelected() {
        editor.getConfigPanel().clearAllConfigurators();
        editor.getToolPanel().clearAllWidgets();
        editor.getToolPanel().setTitle("editor.machine.multiblock.multiblock_pattern.layer");
        editor.getToolPanel().addNewToolBox("editor.machine.multiblock.multiblock_pattern.layer", Icons.WIDGET_CUSTOM, size -> new PatternLayerList(this, size));
        if (editor.getToolPanel().inAnimate()) {
            editor.getToolPanel().getAnimation().appendOnFinish(() -> editor.getToolPanel().show());
        } else {
            editor.getToolPanel().show();
        }
        isSelected = true;
    }

    /**
     * Called when the panel is deselected/switched from.
     */
    public void onPanelDeselected() {
        isSelected = false;
        editor.getToolPanel().setTitle("ldlib.gui.editor.group.tool_box");
        editor.getToolPanel().hide();
        editor.getToolPanel().clearAllWidgets();
        editor.getConfigPanel().clearAllConfigurators();
    }

    public void reloadScene(boolean clearSelected, boolean keepZoom) {
        this.level.clear();
        if (clearSelected) clearSelectedBlocks();
        var holders = project.getBlockPlaceholders();
        var positions = new HashSet<BlockPos>();
        var controllerFace = Direction.NORTH;
        for (int x = 0; x < holders.length; x++) {
            for (int y = 0; y < holders[x].length; y++) {
                for (int z = 0; z < holders[x][y].length; z++) {
                    var holder = holders[x][y][z];
                    if (holder != null && holder.isController()) {
                        controllerFace = holder.getFacing();
                    }
                }
            }
        }
        for (int x = 0; x < holders.length; x++) {
            for (int y = 0; y < holders[x].length; y++) {
                for (int z = 0; z < holders[x][y].length; z++) {
                    var holder = holders[x][y][z];
                    if (holder != null) {
                        val pos = new BlockPos(x, y, z);
                        positions.add(pos);
                        if (visibleLayer >= 0) {
                            switch (project.getLayerAxis()) {
                                case X -> {
                                    if (x != visibleLayer) continue;
                                }
                                case Y -> {
                                    if (y != visibleLayer) continue;
                                }
                                case Z -> {
                                    if (z != visibleLayer) continue;
                                }
                            }
                        }
                        if (holder.isController()) {
                            MBDRegistries.FAKE_MACHINE().blockProperties().rotationState().property.ifPresent(property ->
                                    this.level.addBlock(pos, BlockInfo.fromBlockState(MBDRegistries.FAKE_MACHINE()
                                            .block().defaultBlockState().setValue(property, holder.getFacing()))));
                            Optional.ofNullable(this.level.getBlockEntity(pos)).ifPresent(blockEntity -> {
                                if (blockEntity instanceof MachineBlockEntity machineBlockEntity) {
                                    var controllerMachine = project.getDefinition().createMachine(machineBlockEntity);
                                    machineBlockEntity.setMachine(controllerMachine);
                                    controllerMachine.loadAdditionalTraits();
                                    controllerMachine.getAdditionalTraits().forEach(ITrait::onLoadingTraitInPreview);
                                }
                            });
                        } else {
                            var finalControllerFace = controllerFace;
                            holder.getPredicates().stream().map(holder.predicateResource::getResource).filter(Objects::nonNull)
                                    .filter(predicate -> !predicate.controllerFront.isEnable() || predicate.controllerFront.getValue() == finalControllerFace)
                                    .findAny().ifPresent(predicate -> {
                                        if (predicate.candidates == null) return;
                                        var blockInfo = Arrays.stream(predicate.candidates.get()).findAny();
                                        blockInfo.ifPresent(info -> this.level.addBlock(pos, info));
                                    });
                        }
                    }
                }
            }
        }
        var previousZoom = scene.getZoom();
        scene.setRenderedCore(positions, null);
        if (keepZoom) {
            scene.setZoom(previousZoom);
        }
    }

    private class PredicateConfigurator implements IConfigurable {

        public String mapName(Either<String, File> key) {
            return key.map(l -> l, r -> ChatFormatting.YELLOW + project.getPredicateResource().getStaticResourceName(r) + ChatFormatting.RESET);
        }

        public Either<String, File> mapKey(String name) {
            if (name.startsWith(ChatFormatting.YELLOW.toString()) && name.endsWith(ChatFormatting.RESET.toString())) {
                var realName = name.substring(ChatFormatting.YELLOW.toString().length(), name.length() - ChatFormatting.RESET.toString().length());
                return Either.right(project.getPredicateResource().getStaticResourceFile(realName));
            } else {
                return Either.left(name);
            }
        }

        @Override
        public void buildConfigurator(ConfiguratorGroup father) {
            var placeholders =  selectedBlocks.stream().map(pos -> project.getBlockPlaceholders()[pos.x][pos.y][pos.z]).toList();
            var intersection = new LinkedList<>(placeholders.get(0).getPredicates());
            Runnable notifyUpdate = () -> placeholders.forEach(holder -> {
                var predicates = holder.getPredicates();
                predicates.clear();
                predicates.addAll(intersection);
                reloadScene(false, true);
            });
            for (var placeholder : placeholders) {
                intersection.retainAll(placeholder.getPredicates());
            }
            var predicatesConfigurator = new ArrayConfiguratorGroup<>("mbd2.gui.editor.group.predicate", false,
                    () -> intersection, (getter, setter) -> {
                var group = new WidgetGroup(0, 0, 180, 100);
                var preview = new ImageWidget(50, 0, 80, 80, () -> {
                    var resource = project.getPredicateResource().getResource(getter.get());
                    if (resource == null) return IGuiTexture.EMPTY;
                    return resource.getPreviewTexture();
                }).setBorder(2, ColorPattern.T_WHITE.color);
                preview.setDraggingConsumer(
                        o -> o instanceof Either<?, ?> key && key.map(
                                l -> l instanceof String s && project.getPredicateResource().hasBuiltinResource(s),
                                r -> r instanceof File f && project.getPredicateResource().hasStaticResource(f)),
                        o -> preview.setBorder(2, ColorPattern.GREEN.color),
                        o -> preview.setBorder(2, ColorPattern.T_WHITE.color),
                        o -> {
                            if (o instanceof Either<?, ?> key && key.map(
                                    l -> l instanceof String s && project.getPredicateResource().hasBuiltinResource(s),
                                    r -> r instanceof File f && project.getPredicateResource().hasStaticResource(f))) {
                                setter.accept((Either<String, File>) key);
                            }
                        });
                var resource = project.getPredicateResource();
                var selector = new SelectorWidget(0, 85, 180, 10,
                        resource.allResources().map(Map.Entry::getKey).map(this::mapName).toList(), -1)
                        .setCandidatesSupplier(() -> resource.allResources().map(Map.Entry::getKey).map(this::mapName).toList())
                        .setSupplier(() -> mapName(getter.get()))
                        .setOnChanged(name -> setter.accept(mapKey(name)))
                        .setMaxCount(5)
                        .setIsUp(true)
                        .setButtonBackground(ColorPattern.T_GRAY.rectTexture().setRadius(5))
                        .setBackground(new GuiTextureGroup(ColorPattern.BLACK.rectTexture(), ColorPattern.GRAY.borderTexture(1)))
                        .setValue(mapName(getter.get()));
                group.addWidget(preview);
                group.addWidget(selector);
                return new WrapperConfigurator("", group);
            }, true);
            predicatesConfigurator.setAddDefault(() -> Either.left("any"));
            predicatesConfigurator.setOnAdd(value -> {
                intersection.add(value);
                notifyUpdate.run();
            });
            predicatesConfigurator.setOnRemove(value -> {
                intersection.remove(value);
                notifyUpdate.run();
            });
            predicatesConfigurator.setOnUpdate(values -> {
                intersection.clear();
                intersection.addAll(values);
                notifyUpdate.run();
            });
            predicatesConfigurator.setOnReorder((index, value) -> {});
            father.addConfigurators(predicatesConfigurator);
        }
    }

    private void reloadPredicateConfigurator() {
        editor.getConfigPanel().openConfigurator(MachineEditor.BASIC, new PredicateConfigurator());
    }

    public void setVisibleLayer(int visibleLayer) {
        if (this.visibleLayer == visibleLayer) return;
        this.visibleLayer = visibleLayer;
        reloadScene(false, true);
    }

    public boolean isBlockSelected(Vector3i pos) {
        return selectedBlocks.contains(pos);
    }

    public void clearSelectedBlocks() {
        selectedBlocks.clear();
        if (isSelected) {
            editor.getConfigPanel().clearAllConfigurators(MachineEditor.BASIC);
        }
    }

    public void addSelectedBlock(Vector3i pos) {
        addSelectedBlock(pos, false);
        reloadPredicateConfigurator();
    }

    public void addSelectedBlock(Vector3i pos, boolean clear) {
        if (clear) selectedBlocks.clear();
        selectedBlocks.add(pos);
        reloadPredicateConfigurator();
    }

    public void addSelectedBlocks(Collection<Vector3i> positions, boolean clear) {
        if (clear) selectedBlocks.clear();
        selectedBlocks.addAll(positions);
        reloadPredicateConfigurator();
    }

    public void removeSelectedBlock(Vector3i pos) {
        selectedBlocks.remove(pos);
        if (selectedBlocks.isEmpty()) {
            editor.getConfigPanel().clearAllConfigurators(MachineEditor.BASIC);
        } else {
            reloadPredicateConfigurator();
        }
    }

    /**
     * prepare the button group, you can add buttons / switches here.
     */
    protected void prepareButtonGroup() {
    }

    /**
     * render the scene after the world is rendered.
     * <br/> e.g. <br/>
     * shape frame lines.
     */
    private void renderAfterWorld(SceneWidget sceneWidget) {
        var poseStack = new PoseStack();

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();

        var tessellator = Tesselator.getInstance();
        var buffer = tessellator.getBuilder();
        RenderSystem.enableCull();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        for (var pos : selectedBlocks) {
            RenderBufferUtils.drawCubeFace(poseStack, buffer,
                    pos.x - 0.001f, pos.y - 0.001f, pos.z - 0.001f,
                    pos.x + 1.001f, pos.y + 1.001f, pos.z + 1.001f,
                    0.1f, 0.7f, 0.1f, 0.5f, false);
        }

        // draw predicate dragging highlight
        if (scene.getHoverPosFace() != null &&
                gui.getModularUIGui().getDraggingElement() instanceof Either<?, ?> key &&
                key.map(l -> l instanceof String s && project.getPredicateResource().hasBuiltinResource(s),
                        r -> r instanceof File f && project.getPredicateResource().hasStaticResource(f))) {
            var pos = scene.getHoverPosFace().pos;
            RenderBufferUtils.drawCubeFace(poseStack, buffer,
                    pos.getX() - 0.002f, pos.getY() - 0.002f, pos.getZ() - 0.002f,
                    pos.getX()  + 1.002f, pos.getY() + 1.002f, pos.getZ() + 1.002f,
                    0.1f, 0.7f, 0.7f, 0.5f, false);
        }

        tessellator.end();
    }

    /**
     * Called when the block placeholders are changed in the project.
     */
    public void onBlockPlaceholdersChanged() {
        reloadScene(true, false);
        editor.getToolPanel().getToolBoxes().stream()
                .filter(PatternLayerList.class::isInstance)
                .map(PatternLayerList.class::cast)
                .findAny()
                .ifPresent(PatternLayerList::reloadLayers);
    }

    public void openMenu(double mouseX, double mouseY) {
        if (!selectedBlocks.isEmpty()) {
            var menu = TreeBuilder.Menu.start();
            menu.leaf(Icons.COPY, "ldlib.gui.editor.menu.copy", () -> {
                var placeholders =  selectedBlocks.stream().map(pos -> project.getBlockPlaceholders()[pos.x][pos.y][pos.z]).toList();
                var intersection = new ArrayList<>(placeholders.get(0).getPredicates());
                for (var placeholder : placeholders) {
                    intersection.retainAll(placeholder.getPredicates());
                }
                editor.setCopy(COPY_TAG, intersection);
            });
            if (COPY_TAG.equals(editor.getCopyType()) && editor.getCopied() instanceof List<?>) {
                menu.leaf(Icons.PASTE, "ldlib.gui.editor.menu.paste", () -> {
                    var predicates = (List<Either<String, File>>) editor.getCopied();
                    selectedBlocks.forEach(pos -> {
                        var holder = project.getBlockPlaceholders()[pos.x][pos.y][pos.z];
                        holder.getPredicates().clear();
                        holder.getPredicates().addAll(predicates);
                    });
                    reloadScene(false, true);
                    // refresh configurator
                    reloadPredicateConfigurator();
                });
            }
            if (selectedBlocks.size() == 1) {
                menu.crossLine();
                menu.branch("editor.machine.multiblock.multiblock_pattern.set_as_controller", m -> {
                    for (var facing : Direction.values()) {
                        m.leaf(facing.getName(), () -> {
                            Arrays.stream(project.getBlockPlaceholders()).flatMap(Arrays::stream).flatMap(Arrays::stream)
                                    .filter(BlockPlaceholder::isController)
                                    .forEach(holder -> holder.setController(false));
                            selectedBlocks.stream().map(pos -> project.getBlockPlaceholders()[pos.x][pos.y][pos.z]).forEach(holder -> {
                                holder.setController(true);
                                holder.setFacing(facing);
                            });
                            reloadScene(false, true);
                        });
                    }
                });
            }
            editor.openMenu(mouseX, mouseY, menu);
        }
    }

    @Override
    public void drawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOverElement(mouseX, mouseY)) {
            clickedPosFace = scene.getHoverPosFace();
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isMouseOverElement(mouseX, mouseY)) {
            var hoverPosFace = scene.getHoverPosFace();
            var clickPosFace = scene.getClickPosFace();
            if (hoverPosFace != null && hoverPosFace.equals(clickPosFace)) {
                if (button == 0 && gui != null) {
                    // select blocks by click
                    var pos = new Vector3i(hoverPosFace.pos.getX(), hoverPosFace.pos.getY(), hoverPosFace.pos.getZ());
                    if (isCtrlDown() || isShiftDown()) {
                        if (isBlockSelected(pos)) {
                            removeSelectedBlock(pos);
                        } else {
                            addSelectedBlock(pos);
                        }
                    } else {
                        var clickTime = gui.getTickCount();
                        if (clickTime - lastClickTime < 10) {
                            // select all blocks with same predicates
                            var blocks = new ArrayList<Vector3i>();
                            blocks.add(pos);
                            var holder = project.getBlockPlaceholders()[pos.x()][pos.y()][pos.z()];
                            var predicates = holder.getPredicates();
                            for (int x = 0; x < project.getBlockPlaceholders().length; x++) {
                                for (int y = 0; y < project.getBlockPlaceholders()[x].length; y++) {
                                    for (int z = 0; z < project.getBlockPlaceholders()[x][y].length; z++) {
                                        var holder2 = project.getBlockPlaceholders()[x][y][z];
                                        if (holder2.getPredicates().equals(predicates)) {
                                            blocks.add(new Vector3i(x, y, z));
                                        }
                                    }
                                }
                            }
                            addSelectedBlocks(blocks, true);
                        } else {
                            addSelectedBlock(pos, true);
                        }
                        lastClickTime = gui.getTickCount();
                    }
                }
            }
            // right click to open menu
            if (hoverPosFace != null && hoverPosFace.equals(clickedPosFace) && button == 1) {
                openMenu(mouseX, mouseY);
            }
            // apply predicate dragging
            if (hoverPosFace != null && button == 0 && gui != null &&
                    gui.getModularUIGui().getDraggingElement() instanceof Either<?, ?> key &&
                    key.map(l -> l instanceof String s && project.getPredicateResource().hasBuiltinResource(s),
                            r -> r instanceof File f && project.getPredicateResource().hasStaticResource(f))) {
                var pos = scene.getHoverPosFace().pos;
                var holder = project.getBlockPlaceholders()[pos.getX()][pos.getY()][pos.getZ()];
                holder.getPredicates().clear();
                holder.getPredicates().add((Either<String, File>) key);
                reloadScene(false, true);
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
