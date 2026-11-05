package com.lowdragmc.mbd2.test.tests.ui;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.machine.definition.config.MachineState;
import com.lowdragmc.mbd2.test.framework.TestFixtureProvider;
import com.lowdragmc.mbd2.test.framework.TestMachineBuilder;
import net.minecraft.resources.ResourceLocation;

/**
 * Two machines with the same UI, one hooked by a KubeJS {@code onUI} script, so the script's effect can be
 * captured side by side. The element ids are the ones the wiki snippets select.
 *
 * @see com.lowdragmc.mbd2.test.uitest.KubeJSMachineUIScenario
 */
public class KubeJSMachineUIFixtures implements TestFixtureProvider {

    public static final ResourceLocation SCRIPTED_ID = MBD2.id("kjs_ui_scripted");
    public static final ResourceLocation PLAIN_ID = MBD2.id("kjs_ui_plain");

    public static final String STATUS_ID = "status_label";
    public static final String BUTTON_ID = "flush_button";
    public static final String IDLE_STATE = "idle";
    public static final String STATUS_TEXT = "Crusher";

    @Override
    public void registerMachines(MBDRegistryEvent.Machine event) {
        machine(PLAIN_ID).register(event);
        machine(SCRIPTED_ID).register(event);
    }

    private static TestMachineBuilder machine(ResourceLocation id) {
        return TestMachineBuilder.simple(id)
                .withRootState(MachineState.baseBuilder().child(IDLE_STATE).build())
                .withUI(panelUI())
                .withItemSlots(1, IO.BOTH);
    }

    /** The button is inert on purpose: binding it is the script's job. */
    private static UITemplate panelUI() {
        var root = new UIElement();
        root.setId("machine_panel");
        root.lss("width", "150", StyleOrigin.INLINE);
        root.lss("height", "64", StyleOrigin.INLINE);
        root.lss("padding", "8", StyleOrigin.INLINE);
        root.lss("flex-direction", "column", StyleOrigin.INLINE);
        root.lss("gap", "8", StyleOrigin.INLINE);
        root.style(style -> style.backgroundTexture(Sprites.BORDER));

        var status = new Label();
        status.setId(STATUS_ID);
        status.setText(STATUS_TEXT, false);

        var flush = new Button();
        flush.setId(BUTTON_ID);
        flush.setText("Flush Output", false);
        flush.lss("width", "134", StyleOrigin.INLINE);

        root.addChildren(status, flush);
        return UITemplate.of(root);
    }
}
