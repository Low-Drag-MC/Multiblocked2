package com.lowdragmc.mbd2.common;

import com.lowdragmc.mbd2.Multiblocked2;
import com.lowdragmc.mbd2.config.ConfigHolder;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class CommonProxy {

    public CommonProxy() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        eventBus.register(this);
        CommonProxy.init();
    }

    public static void init() {
        Multiblocked2.LOGGER.info("GTCEu common proxy init!");
        ConfigHolder.init();
    }


    @SubscribeEvent
    public void modConstruct(FMLConstructModEvent event) {

    }

    @SubscribeEvent
    public void commonSetup(FMLCommonSetupEvent e) {
        e.enqueueWork(() -> {

        });
    }

    @SubscribeEvent
    public void loadComplete(FMLLoadCompleteEvent e) {
        e.enqueueWork(() -> {

        });
    }

}
