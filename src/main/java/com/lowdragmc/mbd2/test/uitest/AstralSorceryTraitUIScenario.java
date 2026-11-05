package com.lowdragmc.mbd2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ElementRef;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.ServerContext;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.integration.astralsorcery.AstralSorceryIntegration;
import com.lowdragmc.mbd2.integration.astralsorcery.lumen.LumenStorageCapabilityTrait;
import com.lowdragmc.mbd2.integration.astralsorcery.research.AstralResearcherTrait;
import com.lowdragmc.mbd2.integration.astralsorcery.starlight.StarlightReceiverTrait;
import com.lowdragmc.mbd2.test.tests.trait.astralsorcery.AstralTraitUIFixtures;
import hellfirepvp.astralsorcery.common.lib.ConstellationsAS;
import hellfirepvp.astralsorcery.common.lib.LumenAS;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Checks that the server's values reach the Astral Sorcery trait bars on a real client.
 * Run with {@code gradlew runClient -PldTest=mbd2_astral_trait_ui}.
 */
@LDLRegisterClient(name = "mbd2_astral_trait_ui", group = "mbd2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY, modID = AstralSorceryIntegration.MOD_ID)
public class AstralSorceryTraitUIScenario implements UIScenario {
    /** Chosen at placement time, see MachineAutoIOPanelScenario. */
    private BlockPos pos = BlockPos.ZERO;

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(150).tags("mbd2", "astralsorcery", "ui", "visual")
                .requiresWorld(true).guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.step("start from no screen", ctx -> ctx.mc().setScreen(null))
                .frames(2)
                .server("place the showcase machine and fill its traits", sc -> {
                    var definition = MBDRegistries.MACHINE_DEFINITIONS.get(AstralTraitUIFixtures.SHOWCASE_ID);
                    if (definition == null) {
                        throw new IllegalStateException("the astral showcase fixture is not registered");
                    }
                    pos = sc.player().blockPosition().above(2);
                    sc.level().setBlockAndUpdate(pos, definition.block().defaultBlockState());
                    var machine = machine(sc);
                    for (var trait : machine.getAdditionalTraits()) {
                        if (trait instanceof LumenStorageCapabilityTrait lumen) {
                            lumen.getStorage().setAmount(LumenAS.AEVITAS.get(), 1200);
                            lumen.getStorage().setAmount(LumenAS.ARMARA.get(), 300);
                        } else if (trait instanceof StarlightReceiverTrait starlight) {
                            starlight.getBuffer().setAmount(ConstellationsAS.ARMARA.getId(), 42.5f);
                        } else if (trait instanceof AstralResearcherTrait researcher) {
                            researcher.bindTo(sc.player());
                        }
                    }
                })
                .waitUntil("the client to have the machine", ctx ->
                        ctx.level() != null && ctx.level().getBlockEntity(pos) instanceof IMachineBlockEntity)
                .server("open its ui the way a player would", sc -> BlockUIMenuType.openUI(sc.player(), pos))
                .awaitScreen(ModularUIContainerScreen.class)
                .awaitModularUI()
                .frames(6)
                .waitUntil("a lumen bar per filtered type, the unfilled one included", ctx -> {
                    var texts = barTexts(ctx);
                    ctx.attach("bars", String.join(" | ", texts));
                    return texts.contains("1200/4000") && texts.contains("300/4000") && texts.contains("0/4000");
                })
                .waitUntil("a starlight bar per filtered constellation", ctx -> {
                    var texts = barTexts(ctx);
                    return texts.contains("42.50/1000") && texts.contains("0/1000");
                })
                .waitUntil("the researcher label to name the player", ctx -> {
                    var text = labelText(ctx, "researcher-label");
                    ctx.attach("researcher", text);
                    return ctx.mc().player != null && text.contains(ctx.mc().player.getGameProfile().getName());
                })
                .screenshot("astral_trait_ui")
                .step("close the ui", ctx -> ctx.mc().setScreen(null))
                .teardownServer("remove the machine", sc -> sc.level().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()));
    }

    private static List<String> barTexts(TestContext ctx) {
        return ctx.query().withClass("astral-bar-label").list().stream().map(ElementRef::text).toList();
    }

    private static String labelText(TestContext ctx, String styleClass) {
        return ctx.query().withClass(styleClass).optional().map(ElementRef::text).orElse("");
    }

    private MBDMachine machine(ServerContext sc) {
        if (sc.level().getBlockEntity(pos) instanceof IMachineBlockEntity be && be.getMetaMachine() instanceof MBDMachine machine) {
            return machine;
        }
        throw new AssertionError("no MBD machine on the server at " + pos);
    }
}
