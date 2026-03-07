package com.lowdragmc.mbd2.api.recipe;

import com.google.common.collect.Multimap;
import com.google.common.collect.Queues;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.ArrayConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.math.Size;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeCapabilityHolder;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.api.recipe.event.FuelRecipeUIEvent;
import com.lowdragmc.mbd2.api.recipe.event.RecipeUIEvent;
import com.lowdragmc.mbd2.api.recipe.event.TransferProxyRecipeEvent;
import com.lowdragmc.mbd2.utils.FormattingUtil;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author KilaBash
 * @date 2023/2/20
 * @implNote MBDRecipeType
 */
@Accessors(chain = true)
@RemapPrefixForJS("kjs$")
public class MBDRecipeType implements RecipeType<MBDRecipe>, INBTSerializable<CompoundTag>, IConfigurable, IPersistedSerializable {
    public static final MBDRecipeType DUMMY = new MBDRecipeType(MBD2.id("dummy"));

    public interface UICreator {
        UICreator DEFAULT = recipe -> UI.of();
        UI create(MBDRecipe recipe);
    }

    @Configurable(name = "recipe_type.registry_name", tips = {
            "recipe_type.registry_name.tooltip",
            "config.require_restart"
    }, forceUpdate = false)
    @Getter
    private ResourceLocation registryName;
    @Setter
    @Getter
    private MBDRecipeBuilder recipeBuilder;
    @Setter
    @Getter
    @Configurable(name = "recipe_type.icon", tips = "recipe_type.icon.tooltip")
    private IGuiTexture icon = SpriteTexture.of(LDLib2.id("textures/gui/icon.png"));
    @Setter
    @Getter
    @Configurable(name = "recipe_type.fuel_icon", tips = "recipe_type.fuel_icon.tooltip")
    private IGuiTexture fuelIcon = SpriteTexture.of(LDLib2.id("textures/gui/icon.png"));
    @Setter
    @Getter
    @Configurable(name = "recipe_type.require_fuel", tips = "recipe_type.require_fuel.tooltip")
    protected boolean requireFuelForWorking;
    @Setter
    @Getter
    @Configurable(name = "recipe_type.is_xei_visible", tips = "recipe_type.is_xei_visible.tooltip")
    protected boolean isXEIVisible = true;
    @Setter
    @Getter
    @Configurable(name = "recipe_type.is_proxy_recipe_xei_visible", tips = "recipe_type.is_proxy_recipe_xei_visible.tooltip")
    protected boolean isProxyRecipeXEIVisible = false;
    private final List<RecipeType<?>> proxyRecipeTypes = new ArrayList<>();
    @Getter
    protected final Map<ResourceLocation, MBDRecipe> builtinRecipes = new LinkedHashMap<>();
    @Setter
    @Getter
    protected UICreator uiCreator = UICreator.DEFAULT;
    @Setter
    @Getter
    protected Size uiSize = Size.of(50, 50);
    @Setter
    @Getter
    protected UICreator fuelUICreator = UICreator.DEFAULT;
    @Setter
    @Getter
    protected Size fuelUISize = Size.of(50, 50);

    // run-time
    @Nullable
    @Setter
    @Getter
    private File projectFile;
    @Getter
    @Deprecated
    protected final Map<RecipeType<?>, List<MBDRecipe>> proxyRecipes = new HashMap<>();

    public MBDRecipeType(ResourceLocation registryName, RecipeType<?>... proxyRecipes) {
        this.registryName = registryName;
        recipeBuilder = new MBDRecipeBuilder(registryName, this);
        proxyRecipeTypes.addAll(Arrays.asList(proxyRecipes));
    }

    /**
     * This method is used to clear the proxy recipes cache.
     */
    public void onRecipeManagerLoaded(Multimap<RecipeType<?>, RecipeHolder<?>> byType,
                                      Map<ResourceLocation, RecipeHolder<?>> byName) {
        // append builtin recipes
        builtinRecipes.forEach((id, recipe) -> byName.put(id, new RecipeHolder<>(id, recipe)));

        // load proxy recipes
        proxyRecipes.clear();
        for (var type : proxyRecipeTypes) {
            var recipes = new ArrayList<MBDRecipe>();
            for (var recipe : byType.get(type)) {
                if (recipe == null) continue;
                var mbdRecipe = toMBDrecipe(type, recipe.id(), recipe.value());
                if (mbdRecipe != null) {
                    recipes.add(mbdRecipe);
                    byName.put(mbdRecipe.id, new RecipeHolder<>(mbdRecipe.id, mbdRecipe));
                }
            }
            proxyRecipes.put(type, recipes);
        }
    }

