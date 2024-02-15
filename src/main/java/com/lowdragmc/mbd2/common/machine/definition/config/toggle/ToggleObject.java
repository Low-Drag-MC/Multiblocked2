package com.lowdragmc.mbd2.common.machine.definition.config.toggle;

import com.lowdragmc.lowdraglib.gui.editor.configurator.IToggleConfigurable;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import lombok.Getter;
import lombok.Setter;

public abstract class ToggleObject<T> implements IToggleConfigurable {

    @Getter
    @Setter
    @Persisted
    protected boolean enable;

    public abstract T getValue();

    public abstract void setValue(T value);

    public static <T> ToggleObject<T> ofDisabled() {
        return of(false, null);
    }

    public static <T> ToggleObject<T> ofDisabled(T value) {
        return of(false, value);
    }

    public static <T> ToggleObject<T> of(boolean enabled, T value) {
        var wrapper = new ToggleObject<T>() {
            @Getter
            @Setter
            private T value;
        };
        wrapper.setValue(value);
        wrapper.setEnable(enabled);
        return wrapper;
    }

}
