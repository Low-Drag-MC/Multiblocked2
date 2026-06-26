package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.lowdraglib2.configurator.IToggleConfigurable;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import lombok.Getter;
import lombok.Setter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ToggleAutoIO extends AutoIO implements IToggleConfigurable {
    @Getter
    @Setter
    @Persisted
    private boolean enable;

    @Override
    public BulkIOState getAllIOState() {
        return enable ? super.getAllIOState() : BulkIOState.NONE;
    }

    @Override
    public void setAllIO(IO io) {
        super.setAllIO(io);
        enable = io != IO.NONE;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void buildConfigurator(ConfiguratorGroup father) {
        BulkIOConfigurators.add(father, "config.definition.trait.io.all",
                "config.definition.trait.io.all.auto.tooltip", this::getAllIOState, this::setAllIO);
        IToggleConfigurable.super.buildConfigurator(father);
    }
}
