package com.lowdragmc.mbd2.test.tests.blueprint;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.TypeConstant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.blueprint.builtin.BuiltinBlueprints;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.machine.definition.config.blueprint.MachineBlueprintBinding;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import net.minecraft.resources.ResourceLocation;

/**
 * Machines carrying the {@code auto_io_panel} built-in, one per question the panel raises.
 *
 * <p>The trait names are the registry defaults an editor would show — {@code item_slot} for the first
 * item-slot trait, {@code fluid_tank} for the tank — because that is what someone binding this
 * blueprint would actually type.</p>
 */
public class AutoIOPanelFixtures implements TestFixtureProvider {

    /** One trait, one tab. */
    public static final ResourceLocation ONE_TAB_ID = MBD2.id("auto_io_panel_one_tab");
    /** Two bindings of the same blueprint, which must append rather than each build their own strip. */
    public static final ResourceLocation TWO_TABS_ID = MBD2.id("auto_io_panel_two_tabs");
    /** Bound to a trait that does not exist, and so must add nothing at all. */
    public static final ResourceLocation NO_TRAIT_ID = MBD2.id("auto_io_panel_no_trait");
    /** No blueprint: the machine UI as it is without any of this. */
    public static final ResourceLocation CONTROL_ID = MBD2.id("auto_io_panel_control");

    public static final String ITEM_TRAIT = "item_slot";
    public static final String FLUID_TRAIT = "fluid_tank";

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        machine(ONE_TAB_ID)
                .withBlueprint(panelFor(ITEM_TRAIT))
                .register(event);

        machine(TWO_TABS_ID)
                .withBlueprint(panelFor(ITEM_TRAIT))
                .withBlueprint(panelFor(FLUID_TRAIT))
                .register(event);

        machine(NO_TRAIT_ID)
                .withBlueprint(panelFor("there_is_no_such_trait"))
                .register(event);

        machine(CONTROL_ID).register(event);
    }

    /**
     * An item slot and a fluid tank, so both trait names resolve to something that does auto IO.
     *
     * <p>The item slot's definition sets two faces rather than leaving all six at none. A panel whose
     * every cell reads the same tells you nothing about whether it read anything at all, and the two
     * that are set are the ones the ui scenario compares against an untouched one.</p>
     */
    private static TestMachineBuilder machine(ResourceLocation id) {
        return TestMachineBuilder.simple(id)
                .withItemSlots(1, IO.BOTH, definition -> {
                    definition.getAutoIO().setEnable(true);
                    definition.getAutoIO().setTopIO(IO.IN);
                    definition.getAutoIO().setBottomIO(IO.OUT);
                })
                .withFluidTanks(1, 8000);
    }

    private static MachineBlueprintBinding panelFor(String trait) {
        var binding = new MachineBlueprintBinding();
        binding.setBlueprintPath(BuiltinBlueprints.path("auto_io_panel"));
        return binding.withVariable("trait", constant(TypeHandles.STRING, trait));
    }

    private static TypeConstant constant(TypeHandle handle, Object value) {
        var constant = new TypeConstant();
        constant.init(handle);
        constant.setValue(value);
        return constant;
    }
}
