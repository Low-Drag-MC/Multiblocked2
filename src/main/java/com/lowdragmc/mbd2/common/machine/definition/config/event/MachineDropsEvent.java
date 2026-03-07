package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@Getter
//@LDLRegister(name = "MachineDropsEvent", group = "MachineEvent")
public class MachineDropsEvent extends MachineEvent {
    public final Entity entity;
    public List<ItemStack> drops;

    public MachineDropsEvent(MBDMachine machine, Entity entity, List<ItemStack> drops) {
        super(machine);
        this.entity = entity;
        this.drops = drops;
    }

}
