package com.lowdragmc.mbd2.api.recipe;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeCapabilityHolder;
import com.lowdragmc.mbd2.utils.FormattingUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author KilaBash
 * @date 2023/2/20
 * @implNote MBDRecipeType
 */
@Accessors(chain = true)
public class MBDRecipeType implements RecipeType<MBDRecipe> {
    public static final MBDRecipeType DUMMY = new MBDRecipeType(MBD2.id("dummy"));

    public final ResourceLocation registryName;
    @Setter
    private MBDRecipeBuilder recipeBuilder;
    @Setter
    @Getter
    private MBDRecipeType smallRecipeMap;
    @Setter
    @Getter
    @Nullable
    private Supplier<ItemStack> iconSupplier;
    @Getter
    protected List<Function<CompoundTag, String>> dataInfos = new ArrayList<>();
    @Setter
    @Getter
    protected int maxTooltips = 3;
    @Setter
    @Nullable
    protected BiConsumer<MBDRecipe, WidgetGroup> uiBuilder;
    @Setter
    @Getter
    protected boolean isFuelRecipeType;
    @Getter
    protected final Map<RecipeType<?>, List<MBDRecipe>> proxyRecipes;
    private CompoundTag customUICache;

    public MBDRecipeType(ResourceLocation registryName, RecipeType<?>... proxyRecipes) {
        this.registryName = registryName;
        recipeBuilder = new MBDRecipeBuilder(registryName, this);
        // must be linked to stop json contents from shuffling
        Map<RecipeType<?>, List<MBDRecipe>> map = new Object2ObjectLinkedOpenHashMap<>();
        for (RecipeType<?> proxyRecipe : proxyRecipes) {
            map.put(proxyRecipe, new ArrayList<>());
        }
        this.proxyRecipes = map;
    }

    @Override
    public String toString() {
        return registryName.toString();
    }

    public List<MBDRecipe> searchFuelRecipe(RecipeManager recipeManager, IRecipeCapabilityHolder holder) {
        if (!holder.hasProxies() || !isFuelRecipeType()) return Collections.emptyList();
        List<MBDRecipe> matches = new ArrayList<>();
        for (MBDRecipe recipe : recipeManager.getAllRecipesFor(this)) {
            if (recipe.isFuel && recipe.matchRecipe(holder).isSuccess() && recipe.matchTickRecipe(holder).isSuccess()) {
                matches.add(recipe);
            }
        }
        return matches;
    }

    public List<MBDRecipe> searchRecipe(RecipeManager recipeManager, IRecipeCapabilityHolder holder) {
        if (!holder.hasProxies()) return Collections.emptyList();
        List<MBDRecipe> matches = recipeManager.getAllRecipesFor(this).parallelStream()
                .filter(recipe -> !recipe.isFuel && recipe.matchRecipe(holder).isSuccess() && recipe.matchTickRecipe(holder).isSuccess())
                .collect(Collectors.toList());
        for (List<MBDRecipe> recipes : proxyRecipes.values()) {
            var found = recipes.parallelStream()
                    .filter(recipe -> !recipe.isFuel && recipe.matchRecipe(holder).isSuccess() && recipe.matchTickRecipe(holder).isSuccess())
                    .toList();
            matches.addAll(found);
        }
        return matches;
    }

    //////////////////////////////////////
    //*****     Recipe Builder    ******//
    //////////////////////////////////////

    public MBDRecipeType prepareBuilder(Consumer<MBDRecipeBuilder> onPrepare) {
        onPrepare.accept(recipeBuilder);
        return this;
    }

    public MBDRecipeBuilder recipeBuilder(ResourceLocation id, Object... append) {
        if (append.length > 0) {
            return recipeBuilder.copy(new ResourceLocation(id.getNamespace(),
                    id.getPath() + Arrays.stream(append).map(Object::toString).map(FormattingUtil::toLowerCaseUnder).reduce("", (a, b) -> a + "_" + b)));
        }
        return recipeBuilder.copy(id);
    }

    public MBDRecipeBuilder recipeBuilder(String id, Object... append) {
        return recipeBuilder(MBD2.id(id), append);
    }

    public MBDRecipeBuilder recipeBuilder(Supplier<? extends ItemLike> item, Object... append) {
        return recipeBuilder(item.get(), append);
    }

    public MBDRecipeBuilder recipeBuilder(ItemLike itemLike, Object... append) {
        return recipeBuilder(new ResourceLocation(itemLike.asItem().getDescriptionId()), append);
    }

    public MBDRecipeBuilder copyFrom(MBDRecipeBuilder builder) {
        return recipeBuilder.copyFrom(builder);
    }

    public MBDRecipeType onRecipeBuild(BiConsumer<MBDRecipeBuilder, Consumer<FinishedRecipe>> onBuild) {
        recipeBuilder.onSave(onBuild);
        return this;
    }

    //////////////////////////////////////
    //***********     UI    ************//
    //////////////////////////////////////

    public CompoundTag getCustomUI() {
        if (this.customUICache == null) {
            ResourceManager resourceManager = null;
            if (LDLib.isClient()) {
                resourceManager = Minecraft.getInstance().getResourceManager();
            } else if (Platform.getMinecraftServer() != null) {
                resourceManager = Platform.getMinecraftServer().getResourceManager();
            }
            if (resourceManager == null) {
                this.customUICache = new CompoundTag();
            } else {
                try {
                    var resource = resourceManager.getResourceOrThrow(new ResourceLocation(registryName.getNamespace(), "ui/recipe_type/%s.rtui".formatted(registryName.getPath())));
                    try (InputStream inputStream = resource.open()){
                        try (DataInputStream dataInputStream = new DataInputStream(inputStream);){
                            this.customUICache = NbtIo.read(dataInputStream, NbtAccounter.UNLIMITED);
                        }
                    }
                } catch (Exception e) {
                    this.customUICache = new CompoundTag();
                }
                if (this.customUICache == null) {
                    this.customUICache = new CompoundTag();
                }
            }
        }
        return this.customUICache;
    }

    public boolean hasCustomUI() {
        return !getCustomUI().isEmpty();
    }

    public void reloadCustomUI() {
        this.customUICache = null;
    }


    public MBDRecipe toMBDrecipe(ResourceLocation id, Recipe<?> recipe) {
        var builder = recipeBuilder(id);
        for (var ingredient : recipe.getIngredients()) {
            builder.inputItems(ingredient);
        }
        builder.outputItems(recipe.getResultItem(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)));
        if (recipe instanceof SmeltingRecipe smeltingRecipe) {
            builder.duration(smeltingRecipe.getCookingTime());
        }
        return MBDRecipeSerializer.SERIALIZER.fromJson(id, builder.build().serializeRecipe());
    }

}
