package com.lowdragmc.mbd2.integration.astralsorcery.sky;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.gui.MBDSprites;
import com.lowdragmc.mbd2.integration.astralsorcery.AstralSorceryIntegration;
import hellfirepvp.astralsorcery.common.constellation.level.LevelSkyHandler;
import hellfirepvp.astralsorcery.common.constellation.level.event.CelestialEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nonnull;

/** Passes during one of Astral Sorcery's eclipses. */
@Getter
@Setter
@NoArgsConstructor
@LDLRegister(name = "astral_celestial_event", registry = "mbd2:recipe_condition", modID = AstralSorceryIntegration.MOD_ID)
public class AstralCelestialEventCondition extends RecipeCondition {
    public enum Event {
        SOLAR_ECLIPSE,
        LUNAR_ECLIPSE;

        /** The same key LDLib2's enum selector displays. */
        public Component displayName() {
            return Component.translatable(getDeclaringClass().getName() + "." + name());
        }
    }

    @Configurable(name = "config.recipe.condition.astral_celestial_event.event")
    private Event event = Event.SOLAR_ECLIPSE;

    @Configurable(name = "config.recipe.condition.astral_celestial_event.whole_day",
            tips = "config.recipe.condition.astral_celestial_event.whole_day.tooltip")
    private boolean wholeDay;

    public AstralCelestialEventCondition(Event event, boolean wholeDay) {
        this.event = event;
        this.wholeDay = wholeDay;
    }

    @Override
    public String getType() {
        return "astral_celestial_event";
    }

    @Override
    public Component getTooltips() {
        return Component.translatable(wholeDay
                ? "recipe.condition.astral_celestial_event.tooltip.day"
                : "recipe.condition.astral_celestial_event.tooltip", event.displayName());
    }

    @Override
    public IGuiTexture getIcon() {
        return MBDSprites.ASTRAL_CELESTIAL_EVENT;
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        if (!(recipeLogic.machine.getLevel() instanceof ServerLevel level)) return false;
        return LevelSkyHandler.getContext(level).map(ctx -> {
            var events = ctx.getCelestialEventHandler();
            CelestialEvent celestial = event == Event.SOLAR_ECLIPSE ? events.getSolarEclipse() : events.getLunarEclipse();
            return wholeDay ? celestial.isActiveDay() : celestial.isActiveNow();
        }).orElse(false);
    }
}
