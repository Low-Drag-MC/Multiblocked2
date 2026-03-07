package com.lowdragmc.mbd2.api.capability.recipe;

import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import lombok.Getter;

/**
 * The capability can be input or output or both
 */
@Getter
@KJSBindings
public enum IO {
    IN("import"),
    OUT("export"),
    BOTH("both"),
    NONE("none");

    public final String name;

    IO(String name) {
        this.name = name;
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
