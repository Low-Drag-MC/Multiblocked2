package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.ICancellableEvent;


@Getter
//@LDLRegister(name = "MachineOpenUIEvent", group = "MachineEvent")
public class MachineOpenUIEvent extends MachineEvent implements ICancellableEvent {
    public final Player player;

    public MachineOpenUIEvent(MBDMachine machine, Player player) {
        super(machine);
        this.player = player;
    }

}
