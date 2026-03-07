package com.lowdragmc.mbd2.api.recipe.event;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.integration.kubejs.events.MBDClientEvents;
import com.lowdragmc.mbd2.integration.kubejs.events.MBDServerEvents;
import lombok.Getter;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

@Getter
public class RecipeTypeEvent extends Event {
    public final MBDRecipeType recipeType;

    public RecipeTypeEvent(MBDRecipeType recipeType) {
        this.recipeType = recipeType;
    }

    public RecipeTypeEvent postCustomEvent() {
        // TODO blueprint in the future
//        machine.getDefinition().machineEvents().postGraphEvent(this);
        // post to the KubeJS events
        postKubeJSEvent();
        return this;
    }

    public RecipeTypeEvent postKubeJSEvent() {
        // post to the KubeJS events
        if (MBD2.isKubeJSLoaded()) {
            try {
                if (LDLib2.isClient()) {
                    if (this instanceof ICancellableEvent cancellable) {
                        if (MBDServerEvents.postRecipeTypeEvent(this).interruptFalse()) {
                            cancellable.setCanceled(true);
                        } else if (MBDClientEvents.postRecipeTypeEvent(this).interruptFalse()) {
                            cancellable.setCanceled(true);
                        }
                    }
                } else {
                    if (MBDServerEvents.postRecipeTypeEvent(this).interruptFalse() && this instanceof ICancellableEvent cancellableEvent) {
                        cancellableEvent.setCanceled(true);
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
        return "RecipeTypeEvent{" +
                "recipeType=" + recipeType +
                ", eventName='" + getClass().getSimpleName() + '\'' +
                (this instanceof ICancellableEvent event ?
                        ", isCanceled=" + event.isCanceled() : "") +
                '}';
    }
}
