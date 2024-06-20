package com.lowdragmc.mbd2.common.recipe;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.SelectorConfigurator;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

/**
 * @author KilaBash
 * @date 2022/05/27
 * @implNote DimensionCondition, specific dimension
 */
@Getter
@NoArgsConstructor
public class DimensionCondition extends RecipeCondition {

    public final static DimensionCondition INSTANCE = new DimensionCondition();
    private ResourceLocation dimension = new ResourceLocation("dummy");

    public DimensionCondition(ResourceLocation dimension) {
        this.dimension = dimension;
    }

    @Override
    public String getType() {
        return "dimension";
    }

    @Override
    public Component getTooltips() {
        return Component.translatable("recipe.condition.dimension.tooltip", dimension);
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        Level level = recipeLogic.machine.getLevel();
        return level != null && dimension.equals(level.dimension().location());
    }

    @Nonnull
    @Override
    public JsonObject serialize() {
        JsonObject config = super.serialize();
        config.addProperty("dim", dimension.toString());
        return config;
    }

    @Override
    public RecipeCondition deserialize(@Nonnull JsonObject config) {
        super.deserialize(config);
        dimension = new ResourceLocation(
                GsonHelper.getAsString(config, "dim", "dummy"));
        return this;
    }

    @Override
    public RecipeCondition fromNetwork(FriendlyByteBuf buf) {
        super.fromNetwork(buf);
        dimension = new ResourceLocation(buf.readUtf());
        return this;
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        super.toNetwork(buf);
        buf.writeUtf(dimension.toString());
    }

    @Override
    public CompoundTag toNBT() {
        var tag = super.toNBT();
        tag.putString("dim", dimension.toString());
        return tag;
    }

    @Override
    public RecipeCondition fromNBT(CompoundTag tag) {
        super.fromNBT(tag);
        dimension = new ResourceLocation(tag.getString("dim"));
        return this;
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        super.buildConfigurator(father);
        var selector = new SelectorConfigurator<>(getTranslationKey(),
                () -> this.dimension,
                d -> this.dimension = d,
                new ResourceLocation("dummy"),
                true,
                Minecraft.getInstance().level.registryAccess().registry(Registries.DIMENSION_TYPE).get().keySet().stream().toList(),
                ResourceLocation::toString
        );
        selector.setUp(false);
        selector.setTips("config.recipe.condition.dimension.tooltip");
        father.addConfigurators(selector);
    }
}
