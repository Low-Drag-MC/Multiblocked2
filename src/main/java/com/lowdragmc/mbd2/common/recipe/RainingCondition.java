package com.lowdragmc.mbd2.common.recipe;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

/**
 * @author KilaBash
 * @date 2022/05/27
 * @implNote WhetherCondition, specific whether
 */
@Getter
@NoArgsConstructor
public class RainingCondition extends RecipeCondition {

    public final static RainingCondition INSTANCE = new RainingCondition();
    @Configurable(name = "config.recipe.condition.weather.min")
    @NumberRange(range = {0f, 1f})
    private float minLevel;
    @Configurable(name = "config.recipe.condition.weather.max")
    @NumberRange(range = {0f, 1f})
    private float maxLevel;

    public RainingCondition(float minLevel, float maxLevel) {
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    @Override
    public String getType() {
        return "rain";
    }

    @Override
    public Component getTooltips() {
        return Component.translatable("recipe.condition.rain.tooltip", minLevel, maxLevel);
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        Level level = recipeLogic.machine.getLevel();
        return level != null && level.getRainLevel(1) >= this.minLevel && level.getRainLevel(1) <= this.maxLevel;
    }

    @Nonnull
    @Override
    public JsonObject serialize() {
        JsonObject config = super.serialize();
        config.addProperty("minLevel", minLevel);
        config.addProperty("maxLevel", maxLevel);
        return config;
    }

    @Override
    public RecipeCondition deserialize(@Nonnull JsonObject config) {
        super.deserialize(config);
        minLevel = GsonHelper.getAsFloat(config, "minLevel", 0);
        maxLevel = GsonHelper.getAsFloat(config, "maxLevel", 1);
        return this;
    }

    @Override
    public RecipeCondition fromNetwork(FriendlyByteBuf buf) {
        super.fromNetwork(buf);
        minLevel = buf.readFloat();
        maxLevel = buf.readFloat();
        return this;
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        super.toNetwork(buf);
        buf.writeFloat(minLevel);
        buf.writeFloat(maxLevel);
    }

    @Override
    public CompoundTag toNBT() {
        var tag = super.toNBT();
        tag.putFloat("minLevel", minLevel);
        tag.putFloat("maxLevel", maxLevel);
        return tag;
    }

    @Override
    public RecipeCondition fromNBT(CompoundTag tag) {
        super.fromNBT(tag);
        minLevel = tag.getFloat("minLevel");
        maxLevel = tag.getFloat("maxLevel");
        return this;
    }

}
