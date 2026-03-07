package com.lowdragmc.mbd2.api.recipe.content;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiRenderable;
import com.lowdragmc.lowdraglib2.utils.LocalizationUtils;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

public class Content {
    public static final Function<RecipeCapability<?>, Codec<Content>> CODEC = Util.memoize(Content::codec);
    public static <T> Codec<Content> codec(RecipeCapability<T> capability) {
        return RecordCodecBuilder.create(instance -> instance.group(
                        capability.serializer.codec().fieldOf("content").forGetter(val -> capability.of(val.content)),
                        Codec.BOOL.optionalFieldOf("perTick", false).forGetter(val -> val.perTick),
                        Codec.FLOAT.validate(value -> DataResult.success(Mth.clamp(value, 0, 1))).optionalFieldOf("chance", 1f).forGetter(val -> val.chance),
                        Codec.FLOAT.validate(value -> DataResult.success(Mth.clamp(value, 0, 1))).optionalFieldOf("tierChanceBoost", 0f).forGetter(val -> val.tierChanceBoost),
                        Codec.STRING.optionalFieldOf("slotName", "").forGetter(val -> val.slotName),
                        Codec.STRING.optionalFieldOf("uiName", "").forGetter(val -> val.uiName)
                ).apply(instance, Content::new));
    }

    @Getter
    public Object content;
    @Configurable(name = "editor.machine.recipe_type.content.per_tick", tips = "editor.machine.recipe_type.content.per_tick.tooltip")
    public boolean perTick;
    @Configurable(name = "editor.machine.recipe_type.content.chance", tips = "editor.machine.recipe_type.content.chance.tooltip")
    @ConfigNumber(range = {0f, 1f})
    public float chance;
    @Configurable(name = "editor.machine.recipe_type.content.tier_chance_boost", tips = {
            "editor.machine.recipe_type.content.tier_chance_boost.tooltip.0",
            "editor.machine.recipe_type.content.tier_chance_boost.tooltip.1"
            })
    @ConfigNumber(range = {0f, 1f})
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

    public Content deepCopy(RecipeCapability<?> capability, @Nullable ContentModifier modifier) {
        if (modifier == null || chance == 0) {
            return new Content(capability.deepCopyContent(content), perTick, chance, tierChanceBoost, slotName, uiName);
        } else {
            return new Content(capability.deepCopyContent(content, modifier), perTick, chance, tierChanceBoost, slotName, uiName);
        }
    }

    public IGuiRenderable createOverlay() {
        return (context, x, y, width, height) -> {
            // todo improve
            var row = 0;
            if (chance < 1) {
                String s = chance == 0 ? LocalizationUtils.format("mbd2.gui.content.chance_0_short") : String.format("%.1f", chance * 100) + "%";
                int color = chance == 0 ? 0xff0000 : 0xFFFF00;
                drawSmallString(context.graphics, x, y, width, height, row++, s, color);
            }
            if (perTick) {
                drawSmallString(context.graphics, x, y, width, height, row++,
                        LocalizationUtils.format("mbd2.gui.content.tips.per_tick_short"), 0xFFFF00);
            }
        };
    }

    @OnlyIn(Dist.CLIENT)
    public void drawSmallString(GuiGraphics graphics, float x, float y, float width, float height, int row, String text, int color) {
        var font = Minecraft.getInstance().font;
        var textWidth = font.width(text);
        var posX = x + (width - textWidth);
        var posY = y + row * font.lineHeight / 2f;
        graphics.pose().pushPose();
        graphics.pose().translate(posX + textWidth, posY + font.lineHeight / 2f, 0);
        graphics.pose().scale(0.5f, 0.5f, 1);
        graphics.pose().translate(- posX - textWidth, - posY - font.lineHeight / 2f, 0);

        graphics.drawString(font, text, (int) posX, (int) posY, color);

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

    public static StreamCodec<RegistryFriendlyByteBuf, Content> streamCodec(RecipeCapability<?> capability) {
        return StreamCodec.of((buf, content) -> content.toNetworkContent(capability, buf),
                buf -> fromNetworkContent(capability, buf));
    }

    private void toNetworkContent(RecipeCapability<?> capability, RegistryFriendlyByteBuf buf) {
        ((RecipeCapability) capability).serializer.streamCodec().encode(buf, content);
        buf.writeBoolean(perTick);
        buf.writeFloat(chance);
        buf.writeFloat(tierChanceBoost);
        buf.writeBoolean(!slotName.isEmpty());
        if (!slotName.isEmpty()) {
            buf.writeUtf(slotName);
        }
        buf.writeBoolean(!uiName.isEmpty());
        if (!uiName.isEmpty()) {
            buf.writeUtf(uiName);
        }
    }

    private static Content fromNetworkContent(RecipeCapability<?> capability, RegistryFriendlyByteBuf buf) {
        var content = capability.serializer.streamCodec().decode(buf);
        var perTick = buf.readBoolean();
        float chance = buf.readFloat();
        float tierChanceBoost = buf.readFloat();
        String slotName = null;
        if (buf.readBoolean()) {
            slotName = buf.readUtf();
        }
        String uiName = null;
        if (buf.readBoolean()) {
            uiName = buf.readUtf();
        }
        return new Content(content, perTick, chance, tierChanceBoost, slotName, uiName);
    }
}
