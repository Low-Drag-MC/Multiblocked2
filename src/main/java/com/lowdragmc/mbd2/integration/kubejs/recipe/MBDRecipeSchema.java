package com.lowdragmc.mbd2.integration.kubejs.recipe;

import com.google.gson.JsonElement;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeSerializer;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.api.recipe.ingredient.FluidIngredient;
import com.lowdragmc.mbd2.api.recipe.ingredient.SizedIngredient;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.capability.recipe.FluidRecipeCapability;
import com.lowdragmc.mbd2.common.capability.recipe.ForgeEnergyRecipeCapability;
import com.lowdragmc.mbd2.common.capability.recipe.ItemRecipeCapability;
import com.lowdragmc.mbd2.common.recipe.*;
import com.lowdragmc.mbd2.integration.botania.BotaniaManaRecipeCapability;
import com.lowdragmc.mbd2.integration.create.CreateStressRecipeCapability;
import com.lowdragmc.mbd2.integration.gtm.GTMEnergyRecipeCapability;
import com.lowdragmc.mbd2.integration.mekanism.MekanismChemicalRecipeCapability;
import com.lowdragmc.mbd2.integration.mekanism.MekanismHeatRecipeCapability;
import dev.latvian.mods.kubejs.fluid.FluidLike;
import dev.latvian.mods.kubejs.fluid.FluidStackJS;
import dev.latvian.mods.kubejs.fluid.InputFluid;
import dev.latvian.mods.kubejs.fluid.OutputFluid;
import dev.latvian.mods.kubejs.item.InputItem;
import dev.latvian.mods.kubejs.recipe.RecipeJS;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.kubejs.util.ListJS;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.*;
import java.util.function.Consumer;

public interface MBDRecipeSchema {
    RecipeSchema SCHEMA = new RecipeSchema(MBDRecipeJS.class, MBDRecipeJS::new);

    @Getter
    @Accessors(chain = true, fluent = true)
    class MBDRecipeJS extends RecipeJS {
        @FunctionalInterface
        public interface RecipeBuilder extends Consumer<MBDRecipeJS> { }
        public final Map<RecipeCapability<?>, List<Content>> inputs = new LinkedHashMap<>();
        public final Map<RecipeCapability<?>, List<Content>> outputs = new LinkedHashMap<>();
        public final List<RecipeCondition> conditions = new ArrayList<>();
        public CompoundTag data = new CompoundTag();
        public int duration = 100;
        public int priority;
        public boolean isFuel;
        // runtime
        public boolean perTick;
        @Setter
        public String slotName;
        @Setter
        public String uiName;
        @Setter
        public float chance = 1;
        @Setter
        public float tierChanceBoost = 0;

        //////////////// misc ////////////////
        public MBDRecipeJS duration(int duration) {
            this.duration = duration;
            save();
            return this;
        }

        public MBDRecipeJS priority(int priority) {
            this.priority = priority;
            save();
            return this;
        }

        public MBDRecipeJS isFuel(boolean fuel) {
            isFuel = fuel;
            save();
            return this;
        }

        //////////////// data ////////////////
        public MBDRecipeJS addData(String key, Tag data) {
            this.data.put(key, data);
            save();
            return this;
        }

        public MBDRecipeJS addDataString(String key, String data) {
            this.data.putString(key, data);
            save();
            return this;
        }

        public MBDRecipeJS addDataNumber(String key, double number) {
            this.data.putDouble(key, number);
            save();
            return this;
        }

        public MBDRecipeJS addDataBoolean(String key, boolean bool) {
            this.data.putBoolean(key, bool);
            save();
            return this;
        }

        //////////////// state machine ////////////////
        public MBDRecipeJS perTick(boolean perTick) {
            this.perTick = perTick;
            return this;
        }

        public MBDRecipeJS perTick(RecipeBuilder builder) {
            var lastPerTick = this.perTick;
            this.perTick = true;
            builder.accept(this);
            this.perTick = lastPerTick;
            return this;
        }

