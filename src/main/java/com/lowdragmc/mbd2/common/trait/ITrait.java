package com.lowdragmc.mbd2.common.trait;


import com.lowdragmc.mbd2.common.gui.editor.machine.MachineTraitPanel;
import com.lowdragmc.mbd2.common.machine.MBDMachine;

/**
 * A trait that represent a capability / behaviour / function a machine has, e.g. item container, energy storage, fluid tank, etc.
 * <br/>
 * To provide capability behavior in the world see {@link ICapabilityProviderTrait}. For recipe handling, see {@link IRecipeCapabilityTrait}.
 * To provide UI representation, see {@link IUIProviderTrait}.
 */
public interface ITrait {
    /**
     * @return the machine this trait is attached to
     */
    MBDMachine getMachine();

    /**
     * @return the definition of this trait
     */
    TraitDefinition getDefinition();

    /**
     * It will be called when this trait is being previewed in the editor. see {@link MachineTraitPanel#reloadAdditionalTraits()}
     * <br/>
     * e.g. you can do some storage preparation, or render some preview model.
     */
    default void onLoadingTraitInPreview() {}

    /**
     * Called when the machine is being loaded.
     */
    default void onMachineLoad() {}

    /**
     * Called when the machine is being unloaded.
     */
    default void onMachineUnLoad() {}

    /**
     * Called when the machine is being removed.
     */
    default void onMachineRemoved() {}

    /**
     * Called per server tick.
     */
    default void serverTick() {

    }

    /**
     * Called per client tick.
     */
    default void clientTick() {

    }

}
