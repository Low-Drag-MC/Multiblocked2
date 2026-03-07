//package com.lowdragmc.mbd2.integration.mekanism;
//
//import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
//import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
//import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
//import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
//import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
//import com.lowdragmc.mbd2.api.capability.recipe.IO;
//import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandler;
//import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
//import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
//import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
//import com.lowdragmc.mbd2.integration.mekanism.trait.heat.MekHeatCapabilityTrait;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import net.minecraft.network.chat.Component;
//
//import javax.annotation.Nonnull;
//import java.util.ArrayList;
//
//@Getter
//@NoArgsConstructor
//@LDLRegister(name = "mekanism_heat", registry = "mbd2:recipe_condition", modID = "mekanism")
//public class MEKTemperatureCondition extends RecipeCondition {
//    @Configurable(name = "config.recipe.condition.temperature.min")
//    @ConfigNumber(range = {-Float.MAX_VALUE, Float.MAX_VALUE})
//    private double minTemperature;
//    @Configurable(name = "config.recipe.condition.temperature.max")
//    @ConfigNumber(range = {-Float.MAX_VALUE, Float.MAX_VALUE})
//    private double maxTemperature;
//
//
//    public MEKTemperatureCondition(double minTemperature, double maxTemperature) {
//        this.minTemperature = minTemperature;
//        this.maxTemperature = maxTemperature;
//    }
//
//    public MEKTemperatureCondition(boolean isReverse, double minTemperature, double maxTemperature) {
//        super(isReverse);
//        this.minTemperature = minTemperature;
//        this.maxTemperature = maxTemperature;
//    }
//
//    @Override
//    public Component getTooltips() {
//        return Component.translatable("recipe.condition.mekanism_heat.tooltip", minTemperature, maxTemperature);
//    }
//
//    @Override
//    public IGuiTexture getIcon() {
//        return SpriteTexture.of("mbd2:textures/gui/thermometer.png");
//    }
//
//    @Override
//    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
//        var proxy = recipeLogic.machine.getRecipeCapabilitiesProxy();
//        var toCheck = new ArrayList<IRecipeHandler<?>>();
//        if (recipe.inputs.containsKey(MekanismHeatRecipeCapability.CAP) && proxy.contains(IO.IN, MekanismHeatRecipeCapability.CAP)) {
//            var inputs = proxy.get(IO.IN, MekanismHeatRecipeCapability.CAP);
//            toCheck.addAll(inputs);
//        }
//        if (recipe.outputs.containsKey(MekanismHeatRecipeCapability.CAP) && proxy.contains(IO.OUT, MekanismHeatRecipeCapability.CAP)) {
//            var outputs = proxy.get(IO.OUT, MekanismHeatRecipeCapability.CAP);
//            toCheck.addAll(outputs);
//        }
//        if (proxy.contains(IO.BOTH, MekanismHeatRecipeCapability.CAP)) {
//            toCheck.addAll(proxy.get(IO.BOTH, MekanismHeatRecipeCapability.CAP));
//        }
//        for (IRecipeHandler<?> handler : toCheck) {
//            if (handler instanceof MekHeatCapabilityTrait.HeatRecipeHandler heatRecipeHandler) {
//                var temp = ((MekHeatCapabilityTrait)heatRecipeHandler.trait).getContainer().getTemperature(0);
//                if (temp >= minTemperature && temp <= maxTemperature) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
//}