        public MBDRecipeJS chance(float chance, RecipeBuilder builder) {
            var lastChance = this.chance;
            this.chance = chance;
            builder.accept(this);
            this.chance = chance;
            return this;
        }

        public MBDRecipeJS tierChanceBoost(float tierChanceBoost, RecipeBuilder builder) {
            var lastTierChanceBoost = this.tierChanceBoost;
            this.tierChanceBoost = tierChanceBoost;
            builder.accept(this);
            this.tierChanceBoost = lastTierChanceBoost;
            return this;
        }

        public MBDRecipeJS slotName(String slotName, RecipeBuilder builder) {
            var lastSlotName = this.slotName;
            this.slotName = slotName;
            builder.accept(this);
            this.slotName = lastSlotName;
            return this;
        }

        public MBDRecipeJS uiName(String uiName, RecipeBuilder builder) {
            var lastUiName = this.uiName;
            this.uiName = uiName;
            builder.accept(this);
            this.uiName = lastUiName;
            return this;
        }


        //////////////// ingredients ////////////////
        public MBDRecipeJS inputs(RecipeCapability<?> capability, Object... obj) {
            inputs.computeIfAbsent(capability, c -> new ArrayList<>()).addAll(Arrays.stream(obj)
                    .map(capability::of)
                    .map(o -> new Content(o, perTick, chance, tierChanceBoost, slotName, uiName)).toList());
            save();
            return this;
        }

        public MBDRecipeJS outputs(RecipeCapability<?> capability, Object... obj) {
            outputs.computeIfAbsent(capability, c -> new ArrayList<>()).addAll(Arrays.stream(obj)
                    .map(capability::of)
                    .map(o -> new Content(o, perTick, chance, tierChanceBoost, slotName, uiName)).toList());
            save();
            return this;
        }

        public MBDRecipeJS inputItems(InputItem... items) {
            return inputs(ItemRecipeCapability.CAP, Arrays.stream(items).map(item -> SizedIngredient.create(item.ingredient, item.count)).toArray());
        }

        public MBDRecipeJS outputItems(InputItem... items) {
            return outputs(ItemRecipeCapability.CAP, Arrays.stream(items).map(item -> SizedIngredient.create(item.ingredient, item.count)).toArray());
        }
        
        public MBDRecipeJS inputFluids(FluidIngredientJS... fluids) {
            return inputs(FluidRecipeCapability.CAP, Arrays.stream(fluids).map(FluidIngredientJS::ingredient).toArray());
        }

        public MBDRecipeJS outputFluids(FluidIngredientJS... fluids) {
            return outputs(FluidRecipeCapability.CAP, Arrays.stream(fluids).map(FluidIngredientJS::ingredient).toArray());
        }

        public MBDRecipeJS inputFE(int energy) {
            return inputs(ForgeEnergyRecipeCapability.CAP, energy);
        }

        public MBDRecipeJS outputFE(int energy) {
            return outputs(ForgeEnergyRecipeCapability.CAP, energy);
        }

        public MBDRecipeJS inputMana(int mana) {
            if (!MBD2.isBotaniaLoaded()) {
                throw new IllegalStateException("Try to add a mana ingredient while the botania is not loaded!");
            }
            return inputs(BotaniaManaRecipeCapability.CAP, mana);
        }

        public MBDRecipeJS outputMana(int mana) {
            if (!MBD2.isBotaniaLoaded()) {
                throw new IllegalStateException("Try to add a mana ingredient while the botania is not loaded!");
            }
            return outputs(BotaniaManaRecipeCapability.CAP, mana);
        }

        public MBDRecipeJS inputHeat(double heat) {
            if (!MBD2.isMekanismLoaded()) {
                throw new IllegalStateException("Try to add a heat ingredient while the mekanism is not loaded!");
            }
            return inputs(MekanismHeatRecipeCapability.CAP, heat);
        }

        public MBDRecipeJS outputHeat(double heat) {
            if (!MBD2.isMekanismLoaded()) {
                throw new IllegalStateException("Try to add a heat ingredient while the mekanism is not loaded!");
            }
            return outputs(MekanismHeatRecipeCapability.CAP, heat);
        }

