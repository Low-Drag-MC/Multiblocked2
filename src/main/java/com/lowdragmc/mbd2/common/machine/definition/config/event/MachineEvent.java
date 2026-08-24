package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.integration.kubejs.events.MBDClientEvents;
import com.lowdragmc.mbd2.integration.kubejs.events.MBDServerEvents;
import lombok.Getter;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;


@Getter
public class MachineEvent extends Event {
    public final MBDMachine machine;

    public MachineEvent(MBDMachine machine) {
        this.machine = machine;
    }

    public MachineEvent postCustomEvent() {
        // post to the machine's blueprints
        machine.postBlueprintEvent(this);
        // post to the KubeJS events
        postKubeJSEvent();
        return this;
    }

    public MachineEvent postKubeJSEvent() {
        // post to the KubeJS events
        if (MBD2.isKubeJSLoaded()) {
            try {
                if (LDLib2.isClient()) {
                    if (MBDServerEvents.postMachineEvent(this).interruptFalse() && this instanceof ICancellableEvent cancelable) {
                        cancelable.setCanceled(true);
                    } else if (MBDClientEvents.postMachineEvent(this).interruptFalse() && this instanceof ICancellableEvent cancelable) {
                        cancelable.setCanceled(true);
                    }
                } else {
                    if (MBDServerEvents.postMachineEvent(this).interruptFalse() && this instanceof ICancellableEvent cancelable) {
                        cancelable.setCanceled(true);
                    }
                }
            } catch (Exception e) {
                MBD2.LOGGER.error("Failed to post KubeJS event {}", this, e);
            }
        }
        return this;
    }
    @Override
    public String toString() {
        return "MachineEvent{" +
                "machine=" + machine +
                ", eventName='" + getClass().getSimpleName() + '\'' +
                (this instanceof ICancellableEvent event ?
                        ", isCanceled=" + event.isCanceled() : "") +
                '}';
    }
}
