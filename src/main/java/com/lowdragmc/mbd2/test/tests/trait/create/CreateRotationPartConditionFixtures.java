package com.lowdragmc.mbd2.test.tests.trait.create;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.pattern.FactoryBlockPattern;
import com.lowdragmc.mbd2.api.pattern.Predicates;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import net.minecraft.resources.ResourceLocation;

/**
 * A trait-less multiblock controller whose only part is the kinetic consumer from
 * {@link CreateKineticMachineFixtures}. Used to check that the create rotation condition
 * inspects part traits (issue #220).
 */
public class CreateRotationPartConditionFixtures implements TestFixtureProvider {
    public static final ResourceLocation CONTROLLER_ID = MBD2.id("test_create_rotation_condition_controller");

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        // "CP": controller at the origin, kinetic part one block towards world -X.
        // The pattern factory runs lazily, so the consumer definition is resolvable by then.
        TestMachineBuilder.multiblock(CONTROLLER_ID)
                .withBlockPattern(controller -> FactoryBlockPattern.start()
                        .aisle("CP")
                        .where('C', Predicates.controller(Predicates.any()))
                        .where('P', Predicates.blocks(MBDRegistries.MACHINE_DEFINITIONS
                                .get(CreateKineticMachineFixtures.CONSUMER_MACHINE_ID).block()))
                        .build())
                .register(event);
    }
}
