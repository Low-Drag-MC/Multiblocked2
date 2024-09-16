package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.parameter.ExposedParameter;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.definition.config.event.graphprocess.GraphParameterGet;
import com.lowdragmc.mbd2.common.machine.definition.config.event.graphprocess.GraphParameterSet;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Map;
import java.util.Optional;

@Getter
@LDLRegister(name = "MachineRightClickEvent", group = "MachineEvent")
public class MachineRightClickEvent extends MachineEvent {
    @GraphParameterGet
    public final Player player;
    @GraphParameterGet
    public final InteractionHand hand;
    @GraphParameterGet
    public final BlockHitResult hit;
    @Setter
    @GraphParameterSet(displayName = "interaction result", type = Boolean.class)
    public InteractionResult interactionResult;

    public MachineRightClickEvent(MBDMachine machine, Player player, InteractionHand hand, BlockHitResult hit) {
        super(machine);
        this.player = player;
        this.hand = hand;
        this.hit = hit;
    }

    @Override
    public void bindParameters(Map<String, ExposedParameter> exposedParameters) {
        super.bindParameters(exposedParameters);
        Optional.ofNullable(exposedParameters.get("player")).ifPresent(p -> p.setValue(player));
        Optional.ofNullable(exposedParameters.get("hand")).ifPresent(p -> p.setValue(hand));
        Optional.ofNullable(exposedParameters.get("hit")).ifPresent(p -> p.setValue(hit));
    }

    @Override
    public void gatherParameters(Map<String, ExposedParameter> exposedParameters) {
        super.gatherParameters(exposedParameters);
        Optional.ofNullable(exposedParameters.get("interactionResult")).ifPresent(p -> {
            if (p.getValue() instanceof Boolean result) {
                interactionResult = result ? InteractionResult.SUCCESS : InteractionResult.PASS;
            }
        });
    }
}
