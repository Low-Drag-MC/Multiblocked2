package com.lowdragmc.mbd2.test.tests.trait.create;

import com.lowdragmc.lowdraglib2.client.renderer.impl.IModelRenderer;
import com.lowdragmc.mbd2.integration.create.machine.CreateKineticMachineDefinition;
import com.lowdragmc.mbd2.integration.create.machine.CreateMachineState;
import com.lowdragmc.mbd2.MBD2;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Guards the per-state rotation-renderer wiring. Without these, the user can't pick what
 * rotates and the kinetic machine shaft is silently hardcoded.
 */
@GameTestHolder(MBD2.MOD_ID)
public class CreateMachineStateRendererTests {
    static { @SuppressWarnings("unused") var ignored = CreateMachineState.class; }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void create_machine_state_builder_produces_create_machine_state(GameTestHelper h) {
        var state = CreateMachineState.builder().build(null);
        if (state == null) { h.fail("Builder returned null"); return; }
        if (!(state instanceof CreateMachineState)) {
            h.fail("Builder produced " + state.getClass() + " instead of CreateMachineState");
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void default_root_state_is_create_machine_state(GameTestHelper h) {
        var definition = CreateKineticMachineDefinition.createDefault();
        var rootState = definition.stateMachine().getRootState();
        if (!(rootState instanceof CreateMachineState)) {
            h.fail("createDefault().stateMachine().rootState is " + rootState.getClass()
                    + " — should be CreateMachineState");
            return;
        }
        h.succeed();
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void child_inherits_rotation_renderer_from_parent(GameTestHelper h) {
        // Build a parent with an explicit rotation renderer and a child without one.
        // The child's getRotationRenderer() must walk up to the parent.
        var rotationModelLocation = ResourceLocation.parse("create:block/shaft");
        var rotationRenderer = new IModelRenderer(rotationModelLocation);
        var parent = new CreateMachineState("parent", null, null, null, null, null, rotationRenderer);
        var child = new CreateMachineState("child", parent, null, null, null, null, null);
        parent.addChild(child);

        var resolved = child.getRotationRenderer();
        if (resolved != rotationRenderer) {
            h.fail("Child did not inherit rotationRenderer from parent. Got: " + resolved);
            return;
        }
        h.succeed();
    }
}
