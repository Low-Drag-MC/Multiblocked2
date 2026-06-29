package com.lowdragmc.mbd2.integration.kubejs.recipe;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeSerializer;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.api.recipe.ingredient.EntityIngredient;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.capability.recipe.*;
import com.lowdragmc.mbd2.common.recipe.*;
import com.lowdragmc.mbd2.integration.create.CreateRotation;
import com.lowdragmc.mbd2.integration.create.CreateRotationCondition;
import com.lowdragmc.mbd2.integration.create.CreateRotationRecipeCapability;
import com.lowdragmc.mbd2.integration.mekanism.MEKTemperatureCondition;
import com.lowdragmc.mbd2.integration.mekanism.MekanismChemicalRecipeCapability;
import com.lowdragmc.mbd2.integration.mekanism.MekanismHeatRecipeCapability;
import com.lowdragmc.mbd2.integration.naturesaura.NaturesAuraRecipeCapability;
import com.lowdragmc.mbd2.integration.pneumaticcraft.PNCHeatRecipeCapability;
import com.lowdragmc.mbd2.integration.pneumaticcraft.PNCPressureAirRecipeCapability;
import com.lowdragmc.mbd2.integration.pneumaticcraft.PressureAir;
import com.lowdragmc.mbd2.integration.pneumaticcraft.trait.heat.PNCTemperatureCondition;
import com.lowdragmc.mbd2.integration.pneumaticcraft.trait.pressure.PNCPressureCondition;
import com.mojang.serialization.JsonOps;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeTypeFunction;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeNamespace;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaStorage;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

public interface MBDRecipeSchema {
    RecipeSchema SCHEMA = new RecipeSchema()
            .factory(new KubeRecipeFactory(MBD2.id("mbd_recipe"), MBDRecipeJS.class, MBDRecipeJS::new))
            .constructor();

    static MBDRecipeJS create(MBDRecipeType recipeType) {
        var recipe = new MBDRecipeJS(recipeType);
        var namespace = new RecipeNamespace(new RecipeSchemaStorage(), recipeType.getRegistryName().getNamespace());
        var schemaType = new RecipeSchemaType(namespace, recipeType.getRegistryName(), SCHEMA);
        recipe.type = new RecipeTypeFunction(null, schemaType);
        recipe.initValues(false);
        return recipe;
    }

    class MBDRecipeJS extends KubeRecipe {
        @FunctionalInterface
        public interface RecipeBuilder extends Consumer<MBDRecipeJS> { }

        public final Map<RecipeCapability<?>, List<Content>> inputs = new LinkedHashMap<>();
        public final Map<RecipeCapability<?>, List<Content>> outputs = new LinkedHashMap<>();
        public final List<RecipeCondition> conditions = new ArrayList<>();
        public CompoundTag data = new CompoundTag();
        public int duration = 100;
        public int priority;
        public boolean isXEIHidden;
        // runtime
        public boolean perTick;
        public String slotName;
        public String uiName;
        public float chance = 1;
        public float tierChanceBoost = 0;
        @Nullable
        private final MBDRecipeType recipeType;

        public MBDRecipeJS() {
            this(null);
        }

        public MBDRecipeJS(@Nullable MBDRecipeType recipeType) {
            this.recipeType = recipeType;
        }

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

        public MBDRecipeJS isXEIHidden(boolean xEIHidden) {
            isXEIHidden = xEIHidden;
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
            this.chance = lastChance;
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
                    .filter(Objects::nonNull)
                    .map(capability::of)
                    .map(o -> new Content(o, perTick, chance, tierChanceBoost, slotName, uiName)).toList());
            save();
            return this;
        }

        public MBDRecipeJS outputs(RecipeCapability<?> capability, Object... obj) {
            outputs.computeIfAbsent(capability, c -> new ArrayList<>()).addAll(Arrays.stream(obj)
                    .filter(Objects::nonNull)
                    .map(capability::of)
                    .map(o -> new Content(o, perTick, chance, tierChanceBoost, slotName, uiName)).toList());
            save();
            return this;
        }

        public MBDRecipeJS removeInputs(RecipeCapability<?> capability) {
            inputs.remove(capability);
            save();
            return this;
        }

