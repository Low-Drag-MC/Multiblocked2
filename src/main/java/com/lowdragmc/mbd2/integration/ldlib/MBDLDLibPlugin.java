package com.lowdragmc.mbd2.integration.ldlib;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.runtime.AnnotationDetector;
import com.lowdragmc.lowdraglib.plugin.ILDLibPlugin;
import com.lowdragmc.lowdraglib.plugin.LDLibPlugin;
import com.lowdragmc.mbd2.common.data.MBDSyncedFieldAccessors;
import com.lowdragmc.mbd2.common.trait.TraitDefinition;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@LDLibPlugin
public class MBDLDLibPlugin implements ILDLibPlugin {
    public static Map<String, AnnotationDetector.Wrapper<LDLRegister, ? extends TraitDefinition>> REGISTER_TRAIT_DEFINITIONS = new HashMap<>();

    @Override
    public void onLoad() {
        MBDSyncedFieldAccessors.init();
        AnnotationDetector.scanClasses(LDLRegister.class, TraitDefinition.class,
                AnnotationDetector::checkNoArgsConstructor, AnnotationDetector::toUINoArgsBuilder, AnnotationDetector::UIWrapperSorter,
                l -> REGISTER_TRAIT_DEFINITIONS.putAll(l.stream().collect(Collectors.toMap(w -> w.annotation().name(), w -> w))));
    }
}
