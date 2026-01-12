package com.lowdragmc.mbd2.integration.pneumaticcraft.trait.pressure;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandler;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.integration.pneumaticcraft.PNCPressureAirRecipeCapability;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.desht.pneumaticcraft.common.core.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;

import javax.annotation.Nonnull;
import java.util.ArrayList;

@Getter
@NoArgsConstructor
public class PNCPressureCondition extends RecipeCondition {
    public final static PNCPressureCondition INSTANCE = new PNCPressureCondition();
    @Configurable(name = "config.recipe.condition.is_air", tips = "recipe.capability.pneumatic_pressure_air.type.tooltip")
    private boolean isAir;
    @Configurable(name = "config.recipe.condition.pressure.min")
    @NumberRange(range = {1, Float.MAX_VALUE})
    private float minPressure;
    @Configurable(name = "config.recipe.condition.pressure.max")
    @NumberRange(range = {1, Float.MAX_VALUE})
    private float maxPressure;

    public PNCPressureCondition(boolean isAir, float minPressure, float maxPressure) {
        this.isAir = isAir;
        this.minPressure = minPressure;
        this.maxPressure = maxPressure;
    }

    @Override
    public String getType() {
        return "pneumatic_pressure";
    }

    @Override
    public Component getTooltips() {
        return isAir ?
                Component.translatable("recipe.condition.pneumatic_pressure.air.tooltip", minPressure, maxPressure) :
                Component.translatable("recipe.condition.pneumatic_pressure.pressure.tooltip", minPressure, maxPressure);
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(ModItems.PRESSURE_GAUGE.get());
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        var proxy = recipeLogic.machine.getRecipeCapabilitiesProxy();
        var toCheck = new ArrayList<IRecipeHandler<?>>();
        if (recipe.inputs.containsKey(PNCPressureAirRecipeCapability.CAP) && proxy.contains(IO.IN, PNCPressureAirRecipeCapability.CAP)) {
            var inputs = proxy.get(IO.IN, PNCPressureAirRecipeCapability.CAP);
            toCheck.addAll(inputs);
        }
        if (recipe.outputs.containsKey(PNCPressureAirRecipeCapability.CAP) && proxy.contains(IO.OUT, PNCPressureAirRecipeCapability.CAP)) {
            var outputs = proxy.get(IO.OUT, PNCPressureAirRecipeCapability.CAP);
            toCheck.addAll(outputs);
        }
        if (proxy.contains(IO.BOTH, PNCPressureAirRecipeCapability.CAP)) {
            toCheck.addAll(proxy.get(IO.BOTH, PNCPressureAirRecipeCapability.CAP));
        }
        for (IRecipeHandler<?> handler : toCheck) {
            if (handler instanceof PNCPressureAirHandlerTrait trait) {
                if (isAir) {
                    var air = trait.getHandler().getAir();
                    return air >= minPressure && air <= maxPressure;
                } else {
                    var pressure = trait.getHandler().getPressure();
                    return pressure >= minPressure && pressure <= maxPressure;
                }
            }
        }
        return false;
    }

    @Nonnull
    @Override
    public JsonObject serialize() {
        JsonObject config = super.serialize();
        config.addProperty("isAir", isAir);
        config.addProperty("minPressure", minPressure);
        config.addProperty("maxPressure", maxPressure);
        return config;
    }

    @Override
    public RecipeCondition deserialize(@Nonnull JsonObject config) {
        super.deserialize(config);
        isAir = config.getAsJsonPrimitive("isAir").getAsBoolean();
        minPressure = GsonHelper.getAsFloat(config, "minPressure", 0);
        maxPressure = GsonHelper.getAsFloat(config, "maxPressure", 1);
        return this;
    }

    @Override
    public RecipeCondition fromNetwork(FriendlyByteBuf buf) {
        super.fromNetwork(buf);
        isAir = buf.readBoolean();
        minPressure = buf.readFloat();
        maxPressure = buf.readFloat();
        return this;
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        super.toNetwork(buf);
        buf.writeBoolean(isAir);
        buf.writeFloat(minPressure);
        buf.writeFloat(maxPressure);
    }

    @Override
    public CompoundTag toNBT() {
        var tag = super.toNBT();
        tag.putBoolean("isAir", isAir);
        tag.putFloat("minPressure", minPressure);
        tag.putFloat("maxPressure", maxPressure);
        return tag;
    }

    @Override
    public RecipeCondition fromNBT(CompoundTag tag) {
        super.fromNBT(tag);
        isAir = tag.getBoolean("isAir");
        minPressure = tag.getFloat("minPressure");
        maxPressure = tag.getFloat("maxPressure");
        return this;
    }

}
