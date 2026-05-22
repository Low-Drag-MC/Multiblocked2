package com.lowdragmc.mbd2.api.recipe;

import com.google.common.collect.Multimap;
import com.google.common.collect.Queues;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.IToggleConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigList;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.math.Size;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeCapabilityHolder;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.api.recipe.event.RecipeUIEvent;
import com.lowdragmc.mbd2.api.recipe.event.TransferProxyRecipeEvent;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.utils.FileUtils;
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
    @Getter
    @Configurable(name = "fuelRecipeConfig", tips = "fuelRecipeConfig.tooltip", subConfigurable = true)
    private final FuelRecipeConfig fuelRecipeConfig = new FuelRecipeConfig();
    @Setter
    @Getter
    @Configurable(name = "recipe_type.is_xei_visible", tips = "recipe_type.is_xei_visible.tooltip")
    protected boolean isXEIVisible = true;
    @Setter
    @Getter
    @Configurable(name = "recipe_type.is_proxy_recipe_xei_visible", tips = "recipe_type.is_proxy_recipe_xei_visible.tooltip")
    protected boolean isProxyRecipeXEIVisible = false;
    @Configurable(name = "recipe_type.proxy_recipes", tips = "recipe_type.proxy_recipes.tooltip")
    @ConfigList(configuratorMethod = "configureProxyRecipes", addDefaultMethod = "addDefaultProxyRecipe")
    private final List<ResourceLocation> proxyRecipeTypes = new ArrayList<>();
    @Getter
    protected final Map<ResourceLocation, MBDRecipe> builtinRecipes = new LinkedHashMap<>();
    @Setter
    @Persisted
    protected UITemplate uiTemplate = null;
    @Setter
    @Getter
    protected Size uiSize = Size.of(50, 50);

    // run-time
    @Nullable
    @Setter
    @Getter
    private File projectFile;

    public MBDRecipeType(ResourceLocation registryName, ResourceLocation... proxyRecipes) {
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
        for (var type : proxyRecipeTypes) {
            var recipeType = BuiltInRegistries.RECIPE_TYPE.get(type);
            if (recipeType == null) return;
            for (var recipe : byType.get(recipeType)) {
                if (recipe == null) continue;
                var mbdRecipe = toMBDrecipe(recipeType, recipe.id(), recipe.value());
                if (mbdRecipe != null) {
                    var convertedRecipe = new RecipeHolder<>(mbdRecipe.id, mbdRecipe);
                    byName.put(mbdRecipe.id, convertedRecipe);
                }
            }
        }
    }

    public void onRecipeManagerLoadedKjs(Map<ResourceLocation, RecipeHolder<?>> recipesByName) {
        builtinRecipes.forEach((id, recipe) -> recipesByName.put(id, new RecipeHolder<>(id, recipe)));
        var added = new ArrayList<RecipeHolder<MBDRecipe>>();
        // load proxy recipes
        for (var entry : recipesByName.entrySet()) {
            var key = entry.getKey();
            var recipe = entry.getValue().value();
            var typeId = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
            if (this.proxyRecipeTypes.contains(typeId)) {
                var mbdRecipe = toMBDrecipe(recipe.getType(), key, recipe);
                if (mbdRecipe != null) {
                    added.add(new RecipeHolder<>(mbdRecipe.id, mbdRecipe));
                }
            }
        }

        for (var holder : added) {
            recipesByName.put(holder.id(), holder);
        }
    }

    public static MBDRecipeType createDefault() {
        return new MBDRecipeType(MBD2.id("recipe_type"));
    }

    public UITemplate createDefaultRecipeTemplate() {
        var root = new UIElement();
        root.getLayout()
                .gapAll(4)
                .paddingAll(4);
        root.addClass("panel_bg");
        root.addChild(new Label().setText("Recipe"));
        var progress = new ProgressBar();
        progress.setId("@progress_bar");
        progress.setProgress(0.5f);
        progress.layout(layout -> layout.width(80).height(14));
        root.addChild(progress);
        var duration = new Label();
        duration.setId("@duration");
        duration.setText(Component.translatable("recipe.duration.value", 100));
        root.addChild(duration);
        return UITemplate.of(root, StylesheetManager.MC);
    }

    public UITemplate getUiTemplate() {
        if (uiTemplate == null) {
            uiTemplate = createDefaultRecipeTemplate();
        }
        return uiTemplate;
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
        var projectTag = tag.contains("data") ? tag.getCompound("data") : tag;
        this.registryName = ResourceLocation.parse(projectTag.getCompound("recipe_type").getString("registryName"));
        postTask.add(() -> {
            deserializeNBT(Platform.getFrozenRegistry(), projectTag.getCompound("recipe_type"));
            if (projectTag.contains("ui")) {
                uiTemplate = UITemplate.CODEC.parse(Platform.getFrozenRegistry().createSerializationContext(NbtOps.INSTANCE), projectTag.get("ui")).getOrThrow();
            }
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

    public boolean isRequireFuelForWorking() {
        return fuelRecipeConfig.isEnable();
    }

    public List<RecipeHolder<MBDRecipe>> searchFuelRecipe(RecipeManager recipeManager, IRecipeCapabilityHolder holder) {
        if (!fuelRecipeConfig.isEnable() || !holder.hasProxies()) return Collections.emptyList();
        return fuelRecipeConfig.fuelRecipeTypes.parallelStream().filter(type -> !type.equals(registryName))
                .flatMap(type -> recipeManager.getAllRecipesFor(this).parallelStream())
                .filter(recipe -> recipe.value().matchRecipe(holder).isSuccess() && recipe.value().matchTickRecipe(holder).isSuccess())
                .sorted(Comparator.comparingInt(r -> r.value().priority)).collect(Collectors.toList());
    }

    public List<RecipeHolder<MBDRecipe>> searchRecipe(RecipeManager recipeManager, IRecipeCapabilityHolder holder) {
        if (!holder.hasProxies()) return Collections.emptyList();
        return recipeManager.getAllRecipesFor(this).parallelStream()
                .filter(recipe -> recipe.value().matchRecipe(holder).isSuccess() && recipe.value().matchTickRecipe(holder).isSuccess())
                .sorted(Comparator.comparingInt(r -> r.value().priority)).collect(Collectors.toList());
    }

    /// Recip Builder
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


    /// Serialization
    @Override
    public CompoundTag serializeNBT(@Nonnull HolderLookup.Provider provider) {
        var tag = IPersistedSerializable.super.serializeNBT(provider);
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
        // builtin recipes
        builtinRecipes.clear();
        var recipesTag = tag.getCompound("builtinRecipes");
        for (var key : recipesTag.getAllKeys()) {
            var recipe = MBDRecipeSerializer.SERIALIZER.codec().codec().parse(NbtOps.INSTANCE, recipesTag.getCompound(key)).getOrThrow();
            recipe.recipeType = this;
            builtinRecipes.put(ResourceLocation.parse(key), recipe);
        }
    }

    /// UI
    public void bindXEIRecipeUI(UI ui, MBDRecipe recipe) {
        ui.selectId("@progress_bar", ProgressBar.class).forEach(progress -> {
            progress.setRange(0, Math.max(1, recipe.duration));
            progress.setProgress(Math.max(1, recipe.duration));
            progress.style(style -> style.tooltips(Component.translatable("recipe.duration.value", recipe.duration)));
        });
        ui.selectId("@duration", Label.class).forEach(label ->
                label.setText(Component.translatable("recipe.duration.value", recipe.duration)));
        ui.selectId("@condition", Label.class).forEach(label ->
                label.setText(recipe.conditions.stream()
                        .map(RecipeCondition::getTooltips)
                        .map(Component::getString)
                        .collect(Collectors.joining("\n"))));
//        bindCapIOUI(ui, recipe.inputs, IO.IN);
        bindCapIOUI(ui, recipe.inputs, IO.IN);
        bindCapIOUI(ui, recipe.outputs, IO.OUT);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void bindCapIOUI(UI ui, Map<RecipeCapability<?>, List<Content>> values, IO io) {
        values.forEach((cap, contents) -> {
            for (int i = 0; i < contents.size(); i++) {
                var content = contents.get(i);
                var id = content.uiName.isEmpty() ? "@%s_%s_%d".formatted(cap.name, io.name, i) : content.uiName;
                ui.selectRegex(Pattern.quote(id)).forEach(widget -> {
                    ((RecipeCapability) cap).bindXEIWidget(widget, content, io);
                    var tooltips = new ArrayList<Component>();
                    content.appendTooltip(tooltips);
                    if (!tooltips.isEmpty()) {
                        widget.style(style -> style.tooltips(tooltips.toArray(Component[]::new)));
                    }
                });
            }
        });
    }

    public UI createRecipeUI(MBDRecipe recipe) {
        var ui = uiTemplate == null ? UITemplate.missing().createUI() : uiTemplate.createUI();
        bindXEIRecipeUI(ui, recipe);
        var event = new RecipeUIEvent(this, recipe, ui);
        NeoForge.EVENT_BUS.post(event.postKubeJSEvent());
        return event.getUi();
    }

    private Configurator configureProxyRecipes(Supplier<ResourceLocation> getter, Consumer<ResourceLocation> setter) {
        return new SearchComponentConfigurator<>("editor.machine.recipe_type", getter, setter,
                ResourceLocation.parse("smelting"), true, (word, find) -> {
            for (var recipeType : BuiltInRegistries.RECIPE_TYPE) {
                if (Thread.currentThread().isInterrupted()) return;
                var id = BuiltInRegistries.RECIPE_TYPE.getKey(recipeType);
                if (id != null && id.toString().contains(word.toLowerCase()) && !id.equals(registryName)) {
                    find.accept(id);
                }
            }}, ResourceLocation::toString,
                UIElementProvider.text(recipeType -> Component.literal(recipeType.toString())));
    }

    private ResourceLocation addDefaultProxyRecipe() {
        return BuiltInRegistries.RECIPE_TYPE.getKey(RecipeType.SMELTING);
    }

    public final class FuelRecipeConfig implements IToggleConfigurable {
        @Persisted
        private boolean enable;

        @Configurable(name = "fuelRecipeConfig.fuel_recipe_types", tips = {
                "fuelRecipeConfig.fuel_recipe_types.tooltip.0",
                "fuelRecipeConfig.fuel_recipe_types.tooltip.1"
        })
        @ConfigList(configuratorMethod = "configureFuelRecipeTypes", addDefaultMethod = "addDefaultFuelRecipe")
        @Getter
        private final List<ResourceLocation> fuelRecipeTypes = new ArrayList<>();

        private Configurator configureFuelRecipeTypes(Supplier<ResourceLocation> getter, Consumer<ResourceLocation> setter) {
            return new SearchComponentConfigurator<>("editor.machine.recipe_type", getter, setter,
                    DUMMY.registryName, true, (word, find) -> {
                var wordLower = word.toLowerCase();
                // add recipe type configurator
                var candidates = new HashSet<ResourceLocation>();
                candidates.add(MBDRecipeType.DUMMY.getRegistryName());
                // add all loaded recipe types
                candidates.addAll(MBDRegistries.RECIPE_TYPES.keys());

                for (var candidate : candidates) {
                    if (Thread.currentThread().isInterrupted()) return;
                    if (candidate.toString().contains(wordLower) && !candidate.equals(registryName)) {
                        find.accept(candidate);
                    }
                }

                // add from files
                var path = new File(MBD2.getLocation(), "recipe_type");
                FileUtils.loadNBTFiles(path, ".rt", (file, tag) -> {
                    if (Thread.currentThread().isInterrupted()) return;
                    var recipeType = tag.getCompound("data").getCompound("recipe_type").getString("registryName");
                    if (!recipeType.isEmpty() && LDLib2.isValidResourceLocation(recipeType) && !recipeType.equals(registryName.toString())) {
                        find.accept(ResourceLocation.parse(recipeType));
                    }
                });
                }, ResourceLocation::toString,
                    UIElementProvider.text(recipeType -> Component.literal(recipeType.toString())));
        }

        private ResourceLocation addDefaultFuelRecipe() {
            return DUMMY.registryName;
        }

        @Override
        public boolean isEnable() {
            return enable;
        }

        @Override
        public void setEnable(boolean b) {
            enable = b;
        }
    }
}
