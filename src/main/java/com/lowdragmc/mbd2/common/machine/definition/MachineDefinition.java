package com.lowdragmc.mbd2.common.machine.definition;

import com.google.gson.JsonObject;
import com.lowdragmc.mbd2.api.block.RotationState;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import net.minecraft.util.RandomSource;

/**
 * Machine definition.
 * <br>
 * This is used to define a mbd machine's {@link MBDMachine#getDefinition()} behaviours.
 */
@Getter
public class MachineDefinition {
    protected MBDRecipeType recipeType;
    protected RotationState rotationState;

    protected MachineDefinition(JsonObject jsonObject) {

    }

    public static MachineDefinition fromJson(JsonObject jsonObject) {
        return new MachineDefinition(jsonObject);
    }

    /**
     * Called when the machine is ticked.
     */
    public void animateTick(MBDMachine mbdMachine, RandomSource random) {
    }
}
