package com.lowdragmc.mbd2.common.data;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.utils.ReflectionUtils;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.trait.TraitDefinitionType;

public class MBDTraitDefinitionTypes {

    public static void init() {
        MBDRegistries.TRAIT_DEFINITION_TYPES.unfreeze();

        ReflectionUtils.findAnnotationStaticField(LDLRegister.class, data -> {
            if (data.containsKey("registry") && data.get("registry") instanceof java.lang.String targetRegistry) {
                if (!targetRegistry.equals("mbd2:trait_definition_type")) return false;
            }
            var modId = data.getOrDefault("modID", "").toString();
            if (modId.isEmpty()) return true;
            return LDLib2.isModLoaded(modId);
        }, (field, o) -> {
            if (o instanceof TraitDefinitionType<?> type) {
                MBDRegistries.TRAIT_DEFINITION_TYPES.register(type.name, type);
            }
        }, () -> {});

        MBDRegistries.TRAIT_DEFINITION_TYPES.freeze();
    }
}
