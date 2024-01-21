package com.lowdragmc.mbd2.api.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.utils.NBTToJsonConverter;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.api.recipe.ingredient.FluidIngredient;
import com.lowdragmc.mbd2.api.recipe.ingredient.SizedIngredient;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.capability.recipe.FluidRecipeCapability;
import com.lowdragmc.mbd2.common.capability.recipe.ItemRecipeCapability;
import com.lowdragmc.mbd2.common.recipe.*;
import com.lowdragmc.mbd2.utils.TagUtil;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true, fluent = true)
public class MBDRecipeBuilder {
    public final Map<RecipeCapability<?>, List<Content>> input = new HashMap<>();
    public final Map<RecipeCapability<?>, List<Content>> tickInput = new HashMap<>();
    public final Map<RecipeCapability<?>, List<Content>> output = new HashMap<>();
    public final Map<RecipeCapability<?>, List<Content>> tickOutput = new HashMap<>();
    public CompoundTag data = new CompoundTag();
    public final List<RecipeCondition> conditions = new ArrayList<>();
    @Setter
    public ResourceLocation id;
    @Setter
    public MBDRecipeType recipeType;
    @Setter
    public int duration = 100;
    @Setter
    public boolean perTick;
    @Setter
    public String slotName;
    @Setter
    public String uiName;
    @Setter
    public float chance = 1;
    @Setter
    public float tierChanceBoost = 0;
    @Setter
    public boolean isFuel = false;
    @Setter
    public BiConsumer<MBDRecipeBuilder, Consumer<FinishedRecipe>> onSave;

    public MBDRecipeBuilder(ResourceLocation id, MBDRecipeType recipeType) {
        this.id = id;
        this.recipeType = recipeType;
    }

    public MBDRecipeBuilder(MBDRecipe toCopy, MBDRecipeType recipeType) {
        this.id = toCopy.id;
        this.recipeType = recipeType;
        toCopy.inputs.forEach((k, v) -> this.input.put(k, new ArrayList<>(v)));
        toCopy.outputs.forEach((k, v) -> this.output.put(k, new ArrayList<>(v)));
        toCopy.tickInputs.forEach((k, v) -> this.tickInput.put(k, new ArrayList<>(v)));
        toCopy.tickOutputs.forEach((k, v) -> this.tickOutput.put(k, new ArrayList<>(v)));
        this.conditions.addAll(toCopy.conditions);
        this.data = toCopy.data.copy();
        this.duration = toCopy.duration;
        this.isFuel = toCopy.isFuel;
    }

    public static MBDRecipeBuilder of(ResourceLocation id, MBDRecipeType recipeType) {
        return new MBDRecipeBuilder(id, recipeType);
    }

    public static MBDRecipeBuilder ofRaw() {
        return new MBDRecipeBuilder(MBD2.id("raw"), null);
    }

    public MBDRecipeBuilder copy(String id) {
        return copy(MBD2.id(id));
    }

    public MBDRecipeBuilder copy(ResourceLocation id) {
        MBDRecipeBuilder copy = new MBDRecipeBuilder(id, this.recipeType);
        this.input.forEach((k, v) -> copy.input.put(k, new ArrayList<>(v)));
        this.output.forEach((k, v) -> copy.output.put(k, new ArrayList<>(v)));
        this.tickInput.forEach((k, v) -> copy.tickInput.put(k, new ArrayList<>(v)));
        this.tickOutput.forEach((k, v) -> copy.tickOutput.put(k, new ArrayList<>(v)));
        copy.conditions.addAll(this.conditions);
        copy.data = this.data.copy();
        copy.duration = this.duration;
        copy.chance = this.chance;
        copy.perTick = this.perTick;
        copy.isFuel = this.isFuel;
        copy.uiName = this.uiName;
        copy.slotName = this.slotName;
        copy.onSave = this.onSave;
        return copy;
    }

    public MBDRecipeBuilder copyFrom(MBDRecipeBuilder builder) {
        return builder.copy(builder.id).onSave(null).recipeType(recipeType);
    }

