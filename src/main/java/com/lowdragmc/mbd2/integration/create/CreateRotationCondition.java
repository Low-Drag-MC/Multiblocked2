package com.lowdragmc.mbd2.integration.create;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.integration.create.machine.CreateRotationTrait;
import com.simibubi.create.AllBlocks;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import javax.annotation.Nonnull;

@Getter
@Setter
@NoArgsConstructor
@LDLRegister(name = "create_rotation", registry = "mbd2:recipe_condition", modID = "create")
public class CreateRotationCondition extends RecipeCondition {

    @Configurable(name = "config.recipe.condition.rpm.min")
    @ConfigNumber(range = {0, Float.MAX_VALUE})
    private float minRPM;

    @Configurable(name = "config.recipe.condition.rpm.max")
    @ConfigNumber(range = {0, Float.MAX_VALUE})
    private float maxRPM = Float.MAX_VALUE;

    @Configurable(name = "config.recipe.condition.stress.min")
    @ConfigNumber(range = {0, Float.MAX_VALUE})
    private float minStress;

    @Configurable(name = "config.recipe.condition.stress.max")
    @ConfigNumber(range = {0, Float.MAX_VALUE})
    private float maxStress = Float.MAX_VALUE;

    public CreateRotationCondition(float minRPM, float maxRPM, float minStress, float maxStress) {
        this(false, minRPM, maxRPM, minStress, maxStress);
    }

    public CreateRotationCondition(boolean isReverse, float minRPM, float maxRPM, float minStress, float maxStress) {
        super(isReverse);
        this.minRPM = minRPM;
        this.maxRPM = maxRPM;
        this.minStress = minStress;
        this.maxStress = maxStress;
    }

    @Override
    public String getType() {
        return "create_rotation";
    }

    @Override
    public Component getTooltips() {
        return Component.translatable("recipe.condition.create_rpm.tooltip", minRPM, maxRPM, minStress, maxStress);
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(AllBlocks.SHAFT.asStack());
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        if (!(recipeLogic.machine instanceof MBDMachine machine)) return false;
        for (var trait : machine.getAdditionalTraits()) {
            if (trait instanceof CreateRotationTrait rotationTrait) {
                float rpm = Mth.abs(rotationTrait.getLastSpeed());
                float stress = rpm * rotationTrait.getTorque();
                if (rpm >= minRPM && rpm <= maxRPM && stress >= minStress && stress <= maxStress) {
                    return true;
                }
            }
        }
        return false;
    }
}
