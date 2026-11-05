package com.lowdragmc.mbd2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.test.tests.ui.KubeJSMachineUIFixtures;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * A KubeJS {@code onUI} script's effect on a machine UI, captured next to the same UI without it, for the
 * wiki's KubeJS UI pages. Needs this in {@code runs/client/kubejs/server_scripts/}:
 *
 * <pre>{@code
 * MBDMachineEvents.onUI('mbd2:kjs_ui_scripted', wrapper => {
 *   const { machine, ui } = wrapper.event
 *
 *   const status = ui.selectId('status_label').findFirst().orElse(null)
 *   if (status !== null) {
 *     // setText(String) is @HideFromJS; the two-argument form is the one scripts can reach.
 *     status.setText(`Crusher — ${machine.machineStateName}`, false)
 *   }
 *
 *   const flush = ui.selectId('flush_button').findFirst().orElse(null)
 *   if (flush === null) return
 *   flush.setText('Flush Output (scripted)', false)
 *   flush.addServerEventListener(UIEvents.MOUSE_DOWN, click => {
 *     if (click.button !== 0) return
 *     const slots = machine.getTraitByName('item_slot')
 *     if (slots === null) return
 *     for (let slot = 0; slot < slots.storage.slots; slot++) slots.storage.extractItem(slot, 64, false)
 *   })
 * })
 * }</pre>
 *
 * A missing script would silently capture the unmodified UI, so the caption is checked before capturing.
 */
@LDLRegisterClient(name = "mbd2_kjs_machine_ui", group = "mbd2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class KubeJSMachineUIScenario implements UIScenario {

    private BlockPos plainPos = BlockPos.ZERO;
    private BlockPos scriptedPos = BlockPos.ZERO;

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(150).tags("mbd2", "kubejs", "ui", "visual")
                .requiresWorld(true).guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.step("start from no screen", ctx -> ctx.mc().setScreen(null))
                .frames(2)
                .server("place both machines", sc -> {
                    plainPos = sc.player().blockPosition().above(2);
                    scriptedPos = plainPos.above(2);
                    place(sc, KubeJSMachineUIFixtures.PLAIN_ID, plainPos);
                    place(sc, KubeJSMachineUIFixtures.SCRIPTED_ID, scriptedPos);
                    // Both, so the only difference between the two captures is still the script.
                    idle(sc, plainPos);
                    idle(sc, scriptedPos);
                })
                // The client builds its own half of a machine UI from its own copy of the block
                // entity, which is a tick and a packet behind the placement above.
                .waitUntil("the client to have both machines", ctx -> ctx.level() != null
                        && ctx.level().getBlockEntity(plainPos) instanceof IMachineBlockEntity
                        && ctx.level().getBlockEntity(scriptedPos) instanceof IMachineBlockEntity)

                .server("open the unscripted machine", sc -> BlockUIMenuType.openUI(sc.player(), plainPos))
                .awaitScreen(ModularUIContainerScreen.class)
                .awaitModularUI()
                .frames(4)
                .check("the unscripted caption is the one the fixture ships", ctx ->
                        KubeJSMachineUIFixtures.STATUS_TEXT.equals(caption(ctx)))
                .screenshotElement("kjs-ui-plain", "#machine_panel")

                .step("close it", ctx -> ctx.mc().setScreen(null))
                .frames(2)
                .server("open the scripted machine", sc -> BlockUIMenuType.openUI(sc.player(), scriptedPos))
                .awaitScreen(ModularUIContainerScreen.class)
                .awaitModularUI()
                .frames(4)
                // Fails the run rather than photographing an unmodified UI: see the class doc.
                .check("onUI rewrote the caption, so the script really ran", ctx -> {
                    var text = caption(ctx);
                    ctx.attach("caption", text);
                    return !KubeJSMachineUIFixtures.STATUS_TEXT.equals(text) && text.startsWith("Crusher");
                })
                .check("onUI relabelled the button too", ctx -> {
                    var refs = ctx.query().withId(KubeJSMachineUIFixtures.BUTTON_ID).list();
                    if (refs.isEmpty()) return false;
                    // A Button is a UIElement that owns a TextElement, not one itself.
                    var text = refs.getFirst().as(Button.class).text.getText().getString();
                    ctx.attach("button", text);
                    return text.contains("scripted");
                })
                .screenshotElement("kjs-ui-scripted", "#machine_panel")
                .step("close it", ctx -> ctx.mc().setScreen(null));
    }

    private static void place(com.lowdragmc.lowdraglib2.uitest.ServerContext sc,
                              ResourceLocation id, BlockPos pos) {
        var definition = MBDRegistries.MACHINE_DEFINITIONS.get(id);
        if (definition == null) throw new IllegalStateException(id + " is not registered");
        sc.level().setBlockAndUpdate(pos, definition.block().defaultBlockState());
    }

    /** Puts a placed machine in {@code idle}, which is what the script's caption then reports. */
    private static void idle(com.lowdragmc.lowdraglib2.uitest.ServerContext sc, BlockPos pos) {
        if (sc.level().getBlockEntity(pos) instanceof IMachineBlockEntity holder
                && holder.getMetaMachine() instanceof MBDMachine machine) {
            machine.setMachineState(KubeJSMachineUIFixtures.IDLE_STATE);
        }
    }

    private static String caption(TestContext ctx) {
        return text(ctx, KubeJSMachineUIFixtures.STATUS_ID);
    }

    /** The text an element is actually rendering, which is the only place the script's work shows. */
    private static String text(TestContext ctx, String id) {
        var refs = ctx.query().withId(id).list();
        if (refs.isEmpty()) return "";
        return refs.getFirst().as(TextElement.class).getText().getString();
    }
}