    public <T> MBDRecipeBuilder input(RecipeCapability<T> capability, T... obj) {
        (perTick ? tickInput : input).computeIfAbsent(capability, c -> new ArrayList<>()).addAll(Arrays.stream(obj)
                .map(capability::of)
                .map(o -> new Content(o, chance, tierChanceBoost, slotName, uiName)).toList());
        return this;
    }

    public <T> MBDRecipeBuilder output(RecipeCapability<T> capability, T... obj) {
        (perTick ? tickOutput : output).computeIfAbsent(capability, c -> new ArrayList<>()).addAll(Arrays.stream(obj)
                .map(capability::of)
                .map(o -> new Content(o, chance, tierChanceBoost, slotName, uiName)).toList());
        return this;
    }

    public <T> MBDRecipeBuilder inputs(RecipeCapability<T> capability, Object... obj) {
        (perTick ? tickInput : input).computeIfAbsent(capability, c -> new ArrayList<>()).addAll(Arrays.stream(obj)
                .map(capability::of)
                .map(o -> new Content(o, chance, tierChanceBoost, slotName, uiName)).toList());
        return this;
    }

    public <T> MBDRecipeBuilder outputs(RecipeCapability<T> capability, Object... obj) {
        (perTick ? tickOutput : output).computeIfAbsent(capability, c -> new ArrayList<>()).addAll(Arrays.stream(obj)
                .map(capability::of)
                .map(o -> new Content(o, chance, tierChanceBoost, slotName, uiName)).toList());
        return this;
    }

    public MBDRecipeBuilder addCondition(RecipeCondition condition) {
        conditions.add(condition);
        return this;
    }


    public MBDRecipeBuilder inputItems(Ingredient... inputs) {
        return input(ItemRecipeCapability.CAP, inputs);
    }

    public MBDRecipeBuilder inputItems(ItemStack... inputs) {
        for (ItemStack itemStack : inputs) {
            if (itemStack.isEmpty()) {
                LDLib.LOGGER.error("gt recipe {} input items is empty", id);
                throw new IllegalArgumentException(id + ": input items is empty");
            }
        }
        return input(ItemRecipeCapability.CAP, Arrays.stream(inputs).map(SizedIngredient::create).toArray(Ingredient[]::new));
    }

    public MBDRecipeBuilder inputItems(TagKey<Item> tag, int amount) {
        return inputItems(SizedIngredient.create(tag, amount));
    }

    public MBDRecipeBuilder inputItems(TagKey<Item> tag) {
        return inputItems(tag, 1);
    }

    public MBDRecipeBuilder inputItems(Item input, int amount) {
        return inputItems(new ItemStack(input, amount));
    }

    public MBDRecipeBuilder inputItems(Item input) {
        return inputItems(SizedIngredient.create(new ItemStack(input)));
    }

    public MBDRecipeBuilder inputItems(Supplier<? extends Item> input) {
        return inputItems(input.get());
    }

    public MBDRecipeBuilder inputItems(Supplier<? extends Item> input, int amount) {
        return inputItems(new ItemStack(input.get(), amount));
    }


    // for kjs
    public MBDRecipeBuilder itemOutputs(ItemStack... outputs) {
        return outputItems(outputs);
    }

    public MBDRecipeBuilder outputItems(ItemStack... outputs) {
        for (ItemStack itemStack : outputs) {
            if (itemStack.isEmpty()) {
                LDLib.LOGGER.error("gt recipe {} output items is empty", id);
                throw new IllegalArgumentException(id + ": output items is empty");
            }
        }
        return output(ItemRecipeCapability.CAP, Arrays.stream(outputs).map(SizedIngredient::create).toArray(Ingredient[]::new));
    }

    public MBDRecipeBuilder outputItems(Item input, int amount) {
        return outputItems(new ItemStack(input, amount));
    }

    public MBDRecipeBuilder outputItems(Item input) {
        return outputItems(new ItemStack(input));
    }

