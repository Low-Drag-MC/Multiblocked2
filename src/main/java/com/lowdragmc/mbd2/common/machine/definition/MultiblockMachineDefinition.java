package com.lowdragmc.mbd2.common.machine.definition;

import com.lowdragmc.mbd2.api.machine.IMultiPart;
import com.lowdragmc.mbd2.api.pattern.BlockPattern;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import com.lowdragmc.mbd2.common.machine.definition.config.*;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Multiblock machine definition.
 * <br>
 * This is used to define a mbd machine's {@link MBDMultiblockMachine#getDefinition()} behaviours.
 */
public class MultiblockMachineDefinition extends MBDMachineDefinition {

    protected MultiblockMachineDefinition(ResourceLocation id, StateMachine stateMachine, ConfigBlockProperties blockProperties, ConfigItemProperties itemProperties, ConfigMachineInfo machineInfo) {
        super(id, stateMachine, blockProperties, itemProperties, machineInfo);
    }

    public BlockPattern getPattern() {
        return null;
    }

    public void sortParts(List<IMultiPart> parts) {
    }
}
