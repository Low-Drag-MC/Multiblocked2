package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.parameter.ExposedParameter;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.graphprocessor.GraphParameterGet;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.Optional;


@Getter
@LDLRegister(name = "MachineNeighborChangedEvent", group = "MachineEvent")
public class MachineNeighborChangedEvent extends MachineEvent {
    @GraphParameterGet
    public final Block block;
    @GraphParameterGet(displayName = "pos")
    public final BlockPos fromPos;

    public MachineNeighborChangedEvent(MBDMachine machine, Block block, BlockPos fromPos) {
        super(machine);
        this.block = block;
        this.fromPos = fromPos;
    }

    @Override
    public void bindParameters(Map<String, ExposedParameter> exposedParameters) {
        super.bindParameters(exposedParameters);
        Optional.ofNullable(exposedParameters.get("block")).ifPresent(p -> p.setValue(block));
        Optional.ofNullable(exposedParameters.get("fromPos")).ifPresent(p -> p.setValue(fromPos));
    }
}
