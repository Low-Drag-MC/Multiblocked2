package com.lowdragmc.mbd2.integration.mekanism;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mekanism.api.Coord4D;
import mekanism.api.radiation.IRadiationManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

@Getter
@NoArgsConstructor
public class MEKRadiationCondition extends RecipeCondition {

    public static final MEKRadiationCondition INSTANCE = new MEKRadiationCondition();
    @Configurable(name = "config.recipe.condition.radiation.min")
    @NumberRange(range = {-Float.MAX_VALUE, Float.MAX_VALUE})
    private double minRadiation;
    @Configurable(name = "config.recipe.condition.radiation.max")
    @NumberRange(range = {-Float.MAX_VALUE, Float.MAX_VALUE})
    private double maxRadiation;

    public MEKRadiationCondition(double minRadiation, double maxRadiation) {
        this.minRadiation = minRadiation;
        this.maxRadiation = maxRadiation;
    }

    @Override
    public String getType() {
        return "mekanism_radiation";
    }

    @Override
    public Component getTooltips() {
        return Component.translatable("recipe.condition.mekanism_radiation.tooltip", minRadiation, maxRadiation);
    }

    @Override
    public IGuiTexture getIcon() {
        return new ResourceTexture("mekanism:textures/item/geiger_counter_3.png");
    }

    @Override
    public boolean test(@NotNull MBDRecipe recipe, @NotNull RecipeLogic recipeLogic) {
        double radiation = IRadiationManager.INSTANCE.getRadiationLevel(new Coord4D(recipeLogic.machine.getHolder()));
        return (radiation >= minRadiation && radiation <= maxRadiation) != isReverse();
    }

    @Nonnull
    @Override
    public JsonObject serialize() {
        JsonObject config = super.serialize();
        config.addProperty("minRadiation", minRadiation);
        config.addProperty("maxRadiation", maxRadiation);
        return config;
    }

    @Override
    public RecipeCondition deserialize(@Nonnull JsonObject config) {
        super.deserialize(config);
        minRadiation = GsonHelper.getAsDouble(config, "minRadiation", 0);
        maxRadiation = GsonHelper.getAsDouble(config, "maxRadiation", 1);
        return this;
    }

    @Override
    public RecipeCondition fromNetwork(FriendlyByteBuf buf) {
        super.fromNetwork(buf);
        minRadiation = buf.readDouble();
        maxRadiation = buf.readDouble();
        return this;
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        super.toNetwork(buf);
        buf.writeDouble(minRadiation);
        buf.writeDouble(maxRadiation);
    }

    @Override
    public CompoundTag toNBT() {
        var tag = super.toNBT();
        tag.putDouble("minRadiation", minRadiation);
        tag.putDouble("maxRadiation", maxRadiation);
        return tag;
    }

    @Override
    public RecipeCondition fromNBT(CompoundTag tag) {
        super.fromNBT(tag);
        minRadiation = tag.getDouble("minRadiation");
        maxRadiation = tag.getDouble("maxRadiation");
        return this;
    }
}
