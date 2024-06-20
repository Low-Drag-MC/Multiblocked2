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

import javax.annotation.Nonnull;

/**
 * @author KilaBash
 * @date 2022/05/27
 * @implNote WhetherCondition, specific whether
 */
@Getter
@NoArgsConstructor
public class PositionYCondition extends RecipeCondition {

    public final static PositionYCondition INSTANCE = new PositionYCondition();
    @Configurable(name = "config.recipe.condition.pos_y.min")
    @NumberRange(range = {Integer.MIN_VALUE, Integer.MAX_VALUE})
    private int min;
    @Configurable(name = "config.recipe.condition.pos_y.max")
    @NumberRange(range = {Integer.MIN_VALUE, Integer.MAX_VALUE})
    private int max;

    public PositionYCondition(int min, int max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public String getType() {
        return "pos_y";
    }

    @Override
    public Component getTooltips() {
        return Component.translatable("recipe.condition.pos_y.tooltip", this.min, this.max);
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        int y = recipeLogic.machine.getPos().getY();
        return y >= this.min && y <= this.max;
    }

    @Nonnull
    @Override
    public JsonObject serialize() {
        JsonObject config = super.serialize();
        config.addProperty("min", this.min);
        config.addProperty("max", this.max);
        return config;
    }

    @Override
    public RecipeCondition deserialize(@Nonnull JsonObject config) {
        super.deserialize(config);
        min = GsonHelper.getAsInt(config, "min", Integer.MIN_VALUE);
        max = GsonHelper.getAsInt(config, "max", Integer.MAX_VALUE);
        return this;
    }

    @Override
    public RecipeCondition fromNetwork(FriendlyByteBuf buf) {
        super.fromNetwork(buf);
        min = buf.readVarInt();
        max = buf.readVarInt();
        return this;
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        super.toNetwork(buf);
        buf.writeVarInt(min);
        buf.writeVarInt(max);
    }

    @Override
    public CompoundTag toNBT() {
        var tag = super.toNBT();
        tag.putInt("min", min);
        tag.putInt("max", max);
        return tag;
    }

    @Override
    public RecipeCondition fromNBT(CompoundTag tag) {
        super.fromNBT(tag);
        min = tag.getInt("min");
        max = tag.getInt("max");
        return this;
    }

}
