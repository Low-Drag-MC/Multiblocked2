package com.lowdragmc.mbd2.integration.create;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.integration.create.machine.CreateRotationTrait;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllBlocks;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;

@Getter
@NoArgsConstructor
public class CreateRotationCondition extends RecipeCondition {

    public static final MapCodec<CreateRotationCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("reverse", false).forGetter(val -> val.isReverse),
            Codec.FLOAT.optionalFieldOf("minRPM", 0f).forGetter(val -> val.minRPM),
            Codec.FLOAT.optionalFieldOf("maxRPM", Float.MAX_VALUE).forGetter(val -> val.maxRPM),
            Codec.FLOAT.optionalFieldOf("minStress", 0f).forGetter(val -> val.minStress),
            Codec.FLOAT.optionalFieldOf("maxStress", Float.MAX_VALUE).forGetter(val -> val.maxStress)
    ).apply(instance, CreateRotationCondition::new));

    public final static CreateRotationCondition INSTANCE = new CreateRotationCondition();
    @Configurable(name = "config.recipe.condition.rpm.min")
    @NumberRange(range = {0f, Float.MAX_VALUE})
    private float minRPM;
    @Configurable(name = "config.recipe.condition.rpm.max")
    @NumberRange(range = {0f, Float.MAX_VALUE})
    private float maxRPM;
    @Configurable(name = "config.recipe.condition.stress.min")
    @NumberRange(range = {0f, Float.MAX_VALUE})
    private float minStress;
    @Configurable(name = "config.recipe.condition.stress.min")
    @NumberRange(range = {0f, Float.MAX_VALUE})
    private float maxStress;

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
    public MapCodec<? extends RecipeCondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        var proxy = recipeLogic.machine.getRecipeCapabilitiesProxy();
        var inputs = proxy.get(IO.IN, CreateStressRecipeCapability.CAP);
        if (inputs != null) {
            for (var input : inputs) {
                CreateRotationTrait trait = null;
                if (input instanceof CreateRotationTrait.RPMRecipeHandler handler) {
                    trait = handler.getTrait();
                } else if (input instanceof CreateRotationTrait.StressRecipeHandler handler) {
                    trait = handler.getTrait();
                }
                if (trait != null) {
                    var rpm = Math.abs(trait.getLastSpeed());
                    var stress = rpm * trait.getTorque();
                    if (rpm >= minRPM && rpm <= maxRPM && stress >= minStress && stress <= maxStress) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
