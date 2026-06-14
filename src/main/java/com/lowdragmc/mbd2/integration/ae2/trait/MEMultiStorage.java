package com.lowdragmc.mbd2.integration.ae2.trait;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import net.minecraft.network.chat.Component;

import java.util.List;

public record MEMultiStorage(List<MEStorage> storages) implements MEStorage {
    @Override
    public boolean isPreferredStorageFor(AEKey what, IActionSource source) {
        for (var storage : storages) {
            if (storage.isPreferredStorageFor(what, source)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        MEStorage.checkPreconditions(what, amount, mode, source);
        long inserted = 0;
        for (var storage : storages) {
            if (inserted >= amount) break;
            inserted += storage.insert(what, amount - inserted, mode, source);
        }
        return inserted;
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        MEStorage.checkPreconditions(what, amount, mode, source);
        long extracted = 0;
        for (var storage : storages) {
            if (extracted >= amount) break;
            extracted += storage.extract(what, amount - extracted, mode, source);
        }
        return extracted;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        for (var storage : storages) {
            storage.getAvailableStacks(out);
        }
    }

    @Override
    public Component getDescription() {
        return storages.isEmpty() ? Component.empty() : storages.getFirst().getDescription();
    }
}