    public MBDRecipeBuilder outputItems(Supplier<? extends ItemLike> input) {
        return outputItems(new ItemStack(input.get().asItem()));
    }

    public MBDRecipeBuilder outputItems(Supplier<? extends ItemLike> input, int amount) {
        return outputItems(new ItemStack(input.get().asItem(), amount));
    }

    public MBDRecipeBuilder notConsumable(ItemStack itemStack) {
        float lastChance = this.chance;
        this.chance = 0;
        inputItems(itemStack);
        this.chance = lastChance;
        return this;
    }
    
    public MBDRecipeBuilder notConsumable(Item item) {
        float lastChance = this.chance;
        this.chance = 0;
        inputItems(item);
        this.chance = lastChance;
        return this;
    }

    public MBDRecipeBuilder notConsumable(Supplier<? extends Item> item) {
        float lastChance = this.chance;
        this.chance = 0;
        inputItems(item);
        this.chance = lastChance;
        return this;
    }
    

    public MBDRecipeBuilder inputFluids(FluidStack... inputs) {
        return input(FluidRecipeCapability.CAP, Arrays.stream(inputs).map(fluid -> {
            if (!Platform.isForge() && fluid.getFluid() == Fluids.WATER) { // Special case for fabric, because there all fluids have to be tagged as water to function as water when placed.
                return FluidIngredient.of(fluid);
            } else {
                return FluidIngredient.of(TagUtil.createFluidTag(BuiltInRegistries.FLUID.getKey(fluid.getFluid()).getPath()), fluid.getAmount());
            }
        }).toArray(FluidIngredient[]::new));
    }

    public MBDRecipeBuilder inputFluids(FluidIngredient... inputs) {
        return input(FluidRecipeCapability.CAP, inputs);
    }

    public MBDRecipeBuilder outputFluids(FluidStack... outputs) {
        return output(FluidRecipeCapability.CAP, Arrays.stream(outputs).map(FluidIngredient::of).toArray(FluidIngredient[]::new));
    }

    public MBDRecipeBuilder outputFluids(FluidIngredient... outputs) {
        return output(FluidRecipeCapability.CAP, outputs);
    }

    //////////////////////////////////////
    //**********     DATA    ***********//
    //////////////////////////////////////
    public MBDRecipeBuilder addData(String key, Tag data) {
        this.data.put(key, data);
        return this;
    }

    public MBDRecipeBuilder addData(String key, int data) {
        this.data.putInt(key, data);
        return this;
    }

    public MBDRecipeBuilder addData(String key, long data) {
        this.data.putLong(key, data);
        return this;
    }

    public MBDRecipeBuilder addData(String key, String data) {
        this.data.putString(key, data);
        return this;
    }

    public MBDRecipeBuilder addData(String key, Float data) {
        this.data.putFloat(key, data);
        return this;
    }

    public MBDRecipeBuilder addData(String key, boolean data) {
        this.data.putBoolean(key, data);
        return this;
    }

    public MBDRecipeBuilder blastFurnaceTemp(int blastTemp) {
        return addData("ebf_temp", blastTemp);
    }

    public MBDRecipeBuilder explosivesAmount(int explosivesAmount) {
        return addData("explosives_amount", explosivesAmount);
    }

    public MBDRecipeBuilder explosivesType(ItemStack explosivesType) {
        return addData("explosives_type", explosivesType.save(new CompoundTag()));
    }

    public MBDRecipeBuilder solderMultiplier(int multiplier) {
        return addData("solderMultiplier", multiplier);
    }

    public MBDRecipeBuilder disableDistilleryRecipes(boolean flag) {
        return addData("disable_distillery", flag);
    }

    public MBDRecipeBuilder fusionStartEU(long eu) {
        return addData("eu_to_start", eu);
    }

    //////////////////////////////////////
    //*******     CONDITIONS    ********//
    //////////////////////////////////////

