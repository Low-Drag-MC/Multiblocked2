package com.lowdragmc.mbd2.api.recipe;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.common.capability.recipe.FluidRecipeCapability;
import com.lowdragmc.mbd2.common.capability.recipe.ItemRecipeCapability;
import com.lowdragmc.mbd2.common.recipe.*;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true, fluent = true)
public class MBDRecipeBuilder {
    public final Map<RecipeCapability<?>, List<Content>> input = new HashMap<>();
    public final Map<RecipeCapability<?>, List<Content>> output = new HashMap<>();
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
    public boolean isXEIHidden = false;
    @Setter
    public int priority = 0;
    @Setter
    public BiConsumer<MBDRecipeBuilder, RecipeOutput> onSave;

    public MBDRecipeBuilder(ResourceLocation id, MBDRecipeType recipeType) {
        this.id = id;
        this.recipeType = recipeType;
    }

    public MBDRecipeBuilder(MBDRecipe toCopy, MBDRecipeType recipeType) {
        this.id = toCopy.id;
        this.recipeType = recipeType;
        toCopy.inputs.forEach((k, v) -> this.input.put(k, new ArrayList<>(v)));
        toCopy.outputs.forEach((k, v) -> this.output.put(k, new ArrayList<>(v)));
        this.conditions.addAll(toCopy.conditions);
        this.data = toCopy.data.copy();
        this.duration = toCopy.duration;
        this.isFuel = toCopy.isFuel;
        this.isXEIHidden = toCopy.isXEIHidden;
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
        input.computeIfAbsent(capability, c -> new ArrayList<>()).addAll(Arrays.stream(obj)
                .map(capability::of)
                .map(o -> new Content(o, perTick, chance, tierChanceBoost, slotName, uiName)).toList());
        return this;
    }

    public <T> MBDRecipeBuilder output(RecipeCapability<T> capability, T... obj) {
        output.computeIfAbsent(capability, c -> new ArrayList<>()).addAll(Arrays.stream(obj)
                .map(capability::of)
                .map(o -> new Content(o, perTick, chance, tierChanceBoost, slotName, uiName)).toList());
        return this;
    }

    public <T> MBDRecipeBuilder removeInputs(RecipeCapability<T> capability) {
        input.remove(capability);
        return this;
    }

    public <T> MBDRecipeBuilder removeOutputs(RecipeCapability<T> capability) {
        output.remove(capability);
        return this;
    }

    public <T> MBDRecipeBuilder inputs(RecipeCapability<T> capability, Object... obj) {
        input.computeIfAbsent(capability, c -> new ArrayList<>()).addAll(Arrays.stream(obj)
                .map(capability::of)
                .map(o -> new Content(o, perTick, chance, tierChanceBoost, slotName, uiName)).toList());
        return this;
    }

    public <T> MBDRecipeBuilder outputs(RecipeCapability<T> capability, Object... obj) {
        output.computeIfAbsent(capability, c -> new ArrayList<>()).addAll(Arrays.stream(obj)
                .map(capability::of)
                .map(o -> new Content(o, perTick, chance, tierChanceBoost, slotName, uiName)).toList());
        return this;
    }

    public MBDRecipeBuilder addCondition(RecipeCondition condition) {
        conditions.add(condition);
        return this;
    }

    public MBDRecipeBuilder inputItems(SizedIngredient... inputs) {
        return input(ItemRecipeCapability.CAP, inputs);
    }

    public MBDRecipeBuilder inputItems(Ingredient... inputs) {
        return inputItems(Arrays.stream(inputs).map(ingredient -> new SizedIngredient(ingredient,1)).toArray(SizedIngredient[]::new));
    }

    public MBDRecipeBuilder inputItems(ItemStack... inputs) {
        for (ItemStack itemStack : inputs) {
            if (itemStack.isEmpty()) {
                LDLib2.LOGGER.error("gt recipe {} input items is empty", id);
                throw new IllegalArgumentException(id + ": input items is empty");
            }
        }
        return input(ItemRecipeCapability.CAP, Arrays.stream(inputs).map(stack -> SizedIngredient.of(stack.getItem(), stack.getCount())).toArray(SizedIngredient[]::new));
    }

    public MBDRecipeBuilder inputItems(TagKey<Item> tag, int amount) {
        return inputItems(SizedIngredient.of(tag, amount));
    }

    public MBDRecipeBuilder inputItems(TagKey<Item> tag) {
        return inputItems(tag, 1);
    }

    public MBDRecipeBuilder inputItems(Item input, int amount) {
        return inputItems(new ItemStack(input, amount));
    }

    public MBDRecipeBuilder inputItems(Item input) {
        return inputItems(new ItemStack(input));
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
                LDLib2.LOGGER.error("gt recipe {} output items is empty", id);
                throw new IllegalArgumentException(id + ": output items is empty");
            }
        }
        return output(ItemRecipeCapability.CAP, Arrays.stream(outputs).map(stack -> SizedIngredient.of(stack.getItem(), stack.getCount())).toArray(SizedIngredient[]::new));
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
        return input(FluidRecipeCapability.CAP, Arrays.stream(inputs).map(fluid ->
                new SizedFluidIngredient(FluidIngredient.of(fluid.getFluid()), fluid.getAmount())
        ).toArray(SizedFluidIngredient[]::new));
    }

    public MBDRecipeBuilder inputFluids(SizedFluidIngredient... inputs) {
        return input(FluidRecipeCapability.CAP, inputs);
    }

    public MBDRecipeBuilder inputFluids(FluidIngredient... inputs) {
        return inputFluids(Arrays.stream(inputs).map(ingredient -> new SizedFluidIngredient(ingredient,1000)).toArray(SizedFluidIngredient[]::new));
    }

    public MBDRecipeBuilder outputFluids(FluidStack... outputs) {
        return output(FluidRecipeCapability.CAP, Arrays.stream(outputs).map(SizedFluidIngredient::of).toArray(SizedFluidIngredient[]::new));
    }

    public MBDRecipeBuilder outputFluids(SizedFluidIngredient... outputs) {
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

    public void save(RecipeOutput consumer) {
        if (onSave != null) {
            onSave.accept(this, consumer);
        }
        var location = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), recipeType.getRegistryName().getPath() + "/" + id.getPath());
        consumer.accept(location, buildRawRecipe(), null);
    }

    public MBDRecipe saveAsBuiltinRecipe() {
        MBDRecipe recipe = buildRawRecipe();
        recipeType.builtinRecipes.put(id, recipe);
        return recipe;
    }

    public MBDRecipe buildRawRecipe() {
        return new MBDRecipe(recipeType, id, input, output, conditions, data, duration, isFuel, isXEIHidden, priority);
    }

}
