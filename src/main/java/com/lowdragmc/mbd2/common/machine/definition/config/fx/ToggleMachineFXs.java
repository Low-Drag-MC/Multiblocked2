package com.lowdragmc.mbd2.common.machine.definition.config.fx;

import com.lowdragmc.lowdraglib2.configurator.IToggleConfigurable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.ReadOnlyManaged;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.IntTag;

import java.util.ArrayList;
import java.util.List;

/**
 * The effects a {@link com.lowdragmc.mbd2.common.machine.definition.config.MachineState} plays while
 * the machine sits in it.
 *
 * <p>A toggle rather than a bare list so it follows the same inheritance rule as every other
 * per-state setting — {@code renderer}, {@code shape}, {@code machineSound}: disabled means "whatever
 * my parent state says", not "no effects". A state that genuinely wants silence enables the toggle
 * and leaves the list empty.</p>
 */
@Getter
@Setter
public class ToggleMachineFXs implements IToggleConfigurable {

    protected boolean enable;

    /**
     * The list instance is final, so — exactly as with
     * {@link com.lowdragmc.mbd2.common.machine.definition.config.ConfigMachineSettings#blueprints()} —
     * the persisted form has to say how many elements to create before per-element deserialization
     * can run.
     */
    @Persisted
    @ReadOnlyManaged(serializeMethod = "fxsSerialize", deserializeMethod = "fxsDeserialize")
    private final List<MachineFXConfig> fxs = new ArrayList<>();

    // LDLib2 resolves these reflectively on the declaring class, so the stubs have to be here even
    // though the bodies are shared — see MachineFXConfig's list plumbing.
    protected IntTag fxsSerialize(List<MachineFXConfig> fxs) {
        return MachineFXConfig.sizeTag(fxs);
    }

    protected List<MachineFXConfig> fxsDeserialize(IntTag tag) {
        return MachineFXConfig.listOfSize(tag);
    }
}
