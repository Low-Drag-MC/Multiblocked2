package com.lowdragmc.mbd2.test.tests.runtime;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/**
 * Navigating a saved block entity tag down to a runtime value storage.
 *
 * <p>LDLib writes each persisted field under {@code managed.<persistedKey>}, and {@code TagUtils}
 * splits that key on dots into nested compounds. A machine's storage is therefore
 * {@code managed.runtimeValues}, and a trait's — whose refs get the {@code trait.<name>} prefix from
 * {@code MBDMachine.loadAdditionalTraits} — is {@code managed.trait.<name>.runtimeValues}.</p>
 */
final class RuntimeNbt {

    private RuntimeNbt() {}

    /** The {@code managed} compound, created if absent. */
    static CompoundTag managed(CompoundTag beTag) {
        return child(beTag, "managed");
    }

    /** The machine's own runtime value storage, created if absent. */
    static CompoundTag machineValues(CompoundTag beTag) {
        return child(managed(beTag), "runtimeValues");
    }

    /** A trait's runtime value storage, created if absent. */
    static CompoundTag traitValues(CompoundTag beTag, String traitName) {
        return child(child(child(managed(beTag), "trait"), traitName), "runtimeValues");
    }

    /** The machine's storage as saved, or null when the key is not there at all. */
    @Nullable
    static CompoundTag machineValuesIfPresent(CompoundTag beTag) {
        if (!beTag.contains("managed")) return null;
        var managed = beTag.getCompound("managed");
        return managed.contains("runtimeValues") ? managed.getCompound("runtimeValues") : null;
    }

    /** A trait's storage as saved, or null when any level of the path is missing. */
    @Nullable
    static CompoundTag traitValuesIfPresent(CompoundTag beTag, String traitName) {
        if (!beTag.contains("managed")) return null;
        var tag = beTag.getCompound("managed");
        for (var key : new String[]{"trait", traitName, "runtimeValues"}) {
            if (!tag.contains(key)) return null;
            tag = tag.getCompound(key);
        }
        return tag;
    }

    private static CompoundTag child(CompoundTag parent, String key) {
        if (!parent.contains(key)) {
            parent.put(key, new CompoundTag());
        }
        return parent.getCompound(key);
    }
}
