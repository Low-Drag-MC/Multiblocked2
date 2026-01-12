package com.lowdragmc.mbd2.common.gui.editor.multiblock;

import com.lowdragmc.lowdraglib.client.utils.RenderBufferUtils;
import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.editor.annotation.ConfigSelector;
import com.lowdragmc.lowdraglib.gui.editor.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.gui.editor.configurator.WrapperConfigurator;
import com.lowdragmc.lowdraglib.gui.editor.ui.ConfigPanel;
import com.lowdragmc.lowdraglib.gui.editor.ui.Editor;
import com.lowdragmc.lowdraglib.gui.editor.ui.MenuPanel;
import com.lowdragmc.lowdraglib.gui.editor.ui.sceneeditor.SceneEditorWidget;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.ColorUtils;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import com.lowdragmc.mbd2.api.pattern.MultiblockShapeInfo;
import com.lowdragmc.mbd2.api.pattern.predicates.PredicateBlocks;
import com.lowdragmc.mbd2.api.pattern.predicates.PredicateFluids;
import com.lowdragmc.mbd2.common.gui.editor.MachineEditor;
import com.lowdragmc.mbd2.common.gui.editor.MultiblockMachineProject;
import com.lowdragmc.mbd2.utils.ControllerBlockInfo;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.datafixers.util.Either;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3i;
import org.lwjgl.opengl.GL11;

import java.util.HashSet;
import java.util.Optional;

public class MultiblockAreaPanel extends WidgetGroup {
    @Getter
    protected final MultiblockMachineProject project;
    @Getter
    protected final SceneEditorWidget scene;
    @Getter
    protected final MultiblockPatternPanel patternPanel;
    protected final WidgetGroup buttonGroup;

    // runtime
    private final Runtime runtime = new Runtime();

    public class Runtime implements IConfigurable {
        @Configurable(name = "editor.machine.multiblock.area_panel.sceneRadius", tips="editor.machine.multiblock.area_panel.sceneRadius.tips")
        @NumberRange(range={1, 50})
        private int sceneRadius = 5;

        @Configurable(name = "editor.machine.multiblock.area_panel.area", tips="editor.machine.multiblock.area_panel.area.tips", subConfigurable = true, canCollapse = false, collapse = false)
        private final Area area = new Area();

        public class Area implements IConfigurable {
            @Configurable(name = "editor.machine.multiblock.area_panel.from", tips="editor.machine.multiblock.area_panel.from.tips")
            @NumberRange(range={Integer.MIN_VALUE, Integer.MAX_VALUE}, wheel=1)
            private BlockPos from = Minecraft.getInstance().player.getOnPos();

            @Configurable(name = "editor.machine.multiblock.area_panel.to", tips="editor.machine.multiblock.area_panel.to.tips")
            @NumberRange(range={Integer.MIN_VALUE, Integer.MAX_VALUE}, wheel=1)
            private BlockPos to = Minecraft.getInstance().player.getOnPos();

            @Override
            public void buildConfigurator(ConfiguratorGroup father) {
                IConfigurable.super.buildConfigurator(father);
                father.addConfigurators(new WrapperConfigurator("", new WidgetGroup(0, 0, 150, 5)
                        .addWidget(new ImageWidget(0, -10, 150, 10,
                                new TextTexture("editor.machine.multiblock.area_panel.area.tips")))));
            }
        }

        @Configurable(name = "editor.machine.multiblock.area_panel.controllerOffset", tips="editor.machine.multiblock.area_panel.controllerOffset.tips")
        @NumberRange(range={0, Integer.MAX_VALUE}, wheel=1)
        private Vector3i controllerOffset = new Vector3i(0, 0, 0);
        @Configurable(name = "editor.machine.multiblock.area_panel.controllerFace", tips="editor.machine.multiblock.area_panel.controllerFace.tips")
        @ConfigSelector(candidate = {"north", "south", "west", "east"})
        private Direction controllerFace = Direction.NORTH;

        private boolean isFromClicked = false;

        @ConfigSetter(field = "sceneRadius")
        public void setSceneRadius(int radius) {
            this.sceneRadius = radius;
            reloadScene();
        }

        @ConfigSetter(field = "controllerOffset")
        public void setControllerOffset(Vector3i offset) {
            var minX = Math.min(runtime.area.from.getX(), runtime.area.to.getX());
            var minY = Math.min(runtime.area.from.getY(), runtime.area.to.getY());
            var minZ = Math.min(runtime.area.from.getZ(), runtime.area.to.getZ());
            var maxX = Math.max(runtime.area.from.getX(), runtime.area.to.getX());
            var maxY = Math.max(runtime.area.from.getY(), runtime.area.to.getY());
            var maxZ = Math.max(runtime.area.from.getZ(), runtime.area.to.getZ());
            this.controllerOffset = new Vector3i(
                    Mth.clamp(offset.x, 0, maxX - minX),
                    Mth.clamp(offset.y, 0, maxY - minY),
                    Mth.clamp(offset.z, 0, maxZ - minZ)
                    );
        }

