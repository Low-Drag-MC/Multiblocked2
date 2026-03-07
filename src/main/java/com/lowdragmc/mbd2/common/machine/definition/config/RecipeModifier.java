package com.lowdragmc.mbd2.common.machine.definition.config;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigList;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorSelectorConfigurator;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.ReadOnlyManaged;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.api.recipe.content.ContentModifier;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.recipe.RainingCondition;
import com.mojang.datafixers.util.Pair;
import net.minecraft.nbt.*;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * To modify the controller recipe on the fly. You can use it to make a upgrade/plugin part.
 */
public class RecipeModifier implements IConfigurable, IPersistedSerializable {
    @Configurable(name = "config.recipe.content_modifier", subConfigurable = true, tips = {"config.recipe.content_modifier.tooltip"}, collapse = false)
    public final ContentModifier contentModifier = ContentModifier.of(1, 0);
    @Configurable(name = "config.recipe.target_content", tips = {"config.recipe.target_content.tooltip"})
    public final IO targetContent = IO.BOTH;
    @Configurable(name = "config.recipe.duration_modifier", subConfigurable = true, tips = {"config.recipe.duration_modifier.tooltip"}, collapse = false)
    public final ContentModifier durationModifier = ContentModifier.of(1, 0);
    @Configurable(name = "config.recipe.recipe_conditions")
    @ConfigList(configuratorMethod = "recipeConditionConfigurator", addDefaultMethod = "defaultRecipeCondition")
    public final List<RecipeCondition> recipeConditions = new ArrayList<>();
    @Configurable(name = "config.machine_settings.max_parallel", subConfigurable = true, tips = "config.machine_settings.max_parallel.tooltip", collapse = false)
    @ConfigNumber(range = {1, Integer.MAX_VALUE})
    public final ContentModifier maxParallel = ContentModifier.identity();


    protected Configurator recipeConditionConfigurator(Supplier<RecipeCondition> getter, Consumer<RecipeCondition> setter) {
        return new ConfiguratorSelectorConfigurator<>("config.recipe.recipe_condition.type",
                () -> getter.get().getType(), type -> {
            var condition = MBDRegistries.RECIPE_CONDITIONS.get(type);
            if (condition != null) {
                setter.accept(condition.value().get());
            }
        }, "rain", true, MBDRegistries.RECIPE_CONDITIONS.registry().keySet().stream().toList(),
                String::toString, (type, container) -> getter.get().buildConfigurator(container));
    }

    protected RecipeCondition defaultRecipeCondition() {
        return new RainingCondition();
    }

    public static class RecipeModifiers implements IConfigurable, IPersistedSerializable {
        @Configurable(name = "config.recipe.recipe_modifiers")
        @ConfigList(configuratorMethod = "recipeModifierConfigurator", addDefaultMethod = "defaultRecipeModifier")
        @ReadOnlyManaged(serializeMethod = "recipeModifiersSerialize", deserializeMethod = "recipeModifiersDeserialize")
        public final List<RecipeModifier> recipeModifiers = new ArrayList<>();

        protected IntTag recipeModifiersSerialize(List<RecipeModifier> groups) {
            return IntTag.valueOf(groups.size());
        }

        protected List<RecipeModifier> recipeModifiersDeserialize(IntTag tag) {
            var groups = new ArrayList<RecipeModifier>();
            for (int i = 0; i < tag.getAsInt(); i++) {
                groups.add(defaultRecipeModifier());
            }
            return groups;
        }

        protected Configurator recipeModifierConfigurator(Supplier<RecipeModifier> getter, Consumer<RecipeModifier> setter) {
            var group = new ConfiguratorGroup("", false).hideTitle();
            getter.get().buildConfigurator(group);
            return group;
        }

        protected RecipeModifier defaultRecipeModifier() {
            return new RecipeModifier();
        }

        /**
         * Apply the modifiers to the recipe.
         *
         * @param recipeLogic the recipe logic
         * @param recipe      the original recipe
         * @return the modified recipe with the max parallel number
         */
        public @Nonnull MBDRecipe applyModifiers(RecipeLogic recipeLogic, @Nonnull MBDRecipe recipe) {
            if (recipeModifiers.isEmpty()) return recipe;
            var contentModifiers = new ArrayList<Pair<ContentModifier, IO>>();
            var durationModifiers = new ArrayList<ContentModifier>();

            for (var modifier : recipeModifiers) {
                if (checkConditions(recipeLogic, recipe, modifier)) {
                    if (!modifier.contentModifier.isIdentity() && modifier.targetContent != IO.NONE) {
                        contentModifiers.add(Pair.of(modifier.contentModifier, modifier.targetContent));
                    }
                    if (!modifier.durationModifier.isIdentity()) {
                        durationModifiers.add(modifier.durationModifier);
                    }
                }
            }
            if (!contentModifiers.isEmpty()) {
                var inputModifiers = contentModifiers.stream().filter(pair -> pair.getSecond() == IO.IN || pair.getSecond() == IO.BOTH).map(Pair::getFirst).toList();
                var outputModifiers = contentModifiers.stream().filter(pair -> pair.getSecond() == IO.OUT || pair.getSecond() == IO.BOTH).map(Pair::getFirst).toList();
                if (!inputModifiers.isEmpty()) {
                    recipe = recipe.copy(inputModifiers.stream().reduce(ContentModifier.IDENTITY, ContentModifier::merge), false, IO.IN);
                }
                if (!outputModifiers.isEmpty()) {
                    recipe = recipe.copy(outputModifiers.stream().reduce(ContentModifier.IDENTITY, ContentModifier::merge), false, IO.OUT);
                }
            }
            if (!durationModifiers.isEmpty()) {
                if (contentModifiers.isEmpty()) {
                    recipe = recipe.copy();
                }
                recipe.duration = durationModifiers.stream().reduce(ContentModifier.IDENTITY, ContentModifier::merge).apply(recipe.duration).intValue();
            }
            return recipe;
        }

        /**
         * Get the max parallel number of the recipe.
         */
        public ContentModifier getMaxParallel(RecipeLogic recipeLogic, @Nonnull MBDRecipe recipe) {
            if (recipeModifiers.isEmpty()) return ContentModifier.IDENTITY;
            var maxParallel = ContentModifier.IDENTITY;
            for (var modifier : recipeModifiers) {
                if (!modifier.maxParallel.isIdentity() && checkConditions(recipeLogic, recipe, modifier)) {
                    maxParallel = maxParallel.merge(modifier.maxParallel);
                }
            }
            return maxParallel;
        }

        private boolean checkConditions(RecipeLogic recipeLogic, @Nonnull MBDRecipe recipe, RecipeModifier modifier) {
            var or = new HashMap<String, List<RecipeCondition>>();
            var success = true;
            for (RecipeCondition condition : modifier.recipeConditions) {
                if (condition.isOr()) {
                    or.computeIfAbsent(condition.getType(), type -> new ArrayList<>()).add(condition);
                } else if (condition.test(recipe, recipeLogic) == condition.isReverse()) {
                    success = false;
                    break;
                }
            }
            for (List<RecipeCondition> conditions : or.values()) {
                MBDRecipe finalRecipe = recipe;
                if (conditions.stream().allMatch(condition -> condition.test(finalRecipe, recipeLogic) == condition.isReverse())) {
                    success = false;
                    break;
                }
            }
            return success;
        }
    }
}
