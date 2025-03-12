package com.lowdragmc.mbd2.common.data;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.runtime.AnnotationDetector;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.MultiblockMachineDefinition;
import com.lowdragmc.mbd2.integration.create.machine.CreateKineticMachineDefinition;
import net.neoforged.fml.ModLoader;

public class MBDMachineDefinitionTypes {

    public static void init() {
        MBDRegistries.MACHINE_DEFINITION_TYPES.unfreeze();
        register(MBDMachineDefinition.class);
        register(MultiblockMachineDefinition.class);
        if (MBD2.isCreateLoaded()) {
            register(CreateKineticMachineDefinition.class);
        }
        ModLoader.postEvent(new MBDRegistryEvent.MachineDefinitionType());
        MBDRegistries.MACHINE_DEFINITION_TYPES.freeze();
    }

    public static void register(Class<? extends MBDMachineDefinition> clazz) {
        if (clazz.isAnnotationPresent(LDLRegister.class)) {
            var annotation = clazz.getAnnotation(LDLRegister.class);
            if (!annotation.modID().isEmpty()) {
                if (!LDLib.isModLoaded(annotation.modID())) {
                    MBD2.LOGGER.info("Skipping registration of machine definition: " + clazz.getName() + " - Mod not loaded: " + annotation.modID());
                    return;
                }
            }
            try {
                var constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
                MBDRegistries.MACHINE_DEFINITION_TYPES.register(clazz.getAnnotation(LDLRegister.class).name(),
                        new AnnotationDetector.Wrapper<>(annotation, clazz, () -> {
                            try {
                                return constructor.newInstance();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }));
            } catch (NoSuchMethodException e) {
                MBD2.LOGGER.error("Failed to register machine definition: " + clazz.getName() + " - No default constructor found");
            }
        } else {
            MBD2.LOGGER.error("Failed to register machine definition: " + clazz.getName() + " - No annotation found");
        }
    }


}
