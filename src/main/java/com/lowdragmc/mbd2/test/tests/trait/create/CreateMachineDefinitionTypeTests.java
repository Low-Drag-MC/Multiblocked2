package com.lowdragmc.mbd2.test.tests.trait.create;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.integration.create.machine.CreateKineticMachineDefinition;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Guards the machine-definition-type registration. The type must live in the
 * mbd2:machine_definition_type registry, otherwise saved .cm projects never load
 * and users can't create kinetic machines from the editor.
 */
// No @GameTestHolder: registered via MBDTestRegistry#onRegisterGameTests (mod-load guarded)
// to avoid NeoForge force-loading this soft-dep class when the mod is absent.
public class CreateMachineDefinitionTypeTests {
    static { @SuppressWarnings("unused") var ignored = CreateKineticMachineDefinition.TYPE; }

    @GameTest(template = "empty_simple", templateNamespace = MBD2.MOD_ID)
    @PrefixGameTestTemplate(false)
    public static void create_machine_type_is_registered(GameTestHelper h) {
        var type = CreateKineticMachineDefinition.TYPE;
        boolean found = false;
        for (var registered : MBDRegistries.MACHINE_DEFINITION_TYPES) {
            if (registered == type) {
                found = true;
                break;
            }
        }
        if (!found) {
            h.fail("CreateKineticMachineDefinition.TYPE is not registered in mbd2:machine_definition_type");
            return;
        }
        // The type must produce a usable definition instance.
        if (!(type.createDefinition() instanceof CreateKineticMachineDefinition)) {
            h.fail("createDefinition() did not return a CreateKineticMachineDefinition");
            return;
        }
        h.succeed();
    }
}
