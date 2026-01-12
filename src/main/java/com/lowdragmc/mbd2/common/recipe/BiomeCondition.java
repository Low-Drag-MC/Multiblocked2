package com.lowdragmc.mbd2.common.recipe;

import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author KilaBash
 * @date 2022/05/27
 * @implNote DimensionCondition, specific dimension
 */
@Getter
@Setter
@NoArgsConstructor
public class BiomeCondition extends RecipeCondition {
    public static final MapCodec<BiomeCondition> CODEC = RecordCodecBuilder
            .mapCodec(instance -> instance.group(
                            Codec.BOOL.optionalFieldOf("reverse", false).forGetter(val -> val.isReverse),
                            ResourceLocation.CODEC.fieldOf("biome").forGetter(val -> val.biome)
                    ).apply(instance, BiomeCondition::new));

    public final static BiomeCondition INSTANCE = new BiomeCondition();
    private ResourceLocation biome = ResourceLocation.parse("dummy");

    public BiomeCondition(ResourceLocation biome) {
        this(false, biome);
    }

    public BiomeCondition(boolean isReverse, ResourceLocation biome) {
        super(isReverse);
        this.biome = biome;
    }

    @Override
    public String getType() {
        return "biome";
    }

    @Override
    public Component getTooltips() {
        return Component.translatable("recipe.condition.biome.tooltip", LocalizationUtils.format("biome.%s.%s", biome.getNamespace(), biome.getPath()));
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        Level level = recipeLogic.machine.getLevel();
        if (level == null) return false;
        Holder<Biome> biome = level.getBiome(recipeLogic.machine.getPos());
        return biome.is(this.biome);
    }

    @Override
    public MapCodec<? extends RecipeCondition> codec() {
        return CODEC;
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        super.buildConfigurator(father);
        var selector = new SearchComponentConfigurator<>(getTranslationKey(),
                () -> this.biome,
                b -> this.biome = b,
                ResourceLocation.parse("dummy"),
                true,
                this::search,
                ResourceLocation::toString
        );
        selector.setUp(false);
        selector.setTips("config.recipe.condition.biome.tooltip");
        father.addConfigurators(selector);
    }

    protected void search(String word, Consumer<ResourceLocation> find) {
        var wordLower = word.toLowerCase();
        for (var biomeEntry : Minecraft.getInstance().level.registryAccess().registry(Registries.BIOME).get().keySet()) {
            if (Thread.currentThread().isInterrupted()) return;
            if (biomeEntry.toString().contains(wordLower)) {
                find.accept(biomeEntry);
            }
        }
    }
}
