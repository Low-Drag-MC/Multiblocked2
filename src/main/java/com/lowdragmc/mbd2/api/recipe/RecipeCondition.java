package com.lowdragmc.mbd2.api.recipe;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.MapCodec;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import javax.annotation.Nonnull;

/**
 * @author KilaBash
 * @date 2022/05/27
 * @implNote RecipeCondition, global conditions
 */
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public abstract class RecipeCondition implements IConfigurable {

    public static final Codec<RecipeCondition> CODEC = MBDRegistries.RECIPE_CONDITIONS.codec()
            .dispatch(c -> MBDRegistries.RECIPE_CONDITIONS.get(c.getType()), RecipeCondition::codec);

    public static final StreamCodec<RegistryFriendlyByteBuf, RecipeCondition> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    @Configurable(name = "config.recipe.condition.reverse", tips = "config.recipe.condition.reverse.tooltip")
    @Setter
    @Getter
    protected boolean isReverse;

    public abstract String getType();

    public String getTranslationKey() {
        return "recipe.condition." + getType();
    }

    public boolean isOr() {
        return true;
    }

    public abstract Component getTooltips();

    public abstract boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic);

    public IGuiTexture getIcon() {
        return new ResourceTexture("mbd2:textures/gui/condition/" + getType() + ".png");
    }

    public RecipeCondition copy() {
        Codec codec = codec().codec();
        return (RecipeCondition) codec.parse(JavaOps.INSTANCE, codec.encodeStart(JavaOps.INSTANCE, this).getOrThrow()).getOrThrow();
    }

    public abstract MapCodec<? extends RecipeCondition> codec();

}
