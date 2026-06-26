package com.lowdragmc.mbd2.integration.kubejs.wrapper;

import com.lowdragmc.mbd2.api.recipe.ingredient.EntityIngredient;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public interface EntityIngredientWrapper {
    @HideFromJS
    static EntityIngredient wrap(Object o) {
        if (o instanceof EntityIngredient ingredient) {
            return ingredient;
        }
        if (o instanceof EntityType<?> entityType) {
            return EntityIngredient.of(1, entityType);
        }
        if (o instanceof CharSequence sequence) {
            var str = sequence.toString();
            int x = str.indexOf('x');
            if (x > 0 && x < str.length() - 2 && str.charAt(x + 1) == ' ') {
                var entityType = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(str.substring(x + 2)));
                return EntityIngredient.of(Integer.parseInt(str.substring(0, x).trim()), entityType);
            }
            return EntityIngredient.of(1, BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(str)));
        }
        return EntityIngredient.of();
    }
}
