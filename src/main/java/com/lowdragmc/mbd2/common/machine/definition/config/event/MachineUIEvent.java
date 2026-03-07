package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Getter
@Setter
//@LDLRegister(name = "MachineUIEvent", group = "MachineEvent")
public class MachineUIEvent extends MachineEvent {
    @Nullable
    public UI ui;
    public Player player;

    public MachineUIEvent(MBDMachine machine, @Nonnull UI ui, Player player) {
        super(machine);
        this.ui = ui;
        this.player = player;
    }
}