        @ConfigSetter(field = "from")
        public void setFrom(BlockPos from) {
            this.area.from = from;
            setControllerOffset(controllerOffset);
        }

        @ConfigSetter(field = "to")
        public void setTo(BlockPos to) {
            this.area.to = to;
            setControllerOffset(controllerOffset);
        }

        @Override
        public void buildConfigurator(ConfiguratorGroup father) {
            IConfigurable.super.buildConfigurator(father);
            var buttonGroup = new WidgetGroup(0, 0, 100, 22);
            buttonGroup.addWidget(new ButtonWidget(0, 0, 100, 10,
                    new GuiTextureGroup(
                            ColorPattern.T_GRAY.rectTexture().setRadius(5),
                            new TextTexture("editor.machine.multiblock.area_panel.generatePattern.button.0")),
                    cd -> {
                        DialogWidget.showNotification(Editor.INSTANCE,
                                "editor.machine.multiblock.area_panel.generatePattern.button.0.title",
                                "editor.machine.multiblock.area_panel.generatePattern.button.0.info");
                        generatePattern();
                    })
                    .setHoverTexture(
                            ColorPattern.WHITE.borderTexture(1).setRadius(5),
                            new TextTexture("editor.machine.multiblock.area_panel.generatePattern.button.0")));
            buttonGroup.addWidget(new ButtonWidget(0, 12, 100, 10,
                    new GuiTextureGroup(
                            ColorPattern.T_GRAY.rectTexture().setRadius(5),
                            new TextTexture("editor.machine.multiblock.area_panel.generatePattern.button.1")),
                    cd -> {
                        DialogWidget.showNotification(Editor.INSTANCE,
                                "editor.machine.multiblock.area_panel.generatePattern.button.1.title",
                                "editor.machine.multiblock.area_panel.generatePattern.button.1.info");
                        generateShapeInfo();
                    })
                    .setHoverTexture(
                            ColorPattern.WHITE.borderTexture(1).setRadius(5),
                            new TextTexture("editor.machine.multiblock.area_panel.generatePattern.button.1")));
            var wrapper = new WrapperConfigurator("editor.machine.multiblock.area_panel.generatePattern", buttonGroup);
            wrapper.setTips("editor.machine.multiblock.area_panel.generatePattern.tips");
            father.addConfigurators(wrapper);
        }

    }

    public MultiblockAreaPanel(MultiblockMachineProject project, MultiblockPatternPanel patternPanel) {
        super(0, MenuPanel.HEIGHT + 16, Editor.INSTANCE.getSize().getWidth() - ConfigPanel.WIDTH, Editor.INSTANCE.getSize().height - MenuPanel.HEIGHT - 16);
        this.project = project;
        this.patternPanel = patternPanel;
        addWidget(scene = new SceneEditorWidget(0, 0, this.getSize().width, this.getSize().height, null));
        addWidget(buttonGroup = new WidgetGroup(0, 0, this.getSize().width, this.getSize().height));
        scene.disableTransformGizmo();
        scene.setRenderFacing(false);
        scene.setRenderSelect(false);
        scene.createScene(Minecraft.getInstance().level);
        scene.setAfterWorldRender(this::renderAfterWorld);
        scene.useCacheBuffer();
        reloadScene();
        prepareButtonGroup();
        buttonGroup.setSize(new Size(Math.max(0, buttonGroup.widgets.size() * 25 - 5), 20));
        buttonGroup.setSelfPosition(new Position(this.getSize().width - buttonGroup.getSize().width - 25, 25));
    }

