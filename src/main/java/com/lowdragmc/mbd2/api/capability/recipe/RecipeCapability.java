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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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
    /** Resolved on first use by {@link #contentType()}. */
    private Type contentType;

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

    /**
     * The type of this capability's content payload — {@code SizedIngredient} for items,
     * {@code Integer} for energy, and so on.
     *
     * <p>A {@link Content} holds its payload as a bare {@code Object} and does not record which
     * capability it belongs to, so anything generic over capabilities has no way to say what it is
     * holding. This recovers that: the answer is already written down in the type argument every
     * subclass must declare to compile, so it costs a subclass nothing and cannot drift out of sync
     * with {@link #serializer}. Returns {@code Object.class} for a subclass raw enough to have not
     * declared one.</p>
     */
    public final Type contentType() {
        // Benign race: two threads may both resolve it, and both get the same answer.
        if (contentType == null) {
            contentType = resolveContentType(getClass());
        }
        return contentType;
    }

    private static Type resolveContentType(Class<?> type) {
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            if (current.getGenericSuperclass() instanceof ParameterizedType parameterized
                    && parameterized.getRawType() == RecipeCapability.class) {
                return parameterized.getActualTypeArguments()[0];
            }
        }
        return Object.class;
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

    public enum XEILayoutType { SLOT, BAR }

    /**
     * layout zone hint for the editor: SLOT capabilities flow into the IO slot columns,
     * BAR capabilities flow into the bar container below the IO row.
     */
    public XEILayoutType xeiLayoutType() {
        return XEILayoutType.SLOT;
    }

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
