package com.lowdragmc.mbd2.common.machine.definition;

import com.google.gson.JsonObject;
import com.lowdragmc.mbd2.api.machine.IMultiPart;
import com.lowdragmc.mbd2.api.pattern.BlockPattern;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;

import java.util.Comparator;
import java.util.List;

/**
 * Multiblock machine definition.
 * <br>
 * This is used to define a mbd machine's {@link MBDMultiblockMachine#getDefinition()} behaviours.
 */
public class MultiblockMachineDefinition extends MachineDefinition{

    protected MultiblockMachineDefinition(JsonObject jsonObject) {
        super(jsonObject);
    }

    public static MultiblockMachineDefinition fromJson(JsonObject jsonObject) {
        return new MultiblockMachineDefinition(jsonObject);
    }

    public BlockPattern getPattern() {
    }

    public void sortParts(List<IMultiPart> parts) {
    }
}
