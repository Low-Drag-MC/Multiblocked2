package com.lowdragmc.mbd2.api.recipe;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.registry.ILDLRegister;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/05/27
 * @implNote RecipeCondition, global conditions
 */
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public abstract class RecipeCondition implements IConfigurable, ILDLRegister<RecipeCondition, Supplier<RecipeCondition>> {
    public final static Codec<RecipeCondition> CODEC = createCodec();
    static Codec<RecipeCondition> createCodec() {
        return MBDRegistries.RECIPE_CONDITIONS.optionalCodec().dispatch(ILDLRegister::getRegistryHolderOptional,
                optional -> optional.map(holder ->
                                MapCodec.assumeMapUnsafe(PersistedParser.createCodec(holder.value())))
                        .orElseThrow());
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, RecipeCondition> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    @Setter
    @Getter
    @Configurable(name = "config.recipe.condition.reverse", tips = "config.recipe.condition.reverse.tooltip")
    protected boolean isReverse;

    public String getType() {
        return name();
    }

    public String getTranslateKey() {
        return "recipe.condition." + getType();
    }

    // todo remove is or
    public boolean isOr() {
        return true;
    }

    public abstract Component getTooltips();

    public abstract boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic);

    public IGuiTexture getIcon() {
        return SpriteTexture.of("mbd2:textures/gui/condition/" + getType() + ".png");
    }

    public RecipeCondition copy() {
        var ops = Platform.getFrozenRegistry().createSerializationContext(NbtOps.INSTANCE);
        return CODEC.parse(ops, CODEC.encodeStart(ops, this).getOrThrow()).getOrThrow();
    }
}
