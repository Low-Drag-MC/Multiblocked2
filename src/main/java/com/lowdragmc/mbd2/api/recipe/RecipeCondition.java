package com.lowdragmc.mbd2.api.recipe;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.mbd2.MBD2;
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
public abstract class RecipeCondition {

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

    protected boolean isReverse;

    public abstract String getType();

    public String getTranslationKey() {
        return "MBD2.recipe.condition." + getType();
    }

    public IGuiTexture getInValidTexture() {
        return new ResourceTexture("MBD2:textures/gui/condition/" + getType() + ".png").getSubTexture(0, 0, 1, 0.5f);
    }

    public IGuiTexture getValidTexture() {
        return new ResourceTexture("MBD2:textures/gui/condition/" + getType() + ".png").getSubTexture(0, 0.5f, 1, 0.5f);
    }

    public boolean isReverse() {
        return isReverse;
    }

    public boolean isOr() {
        return true;
    }

    public RecipeCondition setReverse(boolean reverse) {
        isReverse = reverse;
        return this;
    }

    public abstract Component getTooltips();

    public abstract boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic);

    public abstract RecipeCondition createTemplate();

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

}
