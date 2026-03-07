package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.mbd2.common.machine.MBDMachine;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.ICancellableEvent;

//@LDLRegister(name = "MachineUseCatalystEvent", group = "MachineEvent.Multiblock")
public class MachineUseCatalystEvent extends MachineEvent implements ICancellableEvent {
    public final ItemStack catalyst;
    public final Player player;
    public final InteractionHand hand;

    public MachineUseCatalystEvent(MBDMachine machine, ItemStack catalyst, Player player, InteractionHand hand) {
        super(machine);
        this.catalyst = catalyst;
        this.player = player;
        this.hand = hand;
    }

}
