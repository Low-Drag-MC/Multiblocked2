package com.lowdragmc.mbd2.integration.pneumaticcraft.trait.heat;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandler;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.integration.pneumaticcraft.PNCHeatRecipeCapability;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.desht.pneumaticcraft.common.registry.ModItems;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.ArrayList;

@Getter
@NoArgsConstructor
public class PNCTemperatureCondition extends RecipeCondition {
    public static final MapCodec<PNCTemperatureCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("reverse", false).forGetter(val -> val.isReverse),
            Codec.FLOAT.optionalFieldOf("minTemperature", 0.0f).forGetter(val -> val.minTemperature),
            Codec.FLOAT.optionalFieldOf("maxTemperature", Float.MAX_VALUE).forGetter(val -> val.maxTemperature)
    ).apply(instance, PNCTemperatureCondition::new));

    public final static PNCTemperatureCondition INSTANCE = new PNCTemperatureCondition();
    @Configurable(name = "config.recipe.condition.temperature.min")
    @NumberRange(range = {-Float.MAX_VALUE, Float.MAX_VALUE})
    private float minTemperature;
    @Configurable(name = "config.recipe.condition.temperature.max")
    @NumberRange(range = {-Float.MAX_VALUE, Float.MAX_VALUE})
    private float maxTemperature;

    public PNCTemperatureCondition(float minTemperature, float maxTemperature) {
        this.minTemperature = minTemperature;
        this.maxTemperature = maxTemperature;
    }

    public PNCTemperatureCondition(boolean isReverse, float minTemperature, float maxTemperature) {
        super(isReverse);
        this.minTemperature = minTemperature;
        this.maxTemperature = maxTemperature;
    }

    @Override
    public String getType() {
        return "pneumatic_temperature";
    }

    @Override
    public Component getTooltips() {
        return Component.translatable("recipe.condition.pneumatic_temperature.tooltip", minTemperature, maxTemperature);
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(ModItems.HEAT_FRAME.get());
    }

    @Override
    public MapCodec<? extends RecipeCondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        var proxy = recipeLogic.machine.getRecipeCapabilitiesProxy();
        var toCheck = new ArrayList<IRecipeHandler<?>>();
        if (recipe.inputs.containsKey(PNCHeatRecipeCapability.CAP) && proxy.contains(IO.IN, PNCHeatRecipeCapability.CAP)) {
            var inputs = proxy.get(IO.IN, PNCHeatRecipeCapability.CAP);
            toCheck.addAll(inputs);
        }
        if (recipe.outputs.containsKey(PNCHeatRecipeCapability.CAP) && proxy.contains(IO.OUT, PNCHeatRecipeCapability.CAP)) {
            var outputs = proxy.get(IO.OUT, PNCHeatRecipeCapability.CAP);
            toCheck.addAll(outputs);
        }
        if (proxy.contains(IO.BOTH, PNCHeatRecipeCapability.CAP)) {
            toCheck.addAll(proxy.get(IO.BOTH, PNCHeatRecipeCapability.CAP));
        }
        for (IRecipeHandler<?> handler : toCheck) {
            if (handler instanceof PNCHeatExchangerTrait.HeatRecipeHandler heatRecipeHandler) {
                var temp = ((PNCHeatExchangerTrait)heatRecipeHandler.trait).getHandler().getTemperature() - 273;
                if (temp >= minTemperature && temp <= maxTemperature) {
                    return true;
                }
            }
        }
        return false;
    }

}
