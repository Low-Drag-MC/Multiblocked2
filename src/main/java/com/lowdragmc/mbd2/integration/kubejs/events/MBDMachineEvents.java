package com.lowdragmc.mbd2.integration.kubejs.events;

import com.lowdragmc.mbd2.common.machine.definition.config.event.*;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventJS;
import lombok.Getter;

public class MBDMachineEvents {
    public static EventGroup MBD_MACHINE_EVENTS = EventGroup.of("MBDMachineEvents");

    @Getter
    public static class MachineEventJS<E extends MachineEvent> extends EventJS {
        public final E event;

        public MachineEventJS(E event) {
            this.event = event;
        }
    }

    public static class MachineAfterRecipeWorkingEventJS extends MachineEventJS<MachineAfterRecipeWorkingEvent> {
        public MachineAfterRecipeWorkingEventJS(MachineAfterRecipeWorkingEvent event) {
            super(event);
        }
    }

    public static class MachineUIEventJS extends MachineEventJS<MachineUIEvent> {
        public MachineUIEventJS(MachineUIEvent event) {
            super(event);
        }
    }

    public static class MachineBeforeRecipeWorkingEventJS extends MachineEventJS<MachineBeforeRecipeWorkingEvent> {
        public MachineBeforeRecipeWorkingEventJS(MachineBeforeRecipeWorkingEvent event) {
            super(event);
        }
    }

    public static class MachineClientTickEventJS extends MachineEventJS<MachineClientTickEvent> {
        public MachineClientTickEventJS(MachineClientTickEvent event) {
            super(event);
        }
    }

    public static class MachineCustomDataUpdateEventJS extends MachineEventJS<MachineCustomDataUpdateEvent> {
        public MachineCustomDataUpdateEventJS(MachineCustomDataUpdateEvent event) {
            super(event);
        }
    }

    public static class MachineDropsEventJS extends MachineEventJS<MachineDropsEvent> {
        public MachineDropsEventJS(MachineDropsEvent event) {
            super(event);
        }
    }

    public static class MachineNeighborChangedEventJS extends MachineEventJS<MachineNeighborChangedEvent> {
        public MachineNeighborChangedEventJS(MachineNeighborChangedEvent event) {
            super(event);
        }
    }

    public static class MachineOnLoadEventJS extends MachineEventJS<MachineOnLoadEvent> {
        public MachineOnLoadEventJS(MachineOnLoadEvent event) {
            super(event);
        }
    }

    public static class MachineOnRecipeWorkingEventJS extends MachineEventJS<MachineOnRecipeWorkingEvent> {
        public MachineOnRecipeWorkingEventJS(MachineOnRecipeWorkingEvent event) {
            super(event);
        }
    }

    public static class MachineOnRecipeWaitingEventJS extends MachineEventJS<MachineOnRecipeWaitingEvent> {
        public MachineOnRecipeWaitingEventJS(MachineOnRecipeWaitingEvent event) {
            super(event);
        }
    }

    public static class MachineOpenUIEventJS extends MachineEventJS<MachineOpenUIEvent> {
        public MachineOpenUIEventJS(MachineOpenUIEvent event) {
            super(event);
        }
    }

    public static class MachineOnConsumeInputsAfterWorkingEventJS extends MachineEventJS<MachineOnConsumeInputsAfterWorkingEvent> {
        public MachineOnConsumeInputsAfterWorkingEventJS(MachineOnConsumeInputsAfterWorkingEvent event) {
            super(event);
        }
    }

    public static class MachineOnRecipeFinishEventJS extends MachineEventJS<MachineOnRecipeFinishEvent> {
        public MachineOnRecipeFinishEventJS(MachineOnRecipeFinishEvent event) {
            super(event);
        }
    }

    public static class MachinePlacedEventJS extends MachineEventJS<MachinePlacedEvent> {
        public MachinePlacedEventJS(MachinePlacedEvent event) {
            super(event);
        }
    }

    public static class MachineFuelRecipeModifyEventJS extends MachineEventJS<MachineFuelRecipeModifyEvent> {
        public MachineFuelRecipeModifyEventJS(MachineFuelRecipeModifyEvent event) {
            super(event);
        }
    }

    public static class MachineFuelBurningFinishEventJS extends MachineEventJS<MachineFuelBurningFinishEvent> {
        public MachineFuelBurningFinishEventJS(MachineFuelBurningFinishEvent event) {
            super(event);
        }
    }

    public static class MachineRecipeModifyEventBeforeJS extends MachineEventJS<MachineRecipeModifyEvent.Before> {
        public MachineRecipeModifyEventBeforeJS(MachineRecipeModifyEvent.Before event) {
            super(event);
        }
    }

    public static class MachineRecipeModifyEventAfterJS extends MachineEventJS<MachineRecipeModifyEvent.After> {
        public MachineRecipeModifyEventAfterJS(MachineRecipeModifyEvent.After event) {
            super(event);
        }
    }

    public static class MachineRecipeStatusChangedEventJS extends MachineEventJS<MachineRecipeStatusChangedEvent> {
        public MachineRecipeStatusChangedEventJS(MachineRecipeStatusChangedEvent event) {
            super(event);
        }
    }

    public static class MachineRemovedEventJS extends MachineEventJS<MachineRemovedEvent> {
        public MachineRemovedEventJS(MachineRemovedEvent event) {
            super(event);
        }
    }

    public static class MachineRightClickEventJS extends MachineEventJS<MachineRightClickEvent> {
        public MachineRightClickEventJS(MachineRightClickEvent event) {
            super(event);
        }
    }

    public static class MachineStateChangedEventJS extends MachineEventJS<MachineStateChangedEvent> {
        public MachineStateChangedEventJS(MachineStateChangedEvent event) {
            super(event);
        }
    }

    public static class MachineStructureFormedEventJS extends MachineEventJS<MachineStructureFormedEvent> {
        public MachineStructureFormedEventJS(MachineStructureFormedEvent event) {
            super(event);
        }
    }

    public static class MachineStructureInvalidEventJS extends MachineEventJS<MachineStructureInvalidEvent> {
        public MachineStructureInvalidEventJS(MachineStructureInvalidEvent event) {
            super(event);
        }
    }

    public static class MachineTickEventJS extends MachineEventJS<MachineTickEvent> {
        public MachineTickEventJS(MachineTickEvent event) {
            super(event);
        }
    }

    public static class MachineUseCatalystEventJS extends MachineEventJS<MachineUseCatalystEvent> {
        public MachineUseCatalystEventJS(MachineUseCatalystEvent event) {
            super(event);
        }
    }

    public static class MachineCustomKeyframeEventJS extends MachineEventJS<MachineCustomKeyframeEvent> {
        public MachineCustomKeyframeEventJS(MachineCustomKeyframeEvent event) {
            super(event);
        }
    }
}
