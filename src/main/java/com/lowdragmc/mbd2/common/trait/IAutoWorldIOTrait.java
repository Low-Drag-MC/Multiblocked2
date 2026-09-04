package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.runtime.RuntimeAutoWorldIO;

import javax.annotation.Nullable;

/**
 * A trait that moves contents to and from the <b>world</b> — dropped items in a box, fluid blocks in a
 * region — as opposed to {@link IAutoIOTrait}, which talks to neighbouring block capabilities.
 * <p>
 * Exists so a blueprint node can reach these values without knowing which trait it is looking at. The
 * two implementors name their fields differently ({@code autoWorldInput} on the item trait,
 * {@code autoInput} on the fluid trait's <em>definition</em>), and both are needed to answer "is this
 * supported here" before a UI offers the controls.
 */
public interface IAutoWorldIOTrait extends ITrait {
    /** The values for pulling out of the world. */
    RuntimeAutoWorldIO getRuntimeAutoWorldInput();

    /** The values for pushing into the world. */
    RuntimeAutoWorldIO getRuntimeAutoWorldOutput();

    /**
     * Whichever of the two {@code io} names.
     *
     * @return null for {@link IO#BOTH} or {@link IO#NONE} — there is no single set of values for those,
     *         and silently picking one would be a coin flip the caller cannot see
     */
    @Nullable
    default RuntimeAutoWorldIO getRuntimeAutoWorldIO(IO io) {
        return switch (io) {
            case IN -> getRuntimeAutoWorldInput();
            case OUT -> getRuntimeAutoWorldOutput();
            default -> null;
        };
    }
}
