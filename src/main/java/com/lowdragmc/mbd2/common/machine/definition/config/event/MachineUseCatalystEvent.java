package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.parameter.ExposedParameter;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.graphprocessor.GraphParameterGet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.ICancellableEvent;

import java.util.Map;
import java.util.Optional;

@LDLRegister(name = "MachineUseCatalystEvent", group = "MachineEvent.Multiblock")
public class MachineUseCatalystEvent extends MachineEvent implements ICancellableEvent {
    @GraphParameterGet
    public final ItemStack catalyst;
    @GraphParameterGet
    public final Player player;
    @GraphParameterGet
    public final InteractionHand hand;

    public MachineUseCatalystEvent(MBDMachine machine, ItemStack catalyst, Player player, InteractionHand hand) {
        super(machine);
        this.catalyst = catalyst;
        this.player = player;
        this.hand = hand;
    }

    @Override
    public void bindParameters(Map<String, ExposedParameter> exposedParameters) {
        super.bindParameters(exposedParameters);
        Optional.ofNullable(exposedParameters.get("catalyst")).ifPresent(p -> p.setValue(catalyst));
        Optional.ofNullable(exposedParameters.get("player")).ifPresent(p -> p.setValue(player));
        Optional.ofNullable(exposedParameters.get("hand")).ifPresent(p -> p.setValue(hand));
    }
}
