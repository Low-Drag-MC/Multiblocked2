package com.lowdragmc.mbd2.common.machine.definition.config.toggle;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSearch;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ToggleCreativeTab extends ToggleObject<ResourceLocation> {
    public static final ResourceLocation DEFAULT = ResourceLocation.parse("redstone_blocks");
    @Getter
    @Setter
    @Configurable
    @ConfigSearch(searchConfiguratorMethod = "searchConfigurator")
    private ResourceLocation value;

    public ToggleCreativeTab(ResourceLocation value, boolean enable) {
        setValue(value);
        this.enable = enable;
    }

    public ToggleCreativeTab(ResourceLocation value) {
        this(value, true);
    }

    public ToggleCreativeTab(boolean enable) {
        this(DEFAULT, enable);
    }

    public ToggleCreativeTab() {
        this(false);
    }

    private SearchComponentConfigurator.ISearchConfigurator<ResourceLocation> searchConfigurator() {
        return new SearchComponentConfigurator.ISearchConfigurator<>() {

            @Override
            public void search(String word, IResultHandler<ResourceLocation> searchHandler) {
                var wordLower = word.toLowerCase();
                for (var key : BuiltInRegistries.CREATIVE_MODE_TAB.keySet()) {
                    if (Thread.currentThread().isInterrupted()) return;
                    if (key.toString().contains(wordLower)) {
                        searchHandler.accept(key);
                    }
                }
            }

            @Override
            public ResourceLocation defaultValue() {
                return DEFAULT;
            }

            @Override
            public String resultText(ResourceLocation value) {
                return value.toString();
            }
        };
    }
}
