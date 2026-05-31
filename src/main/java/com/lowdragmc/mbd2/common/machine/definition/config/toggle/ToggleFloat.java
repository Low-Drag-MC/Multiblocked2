package com.lowdragmc.mbd2.common.machine.definition.config.toggle;

import com.lowdragmc.lowdraglib2.configurator.IToggleConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A toggleable float container: when {@link #isEnable()} is {@code false} the inner value
 * is treated as "unset" (and skipped from persistence by default — see
 * {@link IToggleConfigurable}). When enabled, {@link #getValue()} carries the active float.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ToggleFloat implements IToggleConfigurable {
    protected boolean enable;

    @Configurable(name = "config.toggle_float.value")
    @ConfigNumber(range = {-Float.MAX_VALUE, Float.MAX_VALUE})
    private float value;

    public static ToggleFloat ofDisabled() {
        return new ToggleFloat(false, 0f);
    }

    public static ToggleFloat of(boolean enabled, float value) {
        return new ToggleFloat(enabled, value);
    }
}
