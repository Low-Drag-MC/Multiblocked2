package com.lowdragmc.mbd2.test.tests.trait.create;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.integration.create.machine.CreateKineticMachineDefinition;
import com.lowdragmc.mbd2.integration.create.machine.CreateKineticMachineProject;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Guards the editor-project wiring: the machine definition type must expose the project type,
 * and instantiating that project must produce a kinetic-aware definition. Without this, the
 * editor cannot create new Create kinetic machines through the File→New menu.
 */
@GameTestHolder(MBD2.MOD_ID)
public class CreateEditorProjectRegistrationTests {
    static {
        @SuppressWarnings("unused") var ignored = CreateKineticMachineDefinition.TYPE;
        @SuppressWarnings("unused") var ignored2 = CreateKineticMachineProject.TYPE;
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void create_kinetic_machine_project_type_resolves_via_definition_type(GameTestHelper h) {
        var projectType = CreateKineticMachineDefinition.TYPE.getEditorProjectType();
        if (projectType == null) {
            h.fail("CreateKineticMachineDefinition.TYPE.getEditorProjectType() returned null");
            return;
        }
        if (projectType != CreateKineticMachineProject.TYPE) {
            h.fail("Expected CreateKineticMachineProject.TYPE but got " + projectType);
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void create_kinetic_machine_project_factory_produces_kinetic_definition(GameTestHelper h) {
        // The ProjectType's factory lambda is the entry point used by the editor "New project"
        // button. It must yield a CreateKineticMachineProject whose definition is a real kinetic
        // definition with non-null kineticMachineSettings.
        var project = CreateKineticMachineProject.TYPE.newEmptyProject();
        if (!(project instanceof CreateKineticMachineProject kineticProject)) {
            h.fail("ProjectType factory did not produce a CreateKineticMachineProject: " + project);
            return;
        }
        if (!(kineticProject.getDefinition() instanceof CreateKineticMachineDefinition definition)) {
            h.fail("CreateKineticMachineProject.getDefinition() did not return a kinetic definition");
            return;
        }
        if (definition.kineticMachineSettings() == null) {
            h.fail("Kinetic settings on the freshly-constructed definition are null");
            return;
        }
        h.succeed();
    }
}
