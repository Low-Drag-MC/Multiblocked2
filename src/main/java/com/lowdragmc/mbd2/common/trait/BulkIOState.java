package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.mbd2.api.capability.recipe.IO;

import java.util.Collection;

public enum BulkIOState {
    MIXED(null, "config.definition.trait.io.all.mixed"),
    IN(IO.IN, "gui.mbd2.io.import"),
    OUT(IO.OUT, "gui.mbd2.io.export"),
    BOTH(IO.BOTH, "gui.mbd2.io.both"),
    NONE(IO.NONE, "gui.mbd2.io.none");

    private final IO io;
    private final String translationKey;

    BulkIOState(IO io, String translationKey) {
        this.io = io;
        this.translationKey = translationKey;
    }

    public IO io() {
        return io;
    }

    public String translationKey() {
        return translationKey;
    }

    public static BulkIOState fromIO(IO io) {
        return switch (io) {
            case IN -> IN;
            case OUT -> OUT;
            case BOTH -> BOTH;
            case NONE -> NONE;
        };
    }

    public static BulkIOState combineIO(Collection<IO> values) {
        BulkIOState result = null;
        for (var value : values) {
            var state = fromIO(value);
            if (result != null && result != state) {
                return MIXED;
            }
            result = state;
        }
        return result == null ? MIXED : result;
    }

    public static BulkIOState combineStates(Collection<BulkIOState> values) {
        BulkIOState result = null;
        for (var state : values) {
            if (state == MIXED || result != null && result != state) {
                return MIXED;
            }
            result = state;
        }
        return result == null ? MIXED : result;
    }
}
