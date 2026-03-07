//package com.lowdragmc.mbd2.integration.pneumaticcraft.trait.pressure;
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
//import com.lowdragmc.mbd2.integration.pneumaticcraft.PNCPressureAirRecipeCapability;
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
//@LDLRegister(name = "pneumatic_pressure", registry = "mbd2:recipe_condition", modID = "pneumaticcraft")
//public class PNCPressureCondition extends RecipeCondition {
//    @Configurable(name = "config.recipe.condition.is_air", tips = "recipe.capability.pneumatic_pressure_air.type.tooltip")
//    private boolean isAir;
//    @Configurable(name = "config.recipe.condition.pressure_value")
//    @ConfigNumber(range = {1, Float.MAX_VALUE}, type = ConfigNumber.Type.FLOAT)
//    private Range value = Range.of(1f, 10f);
//
//    public PNCPressureCondition(boolean isAir, float minValue, float maxValue) {
//        this.isAir = isAir;
//        this.value = Range.of(minValue, maxValue);
//    }
//
//    @Override
//    public Component getTooltips() {
//        var minValue = value.getMin().floatValue();
//        var maxValue = value.getMax().floatValue();
//        return isAir ?
//                Component.translatable("recipe.condition.pneumatic_pressure.air.tooltip", minValue, maxValue) :
//                Component.translatable("recipe.condition.pneumatic_pressure.pressure.tooltip", minValue, maxValue);
//    }
//
//    @Override
//    public IGuiTexture getIcon() {
//        return new ItemStackTexture(ModItems.PRESSURE_GAUGE.get());
//    }
//
//    @Override
//    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
//        var proxy = recipeLogic.machine.getRecipeCapabilitiesProxy();
//        var toCheck = new ArrayList<IRecipeHandler<?>>();
//        if (recipe.inputs.containsKey(PNCPressureAirRecipeCapability.CAP) && proxy.contains(IO.IN, PNCPressureAirRecipeCapability.CAP)) {
//            var inputs = proxy.get(IO.IN, PNCPressureAirRecipeCapability.CAP);
//            toCheck.addAll(inputs);
//        }
//        if (recipe.outputs.containsKey(PNCPressureAirRecipeCapability.CAP) && proxy.contains(IO.OUT, PNCPressureAirRecipeCapability.CAP)) {
//            var outputs = proxy.get(IO.OUT, PNCPressureAirRecipeCapability.CAP);
//            toCheck.addAll(outputs);
//        }
//        if (proxy.contains(IO.BOTH, PNCPressureAirRecipeCapability.CAP)) {
//            toCheck.addAll(proxy.get(IO.BOTH, PNCPressureAirRecipeCapability.CAP));
//        }
//        for (IRecipeHandler<?> handler : toCheck) {
//            if (handler instanceof PNCPressureAirHandlerTrait trait) {
//                if (isAir) {
//                    var air = trait.getHandler().getAir();
//                    return air >= value.getMin().floatValue() && air <= value.getMax().floatValue();
//                } else {
//                    var pressure = trait.getHandler().getPressure();
//                    return pressure >= this.value.getMin().floatValue() && pressure <= this.value.getMax().floatValue();
//                }
//            }
//        }
//        return false;
//    }
//}