    /**
     * reload the scene rendering.
     */
    public void reloadScene() {
        var pos = Minecraft.getInstance().player.getOnPos();
        var level = Minecraft.getInstance().level;
        var blocks = new HashSet<BlockPos>();
        var radius = runtime.sceneRadius;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    var blockPos = pos.offset(x, y, z).immutable();
                    if (level.isLoaded(blockPos)) {
                        blocks.add(pos.offset(x, y, z).immutable());
                    }
                }
            }
        }
        scene.setRenderedCore(blocks);
    }

    public void generateShapeInfo() {
        var minX = Math.min(runtime.area.from.getX(), runtime.area.to.getX());
        var minY = Math.min(runtime.area.from.getY(), runtime.area.to.getY());
        var minZ = Math.min(runtime.area.from.getZ(), runtime.area.to.getZ());
        var maxX = Math.max(runtime.area.from.getX(), runtime.area.to.getX());
        var maxY = Math.max(runtime.area.from.getY(), runtime.area.to.getY());
        var maxZ = Math.max(runtime.area.from.getZ(), runtime.area.to.getZ());
        var controllerPos = new BlockPos(
                runtime.controllerOffset.x + minX,
                runtime.controllerOffset.y + minY,
                runtime.controllerOffset.z + minZ);
        var controllerFace = runtime.controllerFace;
        var blockInfos = new BlockInfo[maxX - minX + 1][maxY - minY + 1][maxZ - minZ + 1];
        var level = Minecraft.getInstance().level;
        assert level != null;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    var pos = new BlockPos(x, y, z);
                    var blockInfo = BlockInfo.fromBlockState(level.getBlockState(pos));
                    if (pos.equals(controllerPos)) {
                        blockInfo = new ControllerBlockInfo(controllerFace);
                    }
                    blockInfos[x - minX][y - minY][z - minZ] = blockInfo;
                }
            }
        }
        project.getMultiblockShapeInfos().add(new MultiblockShapeInfo(blockInfos));
    }

    /**
     * generate the pattern based on selected area.
     */
    public void generatePattern() {
        var minX = Math.min(runtime.area.from.getX(), runtime.area.to.getX());
        var minY = Math.min(runtime.area.from.getY(), runtime.area.to.getY());
        var minZ = Math.min(runtime.area.from.getZ(), runtime.area.to.getZ());
        var maxX = Math.max(runtime.area.from.getX(), runtime.area.to.getX());
        var maxY = Math.max(runtime.area.from.getY(), runtime.area.to.getY());
        var maxZ = Math.max(runtime.area.from.getZ(), runtime.area.to.getZ());
        var controllerPos = new BlockPos(
                runtime.controllerOffset.x + minX,
                runtime.controllerOffset.y + minY,
                runtime.controllerOffset.z + minZ);
        var controllerFace = runtime.controllerFace;

        var blockPlaceholders = new BlockPlaceholder[maxX - minX + 1][maxY - minY + 1][maxZ - minZ + 1];
        var addNewResource = false;
        var level = Minecraft.getInstance().level;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    var blockPos = new BlockPos(x, y, z);
                    var predicateResource = project.getPredicateResource();
                    BlockPlaceholder holder;
                    if (blockPos.equals(controllerPos)) {
                        holder = BlockPlaceholder.controller(predicateResource).setFacing(controllerFace);
                    } else {
                        var block = level.getBlockState(blockPos).getBlock();
                        String id;
                        if (block instanceof LiquidBlock liquidBlock) {
                            var fluid = liquidBlock.getFluid().getSource();
                            id = Optional.ofNullable(ForgeRegistries.FLUIDS.getKey(fluid)).map(ResourceLocation::toString).orElse("any");
                            if (!predicateResource.hasBuiltinResource(id)) {
                                predicateResource.addBuiltinResource(id, new PredicateFluids(fluid));
                                addNewResource = true;
                            }
                        } else {
                            id = block == Blocks.AIR ? "any" : Optional.ofNullable(ForgeRegistries.BLOCKS.getKey(block)).map(ResourceLocation::toString).orElse("any");
                            if (!predicateResource.hasBuiltinResource(id)) {
                                predicateResource.addBuiltinResource(id, new PredicateBlocks(block));
                                addNewResource = true;
                            }
                        }

                        holder = BlockPlaceholder.create(predicateResource, Either.left(id));
                    }
                    blockPlaceholders[x - minX][y - minY][z - minZ] = holder;
                }
            }
        }
        if (addNewResource) {
            Editor.INSTANCE.getResourcePanel().rebuildResource(project.getPredicateResource().name());
        }
        project.setBlockPlaceholders(blockPlaceholders);
        patternPanel.onBlockPlaceholdersChanged();
    }

    /**
     * prepare the button group, you can add buttons / switches here.
     */
    protected void prepareButtonGroup() {
    }

    /**
     * Called when the panel is selected/switched to.
     */
    public void onPanelSelected() {
        Editor.INSTANCE.getConfigPanel().openConfigurator(MachineEditor.BASIC, runtime);
    }

    /**
     * Called when the panel is deselected/switched from.
     */
    public void onPanelDeselected() {
        Editor.INSTANCE.getConfigPanel().clearAllConfigurators();
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isMouseOverElement(mouseX, mouseY)) {
            // select area by click
            var hoverPosFace = scene.getHoverPosFace();
            var clickPosFace = scene.getClickPosFace();
            if (isShiftDown() && hoverPosFace != null && hoverPosFace.equals(clickPosFace)) {
                var pos = hoverPosFace.pos;
                if (runtime.isFromClicked) {
                    runtime.setFrom(pos);
                } else {
                    runtime.setTo(pos);
                }
                runtime.isFromClicked = !runtime.isFromClicked;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * render the scene after the world is rendered.
     * <br/> e.g. <br/>
     * shape frame lines.
     */
    private void renderAfterWorld(SceneWidget sceneWidget) {
        PoseStack poseStack = new PoseStack();

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        var tessellator = Tesselator.getInstance();
        var buffer = tessellator.getBuilder();
        RenderSystem.disableCull();

        var minX = Math.min(runtime.area.from.getX(), runtime.area.to.getX());
        var minY = Math.min(runtime.area.from.getY(), runtime.area.to.getY());
        var minZ = Math.min(runtime.area.from.getZ(), runtime.area.to.getZ());
        var maxX = Math.max(runtime.area.from.getX(), runtime.area.to.getX()) + 1;
        var maxY = Math.max(runtime.area.from.getY(), runtime.area.to.getY()) + 1;
        var maxZ = Math.max(runtime.area.from.getZ(), runtime.area.to.getZ()) + 1;

        // draw corner blocks
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        var fromColor = 0x8f00ff00;
        var toColor = 0x8fff0000;
        RenderBufferUtils.drawCubeFace(poseStack, buffer,
                minX, minY, minZ,
                minX + 1, minY + 1, minZ + 1,
                ColorUtils.red(fromColor), ColorUtils.green(fromColor), ColorUtils.blue(fromColor), 0.2f,
                false);
        RenderBufferUtils.drawCubeFace(poseStack, buffer,
                maxX - 1, maxY - 1, maxZ - 1,
                maxX, maxY, maxZ,
                ColorUtils.red(toColor), ColorUtils.green(toColor), ColorUtils.blue(toColor), 0.2f,
                false);
        // draw controller front face
        var color = 0x2f0000ff;
        var r = ColorUtils.red(color);
        var g = ColorUtils.green(color);
        var b = ColorUtils.blue(color);
        var a = ColorUtils.alpha(color);
        var controllerPos = new BlockPos(
                runtime.controllerOffset.x + minX,
                runtime.controllerOffset.y + minY,
                runtime.controllerOffset.z + minZ);
        switch (runtime.controllerFace) {
            case UP -> RenderBufferUtils.drawCubeFace(poseStack, buffer,
                    controllerPos.getX(), controllerPos.getY() + 1, controllerPos.getZ(),
                    controllerPos.getX() + 1, controllerPos.getY() + 1, controllerPos.getZ() + 1, r, g, b, a, false);
            case DOWN -> RenderBufferUtils.drawCubeFace(poseStack, buffer,
                    controllerPos.getX(), controllerPos.getY(), controllerPos.getZ(),
                    controllerPos.getX() + 1, controllerPos.getY(), controllerPos.getZ() + 1
                    , r, g, b, a, false);
            case NORTH -> RenderBufferUtils.drawCubeFace(poseStack, buffer,
                    controllerPos.getX(), controllerPos.getY(), controllerPos.getZ(),
                    controllerPos.getX() + 1, controllerPos.getY() + 1, controllerPos.getZ()
                    , r, g, b, a, false);
            case SOUTH -> RenderBufferUtils.drawCubeFace(poseStack, buffer,
                    controllerPos.getX(), controllerPos.getY(), controllerPos.getZ() + 1,
                    controllerPos.getX() + 1, controllerPos.getY() + 1, controllerPos.getZ() + 1
                    , r, g, b, a, false);
            case WEST -> RenderBufferUtils.drawCubeFace(poseStack, buffer,
                    controllerPos.getX(), controllerPos.getY(), controllerPos.getZ(),
                    controllerPos.getX(), controllerPos.getY() + 1, controllerPos.getZ() + 1
                    , r, g, b, a, false);
            case EAST -> RenderBufferUtils.drawCubeFace(poseStack, buffer,
                    controllerPos.getX() + 1, controllerPos.getY(), controllerPos.getZ(),
                    controllerPos.getX() + 1, controllerPos.getY() + 1, controllerPos.getZ() + 1
                    , r, g, b, a, false);
        }
        tessellator.end();

        // draw the size box
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
        buffer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        RenderSystem.lineWidth(10);

        color = 0xffffffff;
        RenderBufferUtils.drawCubeFrame(poseStack, buffer,
                minX, minY, minZ,
                maxX, maxY, maxZ,
                ColorUtils.red(color), ColorUtils.green(color), ColorUtils.blue(color), ColorUtils.alpha(color));

        // draw controller box
        color = 0xff00aaaa;
        RenderBufferUtils.drawCubeFrame(poseStack, buffer,
                controllerPos.getX(), controllerPos.getY(), controllerPos.getZ(),
                controllerPos.getX() + 1, controllerPos.getY() + 1, controllerPos.getZ() + 1,
                ColorUtils.red(color), ColorUtils.green(color), ColorUtils.blue(color), ColorUtils.alpha(color));

        tessellator.end();

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }
}
