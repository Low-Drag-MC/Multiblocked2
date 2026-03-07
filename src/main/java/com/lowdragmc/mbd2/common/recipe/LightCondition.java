package com.lowdragmc.mbd2.common.recipe;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.math.Range;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;

import javax.annotation.Nonnull;

@Getter
@Setter
@NoArgsConstructor
@LDLRegister(name = "light", registry = "mbd2:recipe_condition")
public class LightCondition extends RecipeCondition {
    @Configurable(name = "config.recipe.condition.light.sky_light")
    @ConfigNumber(range = {0, 15}, type = ConfigNumber.Type.INTEGER)
    private Range skyLight = Range.of(0, 15);
    @Configurable(name = "config.recipe.condition.light.block_light")
    @ConfigNumber(range = {0, 15}, type = ConfigNumber.Type.INTEGER)
    private Range blockLight = Range.of(0, 15);
    @Configurable(name = "config.recipe.condition.light.can_see_sky", tips = "config.recipe.condition.light.can_see_sky.tooltip")
    private boolean canSeeSky;

    public LightCondition(int minSkyLight, int maxSkyLight, int minBlockLight, int maxBlockLight, boolean canSeeSky) {
        this.skyLight = Range.of(minSkyLight, maxSkyLight);
        this.blockLight = Range.of(minBlockLight, maxBlockLight);
        this.canSeeSky = canSeeSky;
    }

    @Override
    public String getType() {
        return "light";
    }

    @Override
    public Component getTooltips() {
        return Component.translatable("recipe.condition.light.tooltip",
                skyLight.getMin().intValue(),
                skyLight.getMax().intValue(),
                blockLight.getMin().intValue(),
                blockLight.getMax().intValue(),
                canSeeSky);
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(Items.DAYLIGHT_DETECTOR);
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        var level = recipeLogic.machine.getLevel();
        var pos = recipeLogic.getMachine().getPos();
        var blockLight = level.getBrightness(LightLayer.BLOCK, pos);
        var skyLight = level.getBrightness(LightLayer.SKY, pos);
        return blockLight >= this.blockLight.getMin().intValue() &&
                blockLight <= this.blockLight.getMax().intValue() &&
                skyLight >= this.skyLight.getMin().intValue() &&
                skyLight <= this.skyLight.getMax().intValue() &&
                (!canSeeSky || level.canSeeSky(pos));
    }

}
