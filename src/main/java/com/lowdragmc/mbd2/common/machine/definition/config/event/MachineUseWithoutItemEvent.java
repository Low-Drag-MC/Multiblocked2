package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;


@Getter
//@LDLRegister(name = "MachineUseWithoutItemEvent", group = "MachineEvent")
public class MachineUseWithoutItemEvent extends MachineEvent {
    public final Player player;
    public final BlockHitResult hit;
    @Setter
    public InteractionResult interactionResult;

    public MachineUseWithoutItemEvent(MBDMachine machine, Player player, BlockHitResult hit) {
        super(machine);
        this.player = player;
        this.hit = hit;
    }

}
