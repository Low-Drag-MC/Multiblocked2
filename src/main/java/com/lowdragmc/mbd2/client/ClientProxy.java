package com.lowdragmc.mbd2.client;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.block.ProxyPartBlock;
import com.lowdragmc.mbd2.api.blockentity.ProxyPartBlockEntity;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.client.renderer.MBDBESRenderer;
import com.lowdragmc.mbd2.common.item.MBDGadgetsItem;
import com.lowdragmc.mbd2.integration.create.machine.KineticInstanceRenderer;
import net.createmod.catnip.render.SuperByteBufferCache;
import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
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
        // Baked models on a proxy port render through IBlockRendererProvider, but BER-driven renderers
        // (GeckoLib above all) need this registration or they get no render call at all (#236). Ports
        // whose proxy state is model-only stay free: LDLib2's dispatcher mixin drops the renderer when
        // ProxyPartRenderer reports hasBlockEntityRenderer == false.
        e.registerBlockEntityRenderer(ProxyPartBlockEntity.TYPE.get(), MBDBESRenderer::getOrCreate);
        MBDRegistries.FAKE_MACHINE().initRenderer(e);
        MBDRegistries.MACHINE_DEFINITIONS.forEach(definition -> definition.initRenderer(e));
    }

    @SubscribeEvent
    public void registerClientExtensions(RegisterClientExtensionsEvent e) {
        e.registerBlock(MBDClientBlockExtensions.PROXY_PART, ProxyPartBlock.BLOCK);
        e.registerBlock(MBDClientBlockExtensions.MACHINE, MBDRegistries.FAKE_MACHINE().block());
        MBDRegistries.MACHINE_DEFINITIONS.forEach(definition ->
                e.registerBlock(MBDClientBlockExtensions.MACHINE, definition.block()));
    }

    @SubscribeEvent
    public void clientSetup(final FMLClientSetupEvent e) {
        // MBD2's built-in UIs, in the editor's ui resource browser. Read-only, and copyable to a file
        // provider for anyone who wants their own editable version.
        e.enqueueWork(()-> ItemProperties.register(MBDRegistries.GADGETS_ITEM(), MBD2.id("mode"),
                (itemStack, clientWorld, entity, seed) ->
                        Optional.ofNullable(MBDGadgetsItem.getMode(itemStack)).orElse(MBDGadgetsItem.Mode.RECIPE_DEBUGGER).id)
        );
    }
}
