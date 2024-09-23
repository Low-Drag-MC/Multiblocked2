package com.lowdragmc.mbd2.integration.ldlib;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.runtime.AnnotationDetector;
import com.lowdragmc.lowdraglib.plugin.ILDLibPlugin;
import com.lowdragmc.lowdraglib.plugin.LDLibPlugin;
import com.lowdragmc.mbd2.api.pattern.predicates.SimplePredicate;
import com.lowdragmc.mbd2.common.data.MBDSyncedFieldAccessors;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@LDLibPlugin
public class MBDLDLibPlugin implements ILDLibPlugin {
    public static final Map<String, AnnotationDetector.Wrapper<LDLRegister, ? extends SimplePredicate>> REGISTER_PREDICATES = new HashMap<>();
    public static final Map<String, List<Class<? extends MachineEvent>>> REGISTER_MACHINE_EVENTS = new HashMap<>();

    @Override
    public void onLoad() {
        MBDSyncedFieldAccessors.init();
        AnnotationDetector.scanClasses(LDLRegister.class, SimplePredicate.class,
                AnnotationDetector::checkNoArgsConstructor, AnnotationDetector::toUINoArgsBuilder, AnnotationDetector::UIWrapperSorter,
                l -> REGISTER_PREDICATES.putAll(l.stream().collect(Collectors.toMap(w -> w.annotation().name(), w -> w))));
        AnnotationDetector.scanClasses(LDLRegister.class, MachineEvent.class, (a, c) -> true, c -> c, (a, b) -> 0,
                l -> l.forEach(clazz -> REGISTER_MACHINE_EVENTS
                        .computeIfAbsent(clazz.getAnnotation(LDLRegister.class).group(), k -> new ArrayList<>()).add(clazz)));
    }
}
