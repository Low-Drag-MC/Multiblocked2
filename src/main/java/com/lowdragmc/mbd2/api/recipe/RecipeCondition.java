package com.lowdragmc.mbd2.api.recipe;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.mbd2.MBD2;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author KilaBash
 * @date 2022/05/27
 * @implNote RecipeCondition, global conditions
 */
@Accessors(chain = true)
public abstract class RecipeCondition implements IConfigurable {

    @Nullable
    public static RecipeCondition create(Class<? extends RecipeCondition> clazz) {
        if (clazz == null) return null;
        try {
            return clazz.newInstance();
        } catch (Exception ignored) {
            MBD2.LOGGER.error("condition {} has no NonArgsConstructor", clazz);
            return null;
        }
    }

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
        return create(getClass()).deserialize(serialize());
    }

    @Nonnull
    public JsonObject serialize() {
        JsonObject jsonObject = new JsonObject();
        if (isReverse) {
            jsonObject.addProperty("reverse", true);
        }
        return jsonObject;
    }

    public RecipeCondition deserialize(@Nonnull JsonObject config) {
        isReverse = GsonHelper.getAsBoolean(config, "reverse", false);
        return this;
    }

    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeBoolean(isReverse);
    }

    public RecipeCondition fromNetwork(FriendlyByteBuf buf) {
        isReverse = buf.readBoolean();
        return this;
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("reverse", isReverse);
        return tag;
    }

    public RecipeCondition fromNBT(CompoundTag tag) {
        isReverse = tag.getBoolean("reverse");
        return this;
    }
}
