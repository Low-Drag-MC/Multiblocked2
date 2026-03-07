package com.lowdragmc.mbd2.common.capability.recipe.configurators.fluid;

import com.google.gson.JsonParser;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.ui.*;
import com.lowdragmc.mbd2.core.mixins.IngredientAccessor;
import com.lowdragmc.mbd2.core.mixins.ItemValueAccessor;
import com.lowdragmc.mbd2.core.mixins.TagValueAccessor;
import com.mojang.serialization.JsonOps;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SingleFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.TagFluidIngredient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class FluidIngredientConfigurator extends ValueConfigurator<FluidIngredient> {
    public enum FluidIngredientType {
        SINGLE,
        TAG,
        JSON,
    }

    // runtime
    @Nullable
    private FluidIngredientType currentType;

    public FluidIngredientConfigurator(String name,
                                       Supplier<FluidIngredient> getter,
                                       Consumer<FluidIngredient> setter,
                                       @NotNull FluidIngredient defaultValue,
                                       boolean forceUpdate) {
        super(name, getter, setter, defaultValue, forceUpdate);
        if (value == null) value = defaultValue;
        inlineContainer.addChild(new ConfiguratorSelectorConfigurator<>("recipe.capability.item.ingredient.type",
                () -> getType(getter.get()),
                type -> setter.accept(createDefault(type)),
                FluidIngredientType.SINGLE,
                true,
                Arrays.stream(FluidIngredientType.values()).toList(),
                Enum::name,
                (type, group) -> {
                    currentType = type;
                    if (type == FluidIngredientType.SINGLE) {
                        group.addConfigurators(new SingleFluidIngredientConfigurator("",
                                () -> getter.get() instanceof SingleFluidIngredient single ? single : new SingleFluidIngredient(Fluids.WATER.builtInRegistryHolder()),
                                setter::accept,
                                new SingleFluidIngredient(Fluids.WATER.builtInRegistryHolder()),
                                true
                        ));
                    } else if (type == FluidIngredientType.TAG) {
                        group.addConfigurators(new TagFluidIngredientConfigurator("",
                                () -> getter.get() instanceof TagFluidIngredient tag ? tag : new TagFluidIngredient(FluidTags.WATER),
                                setter::accept,
                                new TagFluidIngredient(FluidTags.WATER),
                                true
                        ));
                    } else if (type == FluidIngredientType.JSON) {
                        var op = Platform.getFrozenRegistry().createSerializationContext(JsonOps.INSTANCE);
                        group.addConfigurator(new TextAreaConfigurator("",
                                () -> FluidIngredient.CODEC.encodeStart(op, getter.get())
                                        .map(LDLib2.GSON::toJson)
                                        .map(s -> s.split("\n"))
                                        .result().orElseGet(() -> new String[0]),
                                lines -> {
                            setter.accept(FluidIngredient.CODEC.parse(op, JsonParser.parseString(String.join("\n", lines)))
                                    .result()
                                    .orElse(FluidIngredient.empty()));
                                }, """
                                {
                                  "type": "neoforge:compound",
                                  "ingredients": [
                                     { "fluid": "minecraft:lava" },
                                     { "fluid": "minecraft:water" }
                                  ]
                                }
                                """.split("\n"), true));
                    }
                }));
    }

    public FluidIngredientType getType(FluidIngredient ingredient) {
        // if JSON, then keep JSON.
        if (currentType != null && currentType == FluidIngredientType.JSON) return currentType;
        if (ingredient instanceof SingleFluidIngredient) return FluidIngredientType.SINGLE;
        if (ingredient instanceof TagFluidIngredient) return FluidIngredientType.TAG;
        return FluidIngredientType.JSON;
    }

    public FluidIngredient createDefault(FluidIngredientType type) {
        if (type == FluidIngredientType.SINGLE) return FluidIngredient.single(Fluids.WATER);
        if (type == FluidIngredientType.TAG) return TagFluidIngredient.tag(FluidTags.WATER);
        return FluidIngredient.empty();
    }
}
