package com.lowdragmc.multiblocked.forge;

import com.lowdragmc.multiblocked.Multiblocked;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.spongepowered.asm.launch.MixinInitialisationError;

@Mod(Multiblocked.MOD_ID)
public class MultiblockedForge {
    public MultiblockedForge() {
        Multiblocked.init();
        // registrate must be given the mod event bus on forge before registration
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
//        LDLib.REGISTRATE.registerEventListeners(eventBus);
    }
}
