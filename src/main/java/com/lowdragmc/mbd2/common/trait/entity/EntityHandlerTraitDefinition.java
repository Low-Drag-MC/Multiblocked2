package com.lowdragmc.mbd2.common.trait.entity;

import com.lowdragmc.lowdraglib.client.utils.RenderBufferUtils;
import com.lowdragmc.lowdraglib.gui.editor.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.DefaultValue;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.utils.ColorUtils;
import com.lowdragmc.lowdraglib.utils.ShapeUtils;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.ingredient.EntityIngredient;
import com.lowdragmc.mbd2.common.capability.recipe.EntityRecipeCapability;
import com.lowdragmc.mbd2.common.gui.editor.machine.MachineTraitPanel;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.RecipeCapabilityTraitDefinition;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.Getter;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

@LDLRegister(name = "entity_handler", group = "trait", priority = -99)
public class EntityHandlerTraitDefinition extends RecipeCapabilityTraitDefinition<EntityIngredient> {

    @Getter
    @Configurable(name = "config.definition.trait.entity_handler.area", tips = {
            "config.definition.trait.entity_handler.area.tooltip.0",
            "config.definition.trait.entity_handler.area.tooltip.1"
    })
    @DefaultValue(numberValue = {-1, -1, -1, 2, 2, 2})
    private AABB area = new AABB(-1, -1, -1, 2, 2, 2);

    // runtime
    private final Map<Direction, AABB> areaCache = new EnumMap<>(Direction.class);

    @Override
    public ITrait createTrait(MBDMachine machine) {
        return new EntityHandlerTrait(machine, this);
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(Items.PIG_SPAWN_EGG);
    }

    @Override
    public RecipeCapability<EntityIngredient> getRecipeCapability() {
        return EntityRecipeCapability.CAP;
    }

    @ConfigSetter(field = "area")
    public void setArea(AABB area) {
        this.area = area;
        areaCache.clear();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        super.deserializeNBT(tag);
        areaCache.clear();
    }

    public AABB getArea(@Nullable Direction direction) {
        return (direction == Direction.NORTH || direction == null) ? area : this.areaCache.computeIfAbsent(direction, dir -> ShapeUtils.rotate(area, dir));
    }

    @Override
    public void renderAfterWorldInTraitPanel(MachineTraitPanel panel) {
        super.renderAfterWorldInTraitPanel(panel);
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

        var color = 0xff11aaee;
        RenderBufferUtils.drawCubeFrame(poseStack, buffer,
                (float)area.minX, (float)area.minY, (float)area.minZ,
                (float)area.maxX, (float)area.maxY, (float)area.maxZ,
                ColorUtils.red(color), ColorUtils.green(color), ColorUtils.blue(color), ColorUtils.alpha(color));

        tessellator.end();

        poseStack.popPose();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }
}