        public MBDRecipeJS inputEU(long eu) {
            if (!MBD2.isGTMLoaded()) {
                throw new IllegalStateException("Try to add a eu ingredient while the gtceu is not loaded!");
            }
            return outputs(GTMEnergyRecipeCapability.CAP, eu);
        }

        public MBDRecipeJS outputEU(long eu) {
            if (!MBD2.isGTMLoaded()) {
                throw new IllegalStateException("Try to add a eu ingredient while the gtceu is not loaded!");
            }
            return outputs(GTMEnergyRecipeCapability.CAP, eu);
        }

        public MBDRecipeJS inputStress(float stress) {
            if (!MBD2.isCreateLoaded()) {
                throw new IllegalStateException("Try to add a stress ingredient while the create is not loaded!");
            }
            return inputs(CreateStressRecipeCapability.CAP, stress);
        }

        public MBDRecipeJS outputStress(float stress) {
            if (!MBD2.isCreateLoaded()) {
                throw new IllegalStateException("Try to add a stress ingredient while the create is not loaded!");
            }
            return outputs(CreateStressRecipeCapability.CAP, stress);
        }

        public MBDRecipeJS inputGases(String... stack) {
            if (!MBD2.isMekanismLoaded()) {
                throw new IllegalStateException("Try to add a gas ingredient while the mekanism is not loaded!");
            }
            return inputs(MekanismChemicalRecipeCapability.CAP_GAS, (Object[]) stack);
        }

        public MBDRecipeJS outputGases(String... stack) {
            if (!MBD2.isMekanismLoaded()) {
                throw new IllegalStateException("Try to add a gas ingredient while the mekanism is not loaded!");
            }
            return outputs(MekanismChemicalRecipeCapability.CAP_GAS, (Object[]) stack);
        }

        public MBDRecipeJS inputSlurries(String... stack) {
            if (!MBD2.isMekanismLoaded()) {
                throw new IllegalStateException("Try to add a slurry ingredient while the mekanism is not loaded!");
            }
            return inputs(MekanismChemicalRecipeCapability.CAP_SLURRY, (Object[]) stack);
        }

        public MBDRecipeJS outputSlurries(String... stack) {
            if (!MBD2.isMekanismLoaded()) {
                throw new IllegalStateException("Try to add a slurry ingredient while the mekanism is not loaded!");
            }
            return outputs(MekanismChemicalRecipeCapability.CAP_SLURRY, (Object[]) stack);
        }

        public MBDRecipeJS inputInfusions(String... stack) {
            if (!MBD2.isMekanismLoaded()) {
                throw new IllegalStateException("Try to add a infuse type ingredient while the mekanism is not loaded!");
            }
            return inputs(MekanismChemicalRecipeCapability.CAP_INFUSE, (Object[]) stack);
        }

        public MBDRecipeJS outputInfusions(String... stack) {
            if (!MBD2.isMekanismLoaded()) {
                throw new IllegalStateException("Try to add a infuse type ingredient while the mekanism is not loaded!");
            }
            return outputs(MekanismChemicalRecipeCapability.CAP_INFUSE, (Object[]) stack);
        }

        public MBDRecipeJS inputPigments(String... stack) {
            if (!MBD2.isMekanismLoaded()) {
                throw new IllegalStateException("Try to add a pigment ingredient while the mekanism is not loaded!");
            }
            return inputs(MekanismChemicalRecipeCapability.CAP_PIGMENT, (Object[]) stack);
        }

        public MBDRecipeJS outputPigments(String... stack) {
            if (!MBD2.isMekanismLoaded()) {
                throw new IllegalStateException("Try to add a pigment ingredient while the mekanism is not loaded!");
            }
            return outputs(MekanismChemicalRecipeCapability.CAP_PIGMENT, (Object[]) stack);
        }

        //////////////// condition ////////////////
        public MBDRecipeJS addCondition(RecipeCondition condition) {
            conditions.add(condition);
            save();
            return this;
        }

        public MBDRecipeJS dimension(ResourceLocation dimension) {
            addCondition(new DimensionCondition(dimension));
            return this;
        }

