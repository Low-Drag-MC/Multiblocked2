package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.utils.TagUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The data of the traits that {@link TraitDefinition#isKeepDataOnDrop() keep it} in their machine's dropped item:
 * their persisted fields, under the keys the machine saves them with.
 */
public final class TraitItemData {
    // what a trait of each definition saves before anything happens to it, so an item with nothing to keep stays plain
    private static final Map<TraitDefinition, CompoundTag> FRESH = Collections.synchronizedMap(new WeakHashMap<>());

    private TraitItemData() {}

    public static void rememberFresh(Trait trait) {
        FRESH.computeIfAbsent(trait.getDefinition(), definition -> save(trait, Platform.getFrozenRegistry()));
    }

    /**
     * @return whether the trait had anything to keep, which then went into {@code into}
     */
    public static boolean save(Trait trait, CompoundTag into, HolderLookup.Provider provider) {
        var data = save(trait, provider);
        if (data.isEmpty() || data.equals(FRESH.get(trait.getDefinition()))) return false;
        into.merge(data);
        return true;
    }

    /**
     * @return whether {@code from} held data for the trait
     */
    public static boolean load(Trait trait, CompoundTag from, HolderLookup.Provider provider) {
        var ops = provider.createSerializationContext(NbtOps.INSTANCE);
        var loaded = false;
        for (var ref : trait.getSyncStorage().getPersistedFields()) {
            var data = TagUtils.getTagExtended(from, ref.getPersistedKey());
            if (data == null) continue;
            ref.writePersisted(ops, data);
            ref.markAsDirty();
            loaded = true;
        }
        return loaded;
    }

    private static CompoundTag save(Trait trait, HolderLookup.Provider provider) {
        var ops = provider.createSerializationContext(NbtOps.INSTANCE);
        var tag = new CompoundTag();
        for (var ref : trait.getSyncStorage().getPersistedFields()) {
            var data = ref.readPersisted(ops);
            if (data != null) {
                TagUtils.setTagExtended(tag, ref.getPersistedKey(), data);
            }
        }
        return tag;
    }
}
