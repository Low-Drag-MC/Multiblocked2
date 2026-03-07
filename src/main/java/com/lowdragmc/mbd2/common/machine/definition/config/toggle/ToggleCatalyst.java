package com.lowdragmc.mbd2.common.machine.definition.config.toggle;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSelector;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.SkipPersistedValue;
import com.lowdragmc.mbd2.common.trait.item.ItemFilterSettings;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.level.block.Block;

@Getter
@Setter
public class ToggleCatalyst extends ItemFilterSettings {
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
    @Configurable(name = "config.multiblock_settings.catalyst.candidates", subConfigurable = true,
            tips = "config.multiblock_settings.catalyst.candidates.tooltip")
    private ToggleCandidates candidates = new ToggleCandidates();
    @Configurable(name = "config.multiblock_settings.catalyst.consume_type")
    @ConfigSelector(subConfiguratorBuilder = "catalystTypeConfigurator")
    private CatalystType catalystType = CatalystType.CONSUME_ITEM;
    @Persisted
    private int consumeItemAmount = 0;
    @Persisted
    private int consumeDurabilityValue = 1;

    private void catalystTypeConfigurator(CatalystType type, ConfiguratorGroup group) {
        if (type == CatalystType.CONSUME_ITEM) {
            group.addConfigurators(new NumberConfigurator("config.multiblock_settings.catalyst.consume_type.consume_item.amount",
                    () -> consumeItemAmount,
                    value -> consumeItemAmount = value.intValue(),
                    0,
                    true).setRange(0, 64).setWheel(1));
        } else {
            group.addConfigurators(new NumberConfigurator("config.multiblock_settings.catalyst.consume_type.consume_durability.amount",
                    () -> consumeDurabilityValue,
                    value -> consumeDurabilityValue = value.intValue(),
                    1,
                    true).setRange(1, Integer.MAX_VALUE).setWheel(1));
        }
    }

    @SkipPersistedValue(field = "consumeItemAmount")
    public boolean skipIntFieldPersisted(int value) {
        return catalystType == CatalystType.CONSUME_ITEM || value == 0;
    }

    @SkipPersistedValue(field = "consumeDurabilityValue")
    public boolean skipIntFieldPersisted2(int value) {
        return catalystType == CatalystType.CONSUME_DURABILITY || value == 1;
    }

    public static class ToggleCandidates extends ToggleObject<Block[]> {
        @Getter
        @Setter
        @Configurable
        @DefaultValue(numberValue = {0, 0, 0, 1, 1, 1})
        private Block[] value;

        public ToggleCandidates(Block[] value, boolean enable) {
            setValue(value);
            this.enable = enable;
        }

        public ToggleCandidates(Block[] value) {
            this(value, true);
        }

        public ToggleCandidates(boolean enable) {
            this(new Block[0], enable);
        }

        public ToggleCandidates() {
            this(false);
        }
    }
}
