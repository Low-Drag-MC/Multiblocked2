package com.lowdragmc.mbd2.common.recipe;

import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.SearchComponentConfigurator;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

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
public class DimensionCondition extends RecipeCondition {
    public static final MapCodec<DimensionCondition> CODEC = RecordCodecBuilder
            .mapCodec(instance -> instance.group(
                    Codec.BOOL.optionalFieldOf("reverse", false).forGetter(val -> val.isReverse),
                    ResourceLocation.CODEC.fieldOf("dimension").forGetter(val -> val.dimension)
            ).apply(instance, DimensionCondition::new));

    public final static DimensionCondition INSTANCE = new DimensionCondition();
    private ResourceLocation dimension = ResourceLocation.parse("dummy");

    public DimensionCondition(ResourceLocation dimension) {
        this.dimension = dimension;
    }

    public DimensionCondition(boolean isReverse, ResourceLocation dimension) {
        super(isReverse);
        this.dimension = dimension;
    }

    @Override
    public String getType() {
        return "dimension";
    }

    @Override
    public Component getTooltips() {
        return Component.translatable("recipe.condition.dimension.tooltip", dimension);
    }

    @Override
    public boolean test(@Nonnull MBDRecipe recipe, @Nonnull RecipeLogic recipeLogic) {
        Level level = recipeLogic.machine.getLevel();
        return level != null && dimension.equals(level.dimension().location());
    }

    @Override
    public MapCodec<? extends RecipeCondition> codec() {
        return CODEC;
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        super.buildConfigurator(father);
        var selector = new SearchComponentConfigurator<>(getTranslationKey(),
                () -> this.dimension,
                d -> this.dimension = d,
                ResourceLocation.parse("dummy"),
                true,
                this::search,
                ResourceLocation::toString
        );
        selector.setUp(false);
        selector.setTips("config.recipe.condition.dimension.tooltip");
        father.addConfigurators(selector);
    }

    protected void search(String word, Consumer<ResourceLocation> find) {
        var wordLower = word.toLowerCase();
        for (var biomeEntry : Minecraft.getInstance().level.registryAccess().registry(Registries.DIMENSION_TYPE).get().keySet()) {
            if (Thread.currentThread().isInterrupted()) return;
            if (biomeEntry.toString().contains(wordLower)) {
                find.accept(biomeEntry);
            }
        }
    }
}
