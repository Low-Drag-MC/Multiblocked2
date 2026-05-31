package com.lowdragmc.mbd2.client;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.item.MBDGadgetsItem;
import com.lowdragmc.mbd2.integration.create.machine.KineticInstanceRenderer;
import net.createmod.catnip.render.SuperByteBufferCache;
import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import java.util.Optional;

/**
 * @author KilaBash
 * @date 2023/7/30
 * @implNote ClientProxy
 */
@OnlyIn(Dist.CLIENT)
public class ClientProxy {
    public ClientProxy(IEventBus eventBus) {
        eventBus.register(this);
        if (MBD2.isCreateLoaded()) {
            SuperByteBufferCache.getInstance().registerCompartment(KineticInstanceRenderer.DIRECTIONAL_PARTIAL);
        }
    }

    @SubscribeEvent
    public void registerRenderers(EntityRenderersEvent.RegisterRenderers e) {
        MBDRegistries.FAKE_MACHINE().initRenderer(e);
        MBDRegistries.MACHINE_DEFINITIONS.forEach(definition -> definition.initRenderer(e));
    }

    @SubscribeEvent
    public void clientSetup(final FMLClientSetupEvent e) {
        e.enqueueWork(()-> ItemProperties.register(MBDRegistries.GADGETS_ITEM(), MBD2.id("mode"),
                (itemStack, clientWorld, entity, seed) ->
                        Optional.ofNullable(MBDGadgetsItem.getMode(itemStack)).orElse(MBDGadgetsItem.Mode.RECIPE_DEBUGGER).id)
        );
    }
}