    public void onRecipeManagerLoadedKjs(Map<ResourceLocation, RecipeHolder<?>> recipesByName) {
        builtinRecipes.forEach((id, recipe) -> recipesByName.put(id, new RecipeHolder<>(id, recipe)));
        // load proxy recipes
        proxyRecipes.clear();
        var proxyRecipeTypes = new HashSet<>(this.proxyRecipeTypes);

        if (proxyRecipeTypes.isEmpty()) return;
        for (var entry : recipesByName.entrySet()) {
            var key = entry.getKey();
            var recipe = entry.getValue().value();
            if (proxyRecipeTypes.contains(recipe.getType())) {
                var mbdRecipe = toMBDrecipe(recipe.getType(), key, recipe);
                if (mbdRecipe != null) {
                    proxyRecipes.computeIfAbsent(recipe.getType(), type -> new ArrayList<>()).add(mbdRecipe);
                }
            }
        }
        for (List<MBDRecipe> recipes : proxyRecipes.values()) {
            for (MBDRecipe recipe : recipes) {
                recipesByName.put(recipe.id, new RecipeHolder<>(recipe.id, recipe));
            }
        }
    }

    public static MBDRecipeType createDefault() {
        return new MBDRecipeType(MBD2.id("recipe_type"));
    }

    /**
     * Create recipeType from project tag for product usage.\
     * @param file project file.
     * @param tag project tag.
     * @param postTask Called when the mod is loaded completed. To make sure all resources are available.
     *                 <br/> e.g. items, blocks and other registries are ready.
     */
    public MBDRecipeType loadProductiveTag(@Nullable File file, CompoundTag tag, Deque<Runnable> postTask) {
        this.projectFile = file;
        this.registryName = ResourceLocation.parse(tag.getCompound("recipe_type").getString("registryName"));
        postTask.add(() -> {
            // todo ui
//            var texturesResource = new TexturesResource();
//            texturesResource.deserializeNBT(tag.getCompound("resources").getCompound(TexturesResource.RESOURCE_NAME), Platform.getFrozenRegistry());
//            UIResourceTexture.setCurrentResource(texturesResource, false);
//            deserializeNBT(Platform.getFrozenRegistry(), tag.getCompound("recipe_type"));
//            UIResourceTexture.clearCurrentResource();
//            var uiTag = tag.getCompound("ui");
//            var size = uiTag.getCompound("size");
//            setUiSize(new Size(size.getInt("width"), size.getInt("height")));
//            setUiCreator(recipe -> {
//                var recipeUI = new WidgetGroup();
//                recipeUI.setClientSideWidget();
//                IConfigurableWidget.deserializeNBT(recipeUI, uiTag, texturesResource, false, Platform.getFrozenRegistry());
//                bindXEIRecipeUI(recipeUI, recipe);
//                recipeUI.setSelfPosition(0, 0);
//                recipeUI.setBackground(IGuiTexture.EMPTY);
//                return recipeUI;
//            });
//            if (requireFuelForWorking && tag.contains("fuelUI")) {
//                var fuelUITag = tag.getCompound("fuelUI");
//                var fuelSize= fuelUITag.getCompound("size");
//                setFuelUISize(new Size(fuelSize.getInt("width"), fuelSize.getInt("height")));
//                setFuelUICreator(recipe -> {
//                    var recipeUI = new WidgetGroup();
//                    recipeUI.setClientSideWidget();
//                    IConfigurableWidget.deserializeNBT(recipeUI, fuelUITag, texturesResource, false, Platform.getFrozenRegistry());
//                    bindXEIRecipeUI(recipeUI, recipe);
//                    recipeUI.setSelfPosition(0, 0);
//                    recipeUI.setBackground(IGuiTexture.EMPTY);
//                    return recipeUI;
//                });
//            }
        });
        return this;
    }

    /**
     * Indicate if the recipe type is created from project file.
     */
    public boolean isCreatedFromProjectFile() {
        return projectFile != null;
    }

    /**
     * Reload recipe type from project file
     */
    public void reloadFromProjectFile() {
        if (projectFile != null) {
            try {
                var tag = NbtIo.read(projectFile.toPath());
                if (tag != null) {
                    Deque<Runnable> postTask = Queues.newArrayDeque();
                    loadProductiveTag(projectFile, tag, postTask);
                    postTask.forEach(Runnable::run);
                }
            } catch (IOException ignored) {}
        }
    }

    @Override
    public String toString() {
        return registryName.toString();
    }

    public ResourceLocation getFuelRegistryName() {
        return ResourceLocation.fromNamespaceAndPath(registryName.getNamespace(), registryName.getPath() + ".fuel");
    }

