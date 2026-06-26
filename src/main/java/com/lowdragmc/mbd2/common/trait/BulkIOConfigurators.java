package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.SelectorConfigurator;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import lombok.experimental.UtilityClass;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@UtilityClass
public class BulkIOConfigurators {
    @OnlyIn(Dist.CLIENT)
    public static void add(ConfiguratorGroup group, String name, String tips,
                           Supplier<BulkIOState> supplier, Consumer<IO> updater) {
        var configurator = new SelectorConfigurator<>(
                name,
                supplier,
                state -> {
                    if (state != null && state.io() != null) {
                        updater.accept(state.io());
                    }
                },
                BulkIOState.MIXED,
                true,
                List.of(BulkIOState.MIXED, BulkIOState.IN, BulkIOState.OUT, BulkIOState.BOTH, BulkIOState.NONE),
                BulkIOState::translationKey);
        configurator.setTips(tips);
        group.addConfigurator(configurator);
    }
}