        public MBDRecipeJS removeOutputs(RecipeCapability<?> capability) {
            outputs.remove(capability);
            save();
            return this;
        }

        public MBDRecipeJS inputItems(SizedIngredient... items) {
            return inputs(ItemRecipeCapability.CAP, (Object[]) items);
        }

        public MBDRecipeJS outputItems(SizedIngredient... items) {
            return outputs(ItemRecipeCapability.CAP, (Object[]) items);
        }

        public MBDRecipeJS inputItemsDurability(SizedIngredient... items) {
            return inputs(ItemDurabilityRecipeCapability.CAP, (Object[]) items);
        }

        public MBDRecipeJS outputItemsDurability(SizedIngredient... items) {
            return outputs(ItemDurabilityRecipeCapability.CAP, (Object[]) items);
        }

        public MBDRecipeJS inputFluids(SizedFluidIngredient... fluids) {
            return inputs(FluidRecipeCapability.CAP, (Object[]) fluids);
        }

        public MBDRecipeJS outputFluids(SizedFluidIngredient... fluids) {
            return outputs(FluidRecipeCapability.CAP, (Object[]) fluids);
        }

        public MBDRecipeJS inputEntities(EntityIngredient... entities) {
            return inputs(EntityRecipeCapability.CAP, (Object[]) entities);
        }

        public MBDRecipeJS outputEntities(EntityIngredient... entities) {
            return outputs(EntityRecipeCapability.CAP, (Object[]) entities);
        }

        public MBDRecipeJS inputFE(int energy) {
            return inputs(ForgeEnergyRecipeCapability.CAP, energy);
        }

        public MBDRecipeJS outputFE(int energy) {
            return outputs(ForgeEnergyRecipeCapability.CAP, energy);
        }

//        public MBDRecipeJS inputMana(int mana) {
//            if (!MBD2.isBotaniaLoaded()) {
//                throw new IllegalStateException("Try to add a mana ingredient while the botania is not loaded!");
//            }
//            return inputs(BotaniaManaRecipeCapability.CAP, mana);
//        }
//
//        public MBDRecipeJS outputMana(int mana) {
//            if (!MBD2.isBotaniaLoaded()) {
//                throw new IllegalStateException("Try to add a mana ingredient while the botania is not loaded!");
//            }
//            return outputs(BotaniaManaRecipeCapability.CAP, mana);
//        }

        public MBDRecipeJS inputAura(int aura) {
            if (!MBD2.isNaturesAuraLoaded()) {
                throw new IllegalStateException("Try to add a aura ingredient while the nature's aura is not loaded!");
            }
            return inputs(NaturesAuraRecipeCapability.CAP, aura);
        }

        public MBDRecipeJS outputAura(int aura) {
            if (!MBD2.isNaturesAuraLoaded()) {
                throw new IllegalStateException("Try to add a aura ingredient while the nature's aura is not loaded!");
            }
            return outputs(NaturesAuraRecipeCapability.CAP, aura);
        }

//        public MBDRecipeJS inputEmber(double ember) {
//            if (!MBD2.isEmbersLoaded()) {
//                throw new IllegalStateException("Try to add a ember ingredient while the embers is not loaded!");
//            }
//            return inputs(EmbersEmberRecipeCapability.CAP, ember);
//        }
//
//        public MBDRecipeJS outputEmber(double ember) {
//            if (!MBD2.isEmbersLoaded()) {
//                throw new IllegalStateException("Try to add a ember ingredient while the embers is not loaded!");
//            }
//            return outputs(EmbersEmberRecipeCapability.CAP, ember);
//        }

        public MBDRecipeJS inputPNCPressure(float pressure) {
            if (!MBD2.isPneumaticCraftLoaded()) {
                throw new IllegalStateException("Try to add a pressure ingredient while the pneumatic craft is not loaded!");
            }
            return inputs(PNCPressureAirRecipeCapability.CAP, new PressureAir(false, pressure));
        }

