package com.lowdragmc.mbd2.common.trait.fluid;

import com.lowdragmc.lowdraglib2.configurator.IToggleConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigList;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.TagKeySearchComponent;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class FluidFilterSettings implements IToggleConfigurable, Predicate<FluidStack> {
    @Getter
    @Setter
    @Persisted
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
    @Configurable(name = "config.definition.trait.filter.fluids")
    private List<FluidStack> filterFluids = new ArrayList<>();
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.filter.fluid_tags")
    @ConfigList(configuratorMethod = "buildFilterTagsConfigurator", addDefaultMethod = "addDefaultFilterTags")
    private List<ResourceLocation> filterTags = new ArrayList<>();

    @Override
    public boolean test(FluidStack fluidStack) {
        return !enable || matches(fluidStack);
    }

    /** @see com.lowdragmc.mbd2.common.trait.item.ItemFilterSettings#matches */
    public boolean matches(FluidStack fluidStack) {
        for (var filterFluids : filterFluids) {
            if (matchComponent) {
                if (FluidStack.isSameFluidSameComponents(filterFluids, fluidStack)) {
                    return isWhitelist;
                }
            } else if (filterFluids.getFluid() == fluidStack.getFluid()) {
                return isWhitelist;
            }
        }
        for (var filterTag : filterTags) {
            if (fluidStack.getFluid().is(FluidTags.create(filterTag))) {
                return isWhitelist;
            }
        }
        return !isWhitelist;
    }

    private Configurator buildFilterTagsConfigurator(Supplier<ResourceLocation> getter, Consumer<ResourceLocation> setter) {
        return new TagKeySearchComponent.Fluid("",
                () -> FluidTags.create(getter.get()),
                tagKey -> setter.accept(tagKey.location()),
                Tags.Fluids.WATER,
                true
        );
    }

    private ResourceLocation addDefaultFilterTags() {
        return Tags.Fluids.WATER.location();
    }
}
