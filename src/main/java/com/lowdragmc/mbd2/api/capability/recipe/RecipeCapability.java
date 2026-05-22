package com.lowdragmc.mbd2.api.capability.recipe;

import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.registry.ILDLRegister;
import com.lowdragmc.mbd2.api.recipe.content.Content;
import com.lowdragmc.mbd2.api.recipe.content.ContentModifier;
import com.lowdragmc.mbd2.api.recipe.content.IContentSerializer;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Used to detect whether a machine has a certain capability.
 */
public abstract class RecipeCapability<T> implements ILDLRegister<RecipeCapability<?>, RecipeCapability<?>> {
    public static final Codec<RecipeCapability<?>> CODEC = MBDRegistries.RECIPE_CAPABILITIES.codec();

    public final String name;
    public final IContentSerializer<T> serializer;

    protected RecipeCapability(String name, IContentSerializer<T> serializer) {
        this.name = name;
        this.serializer = serializer;
    }

    /**
     * deep copy of this content from serializer.
     * it's not recommended to use this method directly, use {@link #copyContent(Object)} instead.
     */
    public final T deepCopyContent(Object content) {
        return serializer.deepCopyInner((T) content);
    }

    public final T deepCopyContent(Object content, ContentModifier modifier) {
        return serializer.copyWithModifier(deepCopyContent(content), modifier);
    }

    /**
     * copy of this content. recipe need it for searching and such things
     */
    public T copyInner(T content) {
        return serializer.copyInner(content);
    }

    /**
     * deep copy and modify the size attribute for those Content that have the size attribute.
     */
    public T copyWithModifier(T content, ContentModifier modifier){
        return serializer.copyWithModifier(content, modifier);
    }

    @SuppressWarnings("unchecked")
    public final T copyContent(Object content) {
        return copyInner((T) content);
    }

    @SuppressWarnings("unchecked")
    public final T copyContent(Object content, ContentModifier modifier) {
        return copyWithModifier((T) content, modifier);
    }

    /**
     * used for recipe builder via KubeJs.
     */
    public T of(Object o) {
        return serializer.of(o);
    }

    public Component getTraslateComponent() {
        return Component.translatable("recipe.capability.%s.name".formatted(name));
    }

    /**
     * create a default / example content of this capability.
     */
    public abstract T createDefaultContent();

    /**
     * create a preview widget for the content of this capability.
     * <br>
     * it will be used for ui editor. the widget will be resized to (18, 18)
     */
    public abstract UIElement createPreview(Supplier<T> content);

    /**
     * create a widget for recipe viewer (XEI).
     * <br>
     * just create the template, call {@link #bindXEIWidget(UIElement, Content, IO)} to bind the content.
     */
    public abstract UIElement createXEITemplate();

    /**
     * bind the content to the widget. you should do the casting yourself.
     * @param io the ingredient io for the widget. mark it as inputs or outputs or render only..
     */
    public abstract void bindXEIWidget(UIElement element, Content content, IO io);

    /**
     * create a content ui configurator for the content of this capability.
     */
    public abstract void createContentConfigurator(ConfiguratorGroup father, Supplier<T> supplier, Consumer<T> onUpdate);

    /**
     * Get the error info for the left content.
     */
    public abstract Component getLeftErrorInfo(List<T> left);

    //TODO
    public double calculateAmount(List<T> left) {
        return 1;
    }

}
