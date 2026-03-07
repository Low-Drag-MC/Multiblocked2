//package com.lowdragmc.mbd2.integration.create;
//
//import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
//import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
//import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
//import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
//import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
//import com.lowdragmc.mbd2.api.capability.recipe.IO;
//import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
//import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
//import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
//import com.lowdragmc.mbd2.integration.create.machine.CreateRotationTrait;
//import com.simibubi.create.AllBlocks;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import net.minecraft.network.chat.Component;
//
//import javax.annotation.Nonnull;
//
//@Getter
//@NoArgsConstructor
//@LDLRegister(name = "create_rotation", registry = "mbd2:recipe_condition", modID = "create")
//public class CreateRotationCondition extends RecipeCondition {
//    @Configurable(name = "config.recipe.condition.rpm.min")
//    @ConfigNumber(range = {0f, Float.MAX_VALUE})
//    private float minRPM;
//    @Configurable(name = "config.recipe.condition.rpm.max")
//    @ConfigNumber(range = {0f, Float.MAX_VALUE})
//    private float maxRPM;
//    @Configurable(name = "config.recipe.condition.stress.min")
//    @ConfigNumber(range = {0f, Float.MAX_VALUE})
//    private float minStress;
//    @Configurable(name = "config.recipe.condition.stress.min")
//    @ConfigNumber(range = {0f, Float.MAX_VALUE})
//    private float maxStress;
//
//    public CreateRotationCondition(float minRPM, float maxRPM, float minStress, float maxStress) {
//        this(false, minRPM, maxRPM, minStress, maxStress);
//    }
//
//    public CreateRotationCondition(boolean isReverse, float minRPM, float maxRPM, float minStress, float maxStress) {
//        super(isReverse);
//        this.minRPM = minRPM;
//        this.maxRPM = maxRPM;
//        this.minStress = minStress;
//        this.maxStress = maxStress;
//    }
//
//    @Override
//    public Component getTooltips() {
//        return Component.translatable("recipe.condition.create_rpm.tooltip", minRPM, maxRPM, minStress, maxStress);
//    }
//
//    @Override
//    public IGuiTexture getIcon() {
//        return new ItemStackTexture(AllBlocks.SHAFT.asStack());
//    }
//
//    @Override
//    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
//        var proxy = recipeLogic.machine.getRecipeCapabilitiesProxy();
//        var inputs = proxy.get(IO.IN, CreateStressRecipeCapability.CAP);
//        if (inputs != null) {
//            for (var input : inputs) {
//                CreateRotationTrait trait = null;
//                if (input instanceof CreateRotationTrait.RPMRecipeHandler handler) {
//                    trait = handler.getTrait();
//                } else if (input instanceof CreateRotationTrait.StressRecipeHandler handler) {
//                    trait = handler.getTrait();
//                }
//                if (trait != null) {
//                    var rpm = Math.abs(trait.getLastSpeed());
//                    var stress = rpm * trait.getTorque();
//                    if (rpm >= minRPM && rpm <= maxRPM && stress >= minStress && stress <= maxStress) {
//                        return true;
//                    }
//                }
//            }
//        }
//        return false;
//    }
//
//}