        public MBDRecipeJS biome(ResourceLocation biome) {
            addCondition(new BiomeCondition(biome));
            return this;
        }

        public MBDRecipeJS machineLevel(int level) {
            addCondition(new MachineLevelCondition(level));
            return this;
        }

        public MBDRecipeJS positionY(int min, int max) {
            addCondition(new PositionYCondition(min, max));
            return this;
        }

        public MBDRecipeJS raining(int min, int max) {
            addCondition(new RainingCondition(min, max));
            return this;
        }

        public MBDRecipeJS thundering(int min, int max) {
            addCondition(new ThunderCondition(min, max));
            return this;
        }

        public MBDRecipeJS blocksInStructure(int min, int max, Block... blocks) {
            addCondition(new BlockCondition(min, max, blocks));
            return this;
        }

        @Override
        public void deserialize(boolean merge) {
            super.deserialize(merge);
            var mbdRecipe = MBDRecipeSerializer.SERIALIZER.fromJson(getOrCreateId(), json);
            inputs.clear();
            outputs.clear();
            conditions.clear();
            inputs.putAll(mbdRecipe.inputs);
            outputs.putAll(mbdRecipe.outputs);
            conditions.addAll(mbdRecipe.conditions);
            data = mbdRecipe.data;
            duration = mbdRecipe.duration;
            priority = mbdRecipe.priority;
            isFuel = mbdRecipe.isFuel;
        }

        @Override
        public void serialize() {
            var recipeType = MBDRegistries.RECIPE_TYPES.get(type.schemaType.id);
            if (recipeType == null) {
                throw new IllegalStateException("MBD Recipe type " + type.schemaType.id + " not found!");
            }
            json = MBDRecipeSerializer.SERIALIZER.toJson(
                    new MBDRecipe(recipeType, getOrCreateId(), inputs, outputs, conditions, data, duration, isFuel, priority)
            );
        }

    }

    record FluidIngredientJS(FluidIngredient ingredient) implements InputFluid, OutputFluid {
        @Override
        public long kjs$getAmount() {
            return ingredient.getAmount();
        }

        @Override
        public FluidIngredientJS kjs$copy(long amount) {
            FluidIngredient ingredient1 = ingredient.copy();
            ingredient1.setAmount(amount);
            return new FluidIngredientJS(ingredient1);
        }

        @Override
        public boolean matches(FluidLike other) {
            if (other instanceof FluidStackJS fluidStack) {
                return ingredient.test(FluidStack.create(fluidStack.getFluid(), fluidStack.getAmount(), fluidStack.getNbt()));
            }
            return other.matches(this);
        }

        public static FluidIngredientJS of(Object o) {
            if (o instanceof FluidIngredientJS ingredientJS) {
                return ingredientJS;
            } else if (o instanceof FluidIngredient ingredient) {
                return new FluidIngredientJS(ingredient);
            } else if (o instanceof JsonElement json) {
                return new FluidIngredientJS(FluidIngredient.fromJson(json));
            } else if (o instanceof FluidStackJS fluidStackJS) {
                return new FluidIngredientJS(FluidIngredient.of(
                        FluidStack.create(fluidStackJS.getFluid(), fluidStackJS.getAmount(), fluidStackJS.getNbt())));
            }

            var list = ListJS.of(o);
            if (list != null && !list.isEmpty()) {
                List<FluidStack> stacks = new ArrayList<>();
                for (var object : list) {
                    FluidStackJS stackJS = FluidStackJS.of(object);
                    stacks.add(FluidStack.create(stackJS.getFluid(), stackJS.getAmount(), stackJS.getNbt()));
                }
                return new FluidIngredientJS(FluidIngredient.of(stacks.toArray(FluidStack[]::new)));
            } else {
                FluidStackJS stackJS = FluidStackJS.of(o);
                return new FluidIngredientJS(FluidIngredient
                        .of(FluidStack.create(stackJS.getFluid(), stackJS.getAmount(), stackJS.getNbt())));
            }
        }
    }


}
