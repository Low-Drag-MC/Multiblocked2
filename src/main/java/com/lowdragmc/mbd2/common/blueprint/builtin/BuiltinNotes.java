package com.lowdragmc.mbd2.common.blueprint.builtin;

/**
 * The palette the built-in blueprints are annotated with.
 *
 * <p>Shared so that the same thing is the same colour in every built-in: once a reader has learned that
 * the blue backdrop is "read the world" and the green one is "change something", the fifth blueprint is
 * legible before it is read. Colours are ARGB, matching {@code IHasElementColor}.</p>
 */
final class BuiltinNotes {

    private BuiltinNotes() {}

    /** The note that says what the blueprint is for — one per blueprint, top-left, in amber. */
    static final int HEADER_COLOR = 0xFFFFD54F;

    /** Reading the machine, the world, or the event. */
    static final int READ_GROUP = 0xFF4A6E8A;
    /** Working out a number or a decision from what was read. */
    static final int DECIDE_GROUP = 0xFF6E5A8A;
    /** Changing something — the machine, the recipe, the world. */
    static final int ACT_GROUP = 0xFF4A7F5A;
}