    public List<MBDRecipe> searchFuelRecipe(RecipeManager recipeManager, IRecipeCapabilityHolder holder) {
        if (!holder.hasProxies() || !isRequireFuelForWorking()) return Collections.emptyList();
        List<MBDRecipe> matches = new ArrayList<>();
        for (var recipeHolder : recipeManager.getAllRecipesFor(this)) {
            var recipe = recipeHolder.value();
            if (recipe.isFuel && recipe.matchRecipe(holder).isSuccess() && recipe.matchTickRecipe(holder).isSuccess()) {
                matches.add(recipe);
            }
        }
        matches.sort(Comparator.comparingInt(r -> r.priority));
        return matches;
    }

    public List<MBDRecipe> searchRecipe(RecipeManager recipeManager, IRecipeCapabilityHolder holder) {
        if (!holder.hasProxies()) return Collections.emptyList();
        List<MBDRecipe> matches = recipeManager.getAllRecipesFor(this).parallelStream()
                .map(RecipeHolder::value)
                .filter(recipe -> !recipe.isFuel && recipe.matchRecipe(holder).isSuccess() && recipe.matchTickRecipe(holder).isSuccess())
                .collect(Collectors.toList());
        matches.sort(Comparator.comparingInt(r -> r.priority));
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
            return recipeBuilder.copy(ResourceLocation.fromNamespaceAndPath(id.getNamespace(),
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

    @HideFromJS
    public MBDRecipeBuilder recipeBuilder(ItemLike itemLike, Object... append) {
        return recipeBuilder(ResourceLocation.parse(itemLike.asItem().getDescriptionId()), append);
    }

    public Object kjs$recipeBuilder() {
        if (LDLib2.isKubejsLoaded()) {
            // todo
//            return new MBDRecipeSchema.MBDRecipeJS(this);
        }
        throw new UnsupportedOperationException("KubeJS is not loaded");
    }

    public MBDRecipeBuilder copyFrom(MBDRecipeBuilder builder) {
        return recipeBuilder.copyFrom(builder);
    }

    public MBDRecipeType onRecipeBuild(BiConsumer<MBDRecipeBuilder, RecipeOutput> onBuild) {
        recipeBuilder.onSave(onBuild);
        return this;
    }

    @Nullable
    public MBDRecipe toMBDrecipe(RecipeType<?> recipeType, ResourceLocation id, Recipe<?> recipe) {
        MBDRecipe result = null;
        if (recipe instanceof MBDRecipe mbdRecipe) {
            var copied = mbdRecipe.copy();
            copied.recipeType = this;
            result = copied;
        } else {
            if (!recipe.getIngredients().isEmpty()) {
                var newID = ResourceLocation.fromNamespaceAndPath(registryName.getNamespace(), registryName.getPath() + "/" + id.getPath());
                var builder = recipeBuilder(newID).recipeType(this);
                for (var ingredient : recipe.getIngredients()) {
                    builder.inputItems(ingredient);
                }
                builder.outputItems(recipe.getResultItem(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)));
                if (recipe instanceof SmeltingRecipe smeltingRecipe) {
                    builder.duration(smeltingRecipe.getCookingTime());
                }
                builder.isXEIHidden(!isProxyRecipeXEIVisible);
                result = builder.buildRawRecipe();
            }
        }
        var proxyTypeId = BuiltInRegistries.RECIPE_TYPE.getKey(recipeType);
        if (proxyTypeId != null) {
            var event = new TransferProxyRecipeEvent(this, proxyTypeId, recipeType, id, recipe, result);
            NeoForge.EVENT_BUS.post(event.postCustomEvent());
            if (event.isCanceled()) {
                return null;
            }
            return event.mbdRecipe;
        }
        return result;
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        IConfigurable.super.buildConfigurator(father);
        var proxyGroup = new ArrayConfiguratorGroup<>("recipe_type.proxy_recipes", true,
                () -> proxyRecipeTypes,
                (getter, setter) -> new SearchComponentConfigurator<>("editor.machine.recipe_type", getter, setter,
                        RecipeType.SMELTING, true, (word, find) -> {
                    for (var recipeType : BuiltInRegistries.RECIPE_TYPE) {
                        if (Thread.currentThread().isInterrupted()) return;
                        var id = BuiltInRegistries.RECIPE_TYPE.getKey(recipeType);
                        if (id != null && id.toString().contains(word.toLowerCase())) {
                            find.accept(recipeType);
                        }
                    }}, recipeType -> Optional.ofNullable(BuiltInRegistries.RECIPE_TYPE.getKey(recipeType)).map(Object::toString).orElse("missing"),
                        UIElementProvider.text(recipeType -> Component.literal(Optional.ofNullable(BuiltInRegistries.RECIPE_TYPE.getKey(recipeType)).map(Object::toString).orElse("missing")))),
                true);
        proxyGroup.setAddDefault(() -> RecipeType.SMELTING);
        proxyGroup.setOnUpdate(list -> {
            proxyRecipeTypes.clear();
            proxyRecipeTypes.addAll(list);
        });
        proxyGroup.setTips("recipe_type.proxy_recipes.tooltip");
        father.addConfigurators(proxyGroup);
    }

    //////////////////////////////////////
    //********    Serialize    *********//
    //////////////////////////////////////
    @Override
    public CompoundTag serializeNBT(@Nonnull HolderLookup.Provider provider) {
        var tag = IPersistedSerializable.super.serializeNBT(provider);
        // proxyRecipeTypes
        var proxyTag = new ListTag();
        for (var type : proxyRecipeTypes) {
            var location = BuiltInRegistries.RECIPE_TYPE.getKey(type);
            if (location != null) proxyTag.add(StringTag.valueOf(location.toString()));
        }
        tag.put("proxyRecipeTypes", proxyTag);
        // builtin recipes
        var recipesTag = new CompoundTag();
        for (var entry : builtinRecipes.entrySet()) {
            recipesTag.put(entry.getKey().toString(), MBDRecipeSerializer.SERIALIZER.codec().codec().encodeStart(NbtOps.INSTANCE, entry.getValue()).getOrThrow());
        }
        tag.put("builtinRecipes", recipesTag);
        return tag;
    }

    @Override
    public void deserializeNBT(@Nonnull HolderLookup.Provider provider, @Nonnull CompoundTag tag) {
        IPersistedSerializable.super.deserializeNBT(provider, tag);
        // proxyRecipeTypes
        proxyRecipeTypes.clear();
        var proxyTag = tag.getList("proxyRecipeTypes", 8);
        for (Tag type : proxyTag) {
            var location = ResourceLocation.parse(type.getAsString());
            var recipeType = BuiltInRegistries.RECIPE_TYPE.get(location);
            if (recipeType != null) proxyRecipeTypes.add(recipeType);
        }
        // builtin recipes
        builtinRecipes.clear();
        var recipesTag = tag.getCompound("builtinRecipes");
        for (var key : recipesTag.getAllKeys()) {
            var recipe = MBDRecipeSerializer.SERIALIZER.codec().codec().parse(NbtOps.INSTANCE, recipesTag.getCompound(key)).getOrThrow();
            recipe.recipeType = this;
            builtinRecipes.put(ResourceLocation.parse(key), recipe);
        }
    }


    //////////////////////////////////////
    //***********     UI    ************//
    //////////////////////////////////////

    public void bindXEIRecipeUI(UI ui, MBDRecipe recipe) {
        // todo ui
//        WidgetUtils.widgetByIdForEach(ui, "^@progress_bar$", ProgressWidget.class,
//                progress -> progress.setHoverTooltips(Component.translatable("recipe.duration.value", recipe.duration)));
//        WidgetUtils.widgetByIdForEach(ui, "^@duration$", LabelWidget.class,
//                label -> label.setComponent(Component.translatable("recipe.duration.value", recipe.duration)));
//        WidgetUtils.widgetByIdForEach(ui, "^@condition$", TextBoxWidget.class,
//                textBoxWidget -> textBoxWidget.setContent(recipe.conditions.stream().map(RecipeCondition::getTooltips).map(Component::getString).toList()));
//        bindCapIOUI(ui, recipe.inputs, IO.IN);
//        bindCapIOUI(ui, recipe.outputs, IO.OUT);
    }

    private static void bindCapIOUI(UI ui, Map<RecipeCapability<?>, List<Content>> values, IO io) {
//        values.forEach((cap, contents) -> {
//            for (int i = 0; i < contents.size(); i++) {
//                var content = contents.get(i);
//                var id = content.uiName.isEmpty() ? "^@%s_%s_%d$".formatted(cap.name, io.name, i) : Pattern.quote(content.uiName);
//                for (var widget : WidgetUtils.getWidgetsById(ui, id)) {
//                    cap.bindXEIWidget(widget, content, io);
//                    var tooltips = new ArrayList<Component>();
//                    content.appendTooltip(tooltips);
//                    if (!tooltips.isEmpty()) {
//                        widget.appendHoverTooltips(tooltips);
//                    }
//                }
//            }
//        });
    }

    public UI createRecipeUI(MBDRecipe recipe) {
        var ui = uiCreator.create(recipe);
        var event = new RecipeUIEvent(this, recipe, ui);
        NeoForge.EVENT_BUS.post(event.postKubeJSEvent());
        return event.getUi();
    }

    public UI createFuelUI(MBDRecipe recipe) {
        var ui = fuelUICreator.create(recipe);
        var event = new FuelRecipeUIEvent(this, recipe, ui);
        NeoForge.EVENT_BUS.post(event.postKubeJSEvent());
        return event.getUi();
    }
}
