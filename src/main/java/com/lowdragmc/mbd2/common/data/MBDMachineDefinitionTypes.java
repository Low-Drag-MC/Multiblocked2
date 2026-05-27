package com.lowdragmc.mbd2.common.data;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.utils.ReflectionUtils;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.machine.definition.MachineDefinitionType;

public class MBDMachineDefinitionTypes {

    public static void init() {
        MBDRegistries.MACHINE_DEFINITION_TYPES.unfreeze();

        ReflectionUtils.findAnnotationStaticField(LDLRegister.class, data -> {
            if (data.containsKey("registry") && data.get("registry") instanceof java.lang.String targetRegistry) {
                if (!targetRegistry.equals("mbd2:machine_definition_type")) return false;
            }
            var modId = data.getOrDefault("modID", "").toString();
            if (modId.isEmpty()) return true;
            return LDLib2.isModLoaded(modId);
        }, (field, o) -> {
            if (o instanceof MachineDefinitionType<?> type) {
                MBDRegistries.MACHINE_DEFINITION_TYPES.register(type.name, type);
            }
        }, () -> {});

        MBDRegistries.MACHINE_DEFINITION_TYPES.freeze();
    }
}
