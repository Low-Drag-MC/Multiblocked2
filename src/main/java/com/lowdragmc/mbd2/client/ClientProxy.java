package com.lowdragmc.mbd2.client;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.CommonProxy;
import com.lowdragmc.mbd2.integration.create.machine.KineticInstanceRenderer;
import com.simibubi.create.CreateClient;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * @author KilaBash
 * @date 2023/7/30
 * @implNote ClientProxy
 */
@OnlyIn(Dist.CLIENT)
public class ClientProxy extends CommonProxy {
    public ClientProxy() {
        super();
        if (MBD2.isCreateLoaded()) {
            CreateClient.BUFFER_CACHE.registerCompartment(KineticInstanceRenderer.DIRECTIONAL_PARTIAL);
        }
    }

    @SubscribeEvent
    public void registerRenderers(RegisterRenderers e) {
        MBDRegistries.getFAKE_MACHINE().initRenderer(e);
        MBDRegistries.MACHINE_DEFINITIONS.forEach(definition -> definition.initRenderer(e));
        MBDRegistries.getFAKE_MACHINE().initRenderer(e);
    }
}
