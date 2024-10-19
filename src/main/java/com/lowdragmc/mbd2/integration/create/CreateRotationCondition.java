package com.lowdragmc.mbd2.integration.create;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.integration.create.machine.CreateStressTrait;
import com.simibubi.create.AllBlocks;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;

import javax.annotation.Nonnull;

@Getter
@NoArgsConstructor
public class CreateRotationCondition extends RecipeCondition {

    public final static CreateRotationCondition INSTANCE = new CreateRotationCondition();
    @Configurable(name = "config.recipe.condition.rpm.min")
    @NumberRange(range = {0f, Float.MAX_VALUE})
    private float minRPM;
    @Configurable(name = "config.recipe.condition.rpm.max")
    @NumberRange(range = {0f, Float.MAX_VALUE})
    private float maxRPM;

    public CreateRotationCondition(float minRPM, float maxRPM) {
        this.minRPM = minRPM;
        this.maxRPM = maxRPM;
    }

    @Override
    public String getType() {
        return "create_rpm";
    }

    @Override
    public Component getTooltips() {
        return Component.translatable("recipe.condition.create_rpm.tooltip", minRPM, maxRPM);
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(AllBlocks.SHAFT.asStack());
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        var proxy = recipeLogic.machine.getRecipeCapabilitiesProxy();
        var inputs = proxy.get(IO.IN, CreateStressRecipeCapability.CAP);
        if (inputs != null) {
            for (var input : inputs) {
                if (input instanceof CreateStressTrait trait) {
                    if (trait.getLastSpeed() >= minRPM && trait.getLastSpeed() <= maxRPM) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Nonnull
    @Override
    public JsonObject serialize() {
        JsonObject config = super.serialize();
        config.addProperty("minRPM", minRPM);
        config.addProperty("maxRPM", maxRPM);
        return config;
    }

    @Override
    public RecipeCondition deserialize(@Nonnull JsonObject config) {
        super.deserialize(config);
        minRPM = GsonHelper.getAsFloat(config, "minRPM", 0);
        maxRPM = GsonHelper.getAsFloat(config, "maxRPM", 1);
        return this;
    }

    @Override
    public RecipeCondition fromNetwork(FriendlyByteBuf buf) {
        super.fromNetwork(buf);
        minRPM = buf.readFloat();
        maxRPM = buf.readFloat();
        return this;
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        super.toNetwork(buf);
        buf.writeFloat(minRPM);
        buf.writeFloat(maxRPM);
    }

    @Override
    public CompoundTag toNBT() {
        var tag = super.toNBT();
        tag.putFloat("minRPM", minRPM);
        tag.putFloat("maxRPM", maxRPM);
        return tag;
    }

    @Override
    public RecipeCondition fromNBT(CompoundTag tag) {
        super.fromNBT(tag);
        minRPM = tag.getFloat("minRPM");
        maxRPM = tag.getFloat("maxRPM");
        return this;
    }

}
