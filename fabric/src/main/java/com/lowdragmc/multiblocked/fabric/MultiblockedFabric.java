package com.lowdragmc.multiblocked.fabric;

import com.lowdragmc.multiblocked.Multiblocked;
import net.fabricmc.api.ModInitializer;

public class MultiblockedFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Multiblocked.init();
    }
}
