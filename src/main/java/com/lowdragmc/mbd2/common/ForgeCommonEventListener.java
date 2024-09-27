package com.lowdragmc.mbd2.common;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.pattern.MultiblockWorldSavedData;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * @author KilaBash
 * @date 2022/8/27
 * @implNote ForgeCommonEventListener
 */
@Mod.EventBusSubscriber(modid = MBD2.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeCommonEventListener {

    @SubscribeEvent
    public static void registerCommand(RegisterCommandsEvent event) {
        ServerCommands.createServerCommands().forEach(event.getDispatcher()::register);
    }

    @SubscribeEvent
    public static void registerCommand(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getEntity().isCrouching() && event.getLevel() instanceof ServerLevel serverLevel) {
            var pos = event.getPos();
            for (var state : MultiblockWorldSavedData.getOrCreate(serverLevel).getControllerInPos(pos)) {
                if (state.getController() instanceof MBDMultiblockMachine machine) {
                    if (machine.getDefinition().machineSettings().hasUI() &&
                            machine.getDefinition().multiblockSettings().showUIWhenClickStructure()) {
                        machine.openUI(event.getEntity());
                        event.setCanceled(true);
                    }
                }
            }
        }
    }

}
