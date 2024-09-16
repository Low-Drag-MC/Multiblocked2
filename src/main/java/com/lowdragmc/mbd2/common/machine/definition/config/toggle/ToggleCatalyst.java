package com.lowdragmc.mbd2.common.machine.definition.config.toggle;

import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorSelectorConfigurator;
import com.lowdragmc.lowdraglib.gui.editor.configurator.NumberConfigurator;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.mbd2.common.trait.item.ItemFilterSettings;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

public class ToggleCatalyst extends ItemFilterSettings {
    @Getter
    @Setter
    @Persisted
    protected boolean enable;

    public enum CatalystType {
        CONSUME_ITEM("config.multiblock_settings.catalyst.consume_type.consume_item"),
        CONSUME_DURABILITY("config.multiblock_settings.catalyst.consume_type.consume_durability");

        @Getter
        public final String translateKey;

        CatalystType(String translateKey) {
            this.translateKey = translateKey;
        }
    }

    @Getter
    private CatalystType catalystType = CatalystType.CONSUME_ITEM;
    @Getter
    private int consumeItemAmount = 0;
    @Getter
    private int consumeDurabilityValue = 1;

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        super.buildConfigurator(father);
        father.addConfigurators(new ConfiguratorSelectorConfigurator<>(
                "config.multiblock_settings.catalyst.consume_type",
                false, () -> catalystType,
                type -> catalystType = type,
                CatalystType.CONSUME_ITEM,
                true,
                Arrays.stream(CatalystType.values()).toList(),
                CatalystType::getTranslateKey,
                (type, configurator) -> {
                    if (type == CatalystType.CONSUME_ITEM) {
                        configurator.addConfigurators(new NumberConfigurator("config.multiblock_settings.catalyst.consume_type.consume_item.amount",
                                () -> consumeItemAmount,
                                value -> consumeItemAmount = value.intValue(),
                                0,
                                true).setRange(0, 64).setWheel(1));
                    } else {
                        configurator.addConfigurators(new NumberConfigurator("config.multiblock_settings.catalyst.consume_type.consume_durability.amount",
                                () -> consumeDurabilityValue,
                                value -> consumeDurabilityValue = value.intValue(),
                                1,
                                true).setRange(1, Integer.MAX_VALUE).setWheel(1));
                    }
                }
                ));
    }
}
