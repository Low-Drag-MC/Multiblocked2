package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;


@Getter
//@LDLRegister(name = "MachineNeighborChangedEvent", group = "MachineEvent")
public class MachineNeighborChangedEvent extends MachineEvent {
    public final Block block;
    public final BlockPos fromPos;

    public MachineNeighborChangedEvent(MBDMachine machine, Block block, BlockPos fromPos) {
        super(machine);
        this.block = block;
        this.fromPos = fromPos;
    }

}
