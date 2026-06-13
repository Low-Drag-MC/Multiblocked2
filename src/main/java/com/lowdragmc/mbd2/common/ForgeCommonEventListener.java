package com.lowdragmc.mbd2.common;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.machine.IMultiController;
import com.lowdragmc.mbd2.api.pattern.MultiblockState;
import com.lowdragmc.mbd2.api.pattern.MultiblockWorldSavedData;
import com.lowdragmc.mbd2.common.item.MBDGadgetsItem;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import com.lowdragmc.mbd2.common.machine.definition.MultiblockMachineDefinition;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * @author KilaBash
 * @date 2022/8/27
 * @implNote ForgeCommonEventListener
 */
@EventBusSubscriber(modid = MBD2.MOD_ID)
public class ForgeCommonEventListener {

    @SubscribeEvent
    public static void onServerStart(ServerAboutToStartEvent e) {
        if (!LDLib2.isClient()) {
            var postTask = CommonProxy.getPostTask();
            postTask.forEach(Runnable::run);
            postTask.clear();
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ServerCommands.createServerCommands().forEach(event.getDispatcher()::register);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            var pos = event.getPos();
            if (!event.getEntity().isCrouching() &&
                    !(event.getEntity().getItemInHand(event.getHand()).getItem() instanceof MBDGadgetsItem)) {
                // on multiblock ui click
                for (var state : MultiblockWorldSavedData.getOrCreate(serverLevel).getControllerInPos(pos)) {
                    LongSet openUIMask = state.getMatchContext().getOrDefault("openUIMask", LongSets.EMPTY_SET);
                    if (state.getController() instanceof MBDMultiblockMachine machine) {
                        if (machine.getDefinition().machineSettings().hasUI() &&
                                machine.getDefinition().multiblockSettings().showUIWhenClickStructure() &&
                                openUIMask.contains(pos.asLong())) {
                            machine.openUI(event.getEntity());
                            event.setUseBlock(TriState.FALSE);
                            event.setUseItem(TriState.FALSE);
                            // event.setCanceled(true);
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
                                            machine.setOriginalBlock(originalState);
                                            return true;
                                        }
                                    }
                                    return false;
                                }).orElse(false)) {
                                    // rollback to the original state
                                    serverLevel.setBlockAndUpdate(pos, originalState);
                                } else {
                                    // success
                                    event.setCanceled(true);
                                    return;
                                }
                            }
                        }

                    }
                }
            }
        }

    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            var mwsd = MultiblockWorldSavedData.getOrCreate(serverLevel);
            mwsd.tickSnapshots();
            mwsd.dispatchPendingChecks();
        }
    }

    @SubscribeEvent
    public static void onWorldUnLoad(LevelEvent.Unload event) {
        LevelAccessor world = event.getLevel();
        if (!world.isClientSide() && world instanceof ServerLevel serverLevel) {
            MultiblockWorldSavedData.getOrCreate(serverLevel).releaseExecutorService();
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        var levels = event.getServer().getAllLevels();
        for (var level : levels) {
            if (!level.isClientSide()) {
                MultiblockWorldSavedData.getOrCreate(level).releaseExecutorService();
            }
        }
    }

}
