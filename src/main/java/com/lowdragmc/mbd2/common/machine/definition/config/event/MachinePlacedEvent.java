package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;


@Getter
//@LDLRegister(name = "MachinePlacedEvent", group = "MachineEvent")
public class MachinePlacedEvent extends MachineEvent {
    public final LivingEntity player;
    public final ItemStack itemStack;

    public MachinePlacedEvent(MBDMachine machine, LivingEntity player, ItemStack itemStack) {
        super(machine);
        this.player = player;
        this.itemStack = itemStack;
    }

}
