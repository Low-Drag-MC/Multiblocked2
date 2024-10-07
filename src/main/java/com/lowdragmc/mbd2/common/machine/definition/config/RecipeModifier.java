package com.lowdragmc.mbd2.common.machine.definition.config;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ArrayConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorSelectorConfigurator;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.api.recipe.content.ContentModifier;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.mojang.datafixers.util.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
    public final List<RecipeCondition> recipeConditions = new ArrayList<>();
    @Configurable(name = "config.machine_settings.max_parallel", subConfigurable = true, tips = "config.machine_settings.max_parallel.tooltip", collapse = false)
    @NumberRange(range = {1, Integer.MAX_VALUE})
    private ContentModifier maxParallel = ContentModifier.identity();

    @Override
    public CompoundTag serializeNBT() {
        var tag = IPersistedSerializable.super.serializeNBT();
        ListTag conditions = new ListTag();
        for (RecipeCondition condition : recipeConditions) {
            CompoundTag conditionTag = new CompoundTag();
            conditionTag.putString("type", MBDRegistries.RECIPE_CONDITIONS.getKey(condition.getClass()));
            conditionTag.put("data", condition.toNBT());
            conditions.add(conditionTag);
        }
        tag.put("recipeConditions", conditions);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        IPersistedSerializable.super.deserializeNBT(tag);
        recipeConditions.clear();
        ListTag conditions = tag.getList("recipeConditions", Tag.TAG_COMPOUND);
        for (int i = 0; i < conditions.size(); i++) {
            CompoundTag conditionTag = conditions.getCompound(i);
            var condition = RecipeCondition.create(MBDRegistries.RECIPE_CONDITIONS.get(conditionTag.getString("type")));
            if (condition != null) {
                condition.fromNBT(conditionTag.getCompound("data"));
                recipeConditions.add(condition);
            }
        }
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        IConfigurable.super.buildConfigurator(father);
        var conditions = new ArrayConfiguratorGroup<>("config.recipe.recipe_conditions", false,
                () -> recipeConditions, (getter, setter) -> new ConfiguratorSelectorConfigurator<>("config.recipe.recipe_condition.type", false,
                () -> getter.get().getType(), type -> {
            var current = getter.get();
            var condition = RecipeCondition.create(MBDRegistries.RECIPE_CONDITIONS.get(type));
            if (condition != null) {
                recipeConditions.set(recipeConditions.indexOf(current), condition);
            }
        }, "rain", true, MBDRegistries.RECIPE_CONDITIONS.registry().keySet().stream().toList(),
                String::toString, (type, container) -> {
            var current = getter.get();
            current.buildConfigurator(container);
        }), true);
        conditions.setTips("config.recipe.recipe_conditions.tooltip");
        conditions.setAddDefault(() -> RecipeCondition.create(MBDRegistries.RECIPE_CONDITIONS.get("rain")));
        conditions.setOnAdd(recipeConditions::add);
        conditions.setOnRemove(recipeConditions::remove);
        conditions.setOnUpdate(list -> {
            recipeConditions.clear();
            recipeConditions.addAll(list);
        });
        father.addConfigurators(conditions);
    }

    public static class RecipeModifiers implements ITagSerializable<ListTag>, IConfigurable {
        public final List<RecipeModifier> recipeModifiers = new ArrayList<>();

        @Override
        public ListTag serializeNBT() {
            var modifiers = new ListTag();
            for (RecipeModifier modifier : recipeModifiers) {
                modifiers.add(modifier.serializeNBT());
            }
            return modifiers;
        }

        @Override
        public void deserializeNBT(ListTag modifiers) {
            recipeModifiers.clear();
            for (int i = 0; i < modifiers.size(); i++) {
                var modifier = new RecipeModifier();
                modifier.deserializeNBT(modifiers.getCompound(i));
                recipeModifiers.add(modifier);
            }
        }

        @Override
        public void buildConfigurator(ConfiguratorGroup father) {
            var modifiers = new ArrayConfiguratorGroup<>("config.recipe.recipe_modifiers", true,
                    () -> recipeModifiers, (getter, setter) -> {
                var recipeModifier = getter.get();
                var group = new ConfiguratorGroup("config.recipe.content_modifier", false);
                recipeModifier.buildConfigurator(group);
                return group;
            }, true);
            modifiers.setTips("config.recipe.recipe_modifiers.tooltip");
            modifiers.setAddDefault(RecipeModifier::new);
            modifiers.setOnAdd(recipeModifiers::add);
            modifiers.setOnRemove(recipeModifiers::remove);
            modifiers.setOnUpdate(list -> {
                recipeModifiers.clear();
                recipeModifiers.addAll(list);
            });
            father.addConfigurators(modifiers);
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