        public MBDRecipeJS outputPNCPressure(float pressure) {
            if (!MBD2.isPneumaticCraftLoaded()) {
                throw new IllegalStateException("Try to add a pressure ingredient while the pneumatic craft is not loaded!");
            }
            return outputs(PNCPressureAirRecipeCapability.CAP, new PressureAir(false, pressure));
        }

        public MBDRecipeJS inputPNCAir(int air) {
            if (!MBD2.isPneumaticCraftLoaded()) {
                throw new IllegalStateException("Try to add a air ingredient while the pneumatic craft is not loaded!");
            }
            return inputs(PNCPressureAirRecipeCapability.CAP, new PressureAir(true, air));
        }

        public MBDRecipeJS outputPNCAir(int air) {
            if (!MBD2.isPneumaticCraftLoaded()) {
                throw new IllegalStateException("Try to add a air ingredient while the pneumatic craft is not loaded!");
            }
            return outputs(PNCPressureAirRecipeCapability.CAP, new PressureAir(true, air));
        }

        public MBDRecipeJS inputPNCHeat(double heat) {
            if (!MBD2.isPneumaticCraftLoaded()) {
                throw new IllegalStateException("Try to add a heat ingredient while the pneumatic craft is not loaded!");
            }
            return inputs(PNCHeatRecipeCapability.CAP, heat);
        }

