package com.lowdragmc.mbd2.integration.mekanism.configurator;

import com.google.gson.JsonParser;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorSelectorConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.TextAreaConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ValueConfigurator;
import com.mojang.serialization.JsonOps;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.recipes.ingredients.chemical.ChemicalIngredient;
import mekanism.api.recipes.ingredients.chemical.SingleChemicalIngredient;
import mekanism.api.recipes.ingredients.chemical.TagChemicalIngredient;
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import mekanism.common.registries.MekanismChemicals;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ChemicalIngredientConfigurator extends ValueConfigurator<ChemicalIngredient> {
    public enum ChemicalIngredientType {
        SINGLE,
        TAG,
        JSON,
    }

    @Nullable
    private ChemicalIngredientType currentType;

    public ChemicalIngredientConfigurator(String name,
                                          Supplier<ChemicalIngredient> getter,
                                          Consumer<ChemicalIngredient> setter,
                                          @NotNull ChemicalIngredient defaultValue,
                                          boolean forceUpdate) {
        super(name, getter, setter, defaultValue, forceUpdate);
        if (value == null) value = defaultValue;
        inlineContainer.addChild(new ConfiguratorSelectorConfigurator<>("recipe.capability.mek_chemical.ingredient.type",
                () -> getType(getter.get()),
                type -> setter.accept(createDefault(type)),
                ChemicalIngredientType.SINGLE,
                true,
                Arrays.stream(ChemicalIngredientType.values()).toList(),
                Enum::name,
                (type, group) -> {
                    currentType = type;
                    if (type == ChemicalIngredientType.SINGLE) {
                        group.addConfigurators(new SingleChemicalIngredientConfigurator("",
                                () -> getter.get() instanceof SingleChemicalIngredient single ? single
                                        : (SingleChemicalIngredient) IngredientCreatorAccess.chemical().of(MekanismChemicals.HYDROGEN),
                                setter::accept,
                                (SingleChemicalIngredient) IngredientCreatorAccess.chemical().of(MekanismChemicals.HYDROGEN),
                                true));
                    } else if (type == ChemicalIngredientType.TAG) {
                        var defaultTag = TagKey.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mekanism", "hydrogen"));
                        group.addConfigurators(new TagChemicalIngredientConfigurator("",
                                () -> getter.get() instanceof TagChemicalIngredient tag ? tag : new TagChemicalIngredient(defaultTag),
                                setter::accept,
                                new TagChemicalIngredient(defaultTag),
                                true));
                    } else if (type == ChemicalIngredientType.JSON) {
                        var op = Platform.getFrozenRegistry().createSerializationContext(JsonOps.INSTANCE);
                        var codec = IngredientCreatorAccess.chemical().codec();
                        group.addConfigurator(new TextAreaConfigurator("",
                                () -> codec.encodeStart(op, getter.get())
                                        .map(LDLib2.GSON::toJson)
                                        .map(s -> s.split("\n"))
                                        .result().orElseGet(() -> new String[0]),
                                lines -> {
                                    var text = String.join("\n", lines);
                                    if (text.isBlank()) return;
                                    try {
                                        var json = JsonParser.parseString(text);
                                        codec.parse(op, json).result().ifPresent(setter);
                                    } catch (Exception ignored) {
                                        // partial / invalid JSON while user is typing — keep their text, don't reset
                                    }
                                },
                                """
                                {
                                  "chemical": "mekanism:hydrogen"
                                }
                                """.split("\n"), false));
                    }
                }));
    }

    public ChemicalIngredientType getType(ChemicalIngredient ingredient) {
        if (currentType != null && currentType == ChemicalIngredientType.JSON) return currentType;
        if (ingredient instanceof SingleChemicalIngredient) return ChemicalIngredientType.SINGLE;
        if (ingredient instanceof TagChemicalIngredient) return ChemicalIngredientType.TAG;
        return ChemicalIngredientType.JSON;
    }

    public ChemicalIngredient createDefault(ChemicalIngredientType type) {
        if (type == ChemicalIngredientType.SINGLE) {
            return IngredientCreatorAccess.chemical().of(MekanismChemicals.HYDROGEN);
        }
        if (type == ChemicalIngredientType.TAG) {
            return new TagChemicalIngredient(TagKey.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mekanism", "hydrogen")));
        }
        return IngredientCreatorAccess.chemical().empty();
    }
}

