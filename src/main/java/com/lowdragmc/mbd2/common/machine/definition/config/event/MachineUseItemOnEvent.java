package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;


@Getter
//@LDLRegister(name = "MachineUseItemOnEvent", group = "MachineEvent")
public class MachineUseItemOnEvent extends MachineEvent {
    public final Player player;
    public final InteractionHand hand;
    public final BlockHitResult hit;
    @Setter
    public ItemInteractionResult itemInteractionResult;

    public MachineUseItemOnEvent(MBDMachine machine, Player player, InteractionHand hand, BlockHitResult hit) {
        super(machine);
        this.player = player;
        this.hand = hand;
        this.hit = hit;
    }

}
