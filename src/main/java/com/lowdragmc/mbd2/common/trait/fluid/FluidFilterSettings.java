package com.lowdragmc.mbd2.common.trait.fluid;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.DefaultValue;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IToggleConfigurable;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.mbd2.utils.TagUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

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
    @Configurable(name = "config.definition.trait.filter.match_nbt")
    private boolean matchNBT = false;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.filter.fluids")
    @NumberRange(range = {1, 1})
    private List<FluidStack> filterFluids = new ArrayList<>();
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.filter.fluid_tags", forceUpdate = false)
    @DefaultValue(stringValue = "forge:gaseous")
    private List<ResourceLocation> filterTags = new ArrayList<>();

    @Override
    public boolean test(FluidStack fluidStack) {
        if (!enable) {
            return true;
        }
        for (var filterFluids : filterFluids) {
            if (matchNBT) {
                if (filterFluids.isFluidStackEqual(fluidStack) && Objects.equals(filterFluids.getTag(), fluidStack.getTag())) {
                    return isWhitelist;
                }
            } else if (filterFluids.isFluidStackEqual(fluidStack)) {
                return isWhitelist;
            }
        }
        for (var filterTag : filterTags) {
            if (fluidStack.getFluid().is(TagUtil.optionalTag(ForgeRegistries.FLUIDS.getRegistryKey(), filterTag))) {
                return isWhitelist;
            }
        }
        return !isWhitelist;
    }
}
