package com.lowdragmc.mbd2.common.machine.definition.config.toggle;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import lombok.Getter;
import lombok.Setter;

public class ToggleRenderer extends ToggleObject<IRenderer> {
    @Getter
    @Setter
    @Configurable
    private IRenderer value;

    public ToggleRenderer(IRenderer value, boolean enable) {
        setValue(value);
        this.enable = enable;
    }

    public ToggleRenderer(IRenderer value) {
        this(value, true);
    }

    public ToggleRenderer(boolean enable) {
        this(IRenderer.EMPTY, enable);
    }

    public ToggleRenderer() {
        this(false);
    }

}
