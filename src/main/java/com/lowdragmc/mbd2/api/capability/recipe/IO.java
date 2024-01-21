package com.lowdragmc.mbd2.api.capability.recipe;

import com.lowdragmc.lowdraglib.gui.editor.Icons;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import lombok.Getter;

/**
 * The capability can be input or output or both
 */
@Getter
public enum IO {
    IN("import"),
    OUT("export"),
    BOTH("both"),
    NONE("none");

    public final String name;
    public final IGuiTexture icon;

    IO(String name) {
        this.name = name;
        this.icon = Icons.borderText(getTooltip());
    }

    public String getTooltip() {
        return "gui.mbd2.io." + name;
    }

    public boolean support(IO io) {
        if (io == this) return true;
        if (io == NONE) return false;
        return this == BOTH;
    }

}
