package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.parameter.ExposedParameter;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.mbd2.common.graphprocessor.GraphParameterGet;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.Optional;

@Getter
@Setter
@LDLRegister(name = "MachineUIEvent", group = "MachineEvent")
public class MachineUIEvent extends MachineEvent {
    public WidgetGroup root;
    @GraphParameterGet
    public Player player;

    public MachineUIEvent(MBDMachine machine, WidgetGroup root, Player player) {
        super(machine);
        this.root = root;
        this.player = player;
    }

    @Override
    public void bindParameters(Map<String, ExposedParameter> exposedParameters) {
        super.bindParameters(exposedParameters);
        Optional.ofNullable(exposedParameters.get("player")).ifPresent(p -> p.setValue(player));
    }
}
