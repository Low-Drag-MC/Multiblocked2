package com.lowdragmc.mbd2.integration.astralsorcery.sky;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSearch;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.gui.MBDSprites;
import com.lowdragmc.mbd2.integration.astralsorcery.AstralSorceryIntegration;
import hellfirepvp.astralsorcery.common.constellation.level.LevelSkyHandler;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nonnull;

/**
 * Passes while a constellation is active in Astral Sorcery's sky with at least {@code minDistribution}. Activity is
 * per day, so pair it with {@code day_time} for "at night"; a dimension without a sky context has none active.
 */
@Getter
@Setter
@NoArgsConstructor
@LDLRegister(name = "astral_constellation", registry = "mbd2:recipe_condition", modID = AstralSorceryIntegration.MOD_ID)
public class AstralConstellationCondition extends RecipeCondition {

    @Configurable(name = "config.recipe.condition.astral_constellation.constellation")
    @ConfigSearch(searchConfiguratorMethod = "searchConstellation")
    private ResourceLocation constellation = AstralSorceryIntegration.defaultConstellationId();

    @Configurable(name = "config.recipe.condition.astral_constellation.min_distribution",
            tips = "config.recipe.condition.astral_constellation.min_distribution.tooltip")
    @ConfigNumber(range = {0, 1})
    private float minDistribution;

    public AstralConstellationCondition(ResourceLocation constellation, float minDistribution) {
        this.constellation = constellation;
        this.minDistribution = minDistribution;
    }

    @Override
    public String getType() {
        return "astral_constellation";
    }

    @Override
    public Component getTooltips() {
        return Component.translatable("recipe.condition.astral_constellation.tooltip",
                AstralSorceryIntegration.constellationName(constellation), minDistribution);
    }

    @Override
    public IGuiTexture getIcon() {
        return MBDSprites.ASTRAL_CONSTELLATION;
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        if (!(recipeLogic.machine.getLevel() instanceof ServerLevel level)) return false;
        var cst = AstralSorceryIntegration.constellation(constellation);
        if (cst == null) return false;
        return LevelSkyHandler.getContext(level).map(ctx -> {
            var handler = ctx.getConstellationHandler();
            return handler.isCurrentlyActive(cst) && handler.getDistributionMultiplier(level, cst) >= minDistribution;
        }).orElse(false);
    }

    private SearchComponentConfigurator.ISearchConfigurator<ResourceLocation> searchConstellation() {
        return AstralSorceryIntegration.idSearchConfigurator(AstralSorceryIntegration.defaultConstellationId(),
                AstralSorceryIntegration::constellationIds, AstralSorceryIntegration::constellationName);
    }
}
