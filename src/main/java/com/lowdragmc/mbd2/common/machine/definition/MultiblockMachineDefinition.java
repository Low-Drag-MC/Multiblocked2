package com.lowdragmc.mbd2.common.machine.definition;

import com.lowdragmc.mbd2.api.machine.IMultiPart;
import com.lowdragmc.mbd2.api.pattern.BlockPattern;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import com.lowdragmc.mbd2.common.machine.definition.config.*;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Multiblock machine definition.
 * <br>
 * This is used to define a mbd machine's {@link MBDMultiblockMachine#getDefinition()} behaviours.
 */
public class MultiblockMachineDefinition extends MBDMachineDefinition {

    public MultiblockMachineDefinition(ResourceLocation id,
                                       StateMachine stateMachine,
                                       ConfigBlockProperties blockProperties,
                                       ConfigItemProperties itemProperties,
                                       ConfigMachineSettings machineSettings) {
        super(id, stateMachine, blockProperties, itemProperties, machineSettings);
    }

    public static Builder builder() {
        return new Builder();
    }

    public BlockPattern getPattern() {
        return null;
    }

    public void sortParts(List<IMultiPart> parts) {
    }

    @Setter
    @Accessors(chain = true, fluent = true)
    public static class Builder extends MBDMachineDefinition.Builder {

        protected Builder() {
        }

        public MultiblockMachineDefinition build() {
            return new MultiblockMachineDefinition(id, stateMachine, blockProperties, itemProperties, machineSettings);
        }
    }
}
