package com.lowdragmc.mbd2.api.recipe.content;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class Content {
    @Getter
    public Object content;
    @Configurable(name = "editor.machine.recipe_type.content.per_tick", tips = "editor.machine.recipe_type.content.per_tick.tooltip")
    public boolean perTick;
    @Configurable(name = "editor.machine.recipe_type.content.chance", tips = "editor.machine.recipe_type.content.chance.tooltip")
    @NumberRange(range = {0f, 1f})
    public float chance;
    @Configurable(name = "editor.machine.recipe_type.content.tier_chance_boost", tips = "editor.machine.recipe_type.content.tier_chance_boost.tooltip")
    @NumberRange(range = {0f, 1f})
    public float tierChanceBoost;
    @Configurable(name = "editor.machine.recipe_type.content.slot_name", tips = "editor.machine.recipe_type.content.slot_name.tooltip")
    @Nonnull
    public String slotName;
    @Configurable(name = "editor.machine.recipe_type.content.ui_name", tips = "editor.machine.recipe_type.content.ui_name.tooltip")
    @Nonnull
    public String uiName;

    public Content(Object content, boolean perTick, float chance, float tierChanceBoost, @Nullable String slotName, @Nullable String uiName) {
        this.content = content;
        this.perTick = perTick;
        this.chance = chance;
        this.tierChanceBoost = tierChanceBoost;
        this.slotName = slotName == null ? "" : slotName;
        this.uiName = uiName == null ? "" : uiName;
    }

    public Content(Object content, boolean perTick, float chance, float tierChanceBoost) {
        this(content, perTick, chance, tierChanceBoost, "", "");
    }

    public Content copy(RecipeCapability<?> capability, @Nullable ContentModifier modifier) {
        if (modifier == null || chance == 0) {
            return new Content(capability.copyContent(content), perTick, chance, tierChanceBoost, slotName, uiName);
        } else {
            return new Content(capability.copyContent(content, modifier), perTick, chance, tierChanceBoost, slotName, uiName);
        }
    }

    public IGuiTexture createOverlay() {
        return new IGuiTexture() {
            @Override
            @OnlyIn(Dist.CLIENT)
            public void draw(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, int width, int height) {
                drawChance(graphics, x, y, width, height);
                if (perTick) {
                    drawTick(graphics, x, y, width, height);
                }
            }
        };
    }

    @OnlyIn(Dist.CLIENT)
    public void drawChance(GuiGraphics graphics, float x, float y, int width, int height) {
        if (chance == 1) return;
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);
        graphics.pose().scale(0.5f, 0.5f, 1);
        String s = chance == 0 ? LocalizationUtils.format("mbd2.gui.content.chance_0_short") : String.format("%.1f", chance * 100) + "%";
        int color = chance == 0 ? 0xff0000 : 0xFFFF00;
        Font fontRenderer = Minecraft.getInstance().font;
        graphics.drawString(fontRenderer, s, (int) ((x + (width / 3f)) * 2 - fontRenderer.width(s) + 23), (int) ((y + (height / 3f) + 6) * 2 - height), color, true);
        graphics.pose().popPose();
    }

    @OnlyIn(Dist.CLIENT)
    public void drawTick(GuiGraphics graphics, float x, float y, int width, int height) {
        graphics.pose().pushPose();
        RenderSystem.disableDepthTest();
        graphics.pose().translate(0, 0, 400);
        graphics.pose().scale(0.5f, 0.5f, 1);
        String s = LocalizationUtils.format("mbd2.gui.content.tips.per_tick_short");
        int color = 0xFFFF00;
        Font fontRenderer = Minecraft.getInstance().font;
        graphics.drawString(fontRenderer, s, (int) ((x + (width / 3f)) * 2 - fontRenderer.width(s) + 23), (int) ((y + (height / 3f) + 6) * 2 - height + (chance == 1 ? 0 : 10)), color);
        graphics.pose().popPose();
    }

    public void appendTooltip(List<Component> tooltips) {
        if (chance != 1) {
            if (chance == 0) {
                tooltips.add(Component.translatable("mbd2.gui.content.chance_0"));
            } else {
                tooltips.add(Component.translatable("mbd2.gui.content.chance_1", (int)(chance * 100) + "%"));
            }
        }
        if (tierChanceBoost != 0) {
            tooltips.add(Component.translatable("mbd2.gui.content.tier_boost", (int)(tierChanceBoost * 100) + "%"));
        }
        if (perTick) {
            tooltips.add(Component.translatable("mbd2.gui.content.per_tick"));
        }
    }
}