    public MBDRecipeBuilder dimension(ResourceLocation dimension, boolean reverse) {
        return addCondition(new DimensionCondition(dimension).setReverse(reverse));
    }

    public MBDRecipeBuilder dimension(ResourceLocation dimension) {
        return dimension(dimension, false);
    }

    public MBDRecipeBuilder biome(ResourceLocation biome, boolean reverse) {
        return addCondition(new BiomeCondition(biome).setReverse(reverse));
    }

    public MBDRecipeBuilder biome(ResourceLocation biome) {
        return biome(biome, false);
    }

    public MBDRecipeBuilder rain(float minLevel, float maxLevel, boolean reverse) {
        return addCondition(new RainingCondition(minLevel, maxLevel).setReverse(reverse));
    }

    public MBDRecipeBuilder rain(float minLevel, float maxLevel) {
        return rain(minLevel, maxLevel, false);
    }

    public MBDRecipeBuilder thunder(float minLevel, float maxLevel, boolean reverse) {
        return addCondition(new ThunderCondition(minLevel, maxLevel).setReverse(reverse));
    }

    public MBDRecipeBuilder thunder(float minLevel, float maxLevel) {
        return thunder(minLevel, maxLevel, false);
    }

    public MBDRecipeBuilder posY(int min, int max, boolean reverse) {
        return addCondition(new PositionYCondition(min, max).setReverse(reverse));
    }

    public MBDRecipeBuilder posY(int min, int max) {
        return posY(min, max, false);
    }


    public void toJson(JsonObject json) {
        json.addProperty("type", recipeType.registryName.toString());
        json.addProperty("duration", Math.abs(duration));
        if (data != null && !data.isEmpty()) {
            json.add("data", NBTToJsonConverter.getObject(data));
        }
        json.add("inputs", capabilitiesToJson(input));
        json.add("outputs", capabilitiesToJson(output));
        json.add("tickInputs", capabilitiesToJson(tickInput));
        json.add("tickOutputs", capabilitiesToJson(tickOutput));
        if (!conditions.isEmpty()) {
            JsonArray array = new JsonArray();
            for (RecipeCondition condition : conditions) {
                JsonObject cond = new JsonObject();
                cond.addProperty("type", MBDRegistries.RECIPE_CONDITIONS.getKey(condition.getClass()));
                cond.add("data", condition.serialize());
                array.add(cond);
            }
            json.add("recipeConditions", array);
        }
        if (isFuel) {
            json.addProperty("isFuel", true);
        }
    }

    public JsonObject capabilitiesToJson(Map<RecipeCapability<?>, List<Content>> contents) {
        JsonObject jsonObject = new JsonObject();
        contents.forEach((cap, list) -> {
            JsonArray contentsJson = new JsonArray();
            for (Content content : list) {
                contentsJson.add(cap.serializer.toJsonContent(content));
            }
            jsonObject.add(MBDRegistries.RECIPE_CAPABILITIES.getKey(cap), contentsJson);
        });
        return jsonObject;
    }

    public FinishedRecipe build() {
        return new FinishedRecipe() {
            @Override
            public void serializeRecipeData(JsonObject pJson) {
                toJson(pJson);
            }

            @Override
            public ResourceLocation getId() {
                return new ResourceLocation(id.getNamespace(), recipeType.registryName.getPath() + "/" + id.getPath());
            }

            @Override
            public RecipeSerializer<?> getType() {
                return MBDRecipeSerializer.SERIALIZER;
            }

            @Nullable
            @Override
            public JsonObject serializeAdvancement() {
                return null;
            }

            @Nullable
            @Override
            public ResourceLocation getAdvancementId() {
                return null;
            }
        };
    }

    public void save(Consumer<FinishedRecipe> consumer) {
        if (onSave != null) {
            onSave.accept(this, consumer);
        }
        consumer.accept(build());
    }

    public MBDRecipe buildRawRecipe() {
        return new MBDRecipe(recipeType, id, input, output, tickInput, tickOutput, conditions, data, duration, isFuel);
    }

}
