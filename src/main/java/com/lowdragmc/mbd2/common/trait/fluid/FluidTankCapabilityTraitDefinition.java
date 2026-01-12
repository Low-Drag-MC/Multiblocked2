package com.lowdragmc.mbd2.common.trait.fluid;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.client.utils.RenderBufferUtils;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.TankWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.utils.ColorUtils;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.gui.editor.machine.MachineTraitPanel;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.*;
import com.lowdragmc.mbd2.utils.WidgetUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;

@LDLRegister(name = "fluid_tank", group = "trait", priority = -100)
public class FluidTankCapabilityTraitDefinition extends SimpleCapabilityTraitDefinition {

    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.fluid_tank.tank_size", tips = "config.definition.trait.fluid_tank.tank_size.tooltip")
    @NumberRange(range = {1, Integer.MAX_VALUE})
    private int tankSize = 1;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.fluid_tank.capacity")
    @NumberRange(range = {1, Integer.MAX_VALUE})
    private int capacity = 1000;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.fluid_tank.allow_same_fluids", tips = "config.definition.trait.fluid_tank.allow_same_fluids.tooltip")
    private boolean allowSameFluids = true;
    @Getter
    @Configurable(name = "config.definition.trait.fluid_tank.filter", subConfigurable = true, tips = "config.definition.trait.fluid_tank.filter.tooltip")
    private final FluidFilterSettings fluidFilterSettings = new FluidFilterSettings();
    @Getter
    @Configurable(name = "config.definition.trait.auto_io", subConfigurable = true, tips = "config.definition.trait.fluid_tank.auto_io.tooltip")
    private final ToggleAutoIO autoIO = new ToggleAutoIO();
    @Getter
    @Configurable(name = "config.definition.trait.auto_world_io.input", subConfigurable = true, tips = "config.definition.trait.auto_world_io.input.tooltip")
    private final AutoWorldIO autoInput = new AutoWorldIO().setSpeed(1);
    @Getter
    @Configurable(name = "config.definition.trait.auto_world_io.output", subConfigurable = true, tips = "config.definition.trait.auto_world_io.output.tooltip")
    private final AutoWorldIO autoOutput = new AutoWorldIO().setSpeed(1);
    @Configurable(name = "config.definition.trait.fluid_tank.fancy_renderer", subConfigurable = true,
            tips = {"config.definition.trait.fluid_tank.fancy_renderer.tooltip.0", "config.definition.trait.fluid_tank.fancy_renderer.tooltip.1"})
    private final FluidFancyRendererSettings fancyRendererSettings = new FluidFancyRendererSettings(this);

    @Override
    public SimpleCapabilityTrait createTrait(MBDMachine machine) {
        return new FluidTankCapabilityTrait(machine, this);
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(Items.WATER_BUCKET);
    }

    @Override
    public IRenderer getBESRenderer(IMachine machine) {
        return fancyRendererSettings.getFancyRenderer(machine);
    }

    @Override
    public void createTraitUITemplate(WidgetGroup ui) {
        var prefix = uiPrefixName();
        for (var i = 0; i < this.tankSize; i++) {
            var tankWidget = new TankWidget();
            tankWidget.initTemplate();
            tankWidget.setSelfPosition(new Position(10 + i * 20, 10));
            tankWidget.setSize(new Size(20, 58));
            tankWidget.setOverlay(new ResourceTexture("mbd2:textures/gui/fluid_tank_overlay.png"));
            tankWidget.setId(prefix + "_" + i);
            tankWidget.setShowAmount(false);
            ui.addWidget(tankWidget);
        }
    }

    @Override
    public void initTraitUI(ITrait trait, WidgetGroup group) {
        if (trait instanceof FluidTankCapabilityTrait fluidTankTrait) {
            var prefix = uiPrefixName();
            var guiIO = getGuiIO();
            var ingredientIO = guiIO == IO.IN ? IngredientIO.INPUT : guiIO == IO.OUT ? IngredientIO.OUTPUT : guiIO == IO.BOTH ? IngredientIO.BOTH : IngredientIO.RENDER_ONLY;
            WidgetUtils.widgetByIdForEach(group, "^%s_[0-9]+$".formatted(prefix), TankWidget.class, tankWidget -> {
                var index = WidgetUtils.widgetIdIndex(tankWidget);
                if (index >= 0 && index < fluidTankTrait.storages.length) {
                    tankWidget.setFluidTank(fluidTankTrait.storages[index]);
                    tankWidget.setIngredientIO(ingredientIO);
                    tankWidget.setAllowClickDrained(guiIO.support(IO.IN));
                    tankWidget.setAllowClickFilled(guiIO.support(IO.OUT));
                }
            });
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderAfterWorldInTraitPanel(MachineTraitPanel panel) {
        super.renderAfterWorldInTraitPanel(panel);
        if (!autoInput.enable && !autoOutput.enable) return;
        var poseStack = new PoseStack();
        var tessellator = Tesselator.getInstance();
        var buffer = tessellator.getBuilder();

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        poseStack.pushPose();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
        buffer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        RenderSystem.lineWidth(5);

        if (autoOutput.enable) {
            var color = 0xffee6500;
            RenderBufferUtils.drawCubeFrame(poseStack, buffer,
                    (float)autoOutput.range.minX, (float)autoOutput.range.minY, (float)autoOutput.range.minZ,
                    (float)autoOutput.range.maxX, (float)autoOutput.range.maxY, (float)autoOutput.range.maxZ,
                    ColorUtils.red(color), ColorUtils.green(color), ColorUtils.blue(color), ColorUtils.alpha(color));
        }

        if (autoInput.enable) {
            var color = 0xff11aaee;
            RenderBufferUtils.drawCubeFrame(poseStack, buffer,
                    (float) autoInput.range.minX, (float) autoInput.range.minY, (float) autoInput.range.minZ,
                    (float) autoInput.range.maxX, (float) autoInput.range.maxY, (float) autoInput.range.maxZ,
                    ColorUtils.red(color), ColorUtils.green(color), ColorUtils.blue(color), ColorUtils.alpha(color));
        }
        tessellator.end();

        poseStack.popPose();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }
}
