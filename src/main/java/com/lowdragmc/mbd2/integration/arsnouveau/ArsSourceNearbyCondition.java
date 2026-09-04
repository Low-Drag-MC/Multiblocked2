package com.lowdragmc.mbd2.integration.arsnouveau;

import com.hollingsworth.arsnouveau.api.util.SourceUtil;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.gui.MBDSprites;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nonnull;

/**
 * Gates a recipe on how much Source the jars around the machine are holding.
 *
 * <p>Unlike the recipe capability this does not spend anything — it is for recipes that should only run
 * in a well-stocked area, or only in a depleted one.</p>
 *
 * <p>The count is taken live rather than from {@code ars_nearby_source}'s cache, so the condition works
 * on any machine and never depends on another trait's IO direction. That costs a walk over the block
 * entities in range, on the game thread: {@code RecipeCondition#test} is only ever called from
 * {@code RecipeLogic}'s tick, never from the background recipe search, and Ars Nouveau's own Enchanting
 * Apparatus pays the same price on every craft attempt.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@LDLRegister(name = "ars_source_nearby", registry = "mbd2:recipe_condition", modID = "ars_nouveau")
public class ArsSourceNearbyCondition extends RecipeCondition {

    @Configurable(name = "config.recipe.condition.ars_source_nearby.radius")
    @ConfigNumber(range = {1, 64})
    private int radius = 10;

    @Configurable(name = "config.recipe.condition.ars_source_nearby.min")
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    private int minSource;

    @Configurable(name = "config.recipe.condition.ars_source_nearby.max")
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    private int maxSource = Integer.MAX_VALUE;

    public ArsSourceNearbyCondition(int radius, int minSource, int maxSource) {
        this.radius = radius;
        this.minSource = minSource;
        this.maxSource = maxSource;
    }

    @Override
    public String getType() {
        return "ars_source_nearby";
    }

    @Override
    public Component getTooltips() {
        return Component.translatable("recipe.condition.ars_source_nearby.tooltip", minSource, maxSource, radius);
    }

    @Override
    public IGuiTexture getIcon() {
        return MBDSprites.ARS_SOURCE;
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        if (!(recipeLogic.machine instanceof MBDMachine machine)) return false;
        if (!(machine.getLevel() instanceof ServerLevel level)) return false;
        long total = 0;
        for (var provider : SourceUtil.canTakeSource(machine.getPos(), level, Math.max(1, radius))) {
            total += Math.max(0, provider.getSource().getSource());
            // nothing above the upper bound can change the answer, and a creative jar reports a million
            if (total > maxSource) return false;
        }
        return total >= minSource;
    }
}
