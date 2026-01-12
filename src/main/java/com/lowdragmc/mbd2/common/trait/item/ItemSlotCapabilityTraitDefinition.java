package com.lowdragmc.mbd2.common.trait.item;

import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.client.utils.RenderBufferUtils;
import com.lowdragmc.lowdraglib2.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.editor.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib2.gui.editor.configurator.IToggleConfigurable;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib2.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib2.jei.IngredientIO;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import com.lowdragmc.lowdraglib2.utils.Position;
import com.lowdragmc.lowdraglib2.utils.ShapeUtils;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.gui.editor.machine.MachineTraitPanel;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.AutoIO;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTraitDefinition;
import com.lowdragmc.mbd2.common.trait.ToggleAutoIO;
import com.lowdragmc.mbd2.utils.WidgetUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;

@LDLRegister(name = "item_slot", group = "trait", priority = -100)
@Getter @Setter
public class ItemSlotCapabilityTraitDefinition extends SimpleCapabilityTraitDefinition {

    @Configurable(name = "config.definition.trait.item_slot.slot_size", tips = "config.definition.trait.item_slot.slot_size.tooltip")
    @NumberRange(range = {1, Integer.MAX_VALUE})
    private int slotSize = 1;
    @Configurable(name = "config.definition.trait.item_slot.slot_limit", tips = "config.definition.trait.item_slot.slot_limit.tooltip")
    @NumberRange(range = {1, 64})
    private int slotLimit = 64;
    @Configurable(name = "config.definition.trait.item_slot.filter", subConfigurable = true, tips = "config.definition.trait.item_slot.filter.tooltip")
    private final ItemFilterSettings itemFilterSettings = new ItemFilterSettings();
    @Configurable(name = "config.definition.trait.auto_io", subConfigurable = true, tips = "config.definition.trait.item_slot.auto_io.tooltip")
    private final ToggleAutoIO autoIO = new ToggleAutoIO();
    @Configurable(name = "config.definition.trait.auto_world_io.input", subConfigurable = true, tips = "config.definition.trait.auto_world_io.input.tooltip")
    private final AutoWorldIO autoInput = new AutoWorldIO();
    @Configurable(name = "config.definition.trait.auto_world_io.output", subConfigurable = true, tips = "config.definition.trait.auto_world_io.output.tooltip")
    private final AutoWorldIO autoOutput = new AutoWorldIO();
    @Configurable(name = "config.definition.trait.item_slot.fancy_renderer", subConfigurable = true, tips = "config.definition.trait.item_slot.fancy_renderer.tooltip")
    private final ItemFancyRendererSettings itemRendererSettings = new ItemFancyRendererSettings(this);

    @Override
    public ItemSlotCapabilityTrait createTrait(MBDMachine machine) {
        return new ItemSlotCapabilityTrait(machine, this);
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(Items.CHEST);
    }

    @Override
    public IRenderer getBESRenderer(IMachine machine) {
        return itemRendererSettings.getFancyRenderer(machine);
    }

    @Override
    public void createTraitUITemplate(WidgetGroup ui) {
        var row = Math.ceil(Math.sqrt(slotSize));
        var prefix = uiPrefixName();
        for (var i = 0; i < this.slotSize; i++) {
            var slotWidget = new SlotWidget();
            slotWidget.setSelfPosition(new Position(10 + i % (int) row * 18, 10 + i / (int) row * 18));
            slotWidget.initTemplate();
            slotWidget.setId(prefix + "_" + i);
            ui.addWidget(slotWidget);
        }
    }

    @Override
    public void initTraitUI(ITrait trait, WidgetGroup group) {
        if (trait instanceof ItemSlotCapabilityTrait itemSlotTrait) {
            var prefix = uiPrefixName();
            var guiIO = getGuiIO();
            var ingredientIO = guiIO == IO.IN ? IngredientIO.INPUT : guiIO == IO.OUT ? IngredientIO.OUTPUT : guiIO == IO.BOTH ? IngredientIO.BOTH : IngredientIO.RENDER_ONLY;
            WidgetUtils.widgetByIdForEach(group, "^%s_[0-9]+$".formatted(prefix), SlotWidget.class, slotWidget -> {
                var index = WidgetUtils.widgetIdIndex(slotWidget);
                if (index >= 0 && index < itemSlotTrait.storage.getSlots()) {
                    slotWidget.setHandlerSlot(itemSlotTrait.storage, index);
                    slotWidget.setIngredientIO(ingredientIO);
                    slotWidget.setCanTakeItems(guiIO.support(IO.OUT));
                    slotWidget.setCanPutItems(guiIO.support(IO.IN));
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
