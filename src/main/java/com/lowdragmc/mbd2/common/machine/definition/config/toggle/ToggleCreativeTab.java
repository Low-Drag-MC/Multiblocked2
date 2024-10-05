package com.lowdragmc.mbd2.common.machine.definition.config.toggle;

import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.SelectorConfigurator;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;

public class ToggleCreativeTab extends ToggleObject<ResourceLocation> {
    public static final ResourceLocation DEFAULT = new ResourceLocation("redstone_blocks");
    @Getter
    @Setter
    @Persisted
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

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        super.buildConfigurator(father);
        father.addConfigurators(new SelectorConfigurator<>("value", this::getValue, this::setValue, DEFAULT, true,
                new ArrayList<>(BuiltInRegistries.CREATIVE_MODE_TAB.keySet()), ResourceLocation::toString));
    }
}
