//package com.lowdragmc.mbd2.integration.pneumaticcraft.trait.heat;
//
//import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
//import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
//import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
//import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
//import com.lowdragmc.lowdraglib2.math.Range;
//import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
//import com.lowdragmc.mbd2.api.capability.recipe.IO;
//import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandler;
//import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
//import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
//import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
//import com.lowdragmc.mbd2.integration.pneumaticcraft.PNCHeatRecipeCapability;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import me.desht.pneumaticcraft.common.registry.ModItems;
//import net.minecraft.network.chat.Component;
//
//import javax.annotation.Nonnull;
//import java.util.ArrayList;
//
//@Getter
//@NoArgsConstructor
//@LDLRegister(name = "pneumatic_temperature", registry = "mbd2:recipe_condition", modID = "pneumaticcraft")
//public class PNCTemperatureCondition extends RecipeCondition {
//    @Configurable(name = "config.recipe.condition.temperature")
//    @ConfigNumber(range = {-Float.MAX_VALUE, Float.MAX_VALUE}, type = ConfigNumber.Type.FLOAT)
//    private Range temperature = Range.of(-Float.MAX_VALUE, Float.MAX_VALUE);
//
//    public PNCTemperatureCondition(float minTemperature, float maxTemperature) {
//        this(false, minTemperature, maxTemperature);
//    }
//
//    public PNCTemperatureCondition(boolean isReverse, float minTemperature, float maxTemperature) {
//        super(isReverse);
//        this.temperature = Range.of(minTemperature, maxTemperature);
//    }
//
//    @Override
//    public Component getTooltips() {
//        return Component.translatable("recipe.condition.pneumatic_temperature.tooltip",
//                temperature.getMin().floatValue(), temperature.getMax().floatValue());
//    }
//
//    @Override
//    public IGuiTexture getIcon() {
//        return new ItemStackTexture(ModItems.HEAT_FRAME.get());
//    }
//
//    @Override
//    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
//        var proxy = recipeLogic.machine.getRecipeCapabilitiesProxy();
//        var toCheck = new ArrayList<IRecipeHandler<?>>();
//        if (recipe.inputs.containsKey(PNCHeatRecipeCapability.CAP) && proxy.contains(IO.IN, PNCHeatRecipeCapability.CAP)) {
//            var inputs = proxy.get(IO.IN, PNCHeatRecipeCapability.CAP);
//            toCheck.addAll(inputs);
//        }
//        if (recipe.outputs.containsKey(PNCHeatRecipeCapability.CAP) && proxy.contains(IO.OUT, PNCHeatRecipeCapability.CAP)) {
//            var outputs = proxy.get(IO.OUT, PNCHeatRecipeCapability.CAP);
//            toCheck.addAll(outputs);
//        }
//        if (proxy.contains(IO.BOTH, PNCHeatRecipeCapability.CAP)) {
//            toCheck.addAll(proxy.get(IO.BOTH, PNCHeatRecipeCapability.CAP));
//        }
//        for (IRecipeHandler<?> handler : toCheck) {
//            if (handler instanceof PNCHeatExchangerTrait.HeatRecipeHandler heatRecipeHandler) {
//                var temp = ((PNCHeatExchangerTrait)heatRecipeHandler.trait).getHandler().getTemperature() - 273;
//                if (temp >= temperature.getMin().floatValue() && temp <= temperature.getMax().floatValue()) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
//
//}
