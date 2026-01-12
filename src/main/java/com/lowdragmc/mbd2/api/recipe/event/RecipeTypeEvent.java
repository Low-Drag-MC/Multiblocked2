package com.lowdragmc.mbd2.api.recipe.event;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.editor.ILDLRegister;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.common.graphprocessor.GraphParameterGet;
import com.lowdragmc.mbd2.integration.kubejs.events.MBDClientEvents;
import com.lowdragmc.mbd2.integration.kubejs.events.MBDServerEvents;
import lombok.Getter;
import net.minecraftforge.eventbus.api.Event;

@Getter
public class RecipeTypeEvent extends Event implements ILDLRegister {
    @GraphParameterGet
    public final MBDRecipeType recipeType;

    public RecipeTypeEvent(MBDRecipeType recipeType) {
        this.recipeType = recipeType;
    }

    public RecipeTypeEvent postCustomEvent() {
        // TODO post to the graph events
//        machine.getDefinition().machineEvents().postGraphEvent(this);
        // post to the KubeJS events
        postKubeJSEvent();
        return this;
    }

    public RecipeTypeEvent postKubeJSEvent() {
        // post to the KubeJS events
        if (MBD2.isKubeJSLoaded()) {
            try {
                if (LDLib.isClient()) {
                    if (MBDServerEvents.postRecipeTypeEvent(this).interruptFalse() && isCancelable()) {
                        setCanceled(true);
                    } else if (MBDClientEvents.postRecipeTypeEvent(this).interruptFalse() && isCancelable()) {
                        setCanceled(true);
                    }
                } else {
                    if (MBDServerEvents.postRecipeTypeEvent(this).interruptFalse() && isCancelable()) {
                        setCanceled(true);
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
                ", isCanceled=" + isCanceled() +
                '}';
    }
}
