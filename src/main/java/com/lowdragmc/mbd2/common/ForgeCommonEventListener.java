package com.lowdragmc.mbd2.common;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.machine.IMultiController;
import com.lowdragmc.mbd2.api.pattern.MultiblockState;
import com.lowdragmc.mbd2.api.pattern.MultiblockWorldSavedData;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import com.lowdragmc.mbd2.common.machine.definition.MultiblockMachineDefinition;
import net.minecraft.core.Direction;
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
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ServerCommands.createServerCommands().forEach(event.getDispatcher()::register);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // on multiblock ui click
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            var pos = event.getPos();
            if (!event.getEntity().isCrouching()) {
                for (var state : MultiblockWorldSavedData.getOrCreate(serverLevel).getControllerInPos(pos)) {
                    if (state.getController() instanceof MBDMultiblockMachine machine) {
                        if (machine.getDefinition().machineSettings().hasUI() &&
                                machine.getDefinition().multiblockSettings().showUIWhenClickStructure()) {
                            machine.openUI(event.getEntity());
                            event.setCanceled(true);
                            return;
                        }
                    }
                }
            }
            // on multiblock catalyst candidates
            var originalState = serverLevel.getBlockState(pos);
            var hitBlock = originalState.getBlock();
            if (MultiblockMachineDefinition.CATALYST_CANDIDATES.containsKey(hitBlock)) {
                var held = event.getItemStack();
                var definitions = MultiblockMachineDefinition.CATALYST_CANDIDATES.get(hitBlock);
                for (var definition : definitions) {
                    if (definition.multiblockSettings().catalyst().test(held)) {
                        // check structure
                        var multiblockState = new MultiblockState(serverLevel, pos);
                        for (Direction facing : new Direction[]{Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST}) {
                            if (definition.blockPatternFactory().apply(null).checkPatternAtWithoutController(multiblockState, facing)) {
                                // can be formed, replace with the real controller
                                var controllerState = definition.block().defaultBlockState();
                                if (definition.blockProperties().rotationState().property.isPresent()) {
                                    controllerState = controllerState.setValue(definition.blockProperties().rotationState().property.get(), facing);
                                }
                                serverLevel.setBlockAndUpdate(pos, controllerState);
                                // notify formed
                                if (!IMultiController.ofController(serverLevel, pos).map(controller -> {
                                    if (controller instanceof MBDMultiblockMachine machine && machine.checkPatternWithLock()) {
                                        var success = machine.onCatalystUsed(event.getEntity(), event.getHand(), held);
                                        if (success) {
                                            machine.onStructureFormed();
                                            var mwsd = MultiblockWorldSavedData.getOrCreate(serverLevel);
                                            mwsd.addMapping(machine.getMultiblockState());
                                            mwsd.removeAsyncLogic(machine);
                                            return true;
                                        }
                                    }
                                    return false;
                                }).orElse(false)) {
                                    // rollback to the original state
                                    serverLevel.setBlockAndUpdate(pos, originalState);
                                }
                            }
                        }

                    }
                }
            }
        }

    }

}
