package com.lowdragmc.mbd2.common.recipe;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
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

@Getter
@NoArgsConstructor
public class MachineLevelCondition extends RecipeCondition {

    public final static MachineLevelCondition INSTANCE = new MachineLevelCondition();
    @Configurable(name = "config.recipe.condition.machine_level.level", tips="config.recipe.condition.machine_level.level.tips")
    @NumberRange(range = {0, Integer.MAX_VALUE})
    private int level;

    public MachineLevelCondition(int level) {
        this.level = level;
    }

    @Override
    public String getType() {
        return "machine_level";
    }

    @Override
    public Component getTooltips() {
        return Component.translatable("recipe.condition.machine_level.tooltip", this.level);
    }

    @Override
    public IGuiTexture getIcon() {
        return new TextTexture("LV");
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        return recipeLogic.machine.getMachineLevel() >= this.level;
    }

    @Nonnull
    @Override
    public JsonObject serialize() {
        JsonObject config = super.serialize();
        config.addProperty("level", this.level);
        return config;
    }

    @Override
    public RecipeCondition deserialize(@Nonnull JsonObject config) {
        super.deserialize(config);
        this.level = GsonHelper.getAsInt(config, "level");
        return this;
    }

    @Override
    public RecipeCondition fromNetwork(FriendlyByteBuf buf) {
        super.fromNetwork(buf);
        level = buf.readVarInt();
        return this;
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        super.toNetwork(buf);
        buf.writeVarInt(level);
    }

    @Override
    public CompoundTag toNBT() {
        var tag = super.toNBT();
        tag.putInt("level", level);
        return tag;
    }

    @Override
    public RecipeCondition fromNBT(CompoundTag tag) {
        super.fromNBT(tag);
        level = tag.getInt("level");
        return this;
    }

}
