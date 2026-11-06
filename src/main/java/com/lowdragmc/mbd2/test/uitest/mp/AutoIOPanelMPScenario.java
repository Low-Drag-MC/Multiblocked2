package com.lowdragmc.mbd2.test.uitest.mp;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.mp.MPScenario;
import com.lowdragmc.lowdraglib2.uitest.mp.MPScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.mp.MPScenarioOptions;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.pattern.util.RelativeDirection;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.IAutoIOTrait;
import com.lowdragmc.mbd2.test.tests.blueprint.AutoIOPanelFixtures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The auto IO panel between a dedicated server and a client: each face is painted from what the server published, and
 * a click on either tab repaints its face once the server has applied it.
 */
@LDLRegister(name = "mbd2_mp_auto_io_panel", group = "mbd2", registry = MPScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class AutoIOPanelMPScenario implements MPScenario {
    private static final BlockPos POS = new BlockPos(2, -60, 8);
    private static final int ITEM_TAB = 0;
    private static final int FLUID_TAB = 1;
    private String upBeforeClick = "";

    @Override
    public void configure(MPScenarioOptions options) {
        options.clients("A").tags("mbd2", "mp", "blueprint");
    }

    @Override
    public void define(MPScenarioBuilder s) {
        s.server("place the two-tab machine", sc -> {
            var level = sc.server().overworld();
            level.setBlockAndUpdate(POS, MBDRegistries.MACHINE_DEFINITIONS.get(AutoIOPanelFixtures.TWO_TABS_ID)
                    .block().defaultBlockState());
            var player = sc.player("A");
            player.setGameMode(GameType.SURVIVAL);
            player.teleportTo(level, POS.getX() + 0.5, POS.getY(), POS.getZ() + 2.5, 180f, 0f);
        })
        .client("A", "open the machine and both tabs", b -> b
                .useBlock(POS)
                .awaitScreen(ModularUIContainerScreen.class)
                .awaitModularUI()
                .frames(4)
                .step("open the item tab", ctx -> click(ctx, "handle", ITEM_TAB))
                .frames(4)
                .step("open the fluid tab", ctx -> click(ctx, "handle", FLUID_TAB))
                .frames(4))
        .sync("the published faces reach the client", "A",
                sc -> faces(data(sc.blockEntity(POS, BlockEntity.class))),
                ctx -> faces(data(ctx.clientBlockEntity(POS, BlockEntity.class))))
        .client("A", "the faces are painted from it", b -> b
                .waitUntil("up, down and left to differ", ctx -> {
                    var up = overlay(ctx, "face_UP", ITEM_TAB);
                    var down = overlay(ctx, "face_DOWN", ITEM_TAB);
                    var left = overlay(ctx, "face_LEFT", ITEM_TAB);
                    return !up.equals(down) && !down.equals(left) && !up.equals(left);
                })
                .screenshot("both tabs as published")
                .step("click the item tab's up face", ctx -> {
                    upBeforeClick = overlay(ctx, "face_UP", ITEM_TAB);
                    click(ctx, "face_UP", ITEM_TAB);
                })
                .waitUntil("it to repaint", ctx -> !overlay(ctx, "face_UP", ITEM_TAB).equals(upBeforeClick))
                .check("as out, like the down face", ctx -> {
                    ctx.attach("up", overlay(ctx, "face_UP", ITEM_TAB));
                    ctx.attach("down", overlay(ctx, "face_DOWN", ITEM_TAB));
                    return overlay(ctx, "face_UP", ITEM_TAB).equals(overlay(ctx, "face_DOWN", ITEM_TAB));
                }))
        .server("the item click reached the machine", sc -> {
            var be = sc.blockEntity(POS, BlockEntity.class);
            var up = data(be).getString(key("up", AutoIOPanelFixtures.ITEM_TRAIT));
            var actual = actual(be, AutoIOPanelFixtures.ITEM_TRAIT, RelativeDirection.UP);
            sc.check("item up is set to OUT", actual == IO.OUT, IO.OUT, actual);
            sc.check("item up is published as OUT", "OUT".equals(up), "OUT", up);
        })
        .client("A", "click the fluid tab's left face", b -> b
                .step("click it", ctx -> click(ctx, "face_LEFT", FLUID_TAB))
                .waitUntil("it to repaint", ctx ->
                        !overlay(ctx, "face_LEFT", FLUID_TAB).equals(overlay(ctx, "face_RIGHT", FLUID_TAB)))
                .screenshot("after both clicks"))
        .server("the fluid click reached the machine", sc -> {
            var be = sc.blockEntity(POS, BlockEntity.class);
            var left = data(be).getString(key("left", AutoIOPanelFixtures.FLUID_TRAIT));
            var actual = actual(be, AutoIOPanelFixtures.FLUID_TRAIT, RelativeDirection.LEFT);
            sc.check("fluid left is set to IN", actual == IO.IN, IO.IN, actual);
            sc.check("fluid left is published as IN", "IN".equals(left), "IN", left);
        })
        .teardownAllClients("close the screen", b -> b.closeScreen())
        .teardownServer("remove the machine", sc ->
                sc.server().overworld().setBlockAndUpdate(POS, Blocks.AIR.defaultBlockState()));
    }

    private static String key(String face, String trait) {
        return "mbd2_autoio_" + face + "_" + trait;
    }

    private static CompoundTag data(@Nullable BlockEntity be) {
        return be instanceof IMachineBlockEntity holder && holder.getMetaMachine() instanceof MBDMachine machine
                ? machine.getCustomData() : new CompoundTag();
    }

    @Nullable
    private static IO actual(@Nullable BlockEntity be, String trait, RelativeDirection face) {
        if (!(be instanceof IMachineBlockEntity holder && holder.getMetaMachine() instanceof MBDMachine machine)) return null;
        if (!(machine.getTraitByName(trait) instanceof IAutoIOTrait autoIO)) return null;
        var front = machine.getFrontFacing().orElse(Direction.NORTH);
        return autoIO.getRuntimeAutoIO().getIO(front, face.getActualFacing(front));
    }

    private static String faces(CompoundTag data) {
        var faces = new StringBuilder();
        for (var trait : new String[]{AutoIOPanelFixtures.ITEM_TRAIT, AutoIOPanelFixtures.FLUID_TRAIT}) {
            for (var face : new String[]{"up", "down", "left", "right", "front", "back"}) {
                faces.append(face).append('_').append(trait).append('=').append(data.getString(key(face, trait))).append(' ');
            }
        }
        return faces.toString();
    }

    private static UIElement element(TestContext ctx, String id, int tab) {
        return ctx.query().withId(id).nth(tab).one().as(UIElement.class);
    }

    private static void click(TestContext ctx, String id, int tab) {
        var bounds = ctx.query().withId(id).nth(tab).one().bounds();
        ctx.input().mouseDown(bounds.centerX(), bounds.centerY(), 0);
        ctx.input().mouseUp(bounds.centerX(), bounds.centerY(), 0);
    }

    private static String overlay(TestContext ctx, String id, int tab) {
        for (var style : element(ctx, id, tab).getStyles()) {
            for (var property : style.getPropertiesList()) {
                if (property.name.equals("overlay") && style.getValueSave(property) instanceof IGuiTexture texture) {
                    return texture instanceof ColorRectTexture rect ? Integer.toHexString(rect.color)
                            : texture.getClass().getSimpleName();
                }
            }
        }
        return "none";
    }
}
