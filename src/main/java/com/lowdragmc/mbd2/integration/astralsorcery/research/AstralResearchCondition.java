package com.lowdragmc.mbd2.integration.astralsorcery.research;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSearch;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.gui.MBDSprites;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.integration.astralsorcery.AstralSorceryIntegration;
import hellfirepvp.astralsorcery.common.research.ResearchTier;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

/**
 * Passes when the player the machine's {@link AstralResearcherTrait} is bound to has progressed far enough in
 * Astral Sorcery. Never passes without a bound researcher; an offline owner is judged by their last snapshot.
 */
@Getter
@Setter
@NoArgsConstructor
@LDLRegister(name = "astral_research", registry = "mbd2:recipe_condition", modID = AstralSorceryIntegration.MOD_ID)
public class AstralResearchCondition extends RecipeCondition {
    /** Mirrors Astral Sorcery's tiers so saved recipes do not depend on its enum. */
    public enum Tier {
        SHIMMER,
        ILLUMINATION,
        RESONANCE,
        LUMINANCE,
        RADIANCE;

        public ResearchTier toResearchTier() {
            return ResearchTier.valueOf(name());
        }
    }

    @Configurable(name = "config.recipe.condition.astral_research.min_tier")
    private Tier minTier = Tier.SHIMMER;

    @Configurable(name = "config.recipe.condition.astral_research.attunement",
            tips = "config.recipe.condition.astral_research.attunement.tooltip")
    @ConfigSearch(searchConfiguratorMethod = "searchConstellation")
    private ResourceLocation attunement = AstralSorceryIntegration.ANY;

    @Configurable(name = "config.recipe.condition.astral_research.discovered",
            tips = "config.recipe.condition.astral_research.discovered.tooltip")
    @ConfigSearch(searchConfiguratorMethod = "searchConstellation")
    private ResourceLocation discovered = AstralSorceryIntegration.ANY;

    public AstralResearchCondition(Tier minTier, ResourceLocation attunement, ResourceLocation discovered) {
        this.minTier = minTier;
        this.attunement = attunement;
        this.discovered = discovered;
    }

    @Override
    public String getType() {
        return "astral_research";
    }

    @Override
    public Component getTooltips() {
        var tooltip = Component.translatable("recipe.condition.astral_research.tooltip",
                minTier.toResearchTier().getName());
        if (!AstralSorceryIntegration.ANY.equals(attunement)) {
            tooltip.append(Component.translatable("recipe.condition.astral_research.tooltip.attuned",
                    AstralSorceryIntegration.constellationName(attunement)));
        }
        if (!AstralSorceryIntegration.ANY.equals(discovered)) {
            tooltip.append(Component.translatable("recipe.condition.astral_research.tooltip.discovered",
                    AstralSorceryIntegration.constellationName(discovered)));
        }
        return tooltip;
    }

    @Override
    public IGuiTexture getIcon() {
        return MBDSprites.ASTRAL_RESEARCH;
    }

    /** Conditions are only tested on the game thread, so refreshing here is safe. */
    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        if (!(recipeLogic.machine instanceof MBDMachine machine)) return false;
        // includes a multiblock's parts
        for (var trait : machine.getRecipeLogicTraits(recipeLogic)) {
            if (trait instanceof AstralResearcherTrait researcher) {
                researcher.refresh();
                return satisfiedBy(researcher.getSnapshot());
            }
        }
        return false;
    }

    public boolean satisfiedBy(ResearchSnapshot snapshot) {
        if (!snapshot.isBound() || snapshot.getTier() == null) return false;
        if (!snapshot.getTier().isThisLaterOrEqual(minTier.toResearchTier())) return false;
        if (!AstralSorceryIntegration.ANY.equals(attunement) && !attunement.equals(snapshot.getAttunedConstellation())) {
            return false;
        }
        return AstralSorceryIntegration.ANY.equals(discovered) || snapshot.getKnownConstellations().contains(discovered);
    }

    private SearchComponentConfigurator.ISearchConfigurator<ResourceLocation> searchConstellation() {
        return AstralSorceryIntegration.idSearchConfigurator(AstralSorceryIntegration.ANY,
                AstralSorceryIntegration::constellationIdsOrAny, AstralSorceryIntegration::constellationOrAnyName);
    }
}
