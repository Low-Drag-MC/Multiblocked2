package com.lowdragmc.mbd2.common.trait.item;

import com.lowdragmc.lowdraglib2.configurator.IToggleConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigList;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.TagKeySearchComponent;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ItemFilterSettings implements IToggleConfigurable, Predicate<ItemStack> {
    @Getter
    @Setter
    private boolean enable;

    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.filter.whitelist")
    private boolean isWhitelist = true;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.filter.match_component")
    private boolean matchComponent = false;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.filter.items")
    private List<ItemStack> filterItems = new ArrayList<>();
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.filter.item_tags")
    @ConfigList(configuratorMethod = "buildFilterTagsConfigurator", addDefaultMethod = "addDefaultFilterTags")
    private List<ResourceLocation> filterTags = new ArrayList<>();

    @Override
    public boolean test(ItemStack itemStack) {
        return !enable || matches(itemStack);
    }

    /**
     * The filter's verdict on its own terms, ignoring whether it is switched on.
     * <p>
     * {@link #test} answers "does this settings object let the stack through", which includes "it is
     * switched off, so yes". A caller that decides for itself whether the filter applies — a trait
     * reading the {@code filter.enable} runtime value, which a machine may have turned <b>on</b> over a
     * definition that has it off — needs the verdict without that shortcut, or enabling it would let
     * everything through.
     */
    public boolean matches(ItemStack itemStack) {
        for (var filterItem : filterItems) {
            if (matchComponent) {
                if (ItemStack.isSameItemSameComponents(filterItem, itemStack)) {
                    return isWhitelist;
                }
            } else if (ItemStack.isSameItem(filterItem, itemStack)) {
                return isWhitelist;
            }
        }
        for (var filterTag : filterTags) {
            if (itemStack.is(ItemTags.create(filterTag))) {
                return isWhitelist;
            }
        }
        return !isWhitelist;
    }

    private Configurator buildFilterTagsConfigurator(Supplier<ResourceLocation> getter, Consumer<ResourceLocation> setter) {
        return new TagKeySearchComponent.Item("",
                () -> ItemTags.create(getter.get()),
                tagKey -> setter.accept(tagKey.location()),
                Tags.Items.INGOTS,
                true
        );
    }

    private ResourceLocation addDefaultFilterTags() {
        return Tags.Items.INGOTS.location();
    }
}