        public MBDRecipeJS outputPNCHeat(double heat) {
            if (!MBD2.isPneumaticCraftLoaded()) {
                throw new IllegalStateException("Try to add a heat ingredient while the pneumatic craft is not loaded!");
            }
            return outputs(PNCHeatRecipeCapability.CAP, heat);
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

//        public MBDRecipeJS inputEU(long eu) {
//            if (!MBD2.isGTMLoaded()) {
//                throw new IllegalStateException("Try to add a eu ingredient while the gtceu is not loaded!");
//            }
//            return inputs(GTMEnergyRecipeCapability.CAP, eu);
//        }
//
//        public MBDRecipeJS outputEU(long eu) {
//            if (!MBD2.isGTMLoaded()) {
//                throw new IllegalStateException("Try to add a eu ingredient while the gtceu is not loaded!");
//            }
//            return outputs(GTMEnergyRecipeCapability.CAP, eu);
//        }

        public MBDRecipeJS inputStress(float stress) {
            if (!MBD2.isCreateLoaded()) {
                throw new IllegalStateException("Try to add a stress ingredient while the create is not loaded!");
            }
            return inputs(CreateRotationRecipeCapability.CAP, CreateRotation.stress(stress));
        }

        public MBDRecipeJS outputStress(float stress) {
            if (!MBD2.isCreateLoaded()) {
                throw new IllegalStateException("Try to add a stress ingredient while the create is not loaded!");
            }
            return outputs(CreateRotationRecipeCapability.CAP, CreateRotation.stress(stress));
        }

        public MBDRecipeJS inputRPM(float rpm) {
            if (!MBD2.isCreateLoaded()) {
                throw new IllegalStateException("Try to add a rpm ingredient while the create is not loaded!");
            }
            return inputs(CreateRotationRecipeCapability.CAP, CreateRotation.rpm(rpm));
        }

        public MBDRecipeJS outputRPM(float rpm) {
            if (!MBD2.isCreateLoaded()) {
                throw new IllegalStateException("Try to add a rpm ingredient while the create is not loaded!");
            }
            return outputs(CreateRotationRecipeCapability.CAP, CreateRotation.rpm(rpm));
        }

        public MBDRecipeJS inputChemicals(String... stack) {
            if (!MBD2.isMekanismLoaded()) {
                throw new IllegalStateException("Try to add a chemical ingredient while the mekanism is not loaded!");
            }
            return inputs(MekanismChemicalRecipeCapability.CAP, (Object[]) stack);
        }

        public MBDRecipeJS outputChemicals(String... stack) {
            if (!MBD2.isMekanismLoaded()) {
                throw new IllegalStateException("Try to add a chemical ingredient while the mekanism is not loaded!");
            }
            return outputs(MekanismChemicalRecipeCapability.CAP, (Object[]) stack);
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

        public MBDRecipeJS machineData(CompoundTag data, boolean onlyCheckCustomData) {
            addCondition(new MachineNBTCondition(data, onlyCheckCustomData));
            return this;
        }

        public MBDRecipeJS dayTime(boolean isDay) {
            addCondition(new DayTimeCondition(isDay));
            return this;
        }

        public MBDRecipeJS light(int minSkyLight, int maxSkyLight, int minBlockLight, int maxBlockLight, boolean canSeeSky) {
            addCondition(new LightCondition(minSkyLight, maxSkyLight, minBlockLight, maxBlockLight, canSeeSky));
            return this;
        }

        public MBDRecipeJS redstoneSignal(int minSignal, int maxSignal) {
            addCondition(new RedstoneSignalCondition(minSignal, maxSignal));
            return this;
        }

        // mod compatibility
        public MBDRecipeJS rotationCondition(float minRPM, float maxRPM, float minStress, float maxStress) {
            if (!MBD2.isCreateLoaded()) {
                throw new IllegalStateException("Try to add a rotation condition while the create is not loaded!");
            }
            addCondition(new CreateRotationCondition(minRPM, maxRPM, minStress, maxStress));
            return this;
        }

        public MBDRecipeJS mekTemperatureCondition(double minTemperature, double maxTemperature) {
            if (!MBD2.isMekanismLoaded()) {
                throw new IllegalStateException("Try to add a heat condition while the mekanism is not loaded!");
            }
            addCondition(new MEKTemperatureCondition(minTemperature, maxTemperature));
            return this;
        }

        public MBDRecipeJS pncTemperatureCondition(float minTemperature, float maxTemperature) {
            if (!MBD2.isPneumaticCraftLoaded()) {
                throw new IllegalStateException("Try to add a temperature condition while the pneumatic is not loaded!");
            }
            addCondition(new PNCTemperatureCondition(minTemperature, maxTemperature));
            return this;
        }

        public MBDRecipeJS pncPressureCondition(boolean isAir, float minPressure, float maxPressure) {
            if (!MBD2.isPneumaticCraftLoaded()) {
                throw new IllegalStateException("Try to add a pressure condition while the pneumatic is not loaded!");
            }
            addCondition(new PNCPressureCondition(isAir, minPressure, maxPressure));
            return this;
        }

        private MBDRecipeType getRecipeType() {
            if (recipeType == null) {
                var recipeType = MBDRegistries.RECIPE_TYPES.get(type.schemaType.id);
                if (recipeType == null) {
                    throw new IllegalStateException("MBD Recipe type " + type.schemaType.id + " not found!");
                }
                return recipeType;
            }
            return recipeType;
        }

        @Override
        public void deserialize(boolean merge) {
            super.deserialize(merge);
            var ops = Platform.getFrozenRegistry().createSerializationContext(JsonOps.INSTANCE);
            var mbdRecipe = MBDRecipeSerializer.CODEC.codec().parse(ops, json).getOrThrow();
            inputs.clear();
            outputs.clear();
            conditions.clear();
            inputs.putAll(mbdRecipe.inputs);
            outputs.putAll(mbdRecipe.outputs);
            conditions.addAll(mbdRecipe.conditions);
            data = mbdRecipe.data;
            duration = mbdRecipe.duration;
            priority = mbdRecipe.priority;
            isXEIHidden = mbdRecipe.isXEIHidden;
        }

        @Override
        public void serialize() {
            var ops = Platform.getFrozenRegistry().createSerializationContext(JsonOps.INSTANCE);
            json = MBDRecipeSerializer.CODEC.codec()
                    .encodeStart(ops, buildMBDRecipe())
                    .getOrThrow().getAsJsonObject();
        }

        public MBDRecipe buildMBDRecipe() {
            ResourceLocation recipeId;
            if (id != null) {
                recipeId = id;
            } else if (type != null && type.event != null) {
                recipeId = getOrCreateId();
            } else {
                throw new IllegalStateException("MBD recipe id must be set before building a recipe outside ServerEvents.recipes");
            }
            return new MBDRecipe(
                    getRecipeType(),
                    recipeId,
                    inputs,
                    outputs,
                    conditions,
                    data,
                    duration,
                    isXEIHidden,
                    priority
            );
        }
    }
}
